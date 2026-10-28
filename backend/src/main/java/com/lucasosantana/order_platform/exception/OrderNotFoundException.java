package com.lucasosantana.order_platform.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(UUID id) {
		super("Pedido %s não encontrado.".formatted(id));
	}

}
