package com.baidu.gmall0311.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.bean.UserAddress;
import com.baidu.gmall0311.bean.UserInfo;
import com.baidu.gmall0311.config.RedisUtil;
import com.baidu.gmall0311.service.UserService;
import com.baidu.gmall0311.user.mapper.UserAddressMapper;
import com.baidu.gmall0311.user.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-02 10:25
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAddressMapper addressMapper;

    @Autowired
    private RedisUtil redisUtil;


    //定义一些常量来拼接用户 key
    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Override
    public List<UserInfo> getUserInfoListAll() {
        return userMapper.selectAll();
    }

    @Override
    public void addUser(UserInfo admin) {
        userMapper.insert(admin);
    }

    @Override
    public void updateUser(UserInfo admin) {
        userMapper.updateByPrimaryKeySelective(admin);
    }

    @Override
    public void updateUserByName(String name, UserInfo admin) {

        Example example = new Example((UserInfo.class));

        example.createCriteria().andEqualTo("username",name);

        userMapper.updateByExampleSelective(admin,example);
    }

    @Override
    public void delUser(UserInfo admin) {

        userMapper.deleteByPrimaryKey(admin);
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {

        UserAddress userAddress = new UserAddress();

        userAddress.setUserId(userId);

        return addressMapper.select(userAddress);
    }


    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {

        Jedis jedis = null;

        try {
            //明文密码
            String passwd = userInfo.getPasswd();

            //对明文密码进行加密 MD5
            String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());

            //将加密后的密码设置到对象中
            userInfo.setPasswd(newPwd);

            UserInfo info = userMapper.selectOne(userInfo);

            //如果用户信息不为空 将用户信息保存到缓存中 redis
            if (info != null) {

                //保存用户信息
                jedis = redisUtil.getJedis();

                //定义 key user:userId:info
                String userKey = userKey_prefix + info.getId() + userinfoKey_suffix;

    //            jedis.set(userKey, JSON.toJSONString(info));
                jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));

                return info;
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
     * 根据 userId 去查询 redis 中的用户信息
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {

        Jedis jedis = null;

        try {

            //获取 jedis
            jedis = redisUtil.getJedis();

            //定义key
            String userKey = userKey_prefix + userId + userinfoKey_suffix;

            //获取json串
            String userJson = jedis.get(userKey);

            //判断json串不为空  进行转换  最终返回
            if (StringUtils.isNoneEmpty(userJson)) {

                //将userJson 转换为对象
                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);

                //延长用户的过期时间
                jedis.expire(userKey,userKey_timeOut);

                return userInfo;
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
//    @Override
//    public List<UserAddress> getUserAddressList(String userId) {
//
//        Example example = new Example(UserAddress.class);
//
//        example.createCriteria().andEqualTo(userId);
//
//        List<UserAddress> userAddresses = addressMapper.selectByExample(example);
//
//        return userAddresses;
//    }
}
