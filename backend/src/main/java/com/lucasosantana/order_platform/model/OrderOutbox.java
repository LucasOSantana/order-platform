package com.lucasosantana.order_platform.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutbox {

	private static final int MAX_ERROR_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "last_error", length = MAX_ERROR_LENGTH)
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	public OrderOutbox(UUID orderId, Instant createdAt) {
		this.orderId = orderId;
		this.createdAt = createdAt;
	}

	public void markPublished(Instant now) {
		attempts++;
		publishedAt = now;
		lastError = null;
	}

	public void markFailed(String error) {
		attempts++;
		lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
	}

}
