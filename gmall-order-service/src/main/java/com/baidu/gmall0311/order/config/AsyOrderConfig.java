package com.baidu.gmall0311.order.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @author Alei
 * @create 2019-08-23 23:51
 *
 * 异步线程池
 */
@EnableAsync
@Configuration
public class AsyOrderConfig implements AsyncConfigurer {

    /**
     * 获取异步执行者
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {

        //配置线程池
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        //对线程池进行设置参数
        threadPoolTaskExecutor.setCorePoolSize(10);

        //最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(100);

        //设置等待队列
        threadPoolTaskExecutor.setQueueCapacity(100);

        //初始化方法
        threadPoolTaskExecutor.initialize();

        return threadPoolTaskExecutor;
    }

    /**
     *
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {

        return null;
    }
}
