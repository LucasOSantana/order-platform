package com.lucasosantana.order_platform.messaging;

import java.util.UUID;

public record OrderCreatedMessage(UUID orderId) {
}
