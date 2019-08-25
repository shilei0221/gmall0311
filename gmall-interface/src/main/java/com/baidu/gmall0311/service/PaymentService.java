package com.baidu.gmall0311.service;

import com.baidu.gmall0311.bean.PaymentInfo;

import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-21 21:17
 */
public interface PaymentService {

    /**
     * 保存支付信息
     * @param paymentInfo
     */
    void  savyPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据对象中的属性查询数据
     * @param paymentInfoQuery
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    /**
     * 更新数据
     * @param paymentInfoUPD
     * @param out_trade_no
     */
    void updatePaymentInfo(PaymentInfo paymentInfoUPD, String out_trade_no);

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 创建微信二维码支付
     * @param
     * @param
     * @return
     */
    Map createNative(String orderId, String total_fee);

    /**
     *  支付成功之后 将订单编号 支付结果发送给订单
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    /**
     * 根据 out_trade_no 第三方编号 来查询支付结果
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 延迟队列反复调用
     * @param outTradeNo  第三方交易编号
     * @param delaySec  延迟时间
     * @param checkCount    检查次数
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     *  根据订单id 关闭交易记录
     * @param orderId
     */
    void closePaymentInfo(String orderId);
}
