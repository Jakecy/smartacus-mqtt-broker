package com.http.handler;

import com.alibaba.fastjson.JSONObject;
import com.http.message.Content;
import com.mqtt.utils.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import javax.swing.text.AbstractDocument;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: chihaojie
 * @Date: 2020/1/16 11:38
 * @Version 1.0
 * @Note
 */
@ChannelHandler.Sharable
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {



    private List<String> uriMapping=Arrays.asList("/test","/dashboard");


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
            Content result=new Content();
            result.setContent("响应信息");
            response(ctx,result);
        }catch (Exception  e){
            e.printStackTrace();
        }finally {
            
        }
    }

    private void response(ChannelHandlerContext ctx, Content c) {
        // 1.设置响应
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(JSONObject.toJSONString(c), CharsetUtil.UTF_8));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        // 2.发送
        // 注意必须在使用完之后，close channel
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
}
