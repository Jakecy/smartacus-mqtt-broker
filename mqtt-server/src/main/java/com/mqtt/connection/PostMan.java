package com.mqtt.connection;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.mqtt.message.ClientSub;
import com.mqtt.utils.CompellingUtil;
import io.netty.handler.codec.mqtt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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


    public static  Boolean sendConnAck(String clientId,MqttConnectMessage mqttMessage){
        ClientConnection connection = ConnectionFactory.getConnection(clientId);
        Optional.ofNullable(connection).ifPresent(conn->{
            MqttFixedHeader fixedHeader=new MqttFixedHeader(MqttMessageType.CONNACK,false,MqttQoS.AT_LEAST_ONCE,false,2);
            MqttConnAckVariableHeader connAckVheader=new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED,true);
            MqttConnAckMessage connAckMessage=new MqttConnAckMessage(fixedHeader,connAckVheader);
            connection.getChannel().writeAndFlush(connAckMessage);
        });
        return true;
    }


    /**
     * 放入到订阅队列中
     */
    public static  List<Integer>  add2TopicSubers(String clientId,MqttSubscribeMessage  subMsg){
        System.out.println("================订阅消息=================");
        System.out.println(JSONObject.toJSONString(subMsg));
        MqttMessageIdVariableHeader subVarHeader = subMsg.variableHeader();
        System.out.println(subVarHeader.toString());
        MqttSubscribePayload subscribePayload = subMsg.payload();
        List<MqttTopicSubscription>  topicList = subscribePayload.topicSubscriptions();
        List<Integer>  grantedSubQos=new ArrayList<>(5);
        Optional.ofNullable(topicList).ifPresent(mts->{
             topicList.forEach(sub->{
                 List<ClientSub> topicSubList = topicSubers.get(sub.topicName());
                 if(null !=topicList && !topicList.isEmpty()){
                      //
                     ClientSub  clientSub=new ClientSub();
                     clientSub.setClientId(clientId);
                     clientSub.setSubQos(sub.qualityOfService());
                     topicSubList.add(clientSub);
                     topicSubers.put(sub.topicName(),topicSubList);
                 }else {
                     List<ClientSub> newTopicSub= Lists.newArrayList();
                     ClientSub  clientSub=new ClientSub();
                     clientSub.setClientId(clientId);
                     clientSub.setSubQos(sub.qualityOfService());
                     newTopicSub.add(clientSub);
                     topicSubers.put(sub.topicName(),newTopicSub);
                 }
             });
        });
        return grantedSubQos;
    }

}
