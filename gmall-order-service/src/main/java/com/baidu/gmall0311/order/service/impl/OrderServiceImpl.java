package com.baidu.gmall0311.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.bean.OrderDetail;
import com.baidu.gmall0311.bean.OrderInfo;
import com.baidu.gmall0311.bean.enums.OrderStatus;
import com.baidu.gmall0311.bean.enums.ProcessStatus;
import com.baidu.gmall0311.config.ActiveMQUtil;
import com.baidu.gmall0311.config.RedisUtil;
import com.baidu.gmall0311.order.mapper.OrderDetailMapper;
import com.baidu.gmall0311.order.mapper.OrderInfoMapper;
import com.baidu.gmall0311.service.OrderService;
import com.baidu.gmall0311.service.PaymentService;
import com.baidu.gmall0311.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

/**
 * @author Alei
 * @create 2019-08-20 21:02
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;


    /**
     *  保存订单信息
     * @param orderInfo
     * @return 订单id
     */
    @Override
    @Transactional
    public String saveOrderInfo(OrderInfo orderInfo) {

        //向两张表插入数据 orderInfo orderDetail
        //orderInfo,totalAmount,order_status,user_id,out_trade_no,createTime,expireTime,process_status
        orderInfo.setCreateTime(new Date());

        //设置过期时间 当前系统时间 + 1 天
        Calendar calendar = Calendar.getInstance();

        //设置为一天后
        calendar.add(Calendar.DATE,1);

        //设置过期时间
        orderInfo.setExpireTime(calendar.getTime());

        //设置状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);

        //设置第三方交易编号
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);

        orderInfo.setOutTradeNo(outTradeNo);

        orderInfoMapper.insertSelective(orderInfo);

        //插入订单orderDetail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }

        return orderInfo.getId();
    }

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {

        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            //定义 key
            String tradeNoKey = "user:" + userId + ":tradeCode";

            //生成一个流水号
            String tradeNo = UUID.randomUUID().toString();

            //放入 redis
            jedis.set(tradeNoKey,tradeNo);


            return tradeNo;

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
     * 比较redis中与页面中的流水号是否一致 返回成功失败
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {

        Jedis jedis = null;

        try {

            jedis = redisUtil.getJedis();

            //定义 key
            String tradeNoKey = "user:" + userId + ":tradeCode";

            String tradeNo = jedis.get(tradeNoKey);

            //直接比较
            return tradeCodeNo.equals(tradeNo);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return false;
    }


    /**
     * 比较成功之后 删除redis中的流水号
     * @param userId
     */
    @Override
    public void delTradeCode(String userId) {

        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            //定义 key
            String tradeNoKey = "user:" + userId + ":tradeCode";

            //删除redis中的流水号
            jedis.del(tradeNoKey);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }


    }


    /**
     * 校验库存
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        //调用库存接口 /hasStock?skuId=1022&num=2
        String flag = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);


        return "1".equals(flag);
    }

    /**
     * 根据orderId查询orderInfo
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderId) {

        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail orderDetail = new OrderDetail();

        orderDetail.setOrderId(orderId);

        //将 orderDetail 放入 orderInfo 中
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));

        return orderInfo;
    }

    /**
     * 根据订单id修好状态
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {

        OrderInfo orderInfo = new OrderInfo();

        orderInfo.setId(orderId);

        //修改进程状态
        orderInfo.setProcessStatus(processStatus);

        //修改订单状态
        orderInfo.setOrderStatus(processStatus.getOrderStatus());

        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }


    /**
     *  根据 订单 发送消息给仓库
     * @param orderId
     */
    @Override
    public void sendOrderStatus(String orderId) {
        // 获取链接
        Connection connection = activeMQUtil.getConnection();

        String orderJson=initWareOrder(orderId);
        // 打开链接
        try {
            connection.start();
            // 获取session
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            // 创建队列
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            // 创建提供者
            MessageProducer producer = session.createProducer(order_result_queue);
            // 创建消息对象
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            // 发送消息的内容如何获取？
            textMessage.setText(orderJson);
            // 发送消息
            producer.send(textMessage);
            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    /**
     *获取过期订单
     * @return
     */
    @Override
    public List<OrderInfo> getExpiredOrderList() {
        //orderStatus=UNPAID and exipreTime < new Date()
        Example example = new Example(OrderInfo.class);

        example.createCriteria().andEqualTo("processStatus",ProcessStatus.UNPAID).andLessThan("expireTime",new Date());

        return orderInfoMapper.selectByExample(example);
    }

    /**
     * 处理过期订单
     * @param orderInfo
     */
    @Async //实现异步操作
    @Override
    public void execExpiredOrder(OrderInfo orderInfo) {

        //修改订单状态
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);

        //如果有对应的交易记录 则需要关闭交易记录
        paymentService.closePaymentInfo(orderInfo.getId());

    }

    /**
     * 根据 orderId 获取 orderInfo 然后将 orderInfo 封装成map 再将map转换为劲松字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {

        //根据 orderId 获取 orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);

        //然后将 orderInfo 封装成map
        Map map = initWareOrder(orderInfo);

        //再将 map 转换为 json 字符串
        return JSON.toJSONString(map);
    }

    /**
     * //然后将 orderInfo 封装成map
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId()); // 仓库Id

        // map.put("details","放入订单明细集合")
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        // 声明一个集合来存储订单明细
        ArrayList<HashMap> hashMapArrayList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("skuId",orderDetail.getSkuId());
            hashMap.put("skuNum",orderDetail.getSkuNum());
            hashMap.put("skuName",orderDetail.getSkuName());

            hashMapArrayList.add(hashMap);
        }
        // 将订单明细集合放入details
        map.put("details",hashMapArrayList);


        return map;
    }
    /**
     *  根据参数进行拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
        1.  先获取到原始订单
        2.  wareSkuMap 转换成我们能操作的对象
        3.  创建新的子订单
        4.  给子订单赋值
        5.  保存到数据库
        6.  添加到子订单集合
        7.  修改订单状态
         */
        OrderInfo orderInfoOrigin  = getOrderInfo(orderId);
        // wareSkuMap=[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        // 循环集合
        for (Map map : maps) {
            // 获取仓库Id
            String wareId = (String) map.get("wareId");
            // 获取商品Id
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 创建新的子订单
            OrderInfo subOrderInfo  = new OrderInfo();
            // 给子订单进行赋值操作 只有金额，父Id 不一样，主键id 不一样，仓库Id
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            subOrderInfo.setWareId(wareId);
            // 金额？需要获取到订单明细
            // 创建一个新的子订单明细
            ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();
            // 先获取到原始订单的明细

            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            // 循环原始订单明细与"skuIds":["2","10"] 对应的商品Id 进行比较
            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIds) {
                    if (orderDetail.getSkuId().equals(skuId)){
                        // 则给新的子订单明细
                        orderDetail.setId(null);
                        orderDetailArrayList.add(orderDetail);
                    }
                }
            }
            // 子订单明细集合赋值给子订单
            subOrderInfo.setOrderDetailList(orderDetailArrayList);

            // 可以计算子订单金额
            subOrderInfo.sumTotalAmount();

            // 保存到数据
            saveOrderInfo(subOrderInfo);

            // 将新的子订单添加到集合中
            subOrderInfoList.add(subOrderInfo);

        }
        // 将原始订单进行拆分
        updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return subOrderInfoList;
    }
}
