package com.booking.paymentprocessor.services.interfaces;

import com.booking.paymentprocessor.dto.PaymentRequestDto;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

public interface PaymentService {
     Session createStripeSession(PaymentRequestDto dto) throws StripeException;
}
