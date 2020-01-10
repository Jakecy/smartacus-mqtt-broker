package com.mqtt.message;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 12:48
 * @Version 1.0
 * @Note
 */
@Data
public class ClientSub {

    String   clientId;


    MqttQoS subQos;
}
