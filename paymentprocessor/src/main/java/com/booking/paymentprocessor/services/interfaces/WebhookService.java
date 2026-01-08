package com.booking.paymentprocessor.services.interfaces;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;

public interface WebhookService {

    void handleWebhook(String payload, String sigHeader) throws EventDataObjectDeserializationException, SignatureVerificationException;
}
