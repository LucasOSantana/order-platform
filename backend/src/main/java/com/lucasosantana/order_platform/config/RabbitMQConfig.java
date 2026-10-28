package com.lucasosantana.order_platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lucasosantana.order_platform.messaging.OrderCreatedMessage;

@Configuration
public class RabbitMQConfig {

	public static final String ORDERS_EXCHANGE = "orders.exchange";
	public static final String ORDER_PROCESSING_QUEUE = "orders.processing";
	public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

	public static final String ORDERS_DEAD_LETTER_EXCHANGE = "orders.dlx";
	public static final String ORDER_PROCESSING_DLQ = "orders.processing.dlq";
	public static final String ORDER_PROCESSING_DEAD_ROUTING_KEY = "order.processing.dead";

	@Bean
	DirectExchange ordersExchange() {
		return ExchangeBuilder.directExchange(ORDERS_EXCHANGE).durable(true).build();
	}

	@Bean
	Queue orderProcessingQueue() {
		return QueueBuilder.durable(ORDER_PROCESSING_QUEUE)
				.deadLetterExchange(ORDERS_DEAD_LETTER_EXCHANGE)
				.deadLetterRoutingKey(ORDER_PROCESSING_DEAD_ROUTING_KEY)
				.build();
	}

	@Bean
	Binding orderProcessingBinding(Queue orderProcessingQueue, DirectExchange ordersExchange) {
		return BindingBuilder.bind(orderProcessingQueue).to(ordersExchange).with(ORDER_CREATED_ROUTING_KEY);
	}

	@Bean
	DirectExchange ordersDeadLetterExchange() {
		return ExchangeBuilder.directExchange(ORDERS_DEAD_LETTER_EXCHANGE).durable(true).build();
	}

	@Bean
	Queue orderProcessingDeadLetterQueue() {
		return QueueBuilder.durable(ORDER_PROCESSING_DLQ).build();
	}

	@Bean
	Binding orderProcessingDeadLetterBinding(
			Queue orderProcessingDeadLetterQueue, DirectExchange ordersDeadLetterExchange) {
		return BindingBuilder.bind(orderProcessingDeadLetterQueue)
				.to(ordersDeadLetterExchange)
				.with(ORDER_PROCESSING_DEAD_ROUTING_KEY);
	}

	@Bean
	MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter(OrderCreatedMessage.class.getPackageName());
	}

}
