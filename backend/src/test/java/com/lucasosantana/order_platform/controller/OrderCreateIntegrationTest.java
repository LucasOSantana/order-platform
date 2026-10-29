package com.lucasosantana.order_platform.controller;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lucasosantana.order_platform.TestcontainersConfiguration;
import com.lucasosantana.order_platform.exception.MessagePublishException;
import com.lucasosantana.order_platform.mapper.OrderMapper;
import com.lucasosantana.order_platform.messaging.OrderMessagePublisher;
import com.lucasosantana.order_platform.messaging.OrderOutboxRelay;

import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"app.outbox.relay-interval=1h",
		"spring.rabbitmq.listener.simple.auto-startup=false" })
@AutoConfigureMockMvc
class OrderCreateIntegrationTest {

	private static final String VALID_ORDER = """
			{
			  "customerName": "  Ana Souza ",
			  "customerEmail": "ana@email.com",
			  "totalAmount": 1.00,
			  "items": [
			    { "description": "Mouse sem fio", "quantity": 2, "unitPrice": 89.90 },
			    { "description": "Cabo USB-C", "quantity": 3, "unitPrice": 39.95 }
			  ]
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private OrderOutboxRelay relay;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoSpyBean
	private OrderMapper orderMapper;

	@MockitoSpyBean
	private OrderMessagePublisher publisher;

	@BeforeEach
	void cleanUp() {
		jdbc.update("DELETE FROM orders");
	}

	@Test
	void createsOrderWithServerSideTotalsHistoryAndOutboxEntry() throws Exception {
		var result = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/api/orders/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.customerName").value("Ana Souza"))
				.andExpect(jsonPath("$.status").value("RECEIVED"))
				.andExpect(jsonPath("$.totalAmount").value(299.65))
				.andExpect(jsonPath("$.items[1].lineTotal").value(119.85))
				.andExpect(jsonPath("$.statusHistory[0].toStatus").value("RECEIVED"))
				.andReturn();
		var id = UUID.fromString(jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asString());

		assertThat(jdbc.queryForObject("SELECT total_amount FROM orders WHERE id = ?", BigDecimal.class, id))
				.isEqualByComparingTo("299.65");
		assertThat(count("order_items", id)).isEqualTo(2);
		assertThat(count("order_status_history", id)).isEqualTo(1);
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM order_outbox WHERE order_id = ? AND published_at IS NULL", Integer.class, id))
				.isEqualTo(1);
	}

	@Test
	void relayKeepsEntryPendingWhenPublishingFails() throws Exception {
		var id = createOrder();
		doThrow(new MessagePublishException("broker indisponível")).when(publisher).publishOrderCreated(any());

		relay.publishPending();

		assertThat(jdbc.queryForMap("SELECT published_at, attempts, last_error FROM order_outbox WHERE order_id = ?", id))
				.satisfies(row -> {
					assertThat(row.get("published_at")).isNull();
					assertThat(row.get("attempts")).isEqualTo(1);
					assertThat(row.get("last_error")).isEqualTo("broker indisponível");
				});
	}

	@Test
	void nothingIsPersistedWhenTheTransactionFails() throws Exception {
		// Falha depois de o pedido e o outbox já terem sido salvos, ainda dentro da transação.
		doThrow(new IllegalStateException("falha simulada")).when(orderMapper).toResponse(any());

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
				.andExpect(status().isInternalServerError());

		assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM order_items", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM order_outbox", Integer.class)).isZero();
	}

	private UUID createOrder() throws Exception {
		var body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
	}

	private int count(String table, UUID orderId) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE order_id = ?", Integer.class, orderId);
	}

}
