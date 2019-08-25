package com.baidu.gmall0311.service;

import com.baidu.gmall0311.bean.SkuLsInfo;
import com.baidu.gmall0311.bean.SkuLsParams;
import com.baidu.gmall0311.bean.SkuLsResult;

/**
 * @author Alei
 * @create 2019-08-12 23:04
 */
public interface ListService {


    /**
     * 保存数据到 es 中
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);


    /**
     * 根据用户输入的条件查询结果集
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 记录商品访问次数
     * @param skuId
     */
    void incrHostScore(String skuId);

}
