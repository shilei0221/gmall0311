package com.baidu.gmall0311.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Alei
 * @create 2019-08-11 14:22
 *
 * redis 工具类
 */

public class RedisUtil {

    //创建一个连接池对象
    private JedisPool jedisPool;

    //给连接池进行初始化操作
    public void initJedisPool(String host,int port,int database,int timeOut) {

        //配置连接池的参数
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

        //参数根据自己的机器设置 业务需求来设置
        jedisPoolConfig.setMaxTotal(200);

        //设置最小连接数  留一点多线程并发可以暂时顶一会儿
        jedisPoolConfig.setMinIdle(10);

        //设置最大等待时间
        jedisPoolConfig.setMaxWaitMillis(10 * 1000);

        //如果到最大数 设置等待
        jedisPoolConfig.setBlockWhenExhausted(true);

        //获取到 jedis 对象之后 要做一个自检功能
        jedisPoolConfig.setTestOnBorrow(true);

        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);
    }

    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

}
