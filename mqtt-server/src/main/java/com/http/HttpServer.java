package com.http;

import com.http.initializer.HttpChannelChannelInitializer;
import com.mqtt.MqttServer;
import com.mqtt.config.SystemConfiguration;
import com.mqtt.connection.ConnectionFactory;
import com.mqtt.initializer.MqttChannelChannelInitializer;
import com.mqtt.manager.SessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: chihaojie
 * @Date: 2020/1/16 11:25
 * @Version 1.0
 * @Note
 */
public class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    public HttpServer() {

    }

    public void run() throws Exception{
        EventLoopGroup bossGroup=new NioEventLoopGroup(1);
        NioEventLoopGroup  workerGroup=new NioEventLoopGroup();
        try{
            //实例化session工厂和connection工厂
            ServerBootstrap sboot=new ServerBootstrap();
            sboot.group(bossGroup,workerGroup)
                    //设置通道类型
                    .channel(NioServerSocketChannel.class)
                    //向通道的中添加handler初始化器
                    .childHandler(new HttpChannelChannelInitializer())
                    .option(ChannelOption.SO_BACKLOG,64)
                    //设置子Socket的keepalive时间
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            //绑定端口号
            ChannelFuture cf = sboot.bind(8080).sync();
            cf.channel().closeFuture().sync();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        logger.info("【http   Server Start Ok】");
        HttpServer httpServer = new HttpServer();
        httpServer.run();

    }
}
