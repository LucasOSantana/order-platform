package com.lucasosantana.order_platform.dto;

import java.time.LocalDate;

import com.lucasosantana.order_platform.model.OrderStatus;

public record OrderFilter(
    OrderStatus status, 
    LocalDate createdFrom, 
    LocalDate createdTo) {
}
