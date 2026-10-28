package com.lucasosantana.order_platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.lucasosantana.order_platform.model.OrderStatus;

public record OrderSummaryResponse(
		UUID id,
		String customerName,
		String customerEmail,
		OrderStatus status,
		BigDecimal totalAmount,
		int itemCount,
		Instant createdAt,
		Instant updatedAt) {
}
