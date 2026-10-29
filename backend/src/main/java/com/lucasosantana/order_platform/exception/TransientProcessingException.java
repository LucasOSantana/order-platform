package com.lucasosantana.order_platform.exception;

public class TransientProcessingException extends RuntimeException {

	public TransientProcessingException(String message) {
		super(message);
	}

}
