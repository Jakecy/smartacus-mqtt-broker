package com.mqtt.group;

import com.mqtt.connection.ClientConnection;
import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Author: chihaojie
 * @Date: 2020/1/3 9:32
 * @Version 1.0
 * @Note  群组管理器
 */
public class ClientGroupManager {

    public  static final ConcurrentHashMap<String,ClientGroup>  group=new ConcurrentHashMap<>(64);


    /**
     * 向组内添加成员
     */
    public static void putMemeber(ClientGroup  clientGroup){
        group.put(clientGroup.groupId,clientGroup);
    }

    /**
     * 移除组内成员
     */
    public static void removeMemeber(ClientGroup  clientGroup){
        group.remove(clientGroup.groupId);
    }


    /**
     * 群发消息
     */
    public static  void  sendGroupMessage(){
        //
    }

}
