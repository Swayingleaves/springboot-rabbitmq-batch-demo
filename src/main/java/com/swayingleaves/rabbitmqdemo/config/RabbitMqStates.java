package com.swayingleaves.rabbitmqdemo.config;

/**
 * @author zhenglin
 * @date 2021/7/6
 */
public interface RabbitMqStates {
    String RSD_EXCHANGE_NAME = "rsdDirectExchange";
    String DEAD_RSD_EXCHANGE_NAME = "deadDirectExchange";
    String SOURCE_EXCHANGE_NAME = "sourceDirectExchange";

    String DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    String DEAD_LETTER_ROUTING_KET = "x-dead-letter-routing-key";

}
