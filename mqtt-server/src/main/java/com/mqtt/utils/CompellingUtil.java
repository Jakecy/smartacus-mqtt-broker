package com.mqtt.utils;

import com.mqtt.common.ChannelAttributes;
import io.netty.channel.Channel;

/**
 * @Author: chihaojie
 * @Date: 2020/1/2 17:44
 * @Version 1.0
 * @Note
 */
public class CompellingUtil {


    /**
     * 获取当前连接的clientId
     * @param channel
     * @return
     */
    public static String getClientId(Channel channel) {
        return channel.attr(ChannelAttributes.ATTR_KEY_CLIENTID).get();
    }

    public static  String  getGroupId(Channel channel){
        String clientId=channel.attr(ChannelAttributes.ATTR_KEY_CLIENTID).get();
        //群组划分
        String[] spiltArray = StrUtil.spilt(clientId, "-");
        String groupId=spiltArray[0];
        return groupId;
    }
}
