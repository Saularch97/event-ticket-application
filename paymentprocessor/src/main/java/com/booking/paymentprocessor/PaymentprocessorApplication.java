package com.booking.paymentprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PaymentprocessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentprocessorApplication.class, args);
	}

}
