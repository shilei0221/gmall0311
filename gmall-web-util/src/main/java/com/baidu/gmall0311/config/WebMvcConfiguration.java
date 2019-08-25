package com.baidu.gmall0311.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Alei
 * @create 2019-08-17 20:55
 *
 * 配置拦截器的配置类
 */
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private AuthInterceptor authInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {

        //将定义好的拦截器配置到拦截器中心
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");

        //interceptors
        super.addInterceptors(registry);

    }
}
