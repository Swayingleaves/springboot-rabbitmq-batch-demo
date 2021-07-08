package com.swayingleaves.rabbitmqdemo.service;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 死信队列处理
 *
 * @author zhenglin
 * @date 2021/7/6
 */
@Component
@Slf4j
public class DeadLetterMessageReceiver {

    @RabbitListener(queues = "${spring.rabbitmq.dead-letter}")
    public void receiveSource(Message message, Channel channel) throws IOException {
        log.info("收到死信消息source：" + new String(message.getBody(), StandardCharsets.UTF_8));
        //TODO 这里应该是正常处理放入私信队列的数据后才能确认，否则死信队列的消息就丢失了
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
