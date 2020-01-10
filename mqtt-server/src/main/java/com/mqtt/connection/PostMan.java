package com.mqtt.connection;

import com.mqtt.message.ClientSub;
import com.mqtt.utils.CompellingUtil;
import io.netty.handler.codec.mqtt.*;

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

}
