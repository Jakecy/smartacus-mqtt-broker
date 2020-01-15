package com.mqtt.handler;

import com.alibaba.fastjson.JSONObject;
import com.mqtt.MqttServer;
import com.mqtt.common.ChannelAttributes;
import com.mqtt.config.UsernamePasswordAuth;
import com.mqtt.connection.ClientConnection;
import com.mqtt.connection.ConnectionFactory;
import com.mqtt.group.ClientGroup;
import com.mqtt.group.ClientGroupManager;
import com.mqtt.manager.ClientSession;
import com.mqtt.manager.SessionManager;
import com.mqtt.message.ClientSub;
import com.mqtt.utils.CompellingUtil;
import com.mqtt.utils.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Ref;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mqtt.common.ChannelAttributes.ATTR_KEY_CONNECTION;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;

/**
 * 待处理错误：
 * https://my.oschina.net/LucasZhu/blog/1799075
 *
 * io.netty.util.IllegalReferenceCountException: refCnt: 0
 *         at io.netty.handler.codec.mqtt.MqttPublishMessage.content(MqttPublishMessage.java:49)
 *         at io.netty.handler.codec.mqtt.MqttPublishMessage.payload(MqttPublishMessage.java:42)
 *         at com.mqtt.handler.MqttBrokerHandler.lambda$null$19(MqttBrokerHandler.java:681)
 *         at java.util.Optional.ifPresent(Optional.java:159)
 *         at com.mqtt.handler.MqttBrokerHandler.lambda$handleQos1PubMsg$20(MqttBrokerHandler.java:677)
 */



/**
 * @Author: chihaojie
 * @Date: 2020/1/1 12:47
 * @Version 1.0
 * @Note
 */
@ChannelHandler.Sharable
public class MqttBrokerHandler extends ChannelInboundHandlerAdapter {



    private static final Logger logger = LoggerFactory.getLogger(MqttBrokerHandler.class);



    //private final static ConcurrentMap<String, Channel> pool = new ConcurrentHashMap<>();

    //使用原子性的AtomicInteger作为packetId
    private final AtomicInteger lastPacketId = new AtomicInteger(1);

    //创建一个订阅队列，
    //每个主题对应的客户端
    private final  static ConcurrentMap<String ,List<ClientSub>>  topicMapClient=new ConcurrentHashMap<>();
    //每个客户端订阅的所有主题
    private final  static  ConcurrentMap<String ,List<String>>  clientSubedTopics=new ConcurrentHashMap<>();

    //retained msg Queue
    private final  static ConcurrentMap<String ,MqttPublishMessage>  retainedPubMsgQueue=new ConcurrentHashMap<>();

    //ConcurrentLinkedQueue  vs
    //等待重发的报文
    Queue<MqttPublishMessage> pendingAckPubMsgs = new ConcurrentLinkedQueue<MqttPublishMessage>();

    //Qos2报文实现
    //pubRec等待队列
    //pubRel等待队列
    //pubComp等待队列
    //作为发送端


    // private final static MongoClient client = MongoClients.create(
    //         new ConnectionString("mongodb://121.199.16.77:27017/?maxPoolSize=20"));
    //作为接收端
    //pubRel等待队列
    Queue<MqttPubAckMessage> pendingRelPubMsgs = new ConcurrentLinkedQueue<MqttPubAckMessage>();

    Queue<MqttPublishMessage> notCompletePubMsgs = new ConcurrentLinkedQueue<MqttPublishMessage>();

    //作为发送端处理Qos2级别的消息
    Queue<MqttPublishMessage> notCompletePubMsgsSenderEnd = new ConcurrentLinkedQueue<MqttPublishMessage>();

    Queue<MqttPublishMessage> pendingRecPubMsgs = new ConcurrentLinkedQueue<MqttPublishMessage>();

    //pubComp等待队列
    Queue<MqttPubAckMessage> pendingCompPubMsgs = new ConcurrentLinkedQueue<MqttPubAckMessage>();

    private final SessionManager   sessionManager;

    private final ConnectionFactory  connectionFactory;

    public MqttBrokerHandler(SessionManager sessionManager, ConnectionFactory connectionFactory) {
        this.sessionManager = sessionManager;
        this.connectionFactory=connectionFactory;
    }




    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //socket建立之后，build connection
        ClientConnection connection=  connectionFactory.create(ctx.channel(),sessionManager,connectionFactory);
        ctx.channel().attr(ATTR_KEY_CONNECTION).set(connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //判断当前连接所属的群组
        //对该群组成员发送下线消息
        String clientId = CompellingUtil.getClientId(ctx.channel());
        String groupId = CompellingUtil.getGroupId(ctx.channel());
        //群发下线消息
        ClientGroupManager.sendOffLineGroupMessage(groupId);
        connectionFactory.removeConnection(clientId);
        ctx.channel().close().addListener(CLOSE_ON_FAILURE);
    }


    private final  static  ConcurrentMap<String ,List<String>>  waitAckMsgs=new ConcurrentHashMap<>();

    Queue<MqttPublishMessage> alreadyAckedMsgs = new ConcurrentLinkedQueue<MqttPublishMessage>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
        //判断是否是mqtt协议
        if(!(o instanceof MqttMessage)){
            ctx.close();
            return;
        }
        MqttMessage mqttMessage = (MqttMessage) o;
        ClientConnection connection = ctx.channel().attr(ATTR_KEY_CONNECTION).get();
        try {
            connection.handleMqttMessage(mqttMessage);
        }catch (Exception e){
                 e.printStackTrace();
            logger.error(" exception hanppened while process mqttmessage, message is : {}, exception is : {} ",mqttMessage,e);
            ctx.channel().close().addListeners(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.error(" Close the client Channel due to Exception ");
                }
            });
        }finally {
            ReferenceCountUtil.safeRelease(mqttMessage);
        }

    }



    /**
     * 捕捉IdleStateEvent事件，并处理
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            //如果发送读空闲的话，就发送一个ping消息，如果5秒内接收不到回复，就关闭连接
            IdleStateEvent  idleEvent=(IdleStateEvent)evt;
            if(idleEvent.state().equals(IdleState.READER_IDLE)){
                this.sendPingReq(ctx.channel());
                ctx.channel().eventLoop().schedule(()->{
                    ClientConnection connection = connectionFactory.getConnection(CompellingUtil.getClientId(ctx.channel()));
                    if(connection!=null){
                        Long interval=(connection.getSendMessageLastestTime()-System.currentTimeMillis())/1000;
                        if(interval>2){
                            ctx.close().addListener(CLOSE_ON_FAILURE);
                        }                        }
                    //ctx.close().addListener(CLOSE_ON_FAILURE);
                },1,TimeUnit.SECONDS);
            }else if(idleEvent.state().equals(IdleState.WRITER_IDLE)) {
                //当写空闲时，就发送ping消息给对端
                MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
                ctx.writeAndFlush(new MqttMessage(fixedHeader));
            }

        }else   ctx.fireUserEventTriggered(evt);//把事件往下传递

    }

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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
