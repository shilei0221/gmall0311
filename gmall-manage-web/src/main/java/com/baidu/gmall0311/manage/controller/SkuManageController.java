package com.baidu.gmall0311.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.SkuInfo;
import com.baidu.gmall0311.bean.SkuLsInfo;
import com.baidu.gmall0311.bean.SpuImage;
import com.baidu.gmall0311.bean.SpuSaleAttr;
import com.baidu.gmall0311.service.ListService;
import com.baidu.gmall0311.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-06 22:37
 */
@CrossOrigin
@RestController
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    /**
     * 根据 spuId 进行查询图片列表
     * 根据 spuImage 对象 进行查询图片列表
     * http://localhost:8082/spuImageList?spuId=59
     *
     *
     */
//    @RequestMapping("spuImageList")
//    public List<SpuImage> spuImageList(String spuId) {
//
//        return manageService.getSpuImageList(spuId);
//    }

    @RequestMapping("spuImageList")
    public List<SpuImage> spuImageList(SpuImage spuImage) {

        return manageService.getSpuImageList(spuImage);
    }

    /**
     * 获取销售属性  销售属性值集合
     * @param spuId
     * @return
     */
    //http://localhost:8082/spuSaleAttrList?spuId=59
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        return manageService.getSpuSaleAttrList(spuId);
    }

    // http://localhost:8082/saveSkuInfo

    /**
     * 保存 skuInfo 相关信息数据
     * @param skuInfo
     * @return
     */
    @RequestMapping("saveSkuInfo")
    public String saveSkuInfo(@RequestBody SkuInfo skuInfo) {

        manageService.saveSkuInfo(skuInfo);

        return "ok";
    }

    /**
     * 上传商品 根据skuid
     *
     */
    @RequestMapping("onSale")
    public String onSale(String skuId) {
        //创建要保存的对象
        SkuLsInfo skuLsInfo = new SkuLsInfo();

        //给skuLsInfo 赋值
        //得到 skuInfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //属性拷贝赋值  Spring框架中的对象拷贝赋值方法 前面是源对象 后面是需要赋值的目标对象
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        //Apache 中的拷贝赋值方法是 前面是需要赋值的目标对象 后面是源对象
//        org.apache.commons.beanutils.BeanUtils.copyProperties(skuLsInfo,skuInfo);

        listService.saveSkuLsInfo(skuLsInfo);

        return "OK";
    }
}
