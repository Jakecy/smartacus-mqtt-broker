package com.mqtt.connection;

import com.mqtt.manager.SessionManager;
import com.mqtt.utils.DateUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @Author: chihaojie
 * @Date: 2020/1/2 14:30
 * @Version 1.0
 * @Note  封装客户端连接
 * 客户端连接中，需要存储当前的channel和sessionManager的引用，当前的连接状态
 */
@Data
public class ClientConnection {

    private final Channel channel;

    private final SessionManager  sessionManager;

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


    public ClientConnection(Channel channel, SessionManager sessionManager) {
        this.channel = channel;
        this.sessionManager = sessionManager;
        this.sendMessageLastestTime=DateUtil.nowTime();
    }

    public void handleMqttMessage(MqttMessage mqttMessage) throws Exception{

    }


    /**
     * 增加对消息的发送逻辑
     */



}
