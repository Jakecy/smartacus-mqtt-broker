package com.handler;

import io.netty.buffer.ByteBuf;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 13:45
 * @Version 1.0
 * @Note
 */
public interface MqttHandler {

    void onMessage(String topic, ByteBuf payload);
}
