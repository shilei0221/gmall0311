package com.baidu.gmall0311.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.bean.SkuInfo;
import com.baidu.gmall0311.bean.SkuSaleAttrValue;
import com.baidu.gmall0311.bean.SpuSaleAttr;
import com.baidu.gmall0311.service.ListService;
import com.baidu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-09 23:40
 */
@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
//    @LoginRequire //表示必须登录   演示购物车 所以注掉
    public String item(@PathVariable String skuId , HttpServletRequest request) {

        //调用 service 层
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //包含了销售属性  销售属性值
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        //根据 spuId 查询与skuId 有关的销售属性值集合
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        //在后台拼接 json 字符串 122|124 = 33 123|126 =34 将其先组成map 然后把map转换为json
        //key = 122|124 value = 33
        String key = "";

        //声明一个 map 存放数据
        Map<String,Object> map = new HashMap<>();

        //循环遍历销售属性值集合
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {

            //获取销售属性值对象
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);

            //第一次拼接 key = ""  key = key + 122  key=122
            //第二次拼接 key="122" key=key+ |  key=122|
            //第三次拼接 key="122|" key=key+124 key = 122|124 是我们要的结果
            //第四次拼接 将 key 放入 map 中 map.put(key,skuId) 然后将key 清空
            //什么时候拼接| ？ 当key不等于0的时候
            if (key.length() > 0) {
                key += "|";
            }
            //拼接属性值
            key += skuSaleAttrValue.getSaleAttrValueId();

            //什么时候将key放入map? 当 skuId与下一个skuId不相同的时候 还有拼接到集合最后的时候
            if ((i + 1) == skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())) {
                //将拼接完的key 放入map中
                map.put(key,skuSaleAttrValue.getSkuId());
                //清空key
                key = "";
            }
        }

        //将 map 转换为 json
        String valuesSkuJson = JSON.toJSONString(map);
        System.out.println("*********"+valuesSkuJson);

        //保存数据
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        //数据保存
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);


        //图片渲染 select * from skuImage where skuId = ?
        //两种方案 一种是直接查询 skuImage  另一种是将查询的结果给skuInfo.skuImageList
        //List<SkuImage> skuImageList = manageService.getSkuImageBySkuId(skuId);

        request.setAttribute("skuInfo",skuInfo);

        listService.incrHostScore(skuId);

        return "item";
    }
}
