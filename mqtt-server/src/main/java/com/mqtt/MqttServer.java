package com.mqtt;


import com.mqtt.config.SystemConfiguration;
import com.mqtt.connection.ConnectionFactory;
import com.mqtt.initializer.MqttChannelChannelInitializer;
import com.mqtt.manager.SessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @Author: chihaojie
 * @Date: 2020/1/1 12:39
 * @Version 1.0
 * @Note
 */
public class MqttServer {


    private static final Logger logger = LoggerFactory.getLogger(MqttServer.class);

    public MqttServer() {

    }

    public void run() throws Exception{
        EventLoopGroup bossGroup=new NioEventLoopGroup(1);
        NioEventLoopGroup  workerGroup=new NioEventLoopGroup(128);
        try{
            //实例化session工厂和connection工厂
            SessionManager sessionManager=new SessionManager();
            ConnectionFactory connectionFactory=new ConnectionFactory();
            ServerBootstrap sboot=new ServerBootstrap();
            sboot.group(bossGroup,workerGroup)
                    //设置通道类型
                    .channel(NioServerSocketChannel.class)
                    //向通道的中添加handler初始化器
                    .childHandler(new MqttChannelChannelInitializer(sessionManager,connectionFactory))
                    .option(ChannelOption.SO_BACKLOG,1024)
                    //设置子Socket的keepalive时间
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            //绑定端口号
            Integer port=Integer.valueOf(SystemConfiguration.INSTANCE.getPort());
            ChannelFuture cf = sboot.bind(port).sync();
            System.out.println("Broker initiated...");
            sboot.bind(port).addListeners(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    System.out.println("=========绑定完成==============");
                    //
                }
            });
            cf.channel().closeFuture().sync();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        logger.info("【Mqtt Broker  Server Start Ok】");
        MqttServer mqttServer = new MqttServer();
        mqttServer.run();

    }
}
