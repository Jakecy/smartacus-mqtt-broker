package com.result;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 13:51
 * @Version 1.0
 * @Note
 */
public class MqttConnectResult {

    private final boolean success;
    private final MqttConnectReturnCode returnCode;
    private final ChannelFuture closeFuture;

    public    MqttConnectResult(boolean success, MqttConnectReturnCode returnCode, ChannelFuture closeFuture) {
        this.success = success;
        this.returnCode = returnCode;
        this.closeFuture = closeFuture;
    }

    public boolean isSuccess() {
        return success;
    }

    public MqttConnectReturnCode getReturnCode() {
        return returnCode;
    }

    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }
}
