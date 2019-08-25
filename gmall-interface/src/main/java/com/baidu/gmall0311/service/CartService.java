package com.baidu.gmall0311.service;

import com.baidu.gmall0311.bean.CartInfo;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-18 13:53
 */
public interface CartService {

    //接口最重要的就是 返回值 和 参数列表
    /**
     *
     * 添加购物车
     *
     * @param skuId 商品 id
     * @param userId    用户 id
     * @param skuNum 商品数量
     */
    void  addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 登录的情况下 根据用户id查询购物车中的数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     *  登录状态 下选中状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据userId去查询购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户id 去数据库中查询购物车集合
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
