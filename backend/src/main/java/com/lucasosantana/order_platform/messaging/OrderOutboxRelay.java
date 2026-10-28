package com.lucasosantana.order_platform.messaging;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lucasosantana.order_platform.exception.MessagePublishException;
import com.lucasosantana.order_platform.model.OrderOutbox;
import com.lucasosantana.order_platform.repository.OrderOutboxRepository;

@Component
public class OrderOutboxRelay {

	private static final Logger log = LoggerFactory.getLogger(OrderOutboxRelay.class);

	private final OrderOutboxRepository outboxRepository;
	private final OrderMessagePublisher publisher;
	private final int batchSize;

	public OrderOutboxRelay(
			OrderOutboxRepository outboxRepository,
			OrderMessagePublisher publisher,
			@Value("${app.outbox.batch-size}") int batchSize) {
		this.outboxRepository = outboxRepository;
		this.publisher = publisher;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${app.outbox.relay-interval}")
	@Transactional
	public void publishPending() {
		for (OrderOutbox entry : outboxRepository.lockPending(batchSize)) {
			try {
				publisher.publishOrderCreated(new OrderCreatedMessage(entry.getOrderId()));
				entry.markPublished(Instant.now());
			}
			catch (MessagePublishException | AmqpException ex) {
				entry.markFailed(ex.getMessage());
				log.warn("Falha ao publicar o pedido {} (tentativa {}): {}",
						entry.getOrderId(), entry.getAttempts(), ex.getMessage());
				return;
			}
		}
	}

}
