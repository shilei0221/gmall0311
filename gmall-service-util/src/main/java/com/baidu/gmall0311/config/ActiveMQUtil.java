package com.baidu.gmall0311.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;


import javax.jms.Connection;
import javax.jms.JMSException;


public class ActiveMQUtil {

    //创建连接池工厂配置
   PooledConnectionFactory pooledConnectionFactory = null;

   public  void init(String brokerUrl){

       //创建连接工厂
       ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);

       //连接工厂放入配置对象中
       pooledConnectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);

       //设置超时时间
       pooledConnectionFactory.setExpiryTimeout(2000);

       // 设置出现异常的时候，继续重试连接
       pooledConnectionFactory.setReconnectOnException(true);

       // 设置最大连接数
       pooledConnectionFactory.setMaxConnections(5);
   }
    // 获取连接
    public Connection getConnection(){
        Connection connection = null;
        try {
            connection = pooledConnectionFactory.createConnection();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return  connection;
    }
}
