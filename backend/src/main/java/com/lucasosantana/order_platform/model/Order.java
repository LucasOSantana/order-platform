package com.lucasosantana.order_platform.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Formula;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
	@GeneratedValue(strategy = GenerationType.UUID)
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

	@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
	@OrderBy("id")
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
	@OrderBy("createdAt, id")
	private List<OrderStatusHistory> statusHistory = new ArrayList<>();

	public static Order receive(String customerName, String customerEmail, Instant now) {
		var order = new Order();
		order.customerName = customerName;
		order.customerEmail = customerEmail;
		order.status = OrderStatus.RECEIVED;
		order.totalAmount = BigDecimal.ZERO.setScale(2);
		order.createdAt = now;
		order.updatedAt = now;
		order.statusHistory.add(new OrderStatusHistory(order, null, OrderStatus.RECEIVED, null, now));
		return order;
	}

	public void addItem(String description, int quantity, BigDecimal unitPrice) {
		var item = new OrderItem(this, description, quantity, unitPrice);
		items.add(item);
		totalAmount = totalAmount.add(item.getLineTotal());
		itemCount = items.size();
	}

}
