package com.baidu.gmall0311.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.bean.CartInfo;
import com.baidu.gmall0311.bean.SkuInfo;
import com.baidu.gmall0311.cart.cartconst.CartConst;
import com.baidu.gmall0311.cart.mapper.CartInfoMapper;
import com.baidu.gmall0311.config.RedisUtil;
import com.baidu.gmall0311.service.CartService;
import com.baidu.gmall0311.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-18 13:52
 */
@Service
public class CartInfoServiceImpl implements CartService {

    @Autowired //这里如果想使用 mapper 需要在启动类上加注解扫描mapper位置
    private CartInfoMapper cartInfoMapper;

    @Reference //因为要调用manageService中的方法 所以使用该注解 进行远程调用
    private ManageService manageService;

    @Autowired //注入redis工具类
    private RedisUtil redisUtil;

    String userKey = "";

    /**
     *
     * 添加购物车
     *
     * @param skuId 商品 id
     * @param userId    用户 id
     * @param skuNum 商品数量
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {

        Jedis jedis = null;

        try {
        /*
            登录状态下添加购物车
                1.先判断购物车中是否有要添加的该商品
                    select * from cartInfo where skuId = ? and userId = ?
                    1.1 有商品 数据相加
                    1.2 没有商品 直接添加
                2.添加之后保存到redis缓存中
         */
            CartInfo cartInfo = new CartInfo();

            cartInfo.setSkuId(skuId);

            cartInfo.setUserId( userId);

            //查询出一个用户的购物车中的商品信息
            CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);

            //获取 jedis
            jedis = redisUtil.getJedis();

            //定义 key
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;


            //说明购物车中有该商品
            if (cartInfoExist != null) {

                //有商品  购物车商品数量
                cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);

                //给实时价格（skuInfo.price） 赋值  默认为当前加入购物车时的价格
                cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

                //将修改的数据重新保存到数据库
                cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

                //保存到 redis 中  保存购物车是否设置过期时间 看需求   将两个redis保存合并为一个
//                jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));

            } else {
                //没商品 直接保存到数据库
                CartInfo cartInfo1 = new CartInfo();

                //根据商品id查询商品信息  将数据赋值给 cartInfo1
                SkuInfo skuInfo = manageService.getSkuInfo(skuId);

                cartInfo1.setSkuId(skuId);
                cartInfo1.setCartPrice(skuInfo.getPrice());
                cartInfo1.setSkuPrice(skuInfo.getPrice());
                cartInfo1.setSkuName(skuInfo.getSkuName());
                cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
                cartInfo1.setUserId(userId);
                cartInfo1.setSkuNum(skuNum);

                //插入数据库
                cartInfoMapper.insertSelective(cartInfo1);

                //将 cartInfo1 对象赋值给 cartInfoExist if else 只走一个条件 为了只使用一次保存到redis中 所以使用该方法
                cartInfoExist = cartInfo1;

            }

            //保存到redis
            jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfoExist));


            /*
                如何设置过期时间？  与用户的过期时间一致！

                user:userId:info
             */
            String userKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFOKEY_SUFFIX;

            //获取过期时间  用户的过期时间
            Long ttl = jedis.ttl(userKey);

            //设置过期时间  将商品的过期时间与用户的过期时间一致
            jedis.expire(cartKey,ttl.intValue());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }



    /**
     * 登录的情况下 根据用户id查询购物车中的数据
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId) {

        //创建一个集合存储购物车商品对象 最终返回
        List<CartInfo> cartInfoList = new ArrayList<>();

        Jedis jedis = null;

        try {

            /*
                1.看缓存是否有数据
                    1.1 有 直接获取并返回
                    1.2 无 从数据库中获取信息 并放入缓存中
             */
            //获取jedis
            jedis = redisUtil.getJedis();

            //定义 key
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

            //获取数据 jedis.hget(); 只能获取一条数据不使用  获取redis中所有的value值
            List<String> cartInfoStrList = jedis.hvals(cartKey);

            //判断该redis中有数据的时候在进行遍历
            if (cartInfoStrList != null && cartInfoStrList.size() > 0) {
                //循环遍历
                for (String cartInfoJson : cartInfoStrList) {
                    //将遍历出来的cartInfoJson字符串转换为 对象cartInfo
                    CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);

                    //将转换的对象增加到集合中
                    cartInfoList.add(cartInfo);
                }
                //细节处理  在数据库增加两个字段 创建时间与更新时间 按照更新时间可以进行降序排列
                cartInfoList.sort((s1,s2) -> s1.getId().compareTo(s2.getId()));

                return cartInfoList;
            } else {
                //redis 中没有数据 则从数据库中获取 并添加到缓存
                cartInfoList = loadCartCache(userId);

                return cartInfoList;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return null;
    }


    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {

        //获取数据库中的购物车
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);

        //循环遍历合并条件
        //利用 flag 标志

        //遍历购物车中的数据集合
        for (CartInfo cartInfoCk : cartListCK) {

            //定义标志 来判断登录 未登录
            boolean isMatch = false;

            //遍历根据用户查出来的购物车中的数据
            for (CartInfo cartInfoDB : cartInfoListDB) {

                //判断传入的购物车中的数据中的商品id是否与数据库中该用户的商品id一致  是否为一件商品
                if (cartInfoDB.getSkuId().equals(cartInfoCk.getSkuId())) {
                    //如果是一件商品 数量相加
                    cartInfoDB.setSkuNum(cartInfoCk.getSkuNum() + cartInfoDB.getSkuNum());

                    //将相加之后的数据更新到数据库中
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);

                    //将标志设置为 true 下面代码不再执行
                    isMatch = true;
                }
            }

            //未登录的购物车在数据库中没有的商品 直接添加到数据库  将标志取反为true
            if (!isMatch) {
                //将 userId 放入 cartInfoCk中
                cartInfoCk.setUserId(userId);

                cartInfoMapper.insertSelective(cartInfoCk);
            }
        }

        //将合并后的数据 查询出来  并将数据返回
        List<CartInfo> cartInfoList = loadCartCache(userId);

        //在此处进行合并勾选状态
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartListCK) {
                //判断商品是否相同
                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())) {
                    if ("1".equals(cartInfoCK.getIsChecked())) {
                        //设置状态
                        cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());

                        //设置被选中的商品
                        checkCart(cartInfoCK.getSkuId(),"1",userId);
                    }
                }
            }
        }

        return cartInfoList;
    }

    /**
     *  登录状态 下选中状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        // 获取当前选中的商品
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        String cartInfoJson = jedis.hget(cartKey, skuId);
        // 将cartInfoJson 转换为对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        // 将选中的状态赋值给cartInfo
        cartInfo.setIsChecked(isChecked);
        // 写回redis
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));

        // 新建一个key 来存储所有选中的商品，给订单使用！key = user:userId:checked
        String cartCheckedKey=CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if ("1".equals(isChecked)){
            jedis.hset(cartCheckedKey,skuId,JSON.toJSONString(cartInfo));
        }else {
            jedis.hdel(cartCheckedKey,skuId);
        }

        jedis.close();


    }

    /**
     * 根据userId去查询购物车数据
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {

        Jedis jedis = null;

        List<CartInfo> cartInfoList = new ArrayList<>();

        try {

            jedis = redisUtil.getJedis();

            //定义key
            String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId +CartConst.USER_CHECKED_KEY_SUFFIX;

            List<String> stringList = jedis.hvals(cartCheckedKey);

            if (stringList != null && stringList.size() > 0) {
                for (String cartJson : stringList) {
                    cartInfoList.add(JSON.parseObject(cartJson,CartInfo.class));
                }
            }

            //返回集合
            return cartInfoList;

        } catch (Exception e) {

            e.printStackTrace();
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return null;
    }


    /**
     * 根据用户id 去数据库中查询购物车集合
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {

        Jedis jedis = null;

        try {
            //调用 mapper 根据用户id查询出购物车中的数据
            List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

            //如果该集合中没有数据 返回空
            if (cartInfoList == null || cartInfoList.size() == 0) {
                return null;
            }

            //cartInfoList 将集合数据放入到redis 中
            //获取 redis
            jedis = redisUtil.getJedis();

            //定义 key
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

            //循环遍历集合  循环出一个一个的然后放入集合中
//            for (CartInfo cartInfo : cartInfoList) {
//
//                jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
//            }

            //循环遍历集合 循环出来的数据放入map集合中 然后在将map放入  一次性放入
            Map<String,String> map = new HashMap<>();

            for (CartInfo cartInfo : cartInfoList) {
                //将遍历出来的数据 放入 map 中
                map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
            }

            jedis.hmset(cartKey,map);

            return cartInfoList;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return null;
    }
}
