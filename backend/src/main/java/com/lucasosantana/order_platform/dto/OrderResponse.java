package com.lucasosantana.order_platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lucasosantana.order_platform.model.OrderStatus;

public record OrderResponse(
		UUID id,
		String customerName,
		String customerEmail,
		OrderStatus status,
		BigDecimal totalAmount,
		String failureReason,
		int attempts,
		Instant createdAt,
		Instant updatedAt,
		Instant processedAt,
		List<OrderItemResponse> items,
		List<OrderStatusChangeResponse> statusHistory) {
}
