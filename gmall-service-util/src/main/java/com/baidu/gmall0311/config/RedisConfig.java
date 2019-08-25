package com.baidu.gmall0311.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alei
 * @create 2019-08-11 14:38
 *
 * redis 的配置类
 */
@Configuration //相当于 beans.xml
public class RedisConfig {

    //获取 host port database timeOut
    @Value("${spring.redis.host:disable}") //:disable 表示如果在配置文件中没有获取到数据 则host 为默认值 disable
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:0}")
    private int timeOut;

    //调用 RedisUtil 中初始化initjedispool 方法
    @Bean //表示在 xml中有一个bean标签 注入类
    public RedisUtil getRedisUtil() {

        //判断默认值是否等于当前获取的host 等于返回空(就是判断当前host是否为空)
        if ("disable".equals(host)) {
            return null;
        }

        RedisUtil redisUtil = new RedisUtil();

        //初始化赋值
        redisUtil.initJedisPool(host,port,database,timeOut);

        return redisUtil;
    }
}
