package com.mqtt.connection;

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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.channel.ChannelFutureListener.CLOSE;


/**
 * @Author: chihaojie
 * @Date: 2020/1/2 14:30
 * @Version 1.0
 * @Note  封装客户端连接
 * 客户端连接中，需要存储当前的channel和sessionManager的引用，当前的连接状态
 */

public class ClientConnection {

    private final  ConnectionFactory  connectionFactory;


    private final Channel channel;

    private final SessionManager  sessionManager;

    public ClientConnection(ConnectionFactory connectionFactory, Channel channel, SessionManager sessionManager) {
        this.connectionFactory = connectionFactory;
        this.channel = channel;
        this.sessionManager = sessionManager;
    }

    private Set<String> subTopic=new HashSet<String>();

    private Boolean connected;

    private Long  sendMessageLastestTime; //接收最近一次报文的时间


    //每个socket连接维护一个独立的packetId,packetId从1开始
    private AtomicInteger lastPacketId=new AtomicInteger(1);


    /**
     * 获取本次的packetId
     */
    private  Integer  getNextPacketId(){
     return    lastPacketId.getAndIncrement();
    }


    public ClientConnection(Channel channel, SessionManager sessionManager,ConnectionFactory  connectionFactory) {
        this.channel = channel;
        this.sessionManager = sessionManager;
        this.sendMessageLastestTime=DateUtil.nowTime();
        this.connectionFactory=connectionFactory;
    }

    public void handleMqttMessage(MqttMessage mqttMessage) throws Exception{
        switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT:
                handleConnectMessage(mqttMessage);
                break;
           /* case SUBSCRIBE:
                handleClientSubscribeMessage(ctx,(MqttSubscribeMessage) mqttMessage);
                break;
            case UNSUBSCRIBE:
                handleClientUNSubMessage(ctx,(MqttUnsubscribeMessage) mqttMessage);
                break;
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
            case DISCONNECT:
                handleClientDisconnectMessage(ctx,mqttMessage);
                break;*/
            case DISCONNECT:
                handleDisconnectMessage(mqttMessage);
                break;
            default:
                System.out.println("=============收到非Mqtt===========");
                System.out.println("Unexpected message type: " + mqttMessage.fixedHeader().messageType());
                ReferenceCountUtil.release(mqttMessage);
        }
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
        //清除
        this.channel.close().addListeners(ChannelFutureListener.CLOSE);
        String clientId = CompellingUtil.getClientId(channel);
        connectionFactory.removeConnection(clientId);
    }


}
