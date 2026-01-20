package com.booking.paymentprocessor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"stripe.secretKey=sk_test_chave_falsa_para_teste",
		"stripe.webhook.secret=whsec_chave_falsa_webhook",
		"app.checkout.success-url=http://localhost:8080/success",
		"app.checkout.cancel-url=http://localhost:8080/cancel",

		"spring.rabbitmq.host=localhost",
		"spring.rabbitmq.port=5672",
		"spring.rabbitmq.username=guest",
		"spring.rabbitmq.password=guest"
})
class PaymentprocessorApplicationTests {

	@Test
	void contextLoads() {
	}

}
