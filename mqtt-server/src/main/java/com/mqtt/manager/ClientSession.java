package com.mqtt.manager;

import lombok.Data;

import java.util.Date;

/**
 * @Author: chihaojie
 * @Date: 2019/12/30 14:50
 * @Version 1.0
 * @Note
 */

/**
 * mosquitee第一次提交：
 *  https://github.com/moquette-io/moquette/commit/422fbc4d2c644844d4886afd55c912626d1c4054
 */
@Data
public class ClientSession {

    private String clientId;


    private Long  sendMessageLastestTime; //接收最近一次报文的时间








}
