package com.baidu.gmall0311.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.bean.*;
import com.baidu.gmall0311.config.RedisUtil;
import com.baidu.gmall0311.manage.constant.ManageConst;
import com.baidu.gmall0311.manage.mapper.*;
import com.baidu.gmall0311.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-03 23:40
 */
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private  BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 查询所有的一级分类
     * @return
     */
    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    /**
     * 根据一级分类 id 查询二级分类数据
     * @param catalog1Id
     * @return
     */
    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        BaseCatalog2 baseCatalog2 = new BaseCatalog2();

        baseCatalog2.setCatalog1Id(catalog1Id);

        return baseCatalog2Mapper.select(baseCatalog2);
    }

    /**
     * 根据二级分类 id 查询所有的三级分类数据
     * @param catalog2Id
     * @return
     */
    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {

        BaseCatalog3 baseCatalog3 = new BaseCatalog3();

        baseCatalog3.setCatalog2Id(catalog2Id);

        return baseCatalog3Mapper.select(baseCatalog3);
    }

    /**
     * 根据三级分类id 查询所有的平台属性
     * @param catalog3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {

//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//
//        baseAttrInfo.setCatalog3Id(catalog3Id);

        //根据三级分类id得到平台属性 以及平台属性值
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    /**
     * 添加属性与属性值
     * @param baseAttrInfo
     */
    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //因为添加与修改都要经过该方法实现  所以判断 baseAttrInfo表中是否有id值 如果有则是修改 如果没有则是添加
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {

            //则修改
            //插入数据  baseAttrInfo baseAttrValue 两张表
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            //添加
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //处理 baseAttrValue 表中数据的时候 可以先将其表中的数据进行删除 然后在重新进行添加
        //delete from baseAttrValue where attrId = baseAttrInfo.id
        BaseAttrValue baseAttrValue = new BaseAttrValue();

        //将baseAttrInfo中的id 设置到 baseAttrValue表中
        baseAttrValue.setAttrId(baseAttrInfo.getId());

        //调用mapper中的方法进行删除
        baseAttrValueMapper.delete(baseAttrValue);


        //从 baseAttrInfo 中获取 BaseAttrValue 值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        //判断该属性值是否为空
        if (attrValueList != null && attrValueList.size() > 0){

            //如果不为空 进行遍历
            for (BaseAttrValue attrValue : attrValueList) {

                //将属性id设置到属性值中
                attrValue.setAttrId(baseAttrInfo.getId());

                //进行添加数据
                baseAttrValueMapper.insertSelective(attrValue);

            }
        }
    }

    /**
     * 根据 attrId 获取平台属性值集合数据
     * @param attrId
     * @return
     */
    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {

        //select * from baseAttrValue where attrId = attrId
        BaseAttrValue baseAttrValue = new BaseAttrValue();

        //将attrId 设置到 属性值中
        baseAttrValue.setAttrId(attrId);

        //调用通用mapper的方法查询属性值对象
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);

        return baseAttrValueList;
    }

    /**
     *  //按照业务需求来说  涉及到属性与属性值  是一对多的关系 所有按业务来讲 应该先判断一下属性是否存在 如果不存在就没必要显示属性值
     *         //所有应该先查属性是否存在
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {

        //attrId = baseAttrInfo.id;
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        //select * from baseAttrValue where attrId = attrId
        BaseAttrValue baseAttrValue = new BaseAttrValue();

        //将attrId 设置到 属性值中
        baseAttrValue.setAttrId(attrId);

        //调用通用mapper的方法查询属性值对象
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);

        //将平台属性值对象集合赋值给平台属性对象
        baseAttrInfo.setAttrValueList(baseAttrValueList);


        return baseAttrInfo;
    }

    /**
     * 根据对象属性查询spu集合列表
     * @param spuInfo
     * @return
     */
    @Override
    public List<SpuInfo> spuList(SpuInfo spuInfo) {

        return spuInfoMapper.select(spuInfo);
    }

    /**
     * 根据三级分类id查询spu集合列表
     * @param catalog3Id
     * @return
     */
    @Override
    public List<SpuInfo> spuList(String catalog3Id) {

        SpuInfo spuInfo = new SpuInfo();

        spuInfo.setCatalog3Id(catalog3Id);

        return spuInfoMapper.select(spuInfo);
    }

    /**
     * 查询基本销售属性表
     * @return
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    /**
     * 保存商品数据
     * @param spuInfo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {

        /*
            涉及到的表 四张
                spuInfo
                spuImage
                spuSaleAttr
                spuSaleAttrValue
         */
        //spuInfo
        if (spuInfo.getId() != null && spuInfo.getId().length() > 0) {

            //如果spuInfo的ID不为空，则说明是修改 直接修改
            spuInfoMapper.updateByPrimaryKey(spuInfo);
        } else {
            //说明没有 id 进行添加
            spuInfoMapper.insertSelective(spuInfo);
        }

        //spuImage  获取spuImage的集合
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();

        //判读该集合是否存在值 进行遍历
        if (spuImageList != null && spuImageList.size() > 0) {
            //循环遍历图片列表
            for (SpuImage spuImage : spuImageList) {
                //设置图片的 spuId  因为其他都可以根据上传获取到 只有spuId获取不到 所有进行设置
                spuImage.setSpuId(spuInfo.getId());

                //进行添加
                spuImageMapper.insertSelective(spuImage);
            }
        }

        //spuSaleAttr  获取属性集合
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();

        //判读属性集合是否为空  进行遍历
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {

            //循环遍历
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {

                //进行设置spuId
                spuSaleAttr.setSpuId(spuInfo.getId());
                //进行添加
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //因为属性值在属性中是一对多 是属性中的集合 所有在属性中遍历
                //获取属性值集合
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();

                //判断是否为空 进行遍历
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {

                    //进行循环遍历
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {

                        //进行设置spuId值
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        //进行添加
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    /**
     * 根据spuId查询图片列表
     * @param spuId
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(String spuId) {

        SpuImage spuImage = new SpuImage();

        spuImage.setSpuId(spuId);

        return spuImageMapper.select(spuImage);
    }

    /**
     * 根据spuImage对象查询图片列表
     * @param spuImage
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    /**
     * 根据 spuId 获取销售属性 与 销售属性值
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        //调用mapper
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存 skuInfo 相关信息数据
     * @param skuInfo
     */
    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {

        //skuInfo   库存单元表
        if (skuInfo.getId() != null && skuInfo.getId().length() > 0) {

            //说明有id是修改
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        } else {
            //说没有 添加
            skuInfoMapper.insertSelective(skuInfo);
        }

        //skuImage     库存图片表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();

        if (skuImageList != null && skuImageList.size() > 0) {
            for (SkuImage skuImage : skuImageList) {

                skuImage.setSkuId(skuInfo.getId());

                skuImageMapper.insertSelective(skuImage);
            }
        }

        //skuSaleAttrValue  销售属性值
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {

            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {

                skuSaleAttrValue.setSkuId(skuInfo.getId());

                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }

        //skuAttrValue  平台销售属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();

        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {

            for (SkuAttrValue skuAttrValue : skuAttrValueList) {

                skuAttrValue.setSkuId(skuInfo.getId());

                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
    }

    /**
     * 根据 skuId 查询 skuInfo对象
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        // 获取jedis
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        // redisson
        // return getSkuInfoRedisson(skuId, jedis);
        // Jedis
        return getSkuInfoJedis(jedis,skuId);

    }


    public SkuInfo getSkuInfoJedis(Jedis jedis, String skuId){
        SkuInfo skuInfo = null;
        try {
            jedis = redisUtil.getJedis();

            // 定义key 见名知意 sku:skuId:info
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            // 获取数据
            String skuJson = jedis.get(skuKey);
            if (skuJson==null){
                System.out.println("缓存中没有数据");
                // 上锁去数据库中查询
                String skuLockKey=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
                // 执行 set();
                String lockKey   = jedis.set(skuLockKey, "OK", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                // 已经上锁！
                if ("OK".equals(lockKey)){
                    System.out.println("获取分布式锁！");
                    // 从数据库中取得数据
                    skuInfo = getSkuInfoDB(skuId);
                    // 将是数据放入缓存
                    // 将对象转换成字符串
                    String skuRedisStr = JSON.toJSONString(skuInfo);
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);

                    //删除锁
                    jedis.del(lockKey);

                    return skuInfo;
                }else {
                    // 其他人等待
                    System.out.println("等待！");
                    // 等待
                    Thread.sleep(1000);
                    // 自旋
                    return getSkuInfo(skuId);
                }
            }else {
                // 缓存中有数据！
                // 将其转换为对象
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 如何解决空指针问题？
            if (jedis!=null){
                jedis.close();

            }
        }
        return getSkuInfoDB(skuId);
    }


    private SkuInfo getSkuInfoRedisson(String skuId, Jedis jedis) {
        SkuInfo skuInfo;
        RLock lock = null;
        try {

            jedis = redisUtil.getJedis();
            // 定义key 见名知意 sku:skuId:info
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;

            if (jedis.exists(skuKey)){
                String skuJson = jedis.get(skuKey);
                if (StringUtils.isNoneEmpty(skuJson)){
                    // 将字符串转换为对象
                    skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                    return skuInfo;
                }
            }else {
                Config config = new Config();
                config.useSingleServer().setAddress("redis://192.168.67.218:6379");
                // 集群
//                config.useClusterServers().addNodeAddress("redis://192.168.67.218:6379")
//                        .addNodeAddress("redis://192.168.67.219:6379")
//                        .addNodeAddress("redis://192.168.67.220:6379")
//                        .addNodeAddress("redis://192.168.67.221:6379")
//                        .addNodeAddress("redis://192.168.67.222:6379")
//                        .addNodeAddress("redis://192.168.67.223:6379");
                RedissonClient redissonClient = Redisson.create(config);

                lock = redissonClient.getLock("my-lock");
                // 上锁
                lock.lock();
                // redis 中没有数据，则从数据库中查询，放入到redis
                SkuInfo skuInfoDB = getSkuInfoDB(skuId);

                //if (skuInfoDB!=null){
                jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfoDB));
                //}
                return skuInfoDB;
            }
            lock.unlock();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 如何解决空指针问题？
            if (jedis!=null){
                jedis.close();

            }
        }
        return getSkuInfoDB(skuId);
    }

    // ctrl+alt+m 抽出方法快捷键
    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);
        // 添加skuAttrValue
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> attrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(attrValueList);

        return skuInfo;
    }



//    @Override
//    public SkuInfo getSkuInfo(String skuId) {
//
//        //测试redis的使用
////        Jedis jedis = redisUtil.getJedis();
////
////        jedis.set("name","幂幂");
////        jedis.close();
//
//
//        Jedis jedis = null;
////        SkuInfo skuInfo = null;
//
//        try {
//            /*
//               1. 使用分布式锁解决缓存击穿问题
//
//             */
//
////            //获取 jedis
////            jedis = redisUtil.getJedis();
////
////            //定义 key 见名知意 sku:skuId:info
////            String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
////
////            //获取数据
////            String skuJson = jedis.get(skuKey);
////
////            if (skuJson == null) {
////                System.out.println("缓存中没有数据");
////                //上锁去数据库中查询
////                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
////
////                //执行set()
////                String lockKey = jedis.set(skuLockKey,"OK","NX","PX",ManageConst.SKULOCK_EXPIRE_PX);
////
////                //已近上锁
////                if ("OK".equals(lockKey)) {
////                    System.out.println("获取分布式锁");
////                    //从数据库中取得数据
////                    skuInfo = getSkuInfoDB(skuId);
////
////                    //将是数据放入缓存
////                    //将对象转换为字符串
////                    String skuRedisStr = JSON.toJSONString(skuInfo);
////                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);
////                    return skuInfo;
////                } else {
////                    //其他人等待
////                    System.out.println("等待");
////                    //等待
////                    Thread.sleep(1000);
////                    //自旋
////                    return getSkuInfo(skuId);
////                }
////            } else {
////                //缓存中有数据
////                //将其转换为对象
////                  skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
////                  return skuInfo;
////            }
//
//            /*
//            * 2.使用 Redisson 解决缓存击穿问题
//            */
//            Config config = new Config();
//
//            config.useSingleServer().setAddress("redis://192.168.199.134:6379");
//
//            RedissonClient redissonClient = Redisson.create(config);
//
//            RLock lock = redissonClient.getLock("my-lock");
//            //上锁
//            lock.lock();
//            //获取 jedis
//        jedis = redisUtil.getJedis();
//
//        //定义 key 见名知意 sku:skuId:info
//        String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
//
//        //判断如果这个key存在 获取
//        if (jedis.exists(skuKey)) {
//            //获取key中的数据 因为这里只存字符串所以使用String数据类型就可以了 如果存的是对象使用 hash 数据类型最好
//            String skuJson = jedis.get(skuKey);
//
//            //判断是否为空
//            if (StringUtils.isNoneEmpty(skuJson)) {
//                //将字符串转换为对象
//                SkuInfo skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
//
//                return skuInfo;
//            }
//        } else {
//            //redis 中没有数据 则从数据库中查询 放入到redis中
//            SkuInfo skuInfoDB = getSkuInfoDB(skuId);
//
//            jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfoDB));
//
//            return skuInfoDB;
//        }
//            //解锁
//            lock.unlock();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (jedis != null) {
//                jedis.close();
//            }
//        }
//        return getSkuInfoDB(skuId);
//    }

    //shift + alt 抽取公共方法
//    public SkuInfo getSkuInfoDB(String skuId) {
//        //根据skuId查询 skuInfo对象信息
//        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
//
//        //创建图片信息对象
//        SkuImage skuImage = new SkuImage();
//
//        //将skuId设置到图片对象中
//        skuImage.setSkuId(skuId);
//
//        //根据图片对象将图片列表查询出
//        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
//
//        //将图片列表设置到skuInfo对象中
//        skuInfo.setSkuImageList(skuImageList);
//
//        //添加skuAttrValue
//        SkuAttrValue skuAttrValue = new SkuAttrValue();
//
//        skuAttrValue.setSkuId(skuId);
//
//        List<SkuAttrValue> attrValues = skuAttrValueMapper.select(skuAttrValue);
//
//        skuInfo.setSkuAttrValueList(attrValues);
//
//        return skuInfo;
//    }


    /**
     * 查询销售属性 并锁定销售属性值
     * @param skuInfo
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {

        //第一个参数传递skuId  第二个参数传递spuId
        String skuId = skuInfo.getId();
        String spuId = skuInfo.getSpuId();

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    /**
     * 根据spuId 查询与 skuId 相关的销售属性值集合
     * @param spuId
     * @return
     */
    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {

        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    /**
     * 根据平台属性值Id集合 查找平台属性 平台属性值
     * @param attrValueIdList
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {

        /*
            第一种： 直接将集合转换为字符串
            第二种： 在 mybatis 中使用 foreach 进行循环遍历
         */
        String attrValueIds = StringUtils.join(attrValueIdList.toArray(), ",");

        //调用 mapper 方法参数传入id 20 13 120 ...
        return baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);
    }


    public void demo () {
//        //获取 jedis
//        jedis = redisUtil.getJedis();
//
//        //定义 key 见名知意 sku:skuId:info
//        String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
//
//        //判断如果这个key存在 获取
//        if (jedis.exists(skuKey)) {
//            //获取key中的数据 因为这里只存字符串所以使用String数据类型就可以了 如果存的是对象使用 hash 数据类型最好
//            String skuJson = jedis.get(skuKey);
//
//            //判断是否为空
//            if (StringUtils.isNoneEmpty(skuJson)) {
//                //将字符串转换为对象
//                SkuInfo skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
//
//                return skuInfo;
//            }
//        } else {
//            //redis 中没有数据 则从数据库中查询 放入到redis中
//            SkuInfo skuInfoDB = getSkuInfoDB(skuId);
//
//            jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,  JSON.toJSONString(skuInfoDB));
//
//            return skuInfoDB;
    }
}
