package com.mqtt.group;

import com.mqtt.connection.ClientConnection;
import io.netty.channel.Channel;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Author: chihaojie
 * @Date: 2020/1/2 19:24
 * @Version 1.0
 * @Note  客户端分组
 */
@Data
public class ClientGroup {

  public   String  groupId;//组标识

  //该组订阅的主题
  public   Set<String> topics=new HashSet<String>();


    //组内成员
    public ConcurrentLinkedQueue<ClientConnection>  groupMem=new ConcurrentLinkedQueue<>();

    public ConcurrentHashMap<String,Channel>  clientGroup=new ConcurrentHashMap<>(64);

}
