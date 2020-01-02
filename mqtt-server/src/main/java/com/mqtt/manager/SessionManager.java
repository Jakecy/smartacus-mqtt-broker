package com.mqtt.manager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: chihaojie
 * @Date: 2020/1/2 13:40
 * @Version 1.0
 * @Note   服务端的session管理器
 */
public class SessionManager {

    //定义一个锁
    private transient  final Lock sessionManagerLock=new ReentrantLock();

    //利用conCurrentHashMap来管理客户端的登录session
    private final ConcurrentHashMap<String ,ClientSession>  serverSessionTable= new ConcurrentHashMap<String ,ClientSession>(1024);


    public SessionManager() {

    }

    /**
     * 把客户端登录session放入sessionManger
     */
 public     Boolean putClientSession(ClientSession clientSession){
         ClientSession old = serverSessionTable.put(clientSession.getClientId(), clientSession);
         if(null !=old){
             System.out.println("update old clientSession,the old session is "+old.getClientId());
         }else {
             System.out.println("create new  clientSession,the new  session is: "+ clientSession.getClientId());
         }
         return true;
     }


    /**
     * 从sessionManager中移除clientSession
     */
    public  Boolean removeClientSession(String clientId){
        ClientSession remove = serverSessionTable.remove(clientId);
        if(null !=remove){
            System.out.println(" the client session is removed from sessionManager ,the session is : "+ clientId);
        }else {
            System.out.println(" failed to remove the client sessionsss  ,the session is : "+ clientId);
        }
        return  true;
    }

}
