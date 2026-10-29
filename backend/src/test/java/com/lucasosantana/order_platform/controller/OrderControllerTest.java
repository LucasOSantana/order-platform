package com.lucasosantana.order_platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lucasosantana.order_platform.dto.OrderFilter;
import com.lucasosantana.order_platform.dto.OrderSummaryResponse;
import com.lucasosantana.order_platform.exception.OrderNotFoundException;
import com.lucasosantana.order_platform.model.OrderStatus;
import com.lucasosantana.order_platform.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@Test
	void listReturnsPageInTheFormatExpectedByTheFrontend() throws Exception {
		var summary = new OrderSummaryResponse(UUID.randomUUID(), "Ana Souza", "ana@email.com",
				OrderStatus.COMPLETED, new BigDecimal("150.00"), 2, Instant.now(), Instant.now());
		given(orderService.list(any(), any()))
				.willReturn(new PageImpl<>(List.of(summary), PageRequest.of(1, 20), 21));

		mockMvc.perform(get("/api/orders")
				.param("status", "COMPLETED")
				.param("createdFrom", "2026-10-01")
				.param("createdTo", "2026-10-28")
				.param("page", "1")
				.param("size", "20")
				.param("sort", "totalAmount,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].customerName").value("Ana Souza"))
				.andExpect(jsonPath("$.content[0].itemCount").value(2))
				.andExpect(jsonPath("$.page.number").value(1))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(21))
				.andExpect(jsonPath("$.page.totalPages").value(2));

		var pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(orderService).list(
				eq(new OrderFilter(OrderStatus.COMPLETED, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 28))),
				pageable.capture());
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "totalAmount"));
	}

	@Test
	void listRejectsUnknownStatus() throws Exception {
		mockMvc.perform(get("/api/orders").param("status", "SHIPPED"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'status': SHIPPED."));
	}

	@Test
	void getByIdReturnsNotFoundAsProblemDetail() throws Exception {
		var id = UUID.randomUUID();
		given(orderService.getById(id)).willThrow(new OrderNotFoundException(id));

		mockMvc.perform(get("/api/orders/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Pedido não encontrado"))
				.andExpect(jsonPath("$.detail").value("Pedido %s não encontrado.".formatted(id)));
	}

	@Test
	void createReturnsFieldErrorsInTheFormatExpectedByTheFrontend() throws Exception {
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "customerName": " ",
						  "customerEmail": "nao-e-email",
						  "items": [{ "description": "Mouse", "quantity": 0, "unitPrice": -1 }]
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
						"customerName", "customerEmail", "items[0].quantity", "items[0].unitPrice")))
				.andExpect(jsonPath("$.errors[?(@.field == 'items[0].quantity')].message")
						.value("A quantidade deve ser no mínimo 1."));

		verifyNoInteractions(orderService);
	}

}
