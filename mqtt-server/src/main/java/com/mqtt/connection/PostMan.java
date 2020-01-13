package com.mqtt.connection;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.mqtt.message.ClientSub;
import com.mqtt.utils.CompellingUtil;
import com.mqtt.utils.StrUtil;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final AtomicInteger lastPacketId=new AtomicInteger(1);


    /**
     * 获取本次的packetId
     */
    private static final Integer  getNextPacketId(){
        return    lastPacketId.getAndIncrement();
    }



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


    public static void subAck(String clientId, MqttSubscribeMessage mqttMessage, List<Integer> qos) {
        ClientConnection connection = ConnectionFactory.getConnection(clientId);
        Optional.ofNullable(connection).ifPresent(conn->{
            //发送subAck响应
            MqttFixedHeader  subAckFixedHeader=new MqttFixedHeader(MqttMessageType.SUBACK,
                    false,MqttQoS.AT_LEAST_ONCE,false,0);
            //把sub报文中的messageId取出，然后使用它构造一个subAck的可变报头
            MqttMessageIdVariableHeader subAckVHeader = MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().messageId());
            //这里设定为： 请求多大给多大
            MqttSubAckPayload payload = new MqttSubAckPayload(qos);
            MqttSubAckMessage subAckMessage = new MqttSubAckMessage(subAckFixedHeader, subAckVHeader, payload);
            connection.getChannel().writeAndFlush(subAckMessage);
        });
    }

    /**
     * 放入到订阅队列中
     */
      synchronized    public static  List<Integer>  add2TopicSubers(String clientId,MqttSubscribeMessage  subMsg){
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
                 grantedSubQos.add(sub.qualityOfService().value());
                 if(null !=topicSubList && !topicSubList.isEmpty()){
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
        System.out.println(JSONObject.toJSONString(topicSubers));
        return grantedSubQos;
    }


    public static void unsubAck(String clientId, MqttUnsubscribeMessage mqttMessage) {
        ClientConnection connection = ConnectionFactory.getConnection(clientId);
        Optional.ofNullable(connection).ifPresent(conn->{
            //构造报文
            MqttFixedHeader  unsubAckFixed=new MqttFixedHeader(MqttMessageType.UNSUBACK,
                    false,
                    MqttQoS.AT_MOST_ONCE,
                    false,
                    2);
            MqttMessageIdVariableHeader  unsubAckVh=MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().messageId());
            //
            MqttUnsubAckMessage  unsubAckMessage=new MqttUnsubAckMessage(unsubAckFixed,unsubAckVh);
            //返回响应
            conn.getChannel().writeAndFlush(unsubAckMessage);
        });

    }

   synchronized public static void unsub(String clientId, MqttUnsubscribeMessage mqttMessage) {
        MqttUnsubscribePayload payload = mqttMessage.payload();
        List<String> topics = payload.topics();
        if(topics==null || topics.isEmpty()){
            return;
        }
        topics.forEach(t->{
            List<Integer>  tags=Lists.newArrayList();
            List<ClientSub> topicSubList = topicSubers.get(t.trim());
            Optional.ofNullable(topicSubList).ifPresent(tsl->{
                for (int i = 0; i < topicSubList.size(); i++) {
                     ClientSub tse=topicSubList.get(i);
                    if(tse.getClientId().equals(clientId)){
                        tags.add(i);
                    }
                }
            });
            Optional.ofNullable(tags).ifPresent(tg->{
                tags.forEach(index->{
                    topicSubList.remove(index);
                });
            });
            //重新放入
            topicSubers.put(t.trim(),topicSubList);
        });

    }

    public static void dipatchQos0PubMsg(MqttPublishMessage mqttMessage) {
        String topicName = mqttMessage.variableHeader().topicName();
        //获取订阅者
        Optional.ofNullable(topicName).ifPresent(tn->{
            //获取订阅者
            List<ClientSub> topicSubList = topicSubers.get(tn.trim());
            System.out.println("==============订阅者有================");
            System.out.println(JSONObject.toJSONString(topicSubList));
            if(null !=topicSubList && !topicSubList.isEmpty()){
                //
                topicSubList.forEach(ts->{
                     pubQos0Msg2Suber(mqttMessage,ts,getNextPacketId());
                });
            }
        });
    }

    private static void pubQos0Msg2Suber(MqttPublishMessage pubMsg, ClientSub ts, Integer nextPacketId) {
        String content = StrUtil.ByteBuf2String(pubMsg.payload());
        //String  str = new String(msgBody.array(), msgBody.arrayOffset() + msgBody.readerIndex(), msgBody.readableBytes(),CharsetUtil.UTF_8);
        System.out.println("=======pub的payload===========");
        System.out.println(content);
        //发送消息
        System.out.println("===========转发消息================");
        ClientConnection connection = ConnectionFactory.getConnection(ts.getClientId());
        System.out.println(JSONObject.toJSONString(connection));
        Optional.ofNullable(connection).ifPresent(c->{
            System.out.println("===========转发消息================");
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(pubMsg.variableHeader().topicName(), nextPacketId);
            //retainedDuplicate=duplicate+retained, retained即：针对当前ByteBuf多增加一次引用计数
            final ByteBuf copiedPayload = pubMsg.payload().retainedDuplicate();
            MqttPublishMessage  tpubMsg=new MqttPublishMessage(fixedHeader,varHeader,copiedPayload);
            System.out.println("=============已转发的pub消息==============");
            System.out.println(tpubMsg.toString());
            connection.getChannel().writeAndFlush(tpubMsg);
            int i = tpubMsg.refCnt();
            System.out.println("========已转发消息的引用计数=========="+i);
            //System.out.println(pubMsg.release());
            //System.out.println(tpubMsg.release());
            //tpubMsg.release();
        });

    }
}
