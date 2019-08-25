package com.baidu.gmall0311.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.CartInfo;
import com.baidu.gmall0311.bean.SkuInfo;
import com.baidu.gmall0311.config.LoginRequire;
import com.baidu.gmall0311.service.CartService;
import com.baidu.gmall0311.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-18 0:00
 */
@Controller
public class CartController {

    @Reference
    private CartService cartService; //购物车业务层

    @Autowired
    private CartCookieHandler cartCookieHandler; //未登录要调用的类 存入cookie

    @Reference
    private ManageService manageService;


    /**
     * 添加购物车功能
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {

        //获取参数值
        String skuId = request.getParameter("skuId");

        String skuNum = request.getParameter("skuNum");

        //获取 userId
        String userId = (String) request.getAttribute("userId");

        if (userId != null) {

            //登录  调用接口方法 传入参数
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));

        } else {
            //未登录
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));

            //从 cookie 中获取数据
//            Cookie[] cookies = request.getCookies();
//            for (Cookie cookie : cookies) {
//                if ("user-Key".equals(cookie.getName())) {
//                    String value = cookie.getValue();
//                    userKey = value;
//                } else {
//                    userKey = UUID.randomUUID().toString();
//                }
//            }
//            Cookie cookie = new Cookie("user-Key",userKey);
//
//            response.addCookie(cookie);
//
//            cartService.addToCart(skuId,Integer.parseInt(skuNum),userKey);
        }

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuInfo",skuInfo);

        request.setAttribute("skuNum",skuNum);

        return "success";
    }


    /**
     * 查询购物车列表功能
     * @param request
     * @return
     */
    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response) {

        //获取 userId
        String userId = (String) request.getAttribute("userId");

        //声明一个集合 用来存放 查询出来的购物车集合
        List<CartInfo> cartInfoList = null;

        //判断用户是否登录
        if (userId != null) {

            //合并购物车
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);

            if (cartListCK != null && cartListCK.size() > 0) {
                //调用合并方法
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);

                //清空未登录数据
                cartCookieHandler.deleteCartCookie(request,response);

            } else {

                //登录 获取登录的购物车数据
                cartInfoList = cartService.getCartList(userId);
            }

        } else {
            //未登录 获取未登录购物车中的数据
            cartInfoList = cartCookieHandler.getCartList(request);
        }

        request.setAttribute("cartInfoList",cartInfoList);

        return "cartList";
    }

    /**
     * 实现选中商品时候锁定商品  刷新也不会消失
     * @param request
     * @param response
     */
    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        // 获取到isChecked，skuId，userId
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        String userId = (String) request.getAttribute("userId");

        // 判断状态
        if (userId!=null){
            // 调用service 层
            cartService.checkCart(skuId,isChecked,userId);
        }else {
            // 未登录
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {

        //获取 userId
        String userId = (String)request.getAttribute("userId");

        //获取未登录数据
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);

        if (cartListCK != null && cartListCK.size() > 0) {
            //进行合并
            cartService.mergeToCartList(cartListCK,userId);

            //删除未登录数据
            cartCookieHandler.deleteCartCookie(request,response);
        }

        return "redirect://order.gmall.com/trade";
    }
}
