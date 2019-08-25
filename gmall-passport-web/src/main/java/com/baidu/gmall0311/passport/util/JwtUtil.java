package com.baidu.gmall0311.passport.util;

import io.jsonwebtoken.*;

import java.util.Map;

public class JwtUtil {

    /**
     *  encode 该方法是用来生成token 值的
     *
     * @param key 公共部分 给一个字符串就行
     * @param param 私有部分 存储 user.id  user.nickName
     * @param salt  签名部分也叫盐  给 ip
     * @return
     */
    public static String encode(String key,Map<String,Object> param,String salt){

        //如果 盐 不为空 就将盐与key进行拼接
        if(salt!=null){
            key+=salt;
        }

        //将拼接后的key 进行加密  是一种算法 （有很多种 只是采用了其中一种）
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256,key);

        //将用户信息设置到私有部分存储
        jwtBuilder = jwtBuilder.setClaims(param);

        //生成token 值
        String token = jwtBuilder.compact();

        //返回token 值
        return token;

    }


    /**
     * 解密 token 方法
     * @param token 字符串
     * @param key   key 字符串
     * @param salt 盐 ip
     * @return
     */
    public  static Map<String,Object> decode(String token , String key, String salt){
        Claims claims=null;

        //如果盐不为空 在一次进行拼接 加密需要两个拼接  解密自然也需要两个一起拼接
        if (salt!=null){
            key+=salt;
        }
        try {
            //获取 token 的主体  传入拼接后的 key 进行解密 得到主体部分信息 用户信息
            claims= Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch ( JwtException e) {
            return null;
        }
        //解密出来的私有部分 用户信息
        return  claims;
    }

}
