package com.lucasosantana.order_platform.repository;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.lucasosantana.order_platform.model.Order;
import com.lucasosantana.order_platform.model.OrderStatus;

public final class OrderSpecifications {

	private OrderSpecifications() {
	}

	public static Specification<Order> hasStatus(OrderStatus status) {
		return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
	}

	public static Specification<Order> createdAtOrAfter(Instant from) {
		return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
	}

	public static Specification<Order> createdBefore(Instant to) {
		return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("createdAt"), to);
	}

}
