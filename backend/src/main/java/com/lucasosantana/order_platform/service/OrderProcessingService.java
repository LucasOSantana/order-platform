package com.lucasosantana.order_platform.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.lucasosantana.order_platform.exception.TransientProcessingException;
import com.lucasosantana.order_platform.model.Order;
import com.lucasosantana.order_platform.repository.OrderRepository;

@Service
public class OrderProcessingService {

	private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

	private static final String REJECTED_KEYWORD = "falha";
	private static final String UNSTABLE_KEYWORD = "instavel";

	private final OrderRepository orderRepository;
	private final TransactionTemplate transaction;
	private final Duration delay;
	private final BigDecimal maxTotalAmount;

	public OrderProcessingService(
			OrderRepository orderRepository,
			TransactionTemplate transaction,
			@Value("${app.orders.processing.delay}") Duration delay,
			@Value("${app.orders.processing.max-total-amount}") BigDecimal maxTotalAmount) {
		this.orderRepository = orderRepository;
		this.transaction = transaction;
		this.delay = delay;
		this.maxTotalAmount = maxTotalAmount;
	}

	public void process(UUID orderId) {
		Order order = transaction.execute(status -> start(orderId));
		if (order == null) {
			return;
		}

		sleep(delay);
		String failureReason = validate(order);

		finish(orderId, failureReason);
	}

	public void failAfterRetries(UUID orderId, String reason) {
		finish(orderId, reason);
	}

	private Order start(UUID orderId) {
		var order = orderRepository.findById(orderId).orElse(null);
		if (order == null || order.isFinished()) {
			log.info("Mensagem do pedido {} ignorada: pedido inexistente ou já finalizado", orderId);
			return null;
		}
		order.startAttempt(Instant.now());
		Hibernate.initialize(order.getItems());
		log.info("Pedido {} em processamento (tentativa {})", orderId, order.getAttempts());
		return order;
	}

	private void finish(UUID orderId, String failureReason) {
		transaction.executeWithoutResult(status -> {
			var order = orderRepository.findById(orderId).orElseThrow();
			if (order.isFinished()) {
				return;
			}
			if (failureReason == null) {
				order.complete(Instant.now());
				log.info("Pedido {} concluído", orderId);
			}
			else {
				order.fail(failureReason, Instant.now());
				log.warn("Pedido {} falhou: {}", orderId, failureReason);
			}
		});
	}

	// Regras simuladas de falha
	private String validate(Order order) {
		var descriptions = order.getItems().stream().map(item -> item.getDescription().toLowerCase()).toList();
		if (descriptions.stream().anyMatch(description -> description.contains(UNSTABLE_KEYWORD))) {
			throw new TransientProcessingException("Falha simulada na comunicação com o serviço de estoque.");
		}
		if (order.getTotalAmount().compareTo(maxTotalAmount) > 0) {
			return String.format(Locale.of("pt", "BR"), "Valor do pedido acima do limite de R$ %,.2f.", maxTotalAmount);
		}
		return order.getItems().stream()
				.filter(item -> item.getDescription().toLowerCase().contains(REJECTED_KEYWORD))
				.findFirst()
				.map(item -> "Item recusado na validação de negócio: %s.".formatted(item.getDescription()))
				.orElse(null);
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Processamento interrompido", ex);
		}
	}

}
