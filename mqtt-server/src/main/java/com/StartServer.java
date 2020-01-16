package com;

import com.http.HttpServer;
import com.mqtt.MqttServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: chihaojie
 * @Date: 2020/1/16 14:03
 * @Version 1.0
 * @Note
 */
public class StartServer {

    private static final Logger logger = LoggerFactory.getLogger(StartServer.class);

    public static void main(String[] args)  {
        try {
            logger.info("【http   Server Start Ok】");
            HttpServer httpServer = new HttpServer();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        logger.info("【Mqtt Broker  Server Start Ok】");
                        MqttServer mqttServer = new MqttServer();
                        mqttServer.run();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }).start();
            httpServer.run();

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
