package com.lucasosantana.order_platform.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(nullable = false, length = 255)
	private String description;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "line_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal lineTotal;

	OrderItem(Order order, String description, int quantity, BigDecimal unitPrice) {
		this.order = order;
		this.description = description;
		this.quantity = quantity;
		this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
		this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

}
