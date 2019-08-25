package com.baidu.gmall0311.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.PaymentInfo;
import com.baidu.gmall0311.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author Alei
 * @create 2019-08-22 22:56
 *
 * 在payment项目中添加接收延迟队列的消费端
 */
@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerSkuDeduct(MapMessage mapMessage) throws JMSException {


        //获取消息队列中的数据
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        //判断是否支付成功 调用 checkPayment
        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setOutTradeNo(outTradeNo);

        //需要outTradeNo 在查询一下 才能得到 orderId
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);

        boolean result = paymentService.checkPayment(paymentInfoQuery);

        //result 为 false 表示没有支付成功
        if (!result && checkCount > 0) {
            //继续调用方法发送消息
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }


    }
}
