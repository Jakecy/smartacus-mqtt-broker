package com.message;

import com.handler.RetransmissionHandler;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.util.function.Consumer;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 13:52
 * @Version 1.0
 * @Note
 */
public final class MqttIncomingQos2Publish {

    private final MqttPublishMessage incomingPublish;

    private final RetransmissionHandler<MqttMessage> retransmissionHandler = new RetransmissionHandler<>();

    MqttIncomingQos2Publish(MqttPublishMessage incomingPublish, MqttMessage originalMessage) {
        this.incomingPublish = incomingPublish;
        this.retransmissionHandler.setOriginalMessage(originalMessage);
    }

    MqttPublishMessage getIncomingPublish() {
        return incomingPublish;
    }

    void startPubrecRetransmitTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        this.retransmissionHandler.start(eventLoop);
    }

    void onPubrelReceived() {
        this.retransmissionHandler.stop();
    }
}
