package com.mqtt.utils;

import com.alibaba.fastjson.JSONObject;
import com.mqtt.MqttServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @Author: chihaojie
 * @Date: 2020/1/3 14:18
 * @Version 1.0
 * @Note
 */
public class PropertiesUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    public static final Properties  load(String configfileName){
        try{
            Properties props = new Properties();
            //加载配置文件
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream is = loader.getResourceAsStream(configfileName);
            props.load(is);
            System.out.println(JSONObject.toJSONString(props));
            return props;
        }catch (Exception e){
            logger.error("【failed to read properties from config file】 , exception happened : {}",e.getStackTrace());
        }
       return null;
    }
}
