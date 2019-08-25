package com.baidu.gmall0311.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.bean.CartInfo;
import com.baidu.gmall0311.bean.SkuInfo;
import com.baidu.gmall0311.config.CookieUtil;
import com.baidu.gmall0311.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-18 14:55
 *
 * 操作所有 cookie 相关的数据
 */
@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;

    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {

        /*
            添加商品逻辑
                1.先获取购物车中的所有数据
                    1.1 有 数量相加
                    1.2 无 直接添加
                2.直接写入 cookie 中
         */
        //获取 cookie 中的数据
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);

        List<CartInfo> cartInfoList = new ArrayList<>();

        //判断是否有该商品
        boolean ifExist = false;

        //将 cookieValue 转换为能操作的对象  -  集合对象
        if (StringUtils.isNotEmpty(cookieValue)) {
            cartInfoList = JSON.parseArray(cookieValue,CartInfo.class);

            if (cartInfoList != null && cartInfoList.size() > 0) {
                //循环遍历
                for (CartInfo cartInfo : cartInfoList) {
                    if (cartInfo.getSkuId().equals(skuId)) {  //说明有该商品 进行更新
                        //数量更新
                        cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);

                        //实时价格
                        cartInfo.setSkuPrice(cartInfo.getCartPrice());

                        //给一个标识
                        ifExist = true;
                    }
                }
            }
        }

        //cookie 中没有要添加的商品
        if (!ifExist) {
            //直接添加即可  先添加到集合中 然后将集合统一保存到 cookie 中
            CartInfo cartInfo = new CartInfo();

            //远程调用方法获取商品信息
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            //进行属性赋值
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);

            //将商品对象添加到集合中
            cartInfoList.add(cartInfo);
        }
        //保存 cookie 中
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartInfoList),COOKIE_CART_MAXAGE,true);

    }

    /**
     * 获取 未登录时 cookie 中的购物车数据
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {

        //调用cookie工具类 获取cookie中购物车集合字符串
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);

        //判断cookie中是否有值
        if (StringUtils.isNotEmpty(cookieValue)) {
            //将 cookieValue 值转成集合数据
            List<CartInfo> cartInfoList = JSON.parseArray(cookieValue,CartInfo.class);

            return cartInfoList;
        }

        return null;
    }

    /**
     *    清空未登录数据
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {

        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    /**
     * 修改选中状态
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        // 直接将前台传递过来的状态赋值给当前的商品
        List<CartInfo> cartList = getCartList(request);
        if (cartList!=null && cartList.size()>0){
            // 循环判断是否有相同的商品
            for (CartInfo cartInfo : cartList) {
                if (cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setIsChecked(isChecked);
                }
            }
        }

        // 写回去
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);

    }
}
