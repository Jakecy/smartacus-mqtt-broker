package com.mqtt.connection;

import com.mqtt.message.ClientSub;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: chihaojie
 * @Date: 2020/1/9 16:59
 * @Version 1.0
 * @Note 邮递员
 */
public class PostMan {


    //订阅队列
    //每个主题对应的客户端
    private final  static ConcurrentMap<String ,List<ClientSub>> topicSubers=new ConcurrentHashMap<>();


}
