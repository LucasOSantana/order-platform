package com.lucasosantana.order_platform.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.lucasosantana.order_platform.dto.CreateOrderRequest;
import com.lucasosantana.order_platform.dto.OrderFilter;
import com.lucasosantana.order_platform.dto.OrderResponse;
import com.lucasosantana.order_platform.dto.OrderSummaryResponse;
import com.lucasosantana.order_platform.model.OrderStatus;
import com.lucasosantana.order_platform.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Pedidos")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	@Operation(summary = "Lista pedidos com paginação, ordenação e filtros")
	public PagedModel<OrderSummaryResponse> list(
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		var filter = new OrderFilter(status, createdFrom, createdTo);
		return new PagedModel<>(orderService.list(filter, pageable));
	}

	@PostMapping
	@Operation(summary = "Cria um pedido",
			description = "O pedido é criado em RECEIVED e processado de forma "
					+ "assíncrona. Consulte GET /api/orders/{id} para acompanhar o status.")
	public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
		var order = orderService.create(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(order.id())
				.toUri();
		return ResponseEntity.created(location).body(order);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta os detalhes de um pedido", description = "Inclui itens e histórico de status.")
	public OrderResponse getById(@PathVariable UUID id) {
		return orderService.getById(id);
	}

}
