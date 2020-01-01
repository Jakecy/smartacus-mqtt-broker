package com;

import com.config.MqttClientConfig;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 13:30
 * @Version 1.0
 * @Note
 */
public class MqttClientMainBoot {

    public static void main(String[] args) throws Exception{
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername("client001");
        MqttClient mqttClient = MqttClient.create(clientConfig, new MqttMessageListener());
        mqttClient.connect("localhost", 18883).sync();
    }
}
