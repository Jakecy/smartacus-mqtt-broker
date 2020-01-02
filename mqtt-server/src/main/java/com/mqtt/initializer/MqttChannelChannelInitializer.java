package com.mqtt.initializer;

import com.mqtt.connection.ConnectionFactory;
import com.mqtt.handler.MqttBrokerHandler;
import com.mqtt.manager.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 12:44
 * @Version 1.0
 * @Note
 */
public class MqttChannelChannelInitializer  extends ChannelInitializer {


    private final SessionManager sessionManager;

    private final ConnectionFactory connectionFactory;

    public MqttChannelChannelInitializer(SessionManager sessionManager,ConnectionFactory connectionFactory) {
        this.sessionManager = sessionManager;
        this.connectionFactory=connectionFactory;
    }



    @Override
    protected void initChannel(Channel channel) throws Exception {
        //向该pipeline的channel中添加handler
        // 把netty提供的mqtt解码器放入到Channel通道中
        ChannelPipeline pipeline = channel.pipeline();
        //按顺序加入handler
        //1、把MqttEncoder置于handler链的首部，用于处理消息的编码
        pipeline.addLast("encoder",MqttEncoder.INSTANCE);
        //2、把MqttDecoder置于handler链的第二环处理消息的解码
        pipeline.addLast("decoder",new MqttDecoder());
        //3、把netty提供的心跳handler加入到pipeline
        //IdleStateHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit)
        //30s没有入站消息，则：关闭此连接。 同时，每隔5秒发送一次ping。
        pipeline.addLast("heartbeatHandler",new IdleStateHandler(50,30,0,TimeUnit.SECONDS));
        //4、把自己的handler加入到管道的末端
        //channel.pipeline().addLast("mqttPingHandler", new MqttPingHandler(5));//定义keepalive时间为5s
        pipeline.addLast("brokerHandler",new MqttBrokerHandler(sessionManager,connectionFactory));
    }
}
