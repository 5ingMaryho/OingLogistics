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

    @Value("${message.queue.deliveryMessageCreation}")
    private String queueDeliveryMessageCreation;

    @Value("${message.deliveryMessageCreation.exchange}")
    private String deliveryMessageCreationExchange;

    @Value("${message.queue.hubDeliveryManager}")
    private String queueHubDeliveryManager;

    @Value("${message.hubDeliveryManager.exchange}")
    private String hubDeliveryManagerExchange;

    @Value("${message.queue.companyDeliveryManager}")
    private String queueCompanyDeliveryManager;

    @Value("${message.companyDeliveryManager.exchange}")
    private String companyDeliveryManagerExchange;


    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange);
    }
    @Bean
    public TopicExchange deliveryMessageCreationExchange() {
        return new TopicExchange(deliveryMessageCreationExchange);
    }
    @Bean
    public TopicExchange hubDeliveryManagerExchange() {
        return new TopicExchange(hubDeliveryManagerExchange);
    }
    @Bean
    public TopicExchange companyDeliveryManagerExchange() {
        return new TopicExchange(companyDeliveryManagerExchange);
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
    public Queue queueDeliveryMessageCreation() {
        return new Queue(queueDeliveryMessageCreation, true);
    }

    @Bean
    public Binding bindingDeliveryMessageCreation() {
        return BindingBuilder.bind(queueDeliveryMessageCreation()).to(deliveryMessageCreationExchange()).with(queueDeliveryMessageCreation);
    }
    @Bean
    public Queue queueHubDeliveryManager() {
        return new Queue(queueHubDeliveryManager, true);
    }

    @Bean
    public Binding bindingHubDeliveryManager() {
        return BindingBuilder.bind(queueHubDeliveryManager()).to(hubDeliveryManagerExchange()).with(queueHubDeliveryManager);
    }

    @Bean
    public Queue queueCompanyDeliveryManager() {
        return new Queue(queueCompanyDeliveryManager, true);
    }

    @Bean
    public Binding bindingCompanyDeliveryManager() {
        return BindingBuilder.bind(queueCompanyDeliveryManager()).to(companyDeliveryManagerExchange()).with(queueCompanyDeliveryManager);
    }


}