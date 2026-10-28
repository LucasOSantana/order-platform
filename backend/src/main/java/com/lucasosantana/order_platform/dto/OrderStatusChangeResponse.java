package com.lucasosantana.order_platform.dto;

import java.time.Instant;

import com.lucasosantana.order_platform.model.OrderStatus;

public record OrderStatusChangeResponse(
		OrderStatus fromStatus,
		OrderStatus toStatus,
		String reason,
		Instant changedAt) {
}
