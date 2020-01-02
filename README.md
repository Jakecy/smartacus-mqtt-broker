# smartacus-mqtt-broker
 
 > 欢迎志同道合的小伙伴一起探讨交流

## mqtt协议介绍

![smartacus-d32e5588af75d38f8b0720fc7523961c.jpg](https://upload-images.jianshu.io/upload_images/13084796-c3bd679152b1abff.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

随着工业的快速发展，对设备进行管理和数据采集的需求也越来越高。要解决大量设备的通信问题造就了
物联网(IOT internet of Things)发展。

在这样的网络中，设备之间通过不同接口和各种协议进行彼此交互。要实现工业上的IOT的概念，我们就要了解
Iot的概念。要实现这些设备在复杂网络环境中的通信问题，就引出了很多协议，其中MQTT是最流行的一种。

MQTT（Message Queue Telemetry Transport）是一个轻量级开放的消息协议，用它进行传输，只需要很少的网络带宽即可。

### mqtt协议的特点

. 异步协议

. 压缩消息

. 可以在不稳定的网络环境中进行良好的数据传输

. 支持服务质量Quality Of Service (Qos) 

. 易于新设备的接入





## 计划功能列表
   
 -计划功能列表：
 
|预期功能|难度|功能描述                             |
|:-----   |:------|:----------------------------- |
|客户端登录session   | 中   |xxxx  |
|cleanSession   | 中   |xxxx  |
|支持遗嘱消息  | 中   |xxxx  |
|日志整合   | 中   |xxxx  |
|消息持久化   | 中   |xxxx  |
|支持十万级客户端  | 中   |xxxx  |
|PING报文实现上下线检测   | 中   |xxxx  |
|实现CONNECT报文  | 易 | xxxx|
| 实现SUBSCRIBE报文  | 易 | xxxx|
| 实现PUBLISH报文  | 难 | xxxx|
|Qos服务质量保证  | 难 | xxxx|
|发布订阅模式  | 中 | xxxx|
| 支持auth2认证  | 中   |xxxx  |
| 服务启动、停止、重启  | 中   |xxxx  |
| 多项目自由配置 | 中 | xxxx|
| 支持消息持久化  | 易 | xxxx|



###  已实现的功能列表
|预期功能|难度|完成程度                          |
|:-----   |:------|:----------------------------- |
|PING报文实现上下线检测   | 易   |已完成  |
|实现CONNECT报文  | 易 | 已完成|
|实现CONNECTACK报文  | 易 | 已完成|
| 实现SUBSCRIBE报文  | 易 | 已完成|
| 实现SUBSCRIBEACK报文  | 易 | 已完成|
| 实现PUBLISH报文  | 难 | 已完成|
| 实现PUBLISHACK报文  | 难 | 已完成|
|支持Qos=1服务质量 | 中 | 已完成|
|支持Qos=2服务质量 | 难 | 已完成|
|发布订阅模式  | 中 | 已完成|




##  实现和设计思路


##  实现方案


## 实际完成功能

ping功能已实现 

connect功能已实现 

publish消息发送和转发功能已实现(尚未考虑转发时的断连问题)

publishAck功能已实现

subscribe功能已实现

subAck功能已实现

unsub和unsubAck已实现

Qos1级别的消息已实现

Broker作为接收端处理Qos2级别的消息-已实现

Broker作为发送端处理Qos2级别的消息

Qos2级别的消息已全部实现 


## 关于mqtt协议Qos2服务质量等级

#### publish报文

##### publish报文的发生方向
 发生的方向是：双向的，既可以从客户端发往服务器端，又可以从服务器端发往客户端。

客户端使用 PUBLISH 报文发送应用消息给服务端， 目的是分发到其它订阅匹配的客户端。
服务端使用 PUBLISH 报文发送应用消息给每一个订阅匹配的客户端。

##### publish报文的结构
```
固定报头：
   1、固定报头中的重发标志dup：
              如果是第一次发送此报文，则置dup=0
              如果是重发报文， 则置dup=1
  2、服务质量等级
               Qos=0
               Qos=1
               Qos=2
  3、保留标志
         保留标志的可选值：  retain=1    retain=0


可变报头：
  1、主题名 topicName
  2、报文标识符，只有当Qos=1或2时，pub报文中才有报文标识符
有效载荷
      由应用指定的数据

```


######  Qos1级别的publish报文

 服务质量Qos1会确保消息至少送达一次。Qos1的publish报文的可变报头中包含一个报文标识符，需要pubAck报文确认。

 对于Qos1的分发协议，发送者：

    - 每次发送新的应用消息都分配一个未使用的报文标识符

    - 发送的publish报文必须包含报文标识符且Qos等于1，dup等于0。

    - 必须将这个publish报文看作是未确认的，直到从接收者那里收到对应的pubAck报文

对于Qos1的分发协议的接收者：

  - 响应的pubAck报文必须包含一个报文标识符，这个标识符来自接收到的，已经接收所有权的
    publish报文

  - 发送pubAck报文之后，接收者必须将任何包含相同报文标识符的入站publish报文当作一个新的
    消息，并忽略它的DUP标志的值

###### Qos1的协议的交互流程图

下面这张图是：Qos1的交互流程图：
![mqttQos1报文交互 (3).jpg](https://upload-images.jianshu.io/upload_images/13084796-ec5d4d0efbb2fb16.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

###### Qos1级别的pub报文的缺陷

  ###### 消息重复的问题

  在上图中，我们可以看到，对于发送方来说，当它发送一个pub报文时，它会一直等待着来自对方(接收方)的消息确认报文pubAck。只有收到pubAck报文之后，它才认为该pub报文已经发送成功，否则，就会执行重试。
  即： 再次发送该pub报文给客户端直到收到pubAck确认已经发生成功为止。

  但是，这里有个问题，那就是： 如果接收方已经收到了pub报文，而且它也把pubAck报文发送出去了，但是由于网络状况太差或者其他原因，导致了这个pubAck报文丢失了，发送方接收不到来自接收方的pubAck确认报文。
于是，发送方就会对此pub报文进行二次重发，这样的话，接收方就又收到了一条和原来一模一样的pub报文，实际上由于接收方之前就已经收到并处理过该报文了，所以这次报文对接收方来说，就是一条重复报文。

可见，导致重复消息的原因就是： pubAck报文在网络传输中丢失造成的。


##### Qos2解决Qos1的重复消息问题

  Qos2是最高的服务质量等级，消息丢失和重复都是不可接收的。但是使用这个服务质量等级会有额外的开销。
###### 两步确认过程

   Qos2的pub报文的接收者使用一个两步确认过程来确认收到。

  ###### 发送方和接收方的处理

  对于Qos2的发送方来说：

      . 必须要给要发送的新应用消息分配一个未使用的报文标识符。

      . 发送的pub报文必须包含报文标识符且报文的Qos等于2，dup等于0.

      . 必须将这个pub报文看作是未确认的，直到从接收者那收到pubRec报文。

      . 收到pubRec报文后必须发送一个pubRel报文，pubRel报文必须包含与原始的pub报文相同的报文标识符

      . 必须将这个pubRel报文看作是未确认的，直到从接收者那收到对应的pubComp报文

      . 一旦发送了对应的pubRel报文就不能重发这个publish报文了。


  对于Qos2的接收者来说：

    . 响应的pubRec报文必须包含报文标识符，这个标识符来自接收到的、已经接收所有权的publish报文。

    . 在收到对应的pubRel报文之前，接收者必须发送pubRec报文确认任何后续的具有相同标识符的pub报文。

    . 响应的pubRel报文的pubComp报文必须包含与pubRel报文相同的报文标识符

    . 发送pubComp报文之后，接收者必须将包含相同报文标识符的任何后续的pub报文当做一个新的发布。  

###### Qos2的交互流程

![mqttQos2报文交互 .jpg](https://upload-images.jianshu.io/upload_images/13084796-6751a005e009a751.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


***我们知道，Qos1的消息重复问题的根源在于：pubAck报文的丢失导致。***

所以，如果我们要解决消息的重复问题，就必须致力于解决 pubAck报文的丢失问题，在Qos2中，对应的就是pubRec报文的丢失问题。
即：确保发送方一定可以收到来自接收方的pubRec报文。所以这里就引出了：2步确认过程。

```
step1:  第一次使用应答机制来对pub报文进行确认，从而确保接收方一定可以接收到pub报文。

step2:  第二次使用应答机制来对pubRec报文进行确认，从而确保发送方一定可以接收到pubRec报文。
```
可见：

>  1、pubRec 是对pub的保证，确保pub不会丢失
    2、pubRel 是pubRec的保证，确保pubRec不会丢失
    3、pubComp是pubRel的保证，确保pubRel不会丢失


***
 如果sender没有收到pubRec，就要重发该pub。

如果receiver没有收到pubRel，就会重发pubRec。

如果sender没有接收到pubComp，就会重发pubRel。

***

* 对于接收方来说，收到pubRel报文之后，它的使命就完成了。(即： 它不仅收到了pub消息，而且确信自己也已把pubRec报文送到了发送方。)

* 对于发送方来说，收到了pubRec报文，就意味着，pub报文已经被接收方确定接收了。收到了pubComp报文，就意味着，pubRel报文已经确定被接收方接收了。


### 关于IdleStateHandler的使用说明

  如果要了解IdleStateHandler的使用说明，请查看我的博客地址：
  [IdleStateHandler](https://www.jianshu.com/p/19bee119004f)



## 更多

