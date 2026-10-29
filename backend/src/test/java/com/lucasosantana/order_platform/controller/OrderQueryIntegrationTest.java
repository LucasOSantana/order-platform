package com.lucasosantana.order_platform.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lucasosantana.order_platform.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.time-zone=America/Sao_Paulo")
@AutoConfigureMockMvc
class OrderQueryIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanDatabase() {
		jdbc.update("DELETE FROM orders");
	}

	@Test
	void filtersByStatusAndCreationDateUsingTheConfiguredTimeZone() throws Exception {
		var lateOnFirst = insertOrder("Ana", "COMPLETED", "100.00", Instant.parse("2026-10-02T02:30:00Z"));
		var onSecond = insertOrder("Bruno", "COMPLETED", "200.00", Instant.parse("2026-10-02T15:00:00Z"));
		insertOrder("Carla", "FAILED", "300.00", Instant.parse("2026-10-02T16:00:00Z"));
		insertOrder("Diego", "COMPLETED", "400.00", Instant.parse("2026-10-03T03:00:00Z"));

		mockMvc.perform(get("/api/orders")
				.param("status", "COMPLETED")
				.param("createdFrom", "2026-10-01")
				.param("createdTo", "2026-10-02"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id", contains(onSecond.toString(), lateOnFirst.toString())))
				.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void sortsAndPaginates() throws Exception {
		insertOrder("Ana", "RECEIVED", "300.00", Instant.parse("2026-10-01T12:00:00Z"));
		insertOrder("Bruno", "RECEIVED", "100.00", Instant.parse("2026-10-02T12:00:00Z"));
		insertOrder("Carla", "RECEIVED", "200.00", Instant.parse("2026-10-03T12:00:00Z"));

		mockMvc.perform(get("/api/orders").param("sort", "totalAmount,asc").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].customerName", contains("Bruno", "Carla")))
				.andExpect(jsonPath("$.page.totalElements").value(3))
				.andExpect(jsonPath("$.page.totalPages").value(2));
	}

	@Test
	void rejectsStartDateAfterEndDate() throws Exception {
		mockMvc.perform(get("/api/orders").param("createdFrom", "2026-10-10").param("createdTo", "2026-10-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A data inicial não pode ser posterior à data final."));
	}

	@Test
	void returnsOrderDetailsWithItemsAndStatusHistory() throws Exception {
		var createdAt = Instant.parse("2026-10-01T12:00:00Z");
		var id = insertOrder("Ana", "COMPLETED", "259.70", createdAt);
		insertItem(id, "Mouse sem fio", 2, "89.90");
		insertItem(id, "Cabo USB-C 2m", 2, "39.95");
		insertStatusChange(id, null, "RECEIVED", createdAt);
		insertStatusChange(id, "RECEIVED", "PROCESSING", createdAt.plusSeconds(1));
		insertStatusChange(id, "PROCESSING", "COMPLETED", createdAt.plusSeconds(5));

		mockMvc.perform(get("/api/orders/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(259.70))
				.andExpect(jsonPath("$.items[*].description", contains("Mouse sem fio", "Cabo USB-C 2m")))
				.andExpect(jsonPath("$.items[0].lineTotal").value(179.80))
				.andExpect(jsonPath("$.statusHistory[*].toStatus", contains("RECEIVED", "PROCESSING", "COMPLETED")))
				.andExpect(jsonPath("$.statusHistory[0].fromStatus").isEmpty());

		mockMvc.perform(get("/api/orders"))
				.andExpect(jsonPath("$.content[0].itemCount").value(2));
	}

	private UUID insertOrder(String customerName, String status, String totalAmount, Instant createdAt) {
		var id = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO orders (id, customer_name, customer_email, status, total_amount, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""", id, customerName, customerName.toLowerCase() + "@email.com", status,
				new BigDecimal(totalAmount), Timestamp.from(createdAt), Timestamp.from(createdAt));
		return id;
	}

	private void insertItem(UUID orderId, String description, int quantity, String unitPrice) {
		var price = new BigDecimal(unitPrice);
		jdbc.update("""
				INSERT INTO order_items (order_id, description, quantity, unit_price, line_total)
				VALUES (?, ?, ?, ?, ?)
				""", orderId, description, quantity, price, price.multiply(BigDecimal.valueOf(quantity)));
	}

	private void insertStatusChange(UUID orderId, String from, String to, Instant at) {
		jdbc.update("""
				INSERT INTO order_status_history (order_id, from_status, to_status, created_at)
				VALUES (?, ?, ?, ?)
				""", orderId, from, to, Timestamp.from(at));
	}

}
