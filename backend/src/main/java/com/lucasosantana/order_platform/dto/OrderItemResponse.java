package com.lucasosantana.order_platform.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long id,
		String description,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal) {
}
