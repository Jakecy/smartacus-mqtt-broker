package com.mqtt.connection;

import com.mqtt.manager.SessionManager;
import io.netty.channel.Channel;
import lombok.Data;


/**
 * @Author: chihaojie
 * @Date: 2020/1/2 14:30
 * @Version 1.0
 * @Note  封装客户端连接
 * 客户端连接中，需要存储当前的channel和sessionManager的引用，当前的连接状态
 */
@Data
public class ClientConnection {

    private final Channel channel;

    private final SessionManager  sessionManager;

    private Boolean connected;

    public ClientConnection(Channel channel, SessionManager sessionManager) {
        this.channel = channel;
        this.sessionManager = sessionManager;
    }
}
