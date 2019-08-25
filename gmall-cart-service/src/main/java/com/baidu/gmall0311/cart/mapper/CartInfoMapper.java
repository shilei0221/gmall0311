package com.baidu.gmall0311.cart.mapper;

import com.baidu.gmall0311.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-18 13:52
 */
public interface CartInfoMapper extends Mapper<CartInfo> {

    /**
     * 根据用户id 查询购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> selectCartListWithCurPrice(String userId);
}
