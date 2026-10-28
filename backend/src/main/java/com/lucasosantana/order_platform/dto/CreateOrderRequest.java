package com.lucasosantana.order_platform.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
		@NotBlank(message = "Informe o nome do cliente.")
		@Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
		String customerName,

		@NotBlank(message = "Informe o e-mail do cliente.")
		@Email(message = "Informe um e-mail válido.")
		@Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
		String customerEmail,

		@NotEmpty(message = "Informe ao menos um item.")
		@Size(max = 100, message = "O pedido pode ter no máximo 100 itens.")
		List<@Valid CreateOrderItemRequest> items) {
}
