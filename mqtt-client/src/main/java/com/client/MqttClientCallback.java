package com.client;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 14:03
 * @Version 1.0
 * @Note
 */
public interface MqttClientCallback {


    /**
     * This method is called when the connection to the server is lost.
     *
     * @param cause the reason behind the loss of connection.
     */
    void connectionLost(Throwable cause);

    /**
     * This method is called when the connection to the server is recovered.
     *
     */
    void onSuccessfulReconnect();
}
