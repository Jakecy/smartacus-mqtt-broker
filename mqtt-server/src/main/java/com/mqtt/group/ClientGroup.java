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
  public  String  onLineTopic="ON_LINE_TOPIC";

  public  String  offLineTopic="OFF_LINE_TOPIC";

  public ConcurrentHashMap<String,Channel>  clients=new ConcurrentHashMap<>(64);

  public ConcurrentHashMap<String,Channel>  subOfflineClients=new ConcurrentHashMap<>(64);

}
