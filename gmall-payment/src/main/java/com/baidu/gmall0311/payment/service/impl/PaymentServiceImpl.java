package com.baidu.gmall0311.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baidu.gmall0311.bean.PaymentInfo;
import com.baidu.gmall0311.bean.enums.PaymentStatus;
import com.baidu.gmall0311.config.ActiveMQUtil;
import com.baidu.gmall0311.payment.mapper.PaymentInfoMapper;
import com.baidu.gmall0311.service.PaymentService;
import com.baidu.gmall0311.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-21 21:18
 */
@Service
public class PaymentServiceImpl  implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    /**
     * 保存支付信息
     * @param paymentInfo
     */
    @Override
    public void savyPaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }


    /**
     * 根据对象中的属性查询数据
     * @param paymentInfoQuery
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {

        //select * from paymentInfo where out_trade_no = ?

//        通过第三方交易编号 查询交易记录对象
        return   paymentInfoMapper.selectOne(paymentInfoQuery);
    }

    /**
     * 更新数据
     * @param paymentInfoUPD
     * @param out_trade_no
     */
    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfoUPD, String out_trade_no) {

        Example example = new Example(PaymentInfo.class);

        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);

        paymentInfoMapper.updateByExampleSelective(paymentInfoUPD,example);
    }

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(String orderId) {
//        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        //告诉我退多少钱  和 交易编号
        //我们使用map 封装数据
        Map<String,Object> map = new HashMap<>();

        PaymentInfo paymentInfo = new PaymentInfo(); //创建对象封装数据

        paymentInfo.setOrderId(orderId); //将订单id放入对象中

        PaymentInfo paymentInfoQuery = getPaymentInfo(paymentInfo); //调用方法将对象传入 获取对象数据

        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());

        map.put("refund_amount",paymentInfoQuery.getTotalAmount());

        //更复杂的业务 对号入座 就可以了 在支付宝接口文档 找到对应的参数值 放入就可以

        request.setBizContent(JSON.toJSONString(map));
//        request.setBizContent("{" +
//                "\"out_trade_no\":\"20150320010101001\"," +
//                "\"trade_no\":\"2014112611001004680073956707\"," +
//                "\"refund_amount\":200.12," +
//                "\"refund_currency\":\"USD\"," +
//                "\"refund_reason\":\"正常退款\"," +
//                "\"out_request_no\":\"HZ01RF001\"," +
//                "\"operator_id\":\"OP001\"," +
//                "\"store_id\":\"NJ_S_001\"," +
//                "\"terminal_id\":\"NJ_T_001\"," +
//                "      \"goods_detail\":[{" +
//                "        \"goods_id\":\"apple-01\"," +
//                "\"alipay_goods_id\":\"20010001\"," +
//                "\"goods_name\":\"ipad\"," +
//                "\"quantity\":1," +
//                "\"price\":2000," +
//                "\"goods_category\":\"34543238\"," +
//                "\"categories_tree\":\"124868003|126232002|126252004\"," +
//                "\"body\":\"特价手机\"," +
//                "\"show_url\":\"http://www.alipay.com/xxx.jpg\"" +
//                "        }]," +
//                "      \"refund_royalty_parameters\":[{" +
//                "        \"royalty_type\":\"transfer\"," +
//                "\"trans_out\":\"2088101126765726\"," +
//                "\"trans_out_type\":\"userId\"," +
//                "\"trans_in_type\":\"userId\"," +
//                "\"trans_in\":\"2088101126708402\"," +
//                "\"amount\":0.1," +
//                "\"amount_percentage\":100," +
//                "\"desc\":\"分账给2088101126708402\"" +
//                "        }]," +
//                "\"org_pid\":\"2088101117952222\"" +
//                "  }");

        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }


    /**
     * 创建微信二维码支付
     * @param
     * @param
     * @return
     */
    @Override
    public Map createNative(String orderId, String total_fee) {
        /*
            1.  传递参数
            2.  map 转成 xml 发送请求
            3.  获取结果
         */
        HashMap<String, String> param = new HashMap<>();

        param.put("appid",appid);
        param.put("mch_id",partner);
        param.put("nonce_str",WXPayUtil.generateNonceStr());
        param.put("body","atguigu");
        param.put("out_trade_no",orderId);
        // 注意单位是分
        param.put("total_fee",total_fee);
        param.put("spbill_create_ip", "127.0.0.1");//IP
        param.put("notify_url", "http://order.gmall.com/trade");//回调地址(随便写)
        param.put("trade_type", "NATIVE");//交易类型

        // 使用工具类
        try {
            String xmlParam  = WXPayUtil.generateSignedXml(param, partnerkey);
            // 调用httpClient
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            // 设置https 发送
            httpClient.setHttps(true);
            // 将xml 发送过去
            httpClient.setXmlParam(xmlParam);
            // 设置post 请求
            httpClient.post();

            // 获取结果
            String result  = httpClient.getContent();
            // 将内容转换为map
            Map<String, String> resultMap  = WXPayUtil.xmlToMap(result);
            // 设置数据
            // 新创建一个map 用来存储数据
            Map<String, String> map=new HashMap<>();
            map.put("code_url",resultMap.get("code_url"));
            map.put("total_fee",total_fee);
            map.put("out_trade_no",orderId);

            return map;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     *  支付成功之后 将订单编号 支付结果发送给订单
     * @param paymentInfo
     * @param result
     */
    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {

        /*
            1.创建一个工厂连接
            2、创建连接
            3、打开连接
            4、获取session
            5、创建队列
            6、创建消息提供者
            7、发送消息
            8、关闭
            如果开启事务 则 最后记得提交事务
         */

        //1、获取连接
        Connection connection = activeMQUtil.getConnection();

        //2、打开连接
        try {
            connection.start();

            //3、创建session  开启事务  自动接收
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);

            //4.创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");

            //5、创建提供者
            MessageProducer producer = session.createProducer(payment_result_queue);

            //6、创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();

            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());

            activeMQMapMessage.setString("result",result);

            //7、发送消息
            producer.send(activeMQMapMessage);

            //8、提交
            session.commit();

            //9、关闭连接
            producer.close();

            session.close();

            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 根据 out_trade_no 第三方编号 来查询支付结果
     * @param paymentInfoQuery
     * @return
     */
    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {

//        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");

        if (paymentInfoQuery == null) {
            return false;
        }

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        Map<String,Object> map = new HashMap<>();

        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());

        request.setBizContent(JSON.toJSONString(map));

//        request.setBizContent("{" +
//                "\"out_trade_no\":\"20150320010101001\"," +
//                "\"trade_no\":\"2014112611001004680 073956707\"," +
//                "\"org_pid\":\"2088101117952222\"," +
//                "      \"query_options\":[" +
//                "        \"TRADE_SETTE_INFO\"" +
//                "      ]" +
//                "  }");


        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(response.isSuccess()){

            System.out.println("调用成功");

            /*
                	交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、
                	TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、
                	TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
             */
            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus()))
            {

                //调用更新交易状态
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCallbackTime(new Date());

                //更新
                updatePaymentInfo(paymentInfoUPD,paymentInfoQuery.getOutTradeNo());

                //通知订单支付成功 ActiveMQ 发送消息队列
                sendPaymentResult(paymentInfoQuery,"success");

                return  true;
            }

        } else {
            System.out.println("调用失败");
            return false;
        }
        return false;
    }

    /**
     * 延迟队列反复调用
     * @param outTradeNo  第三方交易编号
     * @param delaySec  延迟时间
     * @param checkCount    检查次数
     */
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {

        //获取连接
        Connection connection = activeMQUtil.getConnection();

        try {

            //打开链接
            connection.start();

            //获取session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //创建队列
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");

            //创建提供者
            MessageProducer producer = session.createProducer(payment_result_check_queue);

            //创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();

            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            activeMQMapMessage.setInt("delaySec",delaySec);
            activeMQMapMessage.setInt("checkCount",checkCount);

            //设置一个延迟队列
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec * 1000);

            //发送消息
            producer.send(activeMQMapMessage);

            //提交
            session.commit();

            //关闭
            producer.close();
            session.close();
            connection.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  根据订单id 关闭交易记录
     * @param orderId
     */
    @Override
    public void closePaymentInfo(String orderId) {

        //更新条件
        Example example = new Example(PaymentInfo.class);

        example.createCriteria().andEqualTo("orderId",orderId);

        //更新内容
        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);


        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }
}
