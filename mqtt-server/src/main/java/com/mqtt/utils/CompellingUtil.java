package com.mqtt.utils;

import com.mqtt.common.ChannelAttr;
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
    public static String clientId(Channel channel) {
        return channel.attr(ChannelAttr.ATTR_KEY_CLIENTID).get();
    }
}
