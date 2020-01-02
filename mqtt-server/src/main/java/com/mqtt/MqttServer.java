package com.mqtt;

import com.mqtt.connection.ConnectionFactory;
import com.mqtt.initializer.MqttChannelChannelInitializer;
import com.mqtt.manager.SessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 12:39
 * @Version 1.0
 * @Note
 */
public class MqttServer {

    public MqttServer() {
    }

    public void run() throws Exception{
        EventLoopGroup bossGroup=new NioEventLoopGroup();
        NioEventLoopGroup  workerGroup=new NioEventLoopGroup();
        try{
            //实例化session工厂和connection工厂
            SessionManager sessionManager=new SessionManager();
            ConnectionFactory connectionFactory=new ConnectionFactory();
            ServerBootstrap sboot=new ServerBootstrap();
            sboot.group(bossGroup,workerGroup)
                    //设置通道类型
                    .channel(NioServerSocketChannel.class)
                    //向通道的中添加handler初始化器
                    .childHandler(new MqttChannelChannelInitializer())
                    .option(ChannelOption.SO_BACKLOG,128)
                    //设置子Socket的keepalive时间
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            //绑定端口号
            ChannelFuture cf = sboot.bind(18883).sync();
            System.out.println("Broker initiated...");

            cf.channel().closeFuture().sync();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        //
        MqttServer mqttServer = new MqttServer();
        mqttServer.run();

    }
}
