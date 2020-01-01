package com.listener;

import com.event.MqttEvent;
import com.handler.MqttHandler;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 14:05
 * @Version 1.0
 * @Note
 */
public class MqttMessageListener  implements MqttHandler {

    private final BlockingQueue<MqttEvent> events;

    public MqttMessageListener() {
        events = new ArrayBlockingQueue<>(100);
    }

    @Override
    public void onMessage(String topic, ByteBuf message) {
        System.out.println("MQTT message [{}], topic [{}]"+ message.toString(StandardCharsets.UTF_8)+topic);
        events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
    }
}
