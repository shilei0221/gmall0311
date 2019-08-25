package com.baidu.gmall0311.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.bean.*;
import com.baidu.gmall0311.config.LoginRequire;
import com.baidu.gmall0311.service.CartService;
import com.baidu.gmall0311.service.ManageService;
import com.baidu.gmall0311.service.OrderService;
import com.baidu.gmall0311.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-02 23:47
 */
@Controller
public class OrderController {

//    @Autowired
    @Reference  //阿里巴巴的注解  启动dubbo 消费者的注解
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    /**
     *
        http://order.gmall.com/trade
        查看订单 点击下订单
     */
    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){

        //调用服务层
        //获取 userId
        String userId = (String) request.getAttribute("userId");

        List<UserAddress> userAddressList = userService.getUserAddressList(userId);

        //显示送货清单 先从购物车中将数据查询出来 放入 orderDetail中

        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);

        //声明集合来存储订单明细
        ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();

        for (CartInfo cartInfo : cartInfoList) {
            //属性赋值
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());

            orderDetailArrayList.add(orderDetail);
        }

        //计算总金额 在 orderInfo 中 先将 orderDetail 集合放入 orderInfo中
        OrderInfo orderInfo = new OrderInfo();

        orderInfo.setOrderDetailList(orderDetailArrayList);

        orderInfo.sumTotalAmount();;

        //存储总金额
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        //存储订单明细
        request.setAttribute("orderDetailArrayList",orderDetailArrayList);

        request.setAttribute("userAddressList",userAddressList);

        //调用生成流水号
        String tradeNo = orderService.getTradeNo(userId);

        request.setAttribute("tradeNo",tradeNo);

        return "trade";
    }

    /**
        http://order.gmall.com/submitOrder

     *
     */
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo) {

        //调用service 进行保存订单
        String userId = (String) request.getAttribute("userId");

        orderInfo.setUserId(userId);

        //计算总金额
        orderInfo.sumTotalAmount();

        //获取流水号进行判断
        String tradeNoPage = request.getParameter("tradeNo");

        //调用比较方法
        boolean result = orderService.checkTradeCode(userId, tradeNoPage);

        if (!result) {
            //错误页面
            request.setAttribute("errMsg","订单出现故障,请您联系管理员进行确认~");
            return "tradeFail";
        }

        //检验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        for (OrderDetail orderDetail : orderDetailList) {
            //遍历订单明细 将订单明细中的商品分别进行校验
            boolean flag = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());

            //表示验证失败
            if (!flag) {
                //错误页面
                request.setAttribute("errMsg",orderDetail.getSkuName() + "库存不足,请您重新下单~");
                return "tradeFail";
            }
//            校验价格是否一致
//            orderDetail.getOrderPrice() == skuInfo.price; 获取skuInfo
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());

            //调用其中的价格进行比较 看返回结果是什么 类型是BigDecimal 所以使用 compareTo进行比较
            int type = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());

            //如果返回零代表相等  -1 代表 后面大 1 代表前面大
            if (type != 0) {
                //错误页面
                request.setAttribute("errMsg",orderDetail.getSkuName() + "价格有变动,请重新下单~");
                return "tradeFail";
            }

            //如果价格有变动的话 在调用购物车中的方法 重新根据用户id 获取一下最新的购物车数据
            cartService.loadCartCache(userId);
        }

        //删除流水号
        orderService.delTradeCode(userId);

        String orderId = orderService.saveOrderInfo(orderInfo);


        //重定向到支付页面
        return "redirect://payment.gmall.com/index?orderId=" + orderId;
    }

    // http://order.gmall.com/orderSplit
    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        // orderId ,wareSkuMap 读代码！
        // http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
        String orderId = request.getParameter("orderId");
        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 获取子订单的集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId,wareSkuMap);

        ArrayList<Map> mapArrayList = new ArrayList<>();
        // 循环子订单集合
        for (OrderInfo orderInfo : subOrderInfoList) {
            // 将orderInfo 转换为map
            Map map = orderService.initWareOrder(orderInfo);
            // 将map 添加到集合中
            mapArrayList.add(map);
        }


        // 返回子订单集合字符串
        return JSON.toJSONString(mapArrayList);
    }
}
