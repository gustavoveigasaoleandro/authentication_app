package com.example.Tecmed_App.Config.amqp;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AMQPConfiguration {

    @Bean
    public Queue QueueAuthorization(){
        return QueueBuilder.nonDurable("authorization.validation").deadLetterExchange("authorization.validation-dlx").build();
    }
    @Bean
    public DirectExchange ExAuthorization(){
        return ExchangeBuilder.directExchange("authorization.ex").build();
    }

    @Bean
    public DirectExchange ResponseExAuthorization(){
        return ExchangeBuilder.directExchange("authorization.response_ex").build();
    }
    @Bean
    public Queue QueueDlqAuthorization(){
        return QueueBuilder.nonDurable("authorization.validation-dlq").build();
    }

    @Bean
    public DirectExchange DlxAuthorization(){
        return ExchangeBuilder.directExchange("authorization.validation-dlx").build();
    }

    @Bean
    public Binding bindingAuthorization(){
        return BindingBuilder.bind(QueueAuthorization()).to(ExAuthorization()).with("");
    }
    @Bean
    public Binding bindingDlxAuthorization(){
        return BindingBuilder.bind(QueueDlqAuthorization()).to(DlxAuthorization()).with("");
    }
    @Bean
    public RabbitAdmin createrRabbitAdmin(ConnectionFactory conn) {
        return new RabbitAdmin( conn);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter){
        RabbitTemplate rabbitTemplate =  new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> inicializaAdmin(RabbitAdmin rabbitAdmin){
        return event -> rabbitAdmin.initialize();
    }
}
