package com.booking.paymentprocessor.services;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private static final String ENDPOINT_SECRET = "whsec_test_secret";
    private static final String PAYLOAD = "{ \"id\": \"evt_123\" }";
    private static final String SIG_HEADER = "t=123,v1=signature";
    private static final String ORDER_ID = "0af40017-9b9b-196c-819b-9b24e92c0003";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private WebhookServiceImpl webhookService;

    private MockedStatic<Webhook> webhookMockedStatic;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookServiceImpl(rabbitTemplate, ENDPOINT_SECRET);
        webhookMockedStatic = mockStatic(Webhook.class);
    }

    @AfterEach
    void tearDown() {
        webhookMockedStatic.close();
    }

    @Test
    void handleWebhook_ShouldProcessPaymentAndNotifyRabbit_WhenEventIsCheckoutCompletedAndDeserializationIsSafe()
            throws SignatureVerificationException, EventDataObjectDeserializationException {

        Event mockEvent = mock(Event.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        Session mockSession = mock(Session.class);

        webhookMockedStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIG_HEADER, ENDPOINT_SECRET))
                .thenReturn(mockEvent);

        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));
        when(mockSession.getClientReferenceId()).thenReturn(ORDER_ID);

        webhookService.handleWebhook(PAYLOAD, SIG_HEADER);

        verify(rabbitTemplate, times(1))
                .convertAndSend("order-status-exchange", "order.paid", ORDER_ID);
    }

    @Test
    void handleWebhook_ShouldProcessPayment_WhenEventIsCheckoutCompletedAndDeserializationIsUnsafe()
            throws SignatureVerificationException, EventDataObjectDeserializationException {

        Event mockEvent = mock(Event.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        Session mockSession = mock(Session.class);

        webhookMockedStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIG_HEADER, ENDPOINT_SECRET))
                .thenReturn(mockEvent);

        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        when(mockDeserializer.getObject()).thenReturn(Optional.empty());
        when(mockDeserializer.deserializeUnsafe()).thenReturn(mockSession);

        when(mockSession.getClientReferenceId()).thenReturn(ORDER_ID);

        webhookService.handleWebhook(PAYLOAD, SIG_HEADER);

        verify(rabbitTemplate).convertAndSend("order-status-exchange", "order.paid", ORDER_ID);
    }

    @Test
    void handleWebhook_ShouldIgnoreEvent_WhenTypeIsNotCheckoutSessionCompleted()
            throws SignatureVerificationException, EventDataObjectDeserializationException {

        Event mockEvent = mock(Event.class);

        webhookMockedStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIG_HEADER, ENDPOINT_SECRET))
                .thenReturn(mockEvent);

        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        webhookService.handleWebhook(PAYLOAD, SIG_HEADER);

        verify(mockEvent, never()).getDataObjectDeserializer();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void handleWebhook_ShouldNotNotifyRabbit_WhenOrderIdIsMissing()
            throws SignatureVerificationException, EventDataObjectDeserializationException {

        Event mockEvent = mock(Event.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        Session mockSession = mock(Session.class);

        webhookMockedStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIG_HEADER, ENDPOINT_SECRET))
                .thenReturn(mockEvent);

        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));

        when(mockSession.getClientReferenceId()).thenReturn(null);

        webhookService.handleWebhook(PAYLOAD, SIG_HEADER);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void handleWebhook_ShouldNotNotifyRabbit_WhenDeserializedObjectIsNotASession()
            throws SignatureVerificationException, EventDataObjectDeserializationException {

        Event mockEvent = mock(Event.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        StripeObject genericObject = mock(StripeObject.class);

        webhookMockedStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIG_HEADER, ENDPOINT_SECRET))
                .thenReturn(mockEvent);

        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(genericObject));

        webhookService.handleWebhook(PAYLOAD, SIG_HEADER);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void handleWebhook_ShouldThrowSignatureException_WhenSignatureIsInvalid() {
        webhookMockedStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIG_HEADER, ENDPOINT_SECRET))
                .thenThrow(new SignatureVerificationException("Invalid signature", "sig_header"));

        assertThrows(SignatureVerificationException.class,
                () -> webhookService.handleWebhook(PAYLOAD, SIG_HEADER));

        verifyNoInteractions(rabbitTemplate);
    }
}
