package com.mqtt.connection;


import com.mqtt.common.ChannelAttr;
import com.mqtt.manager.SessionManager;
import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: chihaojie
 * @Date: 2020/1/2 14:53
 * @Version 1.0
 * @Note
 */
public class ConnectionFactory {



    private static  final ConcurrentHashMap<String,ClientConnection> connectionFactory=new ConcurrentHashMap<>(1024);

    public    ClientConnection  create(Channel channel, SessionManager sessionManager) {
        ClientConnection  connection=new ClientConnection(channel,sessionManager);
        connectionFactory.put(channel.attr(ChannelAttr.ATTR_KEY_CLIENTID).get(),connection);
        return connection;
    }

    public ClientConnection getConnection(String clientId){
        ClientConnection connection = connectionFactory.get(clientId);
        return connection;
    }

    public void removeConnection(String clientId){
        connectionFactory.remove(clientId);
    }


}
