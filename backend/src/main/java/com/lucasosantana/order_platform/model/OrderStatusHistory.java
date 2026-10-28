package com.lucasosantana.order_platform.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 20)
	private OrderStatus toStatus;

	@Column(length = 500)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	OrderStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, String reason, Instant createdAt) {
		this.order = order;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.reason = reason;
		this.createdAt = createdAt;
	}

}
