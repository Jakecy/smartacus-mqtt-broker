package com.mqtt.message;

import lombok.Data;

/**
 * @Author: chihaojie
 * @Date: 2020/1/13 15:09
 * @Version 1.0
 * @Note
 */
@Data
public class WaitingAckQos1PublishMessage {

    String    clientId;

    Integer   messageId;

    String    topic;

    String    payload;

}
