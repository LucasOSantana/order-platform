package com.lucasosantana.order_platform.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.lucasosantana.order_platform.config.RabbitMQConfig;
import com.lucasosantana.order_platform.service.OrderProcessingService;

@Component
public class OrderProcessingListener {

	private final OrderProcessingService processingService;

	public OrderProcessingListener(OrderProcessingService processingService) {
		this.processingService = processingService;
	}

	@RabbitListener(queues = RabbitMQConfig.ORDER_PROCESSING_QUEUE)
	public void onOrderCreated(OrderCreatedMessage message) {
		processingService.process(message.orderId());
	}

}
