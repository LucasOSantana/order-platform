package com.lucasosantana.order_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI orderPlatformOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Order Platform API")
				.version("v1")
				.description("""
						Cadastro assíncrono e consulta de pedidos."""));
	}

}
