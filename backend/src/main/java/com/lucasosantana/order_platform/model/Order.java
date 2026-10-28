package com.lucasosantana.order_platform.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

	@Id
	private UUID id;

	@Column(name = "customer_name", nullable = false, length = 150)
	private String customerName;

	@Column(name = "customer_email", nullable = false, length = 254)
	private String customerEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	@Column(nullable = false)
	private int attempts;

	@Version
	private long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	// Calculado no banco para a listagem não precisar carregar os itens.
	@Formula("(select count(*) from order_items oi where oi.order_id = id)")
	private int itemCount;

	@OneToMany(mappedBy = "order")
	@OrderBy("id")
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "order")
	@OrderBy("createdAt, id")
	private List<OrderStatusHistory> statusHistory = new ArrayList<>();

}
