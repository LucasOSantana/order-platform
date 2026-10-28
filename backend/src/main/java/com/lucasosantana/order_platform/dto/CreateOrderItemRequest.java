package com.lucasosantana.order_platform.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderItemRequest(
		@NotBlank(message = "Informe a descrição do item.")
		@Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
		String description,

		@NotNull(message = "Informe a quantidade.")
		@Min(value = 1, message = "A quantidade deve ser no mínimo 1.")
		Integer quantity,

		@NotNull(message = "Informe o preço unitário.")
		@DecimalMin(value = "0.00", message = "O preço unitário não pode ser negativo.")
		@Digits(integer = 7, fraction = 2, message = "O preço unitário deve ter até 7 dígitos inteiros e 2 casas decimais.")
		BigDecimal unitPrice) {
}
