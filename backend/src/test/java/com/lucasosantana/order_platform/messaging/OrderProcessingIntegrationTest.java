package com.lucasosantana.order_platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.lucasosantana.order_platform.TestcontainersConfiguration;
import com.lucasosantana.order_platform.config.RabbitMQConfig;

import tools.jackson.databind.json.JsonMapper;

// Fluxo completo: POST → outbox → relay → fila → consumidor → status final.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"app.outbox.relay-interval=100ms",
		"app.orders.processing.delay=50ms",
		"spring.rabbitmq.listener.simple.retry.initial-interval=50ms",
		"spring.rabbitmq.listener.simple.retry.max-interval=100ms",
		"spring.rabbitmq.listener.simple.retry.max-retries=2" })
@AutoConfigureMockMvc
class OrderProcessingIntegrationTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private JsonMapper jsonMapper;

	@BeforeEach
	void cleanUp() {
		jdbc.update("DELETE FROM orders");
		amqpAdmin.purgeQueue(RabbitMQConfig.ORDER_PROCESSING_DLQ, false);
	}

	@Test
	void completesValidOrder() throws Exception {
		var id = createOrder("Mouse sem fio", "89.90");

		awaitFinalStatus(id, "COMPLETED");

		var order = order(id);
		assertThat(order.get("attempts")).isEqualTo(1);
		assertThat(order.get("processed_at")).isNotNull();
		assertThat(order.get("failure_reason")).isNull();
		assertThat(history(id)).containsExactly("RECEIVED", "PROCESSING", "COMPLETED");
	}

	@Test
	void failsOrderRejectedByBusinessRuleWithoutRetrying() throws Exception {
		var id = createOrder("Produto com falha", "10.00");

		awaitFinalStatus(id, "FAILED");

		var order = order(id);
		assertThat(order.get("attempts")).isEqualTo(1);
		assertThat(order.get("failure_reason")).isEqualTo("Item recusado na validação de negócio: Produto com falha.");
		assertThat(history(id)).containsExactly("RECEIVED", "PROCESSING", "FAILED");
		assertThat(rabbitTemplate.receive(RabbitMQConfig.ORDER_PROCESSING_DLQ, 500)).isNull();
	}

	@Test
	void retriesTechnicalFailureThenMarksOrderAsFailedAndSendsMessageToDlq() throws Exception {
		var id = createOrder("Item instavel", "10.00");

		awaitFinalStatus(id, "FAILED");

		var order = order(id);
		assertThat(order.get("attempts")).isEqualTo(3);
		assertThat((String) order.get("failure_reason"))
				.startsWith("Processamento falhou após todas as tentativas")
				.contains("Falha simulada na comunicação com o serviço de estoque.");
		assertThat(history(id)).containsExactly("RECEIVED", "PROCESSING", "FAILED");
		assertThat(rabbitTemplate.receiveAndConvert(RabbitMQConfig.ORDER_PROCESSING_DLQ, 5000))
				.isEqualTo(new OrderCreatedMessage(id));
	}

	@Test
	void ignoresDuplicateMessageForFinishedOrder() throws Exception {
		var id = createOrder("Mouse sem fio", "89.90");
		awaitFinalStatus(id, "COMPLETED");
		var processedAt = order(id).get("processed_at");

		rabbitTemplate.convertAndSend(
				RabbitMQConfig.ORDERS_EXCHANGE, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY, new OrderCreatedMessage(id));

		await().during(Duration.ofSeconds(1)).atMost(TIMEOUT).untilAsserted(() -> {
			var order = order(id);
			assertThat(order.get("status")).isEqualTo("COMPLETED");
			assertThat(order.get("attempts")).isEqualTo(1);
			assertThat(order.get("processed_at")).isEqualTo(processedAt);
			assertThat(history(id)).hasSize(3);
		});
	}

	private UUID createOrder(String description, String unitPrice) throws Exception {
		var body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "customerName": "Ana Souza",
				  "customerEmail": "ana@email.com",
				  "items": [{ "description": "%s", "quantity": 1, "unitPrice": %s }]
				}
				""".formatted(description, unitPrice)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
	}

	private void awaitFinalStatus(UUID id, String expected) {
		await().atMost(TIMEOUT).untilAsserted(() -> assertThat(order(id).get("status")).isEqualTo(expected));
	}

	private Map<String, Object> order(UUID id) {
		return jdbc.queryForMap(
				"SELECT status, attempts, failure_reason, processed_at FROM orders WHERE id = ?", id);
	}

	private List<String> history(UUID id) {
		return jdbc.queryForList(
				"SELECT to_status FROM order_status_history WHERE order_id = ? ORDER BY created_at, id", String.class, id);
	}

}
