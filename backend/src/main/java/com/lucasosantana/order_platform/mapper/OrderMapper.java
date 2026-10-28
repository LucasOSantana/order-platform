package com.lucasosantana.order_platform.mapper;

import org.springframework.stereotype.Component;

import com.lucasosantana.order_platform.dto.OrderItemResponse;
import com.lucasosantana.order_platform.dto.OrderResponse;
import com.lucasosantana.order_platform.dto.OrderStatusChangeResponse;
import com.lucasosantana.order_platform.dto.OrderSummaryResponse;
import com.lucasosantana.order_platform.model.Order;
import com.lucasosantana.order_platform.model.OrderItem;
import com.lucasosantana.order_platform.model.OrderStatusHistory;

@Component
public class OrderMapper {

	public OrderSummaryResponse toSummary(Order order) {
		return new OrderSummaryResponse(
				order.getId(),
				order.getCustomerName(),
				order.getCustomerEmail(),
				order.getStatus(),
				order.getTotalAmount(),
				order.getItemCount(),
				order.getCreatedAt(),
				order.getUpdatedAt());
	}

	public OrderResponse toResponse(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getCustomerName(),
				order.getCustomerEmail(),
				order.getStatus(),
				order.getTotalAmount(),
				order.getFailureReason(),
				order.getAttempts(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				order.getProcessedAt(),
				order.getItems().stream().map(this::toItem).toList(),
				order.getStatusHistory().stream().map(this::toStatusChange).toList());
	}

	private OrderItemResponse toItem(OrderItem item) {
		return new OrderItemResponse(
				item.getId(),
				item.getDescription(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getLineTotal());
	}

	private OrderStatusChangeResponse toStatusChange(OrderStatusHistory change) {
		return new OrderStatusChangeResponse(
				change.getFromStatus(),
				change.getToStatus(),
				change.getReason(),
				change.getCreatedAt());
	}

}
