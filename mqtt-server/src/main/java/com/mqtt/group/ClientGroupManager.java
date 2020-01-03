package com.mqtt.group;

import com.alibaba.fastjson.JSONObject;
import com.mqtt.connection.ClientConnection;
import com.mqtt.utils.CompellingUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Author: chihaojie
 * @Date: 2020/1/3 9:32
 * @Version 1.0
 * @Note  群组管理器
 */
@Data
public class ClientGroupManager {

    public  static final ConcurrentHashMap<String,ClientGroup>  group=new ConcurrentHashMap<>(64);



    public static  ClientGroup  getMember(String groupId){
        return group.get(groupId);
    }


    /**
     * 向组内添加成员
     */
    public static void putMemeber(ClientGroup  clientGroup){
        group.put(clientGroup.groupId,clientGroup);
    }

    /**
     * 移除组内成员
     */
    public static void removeMemeber(ClientGroup  clientGroup){
        group.remove(clientGroup.groupId);
    }

    /**
     * 判断当前组是否已存在
     */
    public  static  boolean  exists(String groupId){
        if(group.containsKey(groupId)){
            return  true;
        }else {
            return  false;
        }
    }


    public static void putOfflineSubToClientGroup(Channel channel){
        String groupId = CompellingUtil.getGroupId(channel);
        String clientId = CompellingUtil.getClientId(channel);
        ClientGroup clientGroup = group.get(groupId);
        clientGroup.subOfflineClients.put(clientId,channel);
        System.out.println("=========下线订阅群组=============");
        System.out.println(JSONObject.toJSONString(clientGroup.subOfflineClients));
    }

    /**
     * 群发消息
     */
    public static  void  sendOnlineGroupMessage(String groupId){
        //
    }

    public static  void  sendOffLineGroupMessage(String groupId){
        ClientGroup clientGroup = group.get(groupId);
        ConcurrentHashMap<String, Channel> clients = clientGroup.subOfflineClients;
        clients.forEach((K,V)->{
            MqttFixedHeader pubFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false,
                    MqttQoS.AT_MOST_ONCE, false, 0);
            MqttPublishVariableHeader publishVariableHeader=new MqttPublishVariableHeader(clientGroup.offLineTopic,1);
            ByteBuf payload = Unpooled.buffer(200);
            payload.writeBytes("下线消息".getBytes());
            MqttPublishMessage pubMsg=new MqttPublishMessage(pubFixedHeader,publishVariableHeader,payload);
            V.writeAndFlush(pubMsg);
            System.out.println("=============群发下线消息===============");
        });


    }

}
