package com.swayingleaves.rabbitmqdemo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * @author zhenglin
 */
@Service
@Slf4j
public class SimpleMessageReceiver implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        try {
            consume(message);
            //确认消息消费成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            //进入死信队列
            log.error("发生异常:" + e.getMessage());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    public void consume(Message message){
        //消费消息
        byte[] body = message.getBody();
        String msgStr = new String(body, StandardCharsets.UTF_8);
        JSONObject object = JSONObject.parseObject(msgStr);
        String consumerQueue = message.getMessageProperties().getConsumerQueue();
        log.info("{}:get msg:{}",consumerQueue,object);
    }
}