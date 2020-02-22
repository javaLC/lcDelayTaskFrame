package com.lc.delay.frame.delayserver;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lc.delay.frame.common.rocketmq.RocketMqConfig;
import com.lc.delay.frame.delayserver.dispatch.RocketMqInvokeDispatcher;
import com.lc.delay.frame.delayserver.task.TaskReceiver;

/**
 * @author liuchong
 * @version MyServerApplication.java, v 0.1 2020年02月21日 14:26
 */
@SpringBootApplication
@RestController
@Configuration
public class MyServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyServerApplication.class, args);
    }

    @RequestMapping("/")
    public String host() {
        return "delayServer run ok";
    }

    @Bean("taskReceiverMqConfig")
    @ConfigurationProperties("delay.task.rocket.consume")
    public RocketMqConfig taskReceiverMqConfig() {
        return new RocketMqConfig();
    }

    /**
     * 作为客户端提交任务mq都消费者
     * @return
     */
    @Bean
    public TaskReceiver taskReceiver(@Qualifier("taskReceiverMqConfig") RocketMqConfig config) {
        return new TaskReceiver(config);
    }

    @Bean("taskInvokeMqConfig")
    @ConfigurationProperties("delay.task.rocket.producer")
    public RocketMqConfig taskInvokeMqConfig() {
        return new RocketMqConfig();
    }

    /**
     * 通过mq去通知调用任务，简单来说就是任务执行mq都生产者
     * @param config
     * @return
     */
    @Bean
    public RocketMqInvokeDispatcher dispatcher(@Qualifier("taskInvokeMqConfig") RocketMqConfig config) {
        return new RocketMqInvokeDispatcher(config);
    }

   /* @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(factory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }*/

}
