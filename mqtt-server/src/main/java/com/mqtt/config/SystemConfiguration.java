package com.mqtt.config;

import com.mqtt.common.PropertiesUtil;
import com.mqtt.common.SystemConstant;
import io.netty.handler.codec.mqtt.MqttEncoder;

import java.util.Properties;

/**
 * @Author: chihaojie
 * @Date: 2020/1/3 14:34
 * @Version 1.0
 * @Note
 */
public class SystemConfiguration {

    private final String port;

    public static final SystemConfiguration INSTANCE = new SystemConfiguration();



    public SystemConfiguration() {
        Properties load = PropertiesUtil.load(SystemConstant.configFileName);
        this.port = load.getProperty("port");
    }
}
