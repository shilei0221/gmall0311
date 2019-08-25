package com.baidu.gmall0311.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.*;
import com.baidu.gmall0311.service.ListService;
import com.baidu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-14 20:23
 */
@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
//    @ResponseBody
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request) {

        //设置每页显示的条数
        skuLsParams.setPageSize(2);

        SkuLsResult skuLsResult = listService.search(skuLsParams);

        //获取商品集合对象
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        //获取平台属性值Id集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();

        //根据平台属性值Id集合 查找平台属性 平台属性值
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);

        //urlParam htp://list.gmall.com/list.html?keyword=手机&valueId=1&valueId=81

        String urlParam = makeUrlParam(skuLsParams);

        //声明一个集合来存储面包屑
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();

        //循环 baseAttrInfoList 比较 # 在集合中是否存在 如果存在 则移除
        //利用迭代器移除集合中的数据
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {

            //得到平台属性对象
            BaseAttrInfo baseAttrInfo = iterator.next();

            //获取平台属性值集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

            if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
                //循环对比
                for (String valueId : skuLsParams.getValueId()) {
                    for (BaseAttrValue baseAttrValue : attrValueList) {
                        //条件中的valueId 与属性值的 id相同 则将数据移除
                        if (valueId.equals(baseAttrValue.getId())) {
                            iterator.remove();

                            //制作面包屑 创建一个平台属性值对象
                            BaseAttrValue baseAttrValueTwo = new BaseAttrValue();

                            //将 baseAttrValuetwo 的名称 改为 平台属性名称 平台属性值
                            baseAttrValueTwo.setValueName(baseAttrInfo.getAttrName() + ":" +baseAttrValue.getValueName());

                            //得到最新的 urlParam
                            String makeUrlParam = makeUrlParam(skuLsParams, valueId);

                            //将最新的urlParam设置到该对象中
                            baseAttrValueTwo.setUrlParam(makeUrlParam);

                            //将面包屑添加
                            baseAttrValueArrayList.add(baseAttrValueTwo);
                        }
                    }
                }
            }

        }



        //保存分页  当前页
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        //保存总页数
        request.setAttribute("totalPages",skuLsResult.getTotalPages());

        //保存面包屑
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);

        request.setAttribute("keyword",skuLsParams.getKeyword());

        //保存urlParam参数
        request.setAttribute("urlParam",urlParam);

        //保存平台属性集合
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);

        //保存商品集合
        request.setAttribute("skuLsInfoList",skuLsInfoList);

        return "list";
    }

    /**
     * 制作urlParam
     * @param skuLsParams  用户输入的查询条件
     * @param excludeValueIds   用户点击面包屑获取的平台属性值 id
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams, String ... excludeValueIds) {

//        htp://list.gmall.com/list.html?keyword=手机&valueId=1&valueId=81
        String urlParam = "";

        //判断keyword
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {

            urlParam += "keyword=" + skuLsParams.getKeyword();
        }

        //判断是否有三级分类Id
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {

            //多个条件后面添加 &
            if (urlParam.length() > 0) {
                urlParam += "&";
            }

            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }

        //判断平台属性值id
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {

            //循环遍历
            for (String valueId : skuLsParams.getValueId()) {
                //获取 excludeValueIds 点击平台属性值 id
                if (excludeValueIds != null && excludeValueIds.length > 0) {

                    //单纯只取点击的平台属性值id  因为是数组 每次只能取一个 所以索引为零
                    String excludeValueId = excludeValueIds[0];

                    //用户点击的平台属性值 id 与 原始的urlParam 参数中的 Id 相同 则当前的平台属性值不拼接
                    if (excludeValueId.equals(valueId)) {
                        continue;
                    }

                }

                //拼接valueId
                if (urlParam.length() > 0) {
                    urlParam += "&";
                }
                urlParam += "valueId=" + valueId;
            }
        }
        //拼接好的参数返回
        return urlParam;

    }
}
