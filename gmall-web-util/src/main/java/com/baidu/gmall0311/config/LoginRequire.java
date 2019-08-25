package com.baidu.gmall0311.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Alei
 * @create 2019-08-17 22:01
 *
 * 自定义注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequire {

    //自定义一个属性 true：表示必须登录，必须的  false：表示不需要登录
    boolean autoRedirect() default true;
}
