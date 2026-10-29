package com.lucasosantana.order_platform.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;

import com.lucasosantana.order_platform.service.OrderProcessingService;

@Component
public class OrderProcessingRecoverer extends RejectAndDontRequeueRecoverer {

	private static final Logger log = LoggerFactory.getLogger(OrderProcessingRecoverer.class);

	private final OrderProcessingService processingService;
	private final MessageConverter messageConverter;

	public OrderProcessingRecoverer(OrderProcessingService processingService, MessageConverter messageConverter) {
		this.processingService = processingService;
		this.messageConverter = messageConverter;
	}

	@Override
	public void recover(Message message, Throwable cause) {
		var orderId = ((OrderCreatedMessage) messageConverter.fromMessage(message)).orderId();
		var reason = "Processamento falhou após todas as tentativas: "
				+ NestedExceptionUtils.getMostSpecificCause(cause).getMessage();
		log.error("Pedido {} enviado para a DLQ: {}", orderId, reason);
		processingService.failAfterRetries(orderId, reason);
		super.recover(message, cause);
	}

}
