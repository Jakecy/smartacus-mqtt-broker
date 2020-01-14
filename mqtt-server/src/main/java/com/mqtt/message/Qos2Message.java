package com.mqtt.message;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;

/**
 * @Author: chihaojie
 * @Date: 2020/1/14 11:06
 * @Version 1.0
 * @Note
 */
@Data
public class Qos2Message {

    private final Integer messageId;

    private final String topic;

    private final MqttQoS qos;

    private final String  content;

    private String clientId;

    public Qos2Message(Integer messageId, String topic, MqttQoS qos, String content) {
        this.messageId = messageId;
        this.topic = topic;
        this.qos = qos;
        this.content = content;
    }
}
