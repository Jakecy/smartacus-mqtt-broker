package com.mqtt.common;

import com.alibaba.fastjson.JSONObject;

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
            e.printStackTrace();
        }
       return null;
    }
}
