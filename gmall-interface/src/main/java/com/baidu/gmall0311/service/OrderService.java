package com.baidu.gmall0311.service;

import com.baidu.gmall0311.bean.OrderInfo;
import com.baidu.gmall0311.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-20 21:00
 */
public interface OrderService {

    /**
     *  保存订单信息
     * @param orderInfo
     * @return 订单id
     */
    String saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较redis中与页面中的流水号是否一致 返回成功失败
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    boolean checkTradeCode(String userId,String tradeCodeNo);

    /**
     * 比较成功之后 删除redis中的流水号
     * @param userId
     */
    void delTradeCode(String userId);

    /**
     * 校验库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId,Integer skuNum);

    /**
     * 根据orderId查询orderInfo
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 根据订单id修好状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(String orderId, ProcessStatus processStatus);

    /**
     *  根据 订单 发送消息给仓库
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     *获取过期订单
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     * 将 orderInfo 转换为 map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     *  根据参数进行拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);
}
