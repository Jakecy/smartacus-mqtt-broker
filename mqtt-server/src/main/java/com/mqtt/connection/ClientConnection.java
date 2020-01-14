package com.mqtt.connection;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.mqtt.common.ChannelAttributes;
import com.mqtt.config.UsernamePasswordAuth;
import com.mqtt.manager.SessionManager;
import com.mqtt.utils.CompellingUtil;
import com.mqtt.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;


/**
 * @Author: chihaojie
 * @Date: 2020/1/2 14:30
 * @Version 1.0
 * @Note  封装客户端连接
 * 客户端连接中，需要存储当前的channel和sessionManager的引用，当前的连接状态
 */
@Data
public class ClientConnection {

    private final  ConnectionFactory  connectionFactory;

    private  String clientId;

    private final Channel channel;

    private final SessionManager  sessionManager;

    public ClientConnection(ConnectionFactory connectionFactory, Channel channel, SessionManager sessionManager) {
        this.connectionFactory = connectionFactory;
        this.channel = channel;
        this.sessionManager = sessionManager;
        this.sendMessageLastestTime=DateUtil.nowTime();
    }

    private Set<String> subTopic=new HashSet<String>();

    private Boolean connected;

    private Long  sendMessageLastestTime; //接收最近一次报文的时间


    //每个socket连接维护一个独立的packetId,packetId从1开始
    private AtomicInteger lastPacketId=new AtomicInteger(1);

    //未完成的pub消息
    private final ArrayList<Integer>  nonCompletePubMessageIds= Lists.newArrayList();

    private final ConcurrentHashMap<Integer,MqttPubAckMessage> notAckPubRecMap=new ConcurrentHashMap<>(1024);


    /**
     * 获取本次的packetId
     */
    private  Integer  getNextPacketId(){
     return    lastPacketId.getAndIncrement();
    }



    public void handleMqttMessage(MqttMessage mqttMessage) throws Exception{
        switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT:
                handleConnectMessage(mqttMessage);
                break;
            case PINGREQ:
                handlePingReqMessage(mqttMessage);
                break;
            case SUBSCRIBE:
                handleSubscribeMessage((MqttSubscribeMessage) mqttMessage);
                break;
            case UNSUBSCRIBE:
                handleUnSubMessage((MqttUnsubscribeMessage) mqttMessage);
                break;
            case PUBLISH:
                handlePublishMessage((MqttPublishMessage) mqttMessage);
                break;
            case PUBACK:
                handlePubAckMessage((MqttPubAckMessage) mqttMessage);
           /*
            case PUBLISH:
                handleClientPublishMessage(ctx,mqttMessage);
                break;
            case PUBACK:
                handleClientPubAckMessage(ctx,(MqttPubAckMessage) mqttMessage);
                break;
            case PUBREC:
                handleClientPubRecMessage(ctx,mqttMessage);
                break;
            case PUBREL:
                handleClientPubRelMessage(ctx,mqttMessage);
                break;
            case PUBCOMP:
                handleClientPubCompMessage(ctx, mqttMessage);
                break;
     */
            case DISCONNECT:
                handleDisconnectMessage(mqttMessage);
                break;
            default:
                System.out.println("=============收到非Mqtt===========");
                System.out.println("Unexpected message type: " + mqttMessage.fixedHeader().messageType());
                ReferenceCountUtil.release(mqttMessage);
        }
    }

    private void handlePubAckMessage(MqttPubAckMessage mqttMessage) {
        //处理pubAck消息
        PostMan.handlePubAckMsg(mqttMessage,clientId);
    }

    private void handlePingReqMessage(MqttMessage mqttMessage) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        this.channel.writeAndFlush(new MqttMessage(fixedHeader));
    }

    private void handlePublishMessage(MqttPublishMessage mqttMessage) {
        System.out.println("================publish消息================");
        System.out.println(mqttMessage.toString());
        //判断Qos等级
        MqttQoS mqttQoS = mqttMessage.fixedHeader().qosLevel();
        switch (mqttQoS){

            case AT_MOST_ONCE:
                processQos0PubMessage(mqttMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1PubMessage(mqttMessage);
                break;
            case EXACTLY_ONCE:
                processQos2PubMessage(mqttMessage);
                break;
        }
    }

    /**
     * 处理Qos2级别的pub消息
     * @param mqttMessage
     */
    private void processQos2PubMessage(MqttPublishMessage mqttMessage) {
        int packetId = mqttMessage.variableHeader().packetId();
        if(nonCompletePubMessageIds.contains(packetId)){
            ReferenceCountUtil.release(mqttMessage);
            return;
        }
        processQos2PubMessageAsReceiver(mqttMessage);
        processQos2PubMessageAsSender(mqttMessage);
        ReferenceCountUtil.release(mqttMessage);
    }



    private void processQos2PubMessageAsReceiver(MqttPublishMessage mqttMessage) {
        //作为接收者
        int packetId = mqttMessage.variableHeader().packetId();
        String topic = mqttMessage.variableHeader().topicName();
        ByteBuf payload = mqttMessage.payload();
        //响应pubRec，并把该pubRec放入待确认队列
        nonCompletePubMessageIds.add(packetId);
        MqttPubAckMessage oldRelMsg = notAckPubRecMap.get(packetId);
        if(null ==oldRelMsg){
            //发送pubRec报文
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE,
                                                              false, 0);
            MqttPubAckMessage pubRecMessage = new MqttPubAckMessage(fixedHeader, from(packetId));
            //发送
            channel.writeAndFlush(pubRecMessage);
            //加入等待确认队列
            notAckPubRecMap.put(packetId,pubRecMessage);
        }else {
            channel.writeAndFlush(oldRelMsg);
        }
        //重发
        channel.eventLoop().scheduleAtFixedRate(()->{
            //重发rec报文
            retrySendRecWhenNoRelAcked(packetId);
        },2,2,TimeUnit.SECONDS);

    }



    private void processQos2PubMessageAsSender(MqttPublishMessage mqttMessage) {
        notAckPubRecMap.forEach((k,v)->{

        });
    }

    private void retrySendRecWhenNoRelAcked(Integer packetId) {
        //重发rec报文
        MqttPubAckMessage oldRelMsg = notAckPubRecMap.get(packetId);
        Optional.ofNullable(oldRelMsg).ifPresent(e->{
            channel.writeAndFlush(oldRelMsg);
        });
    }

    private void processQos1PubMessage(MqttPublishMessage mqttMessage) {
        System.out.println(ReferenceCountUtil.refCnt(mqttMessage));
        String clientId = CompellingUtil.getClientId(this.channel);
        //响应ack
        responseAckToSender(clientId,mqttMessage);
       final MqttPublishMessage publishMessage = mqttMessage.retainedDuplicate();
        PostMan.dipatchQos1PubMsg((MqttPublishMessage) publishMessage,clientId);
    }

    private void responseAckToSender(String clientId, MqttPublishMessage mqttMessage) {
        MqttFixedHeader pubAckFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false,
                MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader =MqttMessageIdVariableHeader.from(lastPacketId.get()) ;//MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().packetId());
        MqttPubAckMessage    pubAck=  new MqttPubAckMessage(pubAckFixedHeader, variableHeader);
        channel.writeAndFlush(pubAck);
    }

    private void processQos0PubMessage(MqttMessage mqttMessage) {
        System.out.println(ReferenceCountUtil.refCnt(mqttMessage));
        String clientId = CompellingUtil.getClientId(this.channel);
        PostMan.dipatchQos0PubMsg((MqttPublishMessage) mqttMessage);
    }


    private void handleConnectMessage(MqttMessage mqttMessage) {
        //合法性校验
        //建立连接、进行响应
        Boolean valid=checkValid(mqttMessage);
        if(valid){
            buildConnection(mqttMessage);
            //返回响应
            String clientId = CompellingUtil.getClientId(this.channel);
            PostMan.sendConnAck(clientId, (MqttConnectMessage) mqttMessage);
        }else {
            //丢弃当前连接
            abortConnection();
        }
    }

    private void buildConnection(MqttMessage mqttMessage) {
        MqttConnectMessage connectMsg=(MqttConnectMessage)mqttMessage;
        MqttConnectPayload connectPayload = connectMsg.payload();
        channel.attr(ChannelAttributes.ATTR_KEY_CLIENTID).set(connectPayload.clientIdentifier());
        this.clientId=channel.attr(ChannelAttributes.ATTR_KEY_CLIENTID).get();
        connectionFactory.putConnection(this);
    }

    private void abortConnection() {
        //
        String clientId = CompellingUtil.getClientId(channel);
        connectionFactory.removeConnection(clientId);
        channel.close().addListeners(ChannelFutureListener.CLOSE);
    }

    private Boolean checkValid(MqttMessage mqttMessage) {
        //把此消息转换为connect报文
        MqttConnectMessage connectMsg=(MqttConnectMessage)mqttMessage;
        //取出固定头和可变头以及有效载荷
        MqttConnectVariableHeader connectVariableHeader = connectMsg.variableHeader();
        MqttConnectPayload connectPayload = connectMsg.payload();
        boolean userNameFlag = connectVariableHeader.hasUserName();
        boolean passwordFlag = connectVariableHeader.hasPassword();
        boolean willRetainFlag = connectVariableHeader.isWillRetain();
        boolean willFlagFlag = connectVariableHeader.isWillFlag();
        int willQos = connectVariableHeader.willQos();
        boolean cleanSessionFlag = connectVariableHeader.isCleanSession();
        if(UsernamePasswordAuth.auth){
            if(userNameFlag&& passwordFlag){
                //用户名和密码的校验
                String userName = connectPayload.userName();
                ByteBuf buf = Unpooled.wrappedBuffer(connectPayload.passwordInBytes());
                String password =buf.toString(CharsetUtil.UTF_8);
                if(!(UsernamePasswordAuth.username.equals(userName) &&UsernamePasswordAuth.password.equals(password))){
                   return false;
                }
            }else {
                return false;
            }
        }
        return true;
    }

    void handleDisconnectMessage(MqttMessage  mqttMessage){
        System.out.println("=============处理Disconnect报文之前===========");
        System.out.println(JSONObject.toJSONString(ConnectionFactory.connectionFactory));
        //清除
        this.channel.close().addListeners(ChannelFutureListener.CLOSE);
        String clientId = CompellingUtil.getClientId(channel);
        connectionFactory.removeConnection(clientId);
        System.out.println("=============处理Disconnect报文之后===========");
        System.out.println(JSONObject.toJSONString(ConnectionFactory.connectionFactory));
    }


    /**
     * 处理订阅消息
     * @param mqttMessage
     */
    private void handleSubscribeMessage(MqttSubscribeMessage mqttMessage) {
        //把该主题放到此主题的订阅队列中
        //放入到connection中
        //返回subAck响应
        String clientId = CompellingUtil.getClientId(this.channel);
        List<Integer> qos = PostMan.add2TopicSubers(clientId, mqttMessage);
        List<MqttTopicSubscription>  topicList = mqttMessage.payload().topicSubscriptions();
        Optional.ofNullable(topicList).ifPresent(tl->{
            topicList.forEach(t->{
                this.subTopic.add(t.topicName());
            });
        });
        PostMan.subAck(clientId,mqttMessage,qos);
    }

    private void handleUnSubMessage(MqttUnsubscribeMessage mqttMessage) {
        //取消订阅
        String clientId = CompellingUtil.getClientId(this.channel);
        //退订
        PostMan.unsub(clientId,mqttMessage);
        PostMan.unsubAck(clientId,mqttMessage);
    }



}
