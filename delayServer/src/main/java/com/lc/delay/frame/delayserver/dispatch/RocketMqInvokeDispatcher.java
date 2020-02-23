package com.lc.delay.frame.delayserver.dispatch;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.lc.delay.frame.common.util.DateUtils;
import com.lc.delay.frame.delayserver.util.TaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.lc.delay.frame.common.msg.InvokeMsg;
import com.lc.delay.frame.common.rocketmq.AbsRocketProducer;
import com.lc.delay.frame.common.rocketmq.RocketMqConfig;
import com.lc.delay.frame.delayserver.redis.RedisUtil;
import com.lc.delay.frame.delayserver.task.TaskModel;

/**
 * rockmq形式的mq消息进行调度(将调度和通信放一起，不拆了)
 *
 * @author liuchong
 * @version RocketMqInvokeDispatcher.java, v 0.1 2020年02月21日 21:13
 */
public class RocketMqInvokeDispatcher extends AbsRocketProducer
                                      implements InvokeDispatcher<InvokeMsg> {

    private Logger log = LoggerFactory.getLogger(RocketMqInvokeDispatcher.class);


    @Autowired
    RedisUtil redisUtil;

    public RocketMqInvokeDispatcher(RocketMqConfig config) {
        super(config);
    }

    @Override
    public void dispatch(InvokeMsg param) {
        if (param.getDelay() > 0) {
            invokeExecutor.schedule( ()-> notifyTask(param), param.getDelay(),
                TimeUnit.MILLISECONDS);
        } else {
            notifyTask(param);
        }
    }

    /**
     * 唤起执行任务
     * @param msg
     */
    private void notifyTask(InvokeMsg msg) {
        log.info("开始执行任务请求，通知时间为：" + DateUtils.formatDate(new Date(), DateUtils.yyyy_MM_dd_HH_mm_ss_SSS) + ", 任务信息:"
                 + TaskUtil.toTaskModel(msg).toString());
        sendInvokeMsg(msg);
    }



    // 任务捞取延迟就暂时放这里吧
    private volatile boolean continueFetcher;
    /** 任务获取 */
    private ExecutorService invokeFetcher  = Executors.newSingleThreadExecutor(r -> new Thread(r, "RedisWayInvokeJobFetcher-Thread"));
    /** 任务执行 */
    private ScheduledExecutorService invokeExecutor = Executors.newScheduledThreadPool(16, r -> new Thread(r, "RedisWayInvokeJobExecutor-Thread"));

    @Override
    public void afterPropertiesSet() throws Exception {
        // 父类中初始化rocketmq生产者
        super.afterPropertiesSet();
        continueFetcher = true;

        invokeFetcher.submit(() -> {

            // 可配置化
            int scope = 1;
            while (continueFetcher) {

                // 获取所有任务队列名称。这里单线程进行任务数据获取。如果有很多不同任务，则会影响整体时间执行精度
                redisUtil.fetchAllTaskName().forEach(taskName -> {

                    List<TaskModel> tasks = null;
                    try {
                        tasks = redisUtil.consumeTask(taskName, scope);
                    } catch (Exception e) {
                        log.error("获取任务数据异常", e);
                    }

                    for(TaskModel t : tasks) {

                        dispatch(TaskUtil.toInvokeMsg(t));
                    }
                });
                try {
                    // 上面每次拉去最近scope内的。
                    Thread.sleep(scope * 1000);
                } catch (Exception e) {
                }
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        // 父类中处理生产者的销毁
        super.destroy();
        continueFetcher = false;
        invokeFetcher.shutdownNow();
        invokeExecutor.shutdown();
    }
}
