package com.event;

import lombok.Data;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 14:02
 * @Version 1.0
 * @Note
 */
@Data
public class MqttEvent {

    private final String topic;
    private final String message;
}
