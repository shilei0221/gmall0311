package com.baidu.gmall0311.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baidu.gmall0311.bean.OrderInfo;
import com.baidu.gmall0311.bean.PaymentInfo;
import com.baidu.gmall0311.bean.enums.PaymentStatus;
import com.baidu.gmall0311.payment.config.AlipayConfig;
import com.baidu.gmall0311.service.OrderService;
import com.baidu.gmall0311.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-21 0:15
 */
@Controller
public class PayMentController {


    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient; //将支付接口放入容器

    @Reference
    private PaymentService paymentService;

    @RequestMapping("index")
    public String index(HttpServletRequest request) {

        String orderId = request.getParameter("orderId");

        //保存总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        //保存订单Id
        request.setAttribute("orderId", orderId);

        return "index";
    }

    /**
     * 调用支付宝支付接口进行支付
     *
     * @param request
     * @param response
     * @return
     */
    //http://payment.gmall.com/alipay/submit
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request, HttpServletResponse response) {

        //做一个判断 支付日志中的订单支付状态  如果是已支付  则不生成二维码直接重定向到消息提示页面

        //将支付信息保存到数据库
        //对应的数据库表 paymentInfo
        String orderId = request.getParameter("orderId");

        //调用接口获取orderInfo对象
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //创建交易对象
        PaymentInfo paymentInfo = new PaymentInfo();

        //给交易对象赋值
        paymentInfo.setCreateTime(new Date());

        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        //保存交易记录
        paymentService.savyPaymentInfo(paymentInfo);

        //支付 生成二维码
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        //将上述创建对象的方式放入spring容器中
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        //同步回调路径
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);

        //异步回调路径
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        //  封装参数
        //使用 map 记录数据
        Map<String, Object> map = new HashMap<>();

        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", paymentInfo.getTotalAmount());
        map.put("subject", paymentInfo.getSubject());

        alipayRequest.setBizContent(JSON.toJSONString(map));

//        alipayRequest.setBizContent("{" +
//                "    \"out_trade_no\":\"20150320010101001\"," +
//                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
//                "    \"total_amount\":88.88," +
//                "    \"subject\":\"Iphone6 16G\"," +
//                "    \"body\":\"Iphone6 16G\"," +
//                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
//                "    \"extend_params\":{" +
//                "    \"sys_service_provider_id\":\"2088511833207846\"" +
//                "    }" +
//                "  }");//填充业务参数
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        response.setContentType("text/html;charset=UTF-8");
//        response.getWriter().write(form);//直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();

        //在生成二维码的时候 开始主动询问支付宝的支付结果 每隔15秒一次 公共检查3次即可
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);

        return form;
    }

    /**
     * 同步回调 通过客户支付结果
     */
    @RequestMapping("alipay/callback/return")
    public String callback() {
        //重定向到订单 url
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 异步回调 通知商家支付结果
     */
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String, String> paramMap, HttpServletRequest request) {

//        Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中

        //将剩下参数进行 url_decode 编码，然后进行字典排序 组成字符串 得到待签名字符串
        boolean flag = false; //调用 SDK验证签名

        try {

            flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名

        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //在支付宝中有该笔交易
        if (flag) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            String trade_status = paramMap.get("trade_status");

            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {

                //付款成功之后 修改交易状态
                //如果交易状态为 PAID 或者 CLOSED
                //查询当前交易状态 获取第三方交易编号 通过第三方交易编号 查询交易记录对象 获取状态值进行判断
                String out_trade_no = paramMap.get("out_trade_no");

                PaymentInfo paymentInfoQuery = new PaymentInfo();

                paymentInfoQuery.setOutTradeNo(out_trade_no);

                //select * from paymentInfo where out_trade_no = ?
//                通过第三方交易编号 查询交易记录对象
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);

                //判断状态
                PaymentStatus paymentStatus = paymentInfo.getPaymentStatus();

                if (paymentStatus == PaymentStatus.ClOSED || paymentStatus == PaymentStatus.PAID) {

                    //说明交易当中关闭订单等 出现异常
                    return "failure";
                }

                //修改交易状态
                //调用更新方法
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCreateTime(new Date());

                //更新
                paymentService.updatePaymentInfo(paymentInfoUPD, out_trade_no);


                //TODO 通知订单支付成功  ActiveMQ 发送异步消息队列  下面两个都可以
//                paymentService.sendPaymentResult(paymentInfo.getOrderId(),"success");
                paymentService.sendPaymentResult(paymentInfo,"success");

                //如果状态等于这两个值 说明成功了
                return "success";
            }

        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }


        return "failure";
    }

    /**
     * 写一个控制器调用消息队列 发送异步通知
     *
     * payment.gmall.com/sendPaymentResult?orderId=98&result=success
     */
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result) {

        paymentService.sendPaymentResult(paymentInfo,result);

        return "OK";
    }

    /**
     * 退款开发
     */
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId) {
        //直接调用退款接口
        boolean flag = paymentService.refund(orderId);

        return flag+"";
    }

    /**
     * 微信生成二维码支付
     */
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map createNative(String orderId){

        //做一个判断 支付日志中的订单支付状态  如果是已支付  则不生成二维码直接重定向到消息提示页面

        // 调用服务层数据
        // 第一个参数是订单Id ，第二个参数是多少钱，单位是分
        Map map = paymentService.createNative(orderId +"", "1");
        System.out.println(map.get("code_url"));
        // data = map
        return map;

    }

    /**
     * 查询某个订单是否支付成功  参数传递 orderId
     */
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(PaymentInfo paymentInfo) {

        //paymentInfo 对象中必须有 outTradeNo orderId
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);

        boolean flag = paymentService.checkPayment(paymentInfoQuery);

        return "" + flag;
    }
}
