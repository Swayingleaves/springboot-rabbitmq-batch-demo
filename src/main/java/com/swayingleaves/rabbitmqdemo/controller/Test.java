package com.swayingleaves.rabbitmqdemo.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhenglin
 * @date 2021/7/6
 */
@RestController
@RequestMapping("/test")
public class Test {

    @Autowired
    RabbitTemplate rabbitTemplate;


    @Autowired
    AmqpAdmin rabbitAdmin;

    @GetMapping("/send")
    public String test(String msg,String topic){
        Map<String,String> map = new HashMap<>();
        map.put("msg",msg);
        rabbitTemplate.convertAndSend(topic, JSONObject.toJSONString(map));
        return "ok";
    }

    @GetMapping("/del")
    public String del(String topic){
        String[] split = topic.split(",");
        for (String s : split) {
            rabbitAdmin.deleteQueue(s);
        }
        return "ok";
    }
}
