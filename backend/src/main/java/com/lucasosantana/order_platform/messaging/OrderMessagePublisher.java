package com.lucasosantana.order_platform.messaging;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.lucasosantana.order_platform.config.RabbitMQConfig;
import com.lucasosantana.order_platform.exception.MessagePublishException;

@Component
public class OrderMessagePublisher {

	private static final Logger log = LoggerFactory.getLogger(OrderMessagePublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final Duration confirmTimeout;

	public OrderMessagePublisher(
			RabbitTemplate rabbitTemplate,
			@Value("${app.messaging.publish-confirm-timeout}") Duration confirmTimeout) {
		this.rabbitTemplate = rabbitTemplate;
		this.confirmTimeout = confirmTimeout;
	}

	public void publishOrderCreated(OrderCreatedMessage message) {
		var correlation = new CorrelationData(message.orderId().toString());
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.ORDERS_EXCHANGE, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY, message, correlation);
		awaitConfirmation(correlation, message);
		log.info("Pedido {} publicado na fila {}", message.orderId(), RabbitMQConfig.ORDER_PROCESSING_QUEUE);
	}

	private void awaitConfirmation(CorrelationData correlation, OrderCreatedMessage message) {
		CorrelationData.Confirm confirm;
		try {
			confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new MessagePublishException("Publicação do pedido %s interrompida.".formatted(message.orderId()), ex);
		}
		catch (ExecutionException | TimeoutException ex) {
			throw new MessagePublishException(
					"Sem confirmação do broker para o pedido %s.".formatted(message.orderId()), ex);
		}

		if (!confirm.ack()) {
			throw new MessagePublishException(
					"Broker recusou a mensagem do pedido %s: %s".formatted(message.orderId(), confirm.reason()));
		}
		if (correlation.getReturned() != null) {
			throw new MessagePublishException(
					"Mensagem do pedido %s não foi roteada para nenhuma fila.".formatted(message.orderId()));
		}
	}

}
