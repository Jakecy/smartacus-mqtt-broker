package com.http.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author: chihaojie
 * @Date: 2020/1/16 18:12
 * @Version 1.0
 * @Note
 */
@Data
public class TopicVO {

    private String  topic;

    private List<SubClient> clientList;

}
