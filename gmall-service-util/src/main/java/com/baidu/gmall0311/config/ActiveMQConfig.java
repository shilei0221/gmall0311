package com.baidu.gmall0311.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.Session;

@Configuration
public class ActiveMQConfig {

    //application.properties 配置文件中 tcp://192.168.199.134:61616
    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL ;

    //是否开启监听器
    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;

    // 发送队列  获取activeMq 的实体对象
    @Bean
    public ActiveMQUtil getActiveMQUtil(){
        if ("disabled".equals(brokerURL)){
            return null;
        }
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return  activeMQUtil;
    }

    //配置监听器工厂{给消费消息做的 工厂}
    @Bean(name = "jmsQueueListener")
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {

        if("disabled".equals(listenerEnable)){
            return null;
        }
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);
        // 设置事务
        factory.setSessionTransacted(false);
        // 自动签收
        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        // 设置并发数
        factory.setConcurrency("5");
        // 重连间隔时间
        factory.setRecoveryInterval(5000L);

        return factory;
    }
    // 接收消息
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory ( ){
        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory(brokerURL);
        return activeMQConnectionFactory;
    }
}
