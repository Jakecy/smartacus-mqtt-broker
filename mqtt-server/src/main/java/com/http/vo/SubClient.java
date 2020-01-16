package com.http.vo;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;

/**
 * @Author: chihaojie
 * @Date: 2020/1/16 18:12
 * @Version 1.0
 * @Note
 */
@Data
public class SubClient {

    private  String  clientId;

    private MqttQoS  qos;


}
