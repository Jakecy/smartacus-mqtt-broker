package com.http.handler;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.http.message.Content;
import com.http.result.Result;
import com.http.vo.ClientConnectionVO;
import com.http.vo.SubClient;
import com.http.vo.TopicVO;
import com.mqtt.connection.ClientConnection;
import com.mqtt.connection.ConnectionFactory;
import com.mqtt.connection.PostMan;
import com.mqtt.message.ClientSub;
import com.mqtt.utils.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: chihaojie
 * @Date: 2020/1/16 11:38
 * @Version 1.0
 * @Note
 */
@ChannelHandler.Sharable
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {



    private List<String> uriMapping=Arrays.asList("/test","/dashboard");

    private static  final String  DASHBOARD="/dashboard";

    private static  final String  CONNECTIONS="/connections";

    private static  final String  TOPICS="/topics";




    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        System.out.println("=========Http请求到来=======");
        System.out.println(req.uri());
        try{
            String uri = req.uri();
            //请求方法
            HttpMethod method = req.method();
            //请求头
            HttpHeaders headers = req.headers();
            //请求体
            ByteBuf content = req.content();
            String reqBody=StrUtil.ByteBuf2String(content);
            if(CONNECTIONS.equals(uri) && method.equals(HttpMethod.GET)){
                ConcurrentHashMap<String, ClientConnection> connectionFactory = ConnectionFactory.connectionFactory;
                getConnections(ctx,connectionFactory);
            }
            if(TOPICS.equals(uri) && method.equals(HttpMethod.GET)){
                 getTopics(ctx,req);
            }
        }catch (Exception  e){
            e.printStackTrace();
        }finally {
            
        }
    }




    private void response(ChannelHandlerContext ctx, Object res) {
        // 1.设置响应
       Result result= new Result<Object>().ok(res);
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(JSONObject.toJSONString(result), CharsetUtil.UTF_8));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        // 2.发送
        // 注意必须在使用完之后，close channel
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    private void getConnections(ChannelHandlerContext ctx, ConcurrentHashMap<String,ClientConnection> connectionFactory) {
        ArrayList<ClientConnectionVO> vos = new ArrayList<>();
        if(null !=connectionFactory && !connectionFactory.isEmpty()){
            connectionFactory.forEach((k,v)->{
                ClientConnectionVO  vo=new ClientConnectionVO();
                vo.setClientId(v.getClientId());
                vo.setUsername(v.getUsername());
                vo.setIp(v.getIp());
                vo.setPort(v.getPort());
                vo.setConnectedDate(v.getConnectedDate());
                vo.setProtocolVersion(v.getProtocolVersion());
                vo.setPassword(v.getPassword());
                vos.add(vo);
            });
        }
        // 1.设置响应
        Result result= new Result<Object>().ok(vos);
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(JSONObject.toJSONString(result), CharsetUtil.UTF_8));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        // 2.发送
        // 注意必须在使用完之后，close channel
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);

    }

    private void getTopics(ChannelHandlerContext ctx, FullHttpRequest req) {
        ArrayList<TopicVO> vos = new ArrayList<>();
        ConcurrentMap<String, List<ClientSub>> topicSubers = PostMan.topicSubers;
        if(null !=topicSubers && !topicSubers.isEmpty()){
            topicSubers.forEach((k,v)->{
                TopicVO  vo=new TopicVO();
                vo.setTopic(k);
                List<SubClient>  scList= Lists.newArrayList();
                if(!v.isEmpty()){
                    v.forEach(e->{
                        SubClient client=new SubClient();
                        client.setClientId(e.getClientId());
                        client.setQos(e.getSubQos());
                        scList.add(client);
                    });
                }
                vo.setClientList(scList);
                vos.add(vo);
            });
        }
        // 1.设置响应
        Result result= new Result<Object>().ok(vos);
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(JSONObject.toJSONString(result), CharsetUtil.UTF_8));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        // 2.发送
        // 注意必须在使用完之后，close channel
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
}
