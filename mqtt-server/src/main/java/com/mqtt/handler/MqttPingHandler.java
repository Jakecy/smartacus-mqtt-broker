package com.mqtt.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 12:45
 * @Version 1.0
 * @Note
 */
public class MqttPingHandler extends ChannelInboundHandlerAdapter {

    //keepalive时长
    private final int keepaliveSeconds;
    //等待ping的超时时间
    private ScheduledFuture<?> pingRespTimeout;

    public MqttPingHandler(int keepaliveSeconds) {
        this.keepaliveSeconds = keepaliveSeconds;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof MqttMessage)) {
            ctx.fireChannelRead(msg);
            return;
        }
        MqttMessage message = (MqttMessage) msg;
        if(message.fixedHeader().messageType() == MqttMessageType.PINGREQ){
            System.out.println("===========服务端收到了ping请求==========");
            this.handlePingReq(ctx.channel());
        } else if(message.fixedHeader().messageType() == MqttMessageType.PINGRESP){
            System.out.println("===========服务端收到了ping响应==========");
            this.handlePingResp();
        }else{
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        System.out.println("=============触发用户事件===========");
        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            switch(event.state()){
                case READER_IDLE:
                    this.handlePingReq(ctx.channel());
                    break;
                case WRITER_IDLE:
                    this.sendPingReq(ctx.channel());
                    break;
            }
        }
    }

    /**
     * 5秒钟收不到来自对方的ping，就关闭连接
     * @param channel
     */
    private void sendPingReq(Channel channel){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        channel.writeAndFlush(new MqttMessage(fixedHeader));

       /* if(this.pingRespTimeout == null){
            this.pingRespTimeout = channel.eventLoop().schedule(() -> {
                MqttFixedHeader fixedHeader2 = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
                channel.writeAndFlush(new MqttMessage(fixedHeader2)).addListener(ChannelFutureListener.CLOSE);
                //TODO: what do when the connection is closed ?
            }, this.keepaliveSeconds, TimeUnit.SECONDS);
        }*/
    }

    private void handlePingReq(Channel channel){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        channel.writeAndFlush(new MqttMessage(fixedHeader));
        System.out.println("================服务端发送ping给客户端============");
        System.out.println(fixedHeader.toString());
    }

    private void handlePingResp(){
        if(this.pingRespTimeout != null && !this.pingRespTimeout.isCancelled() && !this.pingRespTimeout.isDone()){
            this.pingRespTimeout.cancel(true);
            this.pingRespTimeout = null;
        }
    }

}
