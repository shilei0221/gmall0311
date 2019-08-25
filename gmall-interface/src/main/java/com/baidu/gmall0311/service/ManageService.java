package com.baidu.gmall0311.service;

import com.baidu.gmall0311.bean.*;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-03 23:36
 */
public interface ManageService {

    /**
     * 查询所有的一级分类
     * @return
     */
     List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类 Id 查询所有的二级分类数据
     * @param catalog1Id
     * @return
     */
     List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 根据二级分类 id 查询所有三级分类数据
     * @param catalog2Id
     * @return
     */
     List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     *根据三级分类 id 查询平台属性集合
     * @param catalog3Id
     * @return
     */
     List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     * 添加属性与属性值
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);


    //根据 attrId 获取属性值集合数据
    List<BaseAttrValue> getAttrValueList(String attrId);

    /**
     *  //按照业务需求来说  涉及到属性与属性值  是一对多的关系 所有按业务来讲 应该先判断一下属性是否存在 如果不存在就没必要显示属性值
     *         //所有应该先查属性是否存在
     * @param attrId
     * @return
     */
    //根据属性id先查询属性是否存在
    BaseAttrInfo getAttrInfo(String attrId);

    /**
     * 根据对象属性查询spu集合列表
     * @param spuInfo
     * @return
     */
    List<SpuInfo> spuList(SpuInfo spuInfo);

    /**
     * 根据三级分类id查询spu集合列表
     * @param catalog3Id
     * @return
     */
    List<SpuInfo> spuList(String catalog3Id);

    /**
     * 查询基本销售属性表
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存商品数据
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId查询图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(String spuId);

    /**
     * 根据spuImage对象查询图片列表
     * @param spuImage
     * @return
     */

    List<SpuImage> getSpuImageList(SpuImage spuImage);

    /**
     * 根据 spuId 获取销售属性 与 销售属性值
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     * 保存 skuInfo 相关信息数据
     * @param skuInfo
     *
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据 skuId 查询 skuInfo对象
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 查询销售属性 并锁定销售属性值
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId 查询与 skuId 相关的销售属性值集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 根据平台属性值Id集合 查找平台属性 平台属性值
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
