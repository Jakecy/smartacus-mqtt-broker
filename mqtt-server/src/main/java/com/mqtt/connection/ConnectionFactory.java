package com.mqtt.connection;


import com.mqtt.manager.SessionManager;
import io.netty.channel.Channel;

/**
 * @Author: chihaojie
 * @Date: 2020/1/2 14:53
 * @Version 1.0
 * @Note
 */
public class ConnectionFactory {


 public ClientConnection create(Channel channel, SessionManager sessionManager) {
        ClientConnection connection=new ClientConnection(channel,sessionManager);
        return connection;
    }

}
