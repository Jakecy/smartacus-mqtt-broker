package com.mqtt.handler;

import com.alibaba.fastjson.JSONObject;
import com.mqtt.common.ChannelAttr;
import com.mqtt.config.UsernamePasswordAuth;
import com.mqtt.model.ClientSubModel;
import com.mqtt.utils.CompellingUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;

/**
 * @Author: chihaojie
 * @Date: 2020/1/1 12:47
 * @Version 1.0
 * @Note
 */
public class MqttBrokerHandler extends ChannelInboundHandlerAdapter {

    private static final String ATTR_CONNECTION = "connectionList";
    private static final AttributeKey<Object> ATTR_KEY_CONNECTION = AttributeKey.valueOf(ATTR_CONNECTION);




    private final static ConcurrentMap<String, Channel> pool = new ConcurrentHashMap<>();

    //使用原子性的AtomicInteger作为packetId
    private final AtomicInteger lastPacketId = new AtomicInteger(1);

    //创建一个订阅队列，
    private final  static ConcurrentMap<String ,List<ClientSubModel>>  subQueue=new ConcurrentHashMap<>();

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



    private ConcurrentHashMap<String, Channel> sessionChannelMap = new ConcurrentHashMap<String, Channel>();




    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //发送ping消息
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        ctx.writeAndFlush(new MqttMessage(fixedHeader));

    }

    /**
     * 服务端能接收到的报文类型有：
     *   CONNECT、PUBLISH、PUBACK、
     *   PUBREC、PUBREL、PUBCOMP、
     *   SUBSCRIBE、UNSUBSCRIBE、PINGREQ、
     *   DISCONNECT、
     * @param ctx
     * @param o
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
        MqttMessage mqttMessage = (MqttMessage) o;
        switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT:
                handleClientConnectMessage(ctx,mqttMessage);
                break;
            case PINGREQ:
                handleClientPingReqMessage(ctx,mqttMessage);
                break;
            case SUBSCRIBE:
                handleClientSubscribeMessage(ctx,(MqttSubscribeMessage) mqttMessage);
                break;
            case UNSUBSCRIBE:
                handleClientUNSubMessage(ctx,(MqttUnsubscribeMessage) mqttMessage);
                break;
            case PUBLISH:
                handleClientPublishMessage(ctx,mqttMessage);
                break;
            case PUBACK:
                handleClientPubAckMessage(ctx,(MqttPubAckMessage) mqttMessage);
                break;
            case PUBREC:
                handleClientPubRecMessage(ctx,mqttMessage);
                break;
            case PUBREL:
                handleClientPubRelMessage(ctx,mqttMessage);
                break;
            case PUBCOMP:
                handleClientPubCompMessage(ctx, mqttMessage);
                break;
            case DISCONNECT:
                handleClientDisconnectMessage(ctx,mqttMessage);
                break;
            default:
                System.out.println("Unexpected message type: " + mqttMessage.fixedHeader().messageType());
                ReferenceCountUtil.release(mqttMessage);
                ctx.close();//这句话会把同client端建立的socket关闭
        }
        //处理完之后，把消息释放掉
        //ReferenceCountUtil.release(mqttMessage);
        // ctx.close();//这句话的作用是，close the Channel,如果server主动关闭同客户端的socket
    }




    private void handleClientPubAckMessage(ChannelHandlerContext ctx, MqttPubAckMessage mqttMessage) {
        //处理确认消息
        System.out.println("=============Broker收到pubAck报文=========");
        System.out.println(mqttMessage.toString());
        System.out.println("=====================此时的待确认pub消息队列=======================");
        System.out.println(JSONObject.toJSONString(pendingAckPubMsgs));
        //从队列中移除此待确认报文
        while (!pendingAckPubMsgs.isEmpty()){
            MqttPublishMessage firstMsg = pendingAckPubMsgs.poll();
            System.out.println("=================取出的待确认报文==============");
            System.out.println(firstMsg.toString());
            Boolean match=firstMsg.variableHeader().packetId()==mqttMessage.variableHeader().messageId() ? true : false;
            System.out.println("==================是否匹配==================");
            System.out.println(match);
            if(match){
                return;
            }
            pendingAckPubMsgs.offer(firstMsg);
        }

    }

    private void handleClientUNSubMessage(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMessage) {
        //取消订阅的报文
        System.out.println("=============Broker收到UNSUB报文=========");
        System.out.println(mqttMessage.toString());
        //把该客户端的订阅从订阅列表中移除
        MqttUnsubscribePayload payload = mqttMessage.payload();
        List<String> topics = payload.topics();
        if(topics==null || topics.isEmpty()){
            ctx.close();
        }
        Attribute<String> clientIdAttr = ctx.channel().attr(ChannelAttr.ATTR_KEY_CLIENTID);
        String clientId = clientIdAttr.get();
        topics.forEach(t->{
            List<ClientSubModel> subModelList = subQueue.get(t);
            if(subModelList!=null && !subModelList.isEmpty()){
                //找到并移除
                Integer index=null;
                for (int i = 0; i <subModelList.size() ; i++) {
                    if(clientId.equals(subModelList.get(i).getClientId())){
                        index=i;
                    }
                }
                //移除
                if(index!=null){
                    subModelList.remove(index);
                }
            }
            //重新放入队列中
            subQueue.put(t,subModelList);
        });

        //构造报文
        MqttFixedHeader  unsubAckFixed=new MqttFixedHeader(MqttMessageType.UNSUBACK,
                false,
                MqttQoS.AT_MOST_ONCE,
                false,
                2);
        MqttMessageIdVariableHeader  unsubAckVh=MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().messageId());
        //
        MqttUnsubAckMessage  unsubAckMessage=new MqttUnsubAckMessage(unsubAckFixed,unsubAckVh);
        //返回响应
        ctx.writeAndFlush(unsubAckMessage);

    }

    private void handleClientSubscribeMessage(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMessage) {
        System.out.println("=============Broker收到SUB报文=========");
        System.out.println(mqttMessage.toString());
        //TODO 收到sub的处理动作：
        //1、 把此订阅放入到订阅队列中
        //2、为此订阅选择合适的Qos
        //3、为此订阅响应subAck报文
        MqttMessageIdVariableHeader subVarHeader = mqttMessage.variableHeader();
        System.out.println("================订阅消息的可变头=================");
        System.out.println(subVarHeader.toString());
        MqttSubscribePayload subscribePayload = mqttMessage.payload();
        List<MqttTopicSubscription> mqttTopicSubscriptions = subscribePayload.topicSubscriptions();
        //订阅队列的内容
        System.out.println("================订阅队列的内容================");
        System.out.println(JSONObject.toJSONString(subQueue));
        //授予的权限值
        List<Integer>  grantedSubQos=new ArrayList<>(5);
        //遍历
        Optional.ofNullable(mqttTopicSubscriptions).ifPresent(mts->{
            mqttTopicSubscriptions.forEach(sub->{
                Attribute<String> clientId = ctx.channel().attr(ChannelAttr.ATTR_KEY_CLIENTID);
                //record the client's subed topic queue
                recordSubedTopicQueueForClient(clientId,sub);
                List<ClientSubModel> subModelList = subQueue.get(sub.topicName());
                System.out.println("==============主题："+sub.topicName()+"的订阅列表==========");
                System.out.println(JSONObject.toJSONString(subModelList));
                grantedSubQos.add(sub.qualityOfService().value());
                if(subModelList==null){
                    subModelList=new ArrayList<>(20);
                    ClientSubModel csm=new ClientSubModel();

                    csm.setClientId(clientId.get());
                    csm.setSubTopic(sub.topicName());
                    csm.setSubQos(sub.qualityOfService());
                    subModelList.add(csm);
                    subQueue.put(sub.topicName(),subModelList);
                }else {
                    List<ClientSubModel> oldTopicSub = subQueue.get(sub.topicName());
                    System.out.println("==============老主题："+sub.topicName()+"的订阅列表==========");
                    System.out.println(JSONObject.toJSONString(oldTopicSub));
                    //新加入
                    ClientSubModel csm=new ClientSubModel();
                    csm.setClientId(clientId.get());
                    csm.setSubTopic(sub.topicName());
                    csm.setSubQos(sub.qualityOfService());
                    subModelList.add(csm);
                    subQueue.put(sub.topicName(),subModelList);

                }
            });
            //发送subAck响应
            MqttFixedHeader  subAckFixedHeader=new MqttFixedHeader(MqttMessageType.SUBACK,
                    false,MqttQoS.AT_LEAST_ONCE,false,0);
            //把sub报文中的messageId取出，然后使用它构造一个subAck的可变报头
            MqttMessageIdVariableHeader subAckVHeader = MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().messageId());
            //这里设定为： 请求多大给多大
            MqttSubAckPayload payload = new MqttSubAckPayload(grantedSubQos);
            MqttSubAckMessage subAckMessage = new MqttSubAckMessage(subAckFixedHeader, subAckVHeader, payload);
            ctx.writeAndFlush(subAckMessage);
            deliveryRetainedPubMsgToSuber(ctx,mqttMessage);
        });
    }

    private void recordSubedTopicQueueForClient(Attribute<String> clientId, MqttTopicSubscription sub) {
        List<String> clientTopicList = clientSubedTopics.get(clientId.get());
        if(clientTopicList==null || clientTopicList.isEmpty()){
            clientTopicList=new ArrayList<>();
            clientTopicList.add(sub.topicName());
        }else {
            clientTopicList.add(sub.topicName());
        }
        clientSubedTopics.put(clientId.get(),clientTopicList);
    }

    private void deliveryRetainedPubMsgToSuber(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMessage) {
        // detect if the topic exists a retained pub msg
        Attribute<String> clientId = ctx.channel().attr(ChannelAttr.ATTR_KEY_CLIENTID);
        //
        List<String> topicList = clientSubedTopics.get(clientId.get());
        Optional.ofNullable(topicList).ifPresent(tl->{
            topicList.forEach(topic->{
                MqttPublishMessage publishMessage = retainedPubMsgQueue.get(topic);
                if(publishMessage!=null){
                    ctx.writeAndFlush(publishMessage);
                }
            });
        });


    }

    private void handleClientDisconnectMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        System.out.println("=============收到DISCONNECT报文=========");
        System.out.println("=======关闭与此客户端的连接===========");
        //TODO 服务器收到disconnect报文之后，要执行俩个动作
        //1、丢弃与此连接关联的所有消息
        //2、理解关闭此连接
        //3、从连接列表中，移除此连接
        ctx.close();
        String clientId = CompellingUtil.getClientId(ctx.channel());
        pool.remove(clientId);
    }

    /**
     * 处理客户端发来的publish消息
     * @param ctx
     * @param mqttMessage
     */
    private void handleClientPublishMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        //对publish消息进行存储并进行后续处理
        MqttPublishMessage  pubMsg= (MqttPublishMessage) mqttMessage;
        //handle retained msg
        handleRetainedPubMsg(ctx,pubMsg);
        storeMqttMessage(pubMsg);
        System.out.println("================处理publish消息===========");
        System.out.println(pubMsg.toString());
        //TODO 判断Qos等级，分等级处理
        MqttQoS mqttQoS = mqttMessage.fixedHeader().qosLevel();
        switch (mqttQoS){
            case AT_MOST_ONCE:
                handleQos0PubMsg(ctx, pubMsg);
                break;
            case AT_LEAST_ONCE:
                handleQos1PubMsg(ctx, pubMsg);
                break;
            case EXACTLY_ONCE:
                handleQos2PubMsg(ctx, pubMsg);
                break;
            default:
                System.out.println("Unexpected Qos level: " + mqttQoS);
                ReferenceCountUtil.release(mqttMessage);
                ctx.close();
        }


    }

    private void handleQos2PubMsg(ChannelHandlerContext ctx, MqttPublishMessage pubMsg) {
        //处理Qos2的消息
        //TODO
        //放入
        notCompletePubMsgs.add(pubMsg);
        final int messageID = pubMsg.variableHeader().packetId();
        //1、 收到pub消息之后，立即发送pubRec，并把此pubRec放入到等待Rel队列
        MqttFixedHeader recFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false,
                MqttQoS.AT_LEAST_ONCE, false, 2);
        MqttPubAckMessage pubRecMessage = new MqttPubAckMessage(recFixedHeader, from(messageID));
        ctx.writeAndFlush(pubRecMessage);
        //把此消息放入待Rel队列中
        pendingRelPubMsgs.offer(pubRecMessage);
        //优化，减少无效的轮询消耗
        if(!pendingRelPubMsgs.isEmpty()){
            ctx.channel().eventLoop().scheduleAtFixedRate(()->{
                retryPubRecWhenNoRel(ctx,pendingRelPubMsgs);
            },2,2,TimeUnit.SECONDS);
        }
        //section 2： forward pub message to dest
        sendPublishToDest(ctx,  pubMsg);
    }

    private void sendPublishToDest(ChannelHandlerContext ctx, MqttPublishMessage pubMsg) {
        System.out.println("============作为发送端处理Qos2级别的消息============");
        //转发publish消息
        notCompletePubMsgsSenderEnd.offer(pubMsg);
        Channel targetChannel = pool.get(pubMsg.variableHeader().topicName());
        //
        MqttFixedHeader pubFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false,
                MqttQoS.EXACTLY_ONCE, true, 0);
        MqttPublishVariableHeader publishVariableHeader=new MqttPublishVariableHeader(pubMsg.variableHeader().topicName(),lastPacketId.intValue());
        MqttPublishMessage  pubMsg2=new MqttPublishMessage(pubFixedHeader,publishVariableHeader,pubMsg.payload());
        Optional.ofNullable(targetChannel).ifPresent(tc->{
            targetChannel.writeAndFlush(pubMsg2);
        });
        //放入Rec等待队列中
        pendingRecPubMsgs.offer(pubMsg2);
        ctx.channel().eventLoop().scheduleAtFixedRate(()->{
            //重发pub消息
            reSendPubFromRecQueue(pendingRecPubMsgs);
        },2,2,TimeUnit.SECONDS);
    }

    private void reSendPubFromRecQueue(Queue<MqttPublishMessage> pendingRecPubMsgs) {
        //重发pub消息
        ArrayList<MqttPublishMessage> pendingRecList = new ArrayList<>();
        while (!pendingRecPubMsgs.isEmpty()) {
            MqttPublishMessage poll = pendingRecPubMsgs.poll();
            if (poll != null) {
                MqttFixedHeader pubFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, true,
                        MqttQoS.EXACTLY_ONCE, true, 0);
                MqttPublishVariableHeader publishVariableHeader = new MqttPublishVariableHeader(poll.variableHeader().topicName(), lastPacketId.intValue());
                MqttPublishMessage resendPub = new MqttPublishMessage(pubFixedHeader, publishVariableHeader, poll.payload());
                Channel channel = pool.get(resendPub.variableHeader().topicName());
                if (channel != null) {
                    channel.writeAndFlush(resendPub);
                    pendingRecList.add(resendPub);
                }
            }
        }
        //重新放入
        if (!pendingRecList.isEmpty()) {
            pendingRecList.forEach(rel -> {
                pendingRecPubMsgs.offer(rel);
            });
        }
    }

    private void handleClientPubRecMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        System.out.println("=================Broker收到pubRec报文================");
        //TODO
        //收到rec报文之后，应该：
        //1、从rec队列中移除某个pub消息
        //2、及时返回Rel
        //3、把当前Rel放入待comp中
        ArrayList<MqttPublishMessage> pendingPubMsgList=new ArrayList<>();
        final int messageId = ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId();
        while (!pendingRecPubMsgs.isEmpty()){
            MqttPublishMessage head = pendingRecPubMsgs.poll();
            if(messageId!=head.variableHeader().packetId()){
                pendingPubMsgList.add(head);
            }
        }
        pendingPubMsgList.forEach(pub->{
            pendingRecPubMsgs.offer(pub);
        });
        //响应Rel
        MqttFixedHeader recFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false,
                MqttQoS.EXACTLY_ONCE, false, 2);
        MqttPubAckMessage pubRelMessage = new MqttPubAckMessage(recFixedHeader, from(messageId));
        ctx.writeAndFlush(pubRelMessage);
        System.out.println("==========响应Rel================");
        System.out.println(pubRelMessage.toString());
        //把当前Rel放入待comp中
        pendingCompPubMsgs.offer(pubRelMessage);
        ctx.channel().eventLoop().scheduleAtFixedRate(()->{
            //重发待comp的Rel报文
            retryPubRelOnNoCompAck(pendingCompPubMsgs);
        },2,2,TimeUnit.SECONDS);
    }

    private void retryPubRelOnNoCompAck(Queue<MqttPubAckMessage> pendingCompPubMsgs) {
        ArrayList<MqttPubAckMessage> pendingCompList=new ArrayList<>();
        while (!pendingCompPubMsgs.isEmpty()){
            MqttPubAckMessage noCompRel = pendingCompPubMsgs.poll();
            MqttPublishMessage oldPub = retrievePubmsgAccoringRel(notCompletePubMsgsSenderEnd, noCompRel);
            MqttFixedHeader recFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, true,
                    MqttQoS.AT_LEAST_ONCE, false, 2);
            MqttPubAckMessage pubRelMessage = new MqttPubAckMessage(recFixedHeader, from(oldPub.variableHeader().packetId()));
            Channel channel = pool.get(oldPub.variableHeader().topicName());
            if(channel!=null){
                channel.writeAndFlush(pubRelMessage);
            }
        }
        //重新放入
        if(!pendingCompList.isEmpty()){
            pendingCompList.forEach(rel->{
                pendingCompPubMsgs.offer(rel);
            });
        }
    }

    private void retryPubRecWhenNoRel(ChannelHandlerContext ctx, Queue<MqttPubAckMessage> pendingRelPubMsgs) {
        //收不到rel的时候重发PubRec
        while (!pendingRelPubMsgs.isEmpty()){
            //取出该消息id
            //notCompletePubMsgs
            //根据packetId查询pub消息
            MqttPublishMessage oldPub = retrievePubmsgAccoringRel(notCompletePubMsgs, pendingRelPubMsgs.poll());
            MqttFixedHeader recFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false,
                    MqttQoS.AT_LEAST_ONCE, false, 2);
            MqttPubAckMessage pubRecMessage = new MqttPubAckMessage(recFixedHeader, from(oldPub.variableHeader().packetId()));
            Channel channel = pool.get(oldPub.variableHeader().topicName());
            if(channel!=null){
                channel.writeAndFlush(pubRecMessage);
            }
        }
    }

    private void handleClientPubRelMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        System.out.println("======================收到Rel报文=============================");
        System.out.println(mqttMessage.toString());
        //收到rel报文之后
        final int messageId = ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId();
        //TODO
        //1、从pubRel等待队列中移除Rec报文
        //2、发送pubComp报文
        //3、此pub消息的处理流程完毕，移除此pub消息
        ArrayList<MqttPubAckMessage> pendingRelList=new ArrayList<>();
        MqttFixedHeader recFixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false,
                MqttQoS.AT_LEAST_ONCE, false, 2);
        MqttPubAckMessage pubCompMessage = new MqttPubAckMessage(recFixedHeader, from(messageId));
        ctx.writeAndFlush(pubCompMessage);
        //移除pub
        removePubMsgFromNotCompletePubMsg(notCompletePubMsgs,mqttMessage);
        while(!pendingRelPubMsgs.isEmpty()){
            MqttPubAckMessage recMsg = pendingRelPubMsgs.poll();
            if(messageId!=recMsg.variableHeader().messageId()){
                pendingRelList.add(recMsg);
            }
        }
        //重新放入
        if(!pendingRelList.isEmpty()){
            pendingRelList.forEach(rel->{
                pendingRelPubMsgs.offer(rel);
            });
        }
    }

    private void removePubMsgFromNotCompletePubMsg(Queue<MqttPublishMessage> notCompletePubMsgs, MqttMessage mqttMessage) {
        ArrayList<MqttPublishMessage> notComplete=new ArrayList<>();
        //遍历
        final int messageId = ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId();
        while (!notCompletePubMsgs.isEmpty()) {
            MqttPublishMessage publishMessage = notCompletePubMsgs.poll();
            if(publishMessage!=null ||  publishMessage.variableHeader().packetId() !=messageId){
                notComplete.add(publishMessage);
            }
        }
        //
        //重新放入
        if(!notComplete.isEmpty()){
            notComplete.forEach(pub->{
                notCompletePubMsgs.offer(pub);
            });
        }
    }

    private MqttPublishMessage retrievePubmsgAccoringRel(Queue<MqttPublishMessage> notCompletePubMsgs, MqttPubAckMessage poll) {
        ArrayList<MqttPublishMessage> notComplete=new ArrayList<>();
        //遍历
        while (!notCompletePubMsgs.isEmpty()) {
            int messageId = poll.variableHeader().messageId();
            MqttPublishMessage publishMessage = notCompletePubMsgs.poll();
            if(publishMessage!=null && publishMessage.variableHeader().packetId()==messageId){
                notComplete.add(publishMessage);
                return publishMessage;
            }
        }
        //重新放入
        if(!notComplete.isEmpty()){
            notComplete.forEach(pub->{
                notCompletePubMsgs.offer(pub);
            });
        }
        return null;
    }

    private void handleClientPubCompMessage(ChannelHandlerContext ctx, MqttMessage pubComp) {
        System.out.println("=========================broker收到pubComp消息==========================");
        System.out.println(pubComp.toString());
        //TODO
        //收到pubComp之后
        //1、移除pubRel等待队列中的对应消息
        //2、释放此pubComp对应的消息
        ArrayList<MqttPubAckMessage> pendingCompList=new ArrayList<>();
        MqttMessageIdVariableHeader subVarHeader = (MqttMessageIdVariableHeader) pubComp.variableHeader();
        int messageId =subVarHeader.messageId();
        while(!pendingCompPubMsgs.isEmpty()){
            MqttPubAckMessage relMsg = pendingCompPubMsgs.poll();
            if(messageId!=relMsg.variableHeader().messageId()){
                pendingCompList.add(relMsg);
            }
        }
        //重新放入
        if(!pendingCompList.isEmpty()){
            pendingCompList.forEach(rel->{
                pendingRelPubMsgs.offer(rel);
            });
        }
        //移除pub
        removePubMsgFromNotCompletePubMsg(notCompletePubMsgs,pubComp);
    }

    private void handleQos0PubMsg(ChannelHandlerContext ctx, MqttPublishMessage pubMsg) {
        System.out.println("=============处理Qos0的消息==============");
        System.out.println(JSONObject.toJSONString(pool));
        Channel targetChannel = pool.get(pubMsg.variableHeader().topicName());
        System.out.println(JSONObject.toJSONString(targetChannel));
        Optional.ofNullable(targetChannel).ifPresent(tc->{
            MqttFixedHeader pubFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false,
                    MqttQoS.AT_LEAST_ONCE, true, 0);
            MqttPublishVariableHeader publishVariableHeader=new MqttPublishVariableHeader(pubMsg.variableHeader().topicName(),lastPacketId.intValue());
            MqttPublishMessage  pubMsg2=new MqttPublishMessage(pubFixedHeader,publishVariableHeader,pubMsg.payload());
            targetChannel.writeAndFlush(pubMsg2);
            System.out.println("==============Qos0转发消息===========");
            System.out.println(pubMsg2);
        });

    }

    private void handleQos1PubMsg(ChannelHandlerContext ctx, MqttPublishMessage pubMsg) {
        //写回一个publishAck
        MqttFixedHeader pubAckFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false,
                MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader =MqttMessageIdVariableHeader.from(lastPacketId.get()) ;//MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().packetId());
        ByteBuf payload = Unpooled.buffer(200);
        payload.writeBytes("服务端发送的响应测试消息".getBytes());
        MqttPubAckMessage    pubAck=  new MqttPubAckMessage(pubAckFixedHeader, variableHeader);
        ctx.writeAndFlush(pubAck);
        //取出publish消息
        System.out.println("=============池中的链接有==============");
        System.out.println(JSONObject.toJSONString(pool));
        System.out.println("==============publish消息的topic是===========");
        System.out.println(pubMsg.variableHeader().topicName());
        Channel targetChannel = pool.get(pubMsg.variableHeader().topicName());
        System.out.println("================处理publish消息,转发===========");
        System.out.println("============目标channel===========");
        System.out.println(JSONObject.toJSONString(targetChannel));
        Optional.ofNullable(targetChannel).ifPresent(tc->{
            MqttFixedHeader pubFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false,
                    MqttQoS.AT_LEAST_ONCE, true, 0);
            MqttPublishVariableHeader publishVariableHeader=new MqttPublishVariableHeader(pubMsg.variableHeader().topicName(),lastPacketId.intValue());
            MqttPublishMessage  pubMsg2=new MqttPublishMessage(pubFixedHeader,publishVariableHeader,pubMsg.payload());
            targetChannel.writeAndFlush(pubMsg2);
            //把此消息放入待确认队列中
            if(pubMsg2.fixedHeader().qosLevel().equals(MqttQoS.AT_LEAST_ONCE)){
                pendingAckPubMsgs.offer(pubMsg2);
            }

        });
        System.out.println("=====================待确认pub消息队列=======================");
        System.out.println(JSONObject.toJSONString(pendingAckPubMsgs));
        EventLoop loop =ctx.channel().eventLoop();
        //对没有收到pubAck的报文进行重发
        loop.scheduleAtFixedRate(()->{
            //
            System.out.println("===============使用eventLoop进行任务调度==========");
            retryPubMsgWhenNoAck(pendingAckPubMsgs);
        },0,2,TimeUnit.SECONDS);
    }

    private void retryPubMsgWhenNoAck(Queue<MqttPublishMessage> pendingAckPubMsgs) {
        //
        System.out.println("=============执行任务调度=============");
        while (!pendingAckPubMsgs.isEmpty()){
            //repub
            MqttPublishMessage oldPubMsg = pendingAckPubMsgs.poll();
            MqttFixedHeader pubFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, true,
                    MqttQoS.AT_LEAST_ONCE, false, 0);
            MqttPublishVariableHeader publishVariableHeader=new MqttPublishVariableHeader(oldPubMsg.variableHeader().topicName(),oldPubMsg.variableHeader().packetId());
            MqttPublishMessage  rePub=new MqttPublishMessage(pubFixedHeader,publishVariableHeader,oldPubMsg.payload());
            //进行发送
            Channel channel = pool.get(oldPubMsg.variableHeader().topicName());
            if(channel!=null){
                channel.writeAndFlush(rePub);
            }
        }

    }

    /**
     * A retained message is a normal MQTT message with the retained flag set to true.
     * The broker stores the last retained message and the corresponding QoS for that topic. Each client that subscribes to a topic pattern that matches the topic of the retained message receives the retained message immediately after they subscribe.
     * The broker stores only one retained message per topic.
     * @param ctx
     * @param pubMsg
     */
    private void handleRetainedPubMsg(ChannelHandlerContext ctx, MqttPublishMessage pubMsg) {
        //
        MqttFixedHeader fixedHeader = pubMsg.fixedHeader();
        MqttPublishVariableHeader variableHeader = pubMsg.variableHeader();
        if(fixedHeader.isRetain()){
            // store the msg & Qos
            retainedPubMsgQueue.put(variableHeader.topicName(),pubMsg);
        }
    }

    private void storeMqttMessage(MqttPublishMessage pubMsg) {
       /* MqttPublishMessageDTO  pub=new MqttPublishMessageDTO();
        pub.setFixedHeader(pubMsg.fixedHeader().toString());
        pub.setVariableHeader(pubMsg.variableHeader().toString());
        pub.setPayload(pubMsg.toString());
        //把消息入库
        MongoDatabase database = client.getDatabase("mqtt");
        MongoCollection<Document> collection = database.getCollection("publish_message");
        Document  doc=Document.parse(JSONObject.toJSONString(pub));
        doc.append("_id",RandomUtils.randomInt());
        collection.insertOne(doc);*/
    }

    /**
     * 处理客户端发来的pingReq报文
     */
    private void handleClientPingReqMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        ctx.writeAndFlush(new MqttMessage(fixedHeader));
    }

    /**
     * 处理client发来的connect报文
     * @param ctx
     * @param mqttMessage
     */
    private void handleClientConnectMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        System.out.println("===========connect的消息=============");
        System.out.println(mqttMessage.toString());
        //TODO 收到connect连接报文之后，服务端应做出如下动作：
        /**
         * 1、长时间没收到connect报文，就关闭连接
         * 2、验证connect报文是否符合要求
         * 3、从connect报文中取出用户名和密码进行验证
         * 4、把此连接的clientId放入到channel的attach中
         */
        //把此消息转换为connect报文
        MqttConnectMessage  connectMsg=(MqttConnectMessage)mqttMessage;
        //取出固定头和可变头以及有效载荷
        MqttConnectVariableHeader connectVariableHeader = connectMsg.variableHeader();
        MqttConnectPayload connectPayload = connectMsg.payload();
        System.out.println(JSONObject.toJSONString(pool));
        System.out.println(connectPayload.clientIdentifier());
        //pool.putIfAbsent(connectPayload.clientIdentifier(),ctx.channel());
        pool.put(connectPayload.clientIdentifier(),ctx.channel());
        //把此连接的clientId放入到channel的attach中
        ctx.channel().attr(ChannelAttr.ATTR_KEY_CLIENTID).set(connectPayload.clientIdentifier());
        //1、从connectVariableHeader里读取 连接标志,共有6个连接标志
        //User Name Flag (1) 用户名标志
        //Password Flag (1) 密码标志
        //Will Retain (0) Will 保留标志
        //Will QoS (01) Will 服务质量
        //Will Flag (1) Will 标志
        //Clean Session (1) 清理会话
        boolean userNameFlag = connectVariableHeader.hasUserName();
        boolean passwordFlag = connectVariableHeader.hasPassword();
        boolean willRetainFlag = connectVariableHeader.isWillRetain();
        boolean willFlagFlag = connectVariableHeader.isWillFlag();
        int willQos = connectVariableHeader.willQos();
        boolean cleanSessionFlag = connectVariableHeader.isCleanSession();
        if(UsernamePasswordAuth.auth){
            if(userNameFlag&& passwordFlag){
                //用户名和密码的校验
                String userName = connectPayload.userName();
                ByteBuf buf = Unpooled.wrappedBuffer(connectPayload.passwordInBytes());
                String password =buf.toString(CharsetUtil.UTF_8);
                if(!(UsernamePasswordAuth.username.equals(userName) &&UsernamePasswordAuth.password.equals(password))){
                    ctx.close();//用户名密码不对拒绝连接
                }
            }else {
                ctx.close();//关闭非法连接
            }
        }
        MqttFixedHeader  fixedHeader=new MqttFixedHeader(MqttMessageType.CONNACK,false,MqttQoS.AT_LEAST_ONCE,false,2);
        MqttConnAckVariableHeader  connAckVheader=new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED,true);
        MqttConnAckMessage connAckMessage=new MqttConnAckMessage(fixedHeader,connAckVheader);
        ctx.writeAndFlush(connAckMessage);
        //ReferenceCountUtil.release(mqttMessage);
        System.out.println("服务端返回给客户端connack响应");
        System.out.println(connAckMessage.toString());

    }

    private void handlePublishMsg(Channel channel, MqttPublishMessage mqttMessage) {
        //对publish消息进行存储并进行后续处理
        System.out.println("================处理publish消息===========");
        //写回一个publishAck
        MqttFixedHeader pubAckFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false,
                MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader =MqttMessageIdVariableHeader.from(1) ;//MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().packetId());
        ByteBuf payload = Unpooled.buffer(200);
        payload.writeBytes("服务端发送的响应测试消息".getBytes());
        MqttPubAckMessage    pubAck=  new MqttPubAckMessage(pubAckFixedHeader, variableHeader);
        channel.writeAndFlush(pubAck);
        //取出publish消息
        System.out.println("===============池中的链接有====================");
        System.out.println(JSONObject.toJSONString(pool));
        Channel targetChannel = pool.get(mqttMessage.variableHeader().topicName());
        MqttFixedHeader pubFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false,
                MqttQoS.AT_MOST_ONCE, false, 0);
        MqttPublishVariableHeader publishVariableHeader=new MqttPublishVariableHeader("TEST",1);
        MqttPublishMessage  pubMsg=new MqttPublishMessage(pubFixedHeader,publishVariableHeader,payload);
        targetChannel.writeAndFlush(pubMsg);
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
                System.out.println("..........readerIdleTimeOut,will close the channel ...... ");
                ctx.close();
            }else if(idleEvent.state().equals(IdleState.WRITER_IDLE)) {
                //当写空闲时，就发送ping消息给对端
                //
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
