package com.baidu.gmall0311.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.baidu.gmall0311.bean.SkuLsInfo;
import com.baidu.gmall0311.bean.SkuLsParams;
import com.baidu.gmall0311.bean.SkuLsResult;
import com.baidu.gmall0311.config.RedisUtil;
import com.baidu.gmall0311.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-12 23:06
 */
@Service
public class ListServiceImpl implements ListService {


    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    /**
     *  做保存数据
     *      针对于查询数据
     *          1.定义 dsl 语句
     *          2.定义执行的动作
     *          3、执行
     *          4.返回数据结果集
     *        保存数据：
     *          定义执行的动作
     *          执行即可~
     *
     */


    // 做保存数据

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {

        // 定义动作 PUT /movie_index/movie/1
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        // 执行
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 根据用户输入的条件查询结果集
     * @param skuLsParams
     * @return
     */
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {

        /*
            查询 dsl 语句方式
                1.定义 dsl 语句
                2.创建执行的动作
                3.执行动作
                4.获取返回结果集

               制作 dls 语句
         */
        // 制作dsl 语句
        String query= makeQueryStringForSearch(skuLsParams);

        // 查询
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        // 执行结果
        SearchResult searchResult =null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 制作返回结果集的方法
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams,searchResult);

        return skuLsResult;
    }

    /**
     * 记录商品访问次数
     * @param skuId
     */
    @Override
    public void incrHostScore(String skuId) {

        Jedis jedis = null;

        try {
            //获取jedis
            jedis = redisUtil.getJedis();

            //定义 key
            String hotKey = "hotScore";

            //使用的数据类型
            Double result = jedis.zincrby(hotKey, 1, "skuId:" + skuId);

            //每十次更新一次 es
            if (result % 10 == 0) {
                //调用更新 es 方法
                updateHotScore(skuId,Math.round(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }

    //更新  hotScore  es 方法
    private void updateHotScore(String skuId, long hotScore) {

        /*
            1.定义 dsl 语句
            2. 定义执行语句
            3. 执行
         */
        String updateDsl = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":"+hotScore+"\n" +
                "  }\n" +
                "}";

        Update update = new Update.Builder(updateDsl).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 制作dsl 语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 创建查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 创建查询query -- bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 设置keyword
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // 设置检索条件match
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            // 给bool 添加must -添加match
            boolQueryBuilder.must(matchQueryBuilder);

            //  设置高亮
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();

            // 设置高亮字段，以及前后缀
            highlighter.field("skuName");
            highlighter.preTags("<span style='color:red'>");
            highlighter.postTags("</span>");

            // 将highlighter 放入高亮
            searchSourceBuilder.highlight(highlighter);
        }
        // 设置三级分类Id
        if (skuLsParams.getCatalog3Id()!=null &&skuLsParams.getCatalog3Id().length()>0){
            // 设置term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            // 给bool 添加filter -添加term
            boolQueryBuilder.filter(termQueryBuilder);
        }

        // 设置平台属性值Id
        if (skuLsParams.getValueId()!=null &&skuLsParams.getValueId().length>0){
            // 循环添加
            for (String valueId : skuLsParams.getValueId()) {
                // 设置term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                // 给bool 添加filter -添加term
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 制作分页： 分页计算公式： 0 2  2,2  ，4,2
        int from = skuLsParams.getPageSize()*(skuLsParams.getPageNo()-1);
        searchSourceBuilder.from(from);
        // 每页显示的大小
        searchSourceBuilder.size(skuLsParams.getPageSize());
        // 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 设置聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        // 调用query查询 将bool 放入query 中
        searchSourceBuilder.query(boolQueryBuilder);

        String query = searchSourceBuilder.toString();

        System.out.println("query:"+query);
        return query;
    }

    /**
     * 制作返回结果集
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        // SkuLsResult
        SkuLsResult skuLsResult = new SkuLsResult();
//        List<SkuLsInfo> skuLsInfoList;
        // 声明一个集合来存储 dsl 语句查询之后的SkuLsInfo结果
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        // 给skuLsInfoArrayList 集合赋值
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if (hits!=null &&hits.size()>0){
            // 循环遍历取出skuLsInfo
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                // 获取高亮的SkuName
                if (hit.highlight!=null && hit.highlight.size()>0){
                    // 获取高亮集合
                    List<String> list = hit.highlight.get("skuName");
                    String skuNameHI = list.get(0);
                    skuLsInfo.setSkuName(skuNameHI);
                }

                // 将es中的skuLsInfo 添加到集合
                skuLsInfoArrayList.add(skuLsInfo);
            }
        }

        // 将集合付给对象
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
//        long total;
        skuLsResult.setTotal(searchResult.getTotal());
//        long totalPages;
        // 计算公式：10 ，3 ，4  9,3,3
        // long totalPages =  searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1
        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
//        List<String> attrValueIdList;
        // 声明一个集合来存储平台属性值Id
        ArrayList<String> arrayList = new ArrayList<>();
        // 给arrayList 赋值
        // 获取平台属性值Id
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();

        if (buckets!=null && buckets.size()>0){
            // 循环获取数据
            for (TermsAggregation.Entry bucket : buckets) {
                String valueId = bucket.getKey();
                arrayList.add(valueId);
            }
        }
        skuLsResult.setAttrValueIdList(arrayList);
        return skuLsResult;
    }

}
