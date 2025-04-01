package com.oingmaryho.business.delivery_service.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY); // __TypeId__ 설정
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Value("${message.order.exchange}")
    private String orderExchange;

    @Value("${message.queue.delivery}")
    private String queueDelivery;

    @Value("${message.queue.order}")
    private String queueOrder;

    @Value("${message.queue.deliveryManager}")
    private String queueDeliveryManager;

    @Value("${message.deliveryManager.exchange}")
    private String deliveryManagerExchange;


    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange);
    }

    @Bean
    public TopicExchange deliveryManagerExchange() {
        return new TopicExchange(deliveryManagerExchange);
    }

    @Bean
    public Queue queueDelivery() {
        return new Queue(queueDelivery, true);
    }

    @Bean
    public Queue queueOrder() {
        return new Queue(queueOrder, true);
    }

    @Bean
    public Binding bindingOrder() {
        return BindingBuilder.bind(queueOrder()).to(orderExchange()).with(queueOrder);
    }

    @Bean
    public Queue queueDeliveryManager() {
        return new Queue(queueDeliveryManager, true);
    }

    @Bean
    public Binding bindingDeliveryManager() {
        return BindingBuilder.bind(queueDeliveryManager()).to(deliveryManagerExchange()).with(queueDeliveryManager);
    }



}