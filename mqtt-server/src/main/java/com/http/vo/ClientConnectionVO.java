package com.http.vo;

import lombok.Data;

import java.util.Date;

/**
 * @Author: chihaojie
 * @Date: 2020/1/16 15:25
 * @Version 1.0
 * @Note
 */
@Data
public class ClientConnectionVO {

    private  String clientId;

    private  String  username;

    private  String  password;

    private String   ip;

    private String  port;

    private String  protocolVersion;

    private Date connectedDate;

}
