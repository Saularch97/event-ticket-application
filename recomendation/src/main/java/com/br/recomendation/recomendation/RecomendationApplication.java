package com.br.recomendation.recomendation;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class RecomendationApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecomendationApplication.class, args);
	}

}
