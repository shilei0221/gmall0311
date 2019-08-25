package com.baidu.gmall0311.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.OrderInfo;
import com.baidu.gmall0311.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-23 22:38
 */
@EnableScheduling
@Component
public class OrderTask {

    @Reference
    private OrderService orderService;


    //开启定时任务  意思是每分钟的第五秒开始执行下面方法
//    @Scheduled(cron = "5 * * * * ?")
//    public void test1() {
//        System.out.println(Thread.currentThread().getName() + "-------------");
//    }
//
//    @Scheduled(cron = "0/5 * * * * ?")
//    public void test2() {
//        System.out.println(Thread.currentThread().getName() + "888888888888888888");
//    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder() {
        //获取过期订单 orderStatus = UNPAID and exipreTime <new Date()>
        List<OrderInfo> orderInfoList = orderService.getExpiredOrderList();

        //循环遍历关闭过期订单
        for (OrderInfo orderInfo : orderInfoList) {
            //orderInfo 表示一个过期订单
            orderService.execExpiredOrder(orderInfo);
        }
    }
}
