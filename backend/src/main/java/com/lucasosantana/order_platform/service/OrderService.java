package com.lucasosantana.order_platform.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lucasosantana.order_platform.dto.CreateOrderRequest;
import com.lucasosantana.order_platform.dto.OrderFilter;
import com.lucasosantana.order_platform.dto.OrderResponse;
import com.lucasosantana.order_platform.dto.OrderSummaryResponse;
import com.lucasosantana.order_platform.exception.InvalidRequestException;
import com.lucasosantana.order_platform.exception.OrderNotFoundException;
import com.lucasosantana.order_platform.mapper.OrderMapper;
import com.lucasosantana.order_platform.model.Order;
import com.lucasosantana.order_platform.model.OrderOutbox;
import com.lucasosantana.order_platform.repository.OrderOutboxRepository;
import com.lucasosantana.order_platform.repository.OrderRepository;
import com.lucasosantana.order_platform.repository.OrderSpecifications;

@Service
@Transactional(readOnly = true)
public class OrderService {

	static final List<String> SORTABLE_FIELDS = List.of("createdAt", "customerName", "status", "totalAmount");

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final OrderOutboxRepository outboxRepository;
	private final OrderMapper orderMapper;
	private final ZoneId zoneId;

	public OrderService(
			OrderRepository orderRepository,
			OrderOutboxRepository outboxRepository,
			OrderMapper orderMapper,
			@Value("${app.time-zone}") ZoneId zoneId) {
		this.orderRepository = orderRepository;
		this.outboxRepository = outboxRepository;
		this.orderMapper = orderMapper;
		this.zoneId = zoneId;
	}

	public Page<OrderSummaryResponse> list(OrderFilter filter, Pageable pageable) {
		if (filter.createdFrom() != null && filter.createdTo() != null
				&& filter.createdFrom().isAfter(filter.createdTo())) {
			throw new InvalidRequestException("A data inicial não pode ser posterior à data final.");
		}

		var specification = OrderSpecifications.hasStatus(filter.status())
				.and(OrderSpecifications.createdAtOrAfter(startOfDay(filter.createdFrom())))
				.and(OrderSpecifications.createdBefore(startOfNextDay(filter.createdTo())));

		return orderRepository.findAll(specification, withStableSort(pageable))
				.map(orderMapper::toSummary);
	}

	@Transactional
	public OrderResponse create(CreateOrderRequest request) {
		var now = Instant.now();
		var order = Order.receive(
						request.customerName().trim(), 
						request.customerEmail().trim(), 
						now
					);
		request.items().forEach(item -> order.addItem(
						item.description().trim(),
						item.quantity(),
						item.unitPrice())
					);

		orderRepository.save(order);
		outboxRepository.save(new OrderOutbox(order.getId(), now));

		log.info("Pedido {} recebido: {} itens, total {}", order.getId(), order.getItems().size(),
				order.getTotalAmount());
		return orderMapper.toResponse(order);
	}

	public OrderResponse getById(UUID id) {
		return orderRepository.findById(id)
				.map(orderMapper::toResponse)
				.orElseThrow(() -> new OrderNotFoundException(id));
	}

	private Pageable withStableSort(Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE_FIELDS.contains(order.getProperty())) {
				throw new InvalidRequestException("Não é possível ordenar por '%s'. Campos permitidos: %s."
						.formatted(order.getProperty(), String.join(", ", SORTABLE_FIELDS)));
			}
		}
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				pageable.getSort().and(Sort.by("id")));
	}

	private Instant startOfDay(LocalDate date) {
		return date == null ? null : date.atStartOfDay(zoneId).toInstant();
	}

	private Instant startOfNextDay(LocalDate date) {
		return date == null ? null : startOfDay(date.plusDays(1));
	}

}
