package com.booking.paymentprocessor.services;

import com.booking.paymentprocessor.dto.PaymentRequestDto;
import com.booking.paymentprocessor.dto.TicketItemDto;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String SECRET_KEY = "sk_test_mock_key";
    private static final String SUCCESS_URL = "http://localhost:8080/success";
    private static final String CANCEL_URL = "http://localhost:8080/cancel";

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "cliente@email.com";
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("100.50");

    private static final String TICKET_ID = "ticket-uuid-123";
    private static final String CATEGORY_NAME = "Pista Premium";
    private static final BigDecimal ITEM_PRICE = new BigDecimal("100.50");

    private static final String STRIPE_SESSION_ID = "cs_test_abc123";
    private static final String STRIPE_SESSION_URL = "https://checkout.stripe.com/pay/cs_test_abc123";

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private MockedStatic<Session> sessionMockedStatic;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(paymentService, "successUrl", SUCCESS_URL);
        ReflectionTestUtils.setField(paymentService, "cancelUrl", CANCEL_URL);

        sessionMockedStatic = mockStatic(Session.class);
    }

    @AfterEach
    void tearDown() {
        sessionMockedStatic.close();
    }

    @Test
    void init_ShouldSetStripeApiKey_WhenCalled() {
        paymentService.init();
        assertEquals(SECRET_KEY, Stripe.apiKey);
    }

    @Test
    void createStripeSession_ShouldReturnSession_WhenRequestIsValid() throws StripeException {
        PaymentRequestDto requestDto = mockPaymentRequestDto();
        Session mockSession = mock(Session.class);

        when(mockSession.getId()).thenReturn(STRIPE_SESSION_ID);
        when(mockSession.getUrl()).thenReturn(STRIPE_SESSION_URL);

        sessionMockedStatic.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                .thenReturn(mockSession);

        Session result = paymentService.createStripeSession(requestDto);

        assertNotNull(result);
        assertEquals(STRIPE_SESSION_ID, result.getId());
    }

    @Test
    void createStripeSession_ShouldBuildCorrectParams_WhenRequestIsValid() throws StripeException {
        PaymentRequestDto requestDto = mockPaymentRequestDto();
        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(STRIPE_SESSION_ID);

        sessionMockedStatic.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                .thenReturn(mockSession);

        ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);

        paymentService.createStripeSession(requestDto);

        sessionMockedStatic.verify(() -> Session.create(paramsCaptor.capture(), any(RequestOptions.class)));

        SessionCreateParams params = paramsCaptor.getValue();

        assertAll("Verifying Session Params",
                () -> assertTrue(params.getSuccessUrl().contains(SUCCESS_URL)),
                () -> assertEquals(USER_EMAIL, params.getCustomerEmail()),
                () -> assertEquals(ORDER_ID.toString(), params.getClientReferenceId()),
                () -> assertEquals(10050L, params.getLineItems().getFirst().getPriceData().getUnitAmount())
        );
    }

    @Test
    void createStripeSession_ShouldUseCorrectIdempotencyKey_WhenRequestIsValid() throws StripeException {
        PaymentRequestDto requestDto = mockPaymentRequestDto();
        Session mockSession = mock(Session.class);

        sessionMockedStatic.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                .thenReturn(mockSession);

        ArgumentCaptor<RequestOptions> optionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);

        paymentService.createStripeSession(requestDto);

        sessionMockedStatic.verify(() -> Session.create(any(SessionCreateParams.class), optionsCaptor.capture()));

        assertEquals("checkout-" + ORDER_ID, optionsCaptor.getValue().getIdempotencyKey());
    }

    @Test
    void createStripeSession_ShouldDefaultToZero_WhenPriceIsNull() throws StripeException {
        TicketItemDto itemNullPrice = new TicketItemDto(CATEGORY_NAME, null, TICKET_ID);
        PaymentRequestDto requestDto = new PaymentRequestDto(ORDER_ID, TOTAL_AMOUNT, List.of(itemNullPrice), USER_EMAIL);

        Session mockSession = mock(Session.class);
        sessionMockedStatic.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                .thenReturn(mockSession);

        ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);

        paymentService.createStripeSession(requestDto);

        sessionMockedStatic.verify(() -> Session.create(paramsCaptor.capture(), any()));

        assertEquals(0L, paramsCaptor.getValue().getLineItems().getFirst().getPriceData().getUnitAmount());
    }

    @Test
    void createStripeSession_ShouldThrowException_WhenStripeApiFails() {
        PaymentRequestDto requestDto = mockPaymentRequestDto();

        sessionMockedStatic.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                .thenThrow(new ApiConnectionException("Stripe is down"));

        assertThrows(ApiConnectionException.class, () -> paymentService.createStripeSession(requestDto));
    }

    private PaymentRequestDto mockPaymentRequestDto() {
        TicketItemDto item = new TicketItemDto(CATEGORY_NAME, ITEM_PRICE, TICKET_ID);

        return new PaymentRequestDto(
                ORDER_ID,
                TOTAL_AMOUNT,
                List.of(item),
                USER_EMAIL
        );
    }
}