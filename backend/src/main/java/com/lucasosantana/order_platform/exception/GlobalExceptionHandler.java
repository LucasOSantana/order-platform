package com.lucasosantana.order_platform.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(OrderNotFoundException.class)
	ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Pedido não encontrado", ex.getMessage());
	}

	@ExceptionHandler(InvalidRequestException.class)
	ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Erro inesperado ao processar a requisição", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
				"Ocorreu um erro inesperado. Tente novamente mais tarde.");
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String parameter = ex instanceof MethodArgumentTypeMismatchException mismatch
				? mismatch.getName()
				: ex.getPropertyName();
		var body = problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"Valor inválido para o parâmetro '%s': %s.".formatted(parameter, ex.getValue()));
		return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		var errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
				.toList();
		var body = problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Alguns campos não passaram na validação.");
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		var body = problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"O corpo da requisição não é um JSON válido ou tem campos com tipo errado.");
		return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
