package com.swayingleaves.rabbitmqdemo.service.impl;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量接收
 * @author zhenglin
 */
@Component
@Slf4j
public class BatchMessageReceiver implements ChannelAwareBatchMessageListener {


    @Override
    public void onMessageBatch(List<Message> messages, Channel channel) {
        if (messages.size() == 0) {
            return;
        }
        Message lastMsg = messages.get(0);
        try {
            //消费消息
            consume(messages);
            //确认消息消费成功
            channel.basicAck(lastMsg.getMessageProperties().getDeliveryTag(), true);
        } catch (Exception e) {
            //进入死信队列
            log.error("发生异常:" + e.getMessage());
            try {
                channel.basicNack(lastMsg.getMessageProperties().getDeliveryTag(), true, false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void consume(List<Message> messages){
        //消费消息
    }
}