package com.swayingleaves.rabbitmqdemo.config;

import com.swayingleaves.rabbitmqdemo.service.impl.BatchMessageReceiver;
import com.swayingleaves.rabbitmqdemo.service.impl.SimpleMessageReceiver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * rabbitmq 配置类
 * @author zhenglin
 */
@Configuration
@Slf4j
public class RabbitConfig {

    @Value("${spring.rabbitmq.topic}")
    private List<String> routingKeys;
    @Value("${spring.rabbitmq.dead-letter}")
    private List<String> deadLetters;

    @Autowired
    AmqpAdmin rabbitAdmin;
    @Autowired
    ConfigurableApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        if (CollectionUtils.isEmpty(routingKeys)) {
            log.info("rabbitmq配置routingKeys为空");
            return;
        }
        //去重
        routingKeys = routingKeys.stream().distinct().collect(Collectors.toList());
        deadLetters = deadLetters.stream().distinct().collect(Collectors.toList());
        //先初始化死信队列
        build(deadLetters);
        //初始化正常队列并绑定死信队列
        build(routingKeys);
        //动态创建MessageListenerContainer
        buildMessageListenerContainer();
    }


    private void build(List<String> rsdRoutingKeys) {
        int size = rsdRoutingKeys.size();
        for (int i = 0; i < size; i++) {
            String routingKey = rsdRoutingKeys.get(i);
            if (StringUtils.isBlank(routingKey)) {
                log.info("routingKey为空");
                continue;
            }
            String rsdExchangeName = RabbitMqStates.RSD_EXCHANGE_NAME;
            Map<String, Object> args = new HashMap<>(2);
            if (!routingKey.contains("DEAD")) {
                // x-dead-letter-exchange 这里声明当前队列绑定的死信交换机
                args.put(RabbitMqStates.DEAD_LETTER_EXCHANGE, RabbitMqStates.DEAD_RSD_EXCHANGE_NAME);
                // x-dead-letter-routing-key 这里声明当前队列的死信路由key
                //这里可以根据topic的值设置绑定不同的死信队列，这里我把所有的都绑在一个上，具体看实际应用
                args.put(RabbitMqStates.DEAD_LETTER_ROUTING_KET, deadLetters.get(0));
            } else {
                rsdExchangeName = RabbitMqStates.DEAD_RSD_EXCHANGE_NAME;
            }
            Queue queue = new Queue(routingKey, true, false, false, args);
            Exchange exchange = new DirectExchange(rsdExchangeName, true, false);
            Binding binding = new Binding(queue.getName(), Binding.DestinationType.QUEUE, exchange.getName(), routingKey, null);
            rabbitAdmin.declareQueue(queue);
            rabbitAdmin.declareExchange(exchange);
            rabbitAdmin.declareBinding(binding);
            log.info("{}/{} routingKey for {} 创建成功", i + 1, size, routingKey);
        }
    }

    /**
     * 通过spring动态创建SimpleMessageListenerContainer
     * 否则多个topic使用一个处理类在批量时会混合接收
     */
    private void buildMessageListenerContainer() {
        BatchMessageReceiver listener = applicationContext.getBean(BatchMessageReceiver.class);
        ConnectionFactory connectionFactory = applicationContext.getBean(ConnectionFactory.class);

        for (String routingKey : routingKeys) {
            String beanName = routingKey + "_SimpleMessageListenerContainer";
            //如果已经存在跳过
            if (applicationContext.containsBean(beanName)) {
                continue;
            }
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleMessageListenerContainer.class);
            //设置构造参数
            beanDefinitionBuilder.addConstructorArgValue(connectionFactory);
            //设置属性值
            beanDefinitionBuilder.addPropertyValue("queues", routingKey);
            beanDefinitionBuilder.addPropertyValue("exposeListenerChannel", true);
            beanDefinitionBuilder.addPropertyValue("prefetchCount", 100);
            beanDefinitionBuilder.addPropertyValue("batchSize", 50);
            beanDefinitionBuilder.addPropertyValue("consumerBatchEnabled", true);
            beanDefinitionBuilder.addPropertyValue("maxConcurrentConsumers", 1);
            beanDefinitionBuilder.addPropertyValue("acknowledgeMode", AcknowledgeMode.MANUAL);
            beanDefinitionBuilder.addPropertyValue("messageListener", listener);

            BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
            BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
            //注册
            beanFactory.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    /**
     * 不是批量的方式创建SimpleMessageListenerContainer
     * @param listener
     * @param connectionFactory
     * @return
     * @throws AmqpException
     * @throws IOException
     */
    @Bean
    public SimpleMessageListenerContainer mqMessageContainer(SimpleMessageReceiver listener, ConnectionFactory connectionFactory) throws AmqpException, IOException {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(routingKeys.toArray(new String[]{}));
        container.setExposeListenerChannel(true);
        //设置每个消费者获取的最大的消息数量
        container.setPrefetchCount(100);
        container.setBatchSize(50);
        //设置批量消费
        container.setConsumerBatchEnabled(true);
        //消费者个数
        container.setConcurrentConsumers(1);
        //设置确认模式为手工确认
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        //监听处理类
        container.setMessageListener(listener);
        return container;
    }
}