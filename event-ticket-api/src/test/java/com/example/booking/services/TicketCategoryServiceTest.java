package com.example.booking.services;

import com.example.booking.builders.EventBuilder;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.exception.TicketCategorySoldOutException;
import com.example.booking.repositories.TicketCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCategoryServiceTest {

    @Mock
    private TicketCategoryRepository repository;

    @InjectMocks
    TicketCategoryServiceImpl ticketCategoryService;

    private static final Long TEST_CATEGORY_ID = 123L;

    private static final String CAT_NAME_VIP = "VIP";
    private static final BigDecimal CAT_PRICE_VIP = BigDecimal.valueOf(150.00);
    private static final int CAT_TICKETS_VIP = 100;

    private static final String CAT_NAME_PISTA = "Pista";
    private static final BigDecimal CAT_PRICE_PISTA = BigDecimal.valueOf(80.00);
    private static final int CAT_TICKETS_PISTA = 500;

    private static final int EXPECTED_LIST_SIZE = 2;
    private static final int EXPECTED_SAVE_CALLS = 2;

    private static final String EXTRACT_PROP_NAME = "name";
    private static final String EXTRACT_PROP_PRICE = "price";
    private static final String EXTRACT_PROP_AVAILABLE = "availableCategoryTickets";


    @Test
    public void createTicketCategoriesForEvent_ShouldReturnListOfTicketCategories_WhenRequestIsValid() {

        Event mockEvent = EventBuilder.anEvent()
                .withEventId(UUID.randomUUID())
                .build();

        List<CreateTicketCategoryRequest> requests = List.of(
                new CreateTicketCategoryRequest(CAT_NAME_VIP, CAT_PRICE_VIP, CAT_TICKETS_VIP),
                new CreateTicketCategoryRequest(CAT_NAME_PISTA, CAT_PRICE_PISTA, CAT_TICKETS_PISTA)
        );

        when(repository.save(any(TicketCategory.class)))
                .thenAnswer(invocation -> {
                    TicketCategory arg = invocation.getArgument(0);
                    arg.setTicketCategoryId(ThreadLocalRandom.current().nextLong());
                    return arg;
                });

        List<TicketCategory> res = ticketCategoryService.createTicketCategoriesForEvent(mockEvent, requests);

        assertThat(res)
                .hasSize(EXPECTED_LIST_SIZE)
                .extracting(EXTRACT_PROP_NAME, EXTRACT_PROP_PRICE, EXTRACT_PROP_AVAILABLE)
                .containsExactly(
                        tuple(CAT_NAME_VIP, CAT_PRICE_VIP, CAT_TICKETS_VIP),
                        tuple(CAT_NAME_PISTA, CAT_PRICE_PISTA, CAT_TICKETS_PISTA)
                );

        verify(repository, times(EXPECTED_SAVE_CALLS)).save(any(TicketCategory.class));
    }

    @Test
    void reserveOneTicket_ShouldReturnTicketCategory_WhenReservationIsSuccessful() {
        TicketCategory mockCategory = new TicketCategory();
        mockCategory.setTicketCategoryId(TEST_CATEGORY_ID);
        mockCategory.setName(CAT_NAME_VIP);
        mockCategory.setAvailableCategoryTickets(10);

        when(repository.decrementQuantity(TEST_CATEGORY_ID)).thenReturn(1);
        when(repository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(mockCategory));

        TicketCategory result = ticketCategoryService.reserveOneTicket(TEST_CATEGORY_ID);

        assertThat(result).isNotNull();
        assertThat(result.getTicketCategoryId()).isEqualTo(TEST_CATEGORY_ID);
        assertThat(result.getName()).isEqualTo(CAT_NAME_VIP);

        verify(repository, times(1)).decrementQuantity(TEST_CATEGORY_ID);
        verify(repository, times(1)).findById(TEST_CATEGORY_ID);
    }

    @Test
    void reserveOneTicket_ShouldThrowSoldOutException_WhenNoRowsUpdated() {
        when(repository.decrementQuantity(TEST_CATEGORY_ID)).thenReturn(0);

        assertThatThrownBy(() -> ticketCategoryService.reserveOneTicket(TEST_CATEGORY_ID))
                .isInstanceOf(TicketCategorySoldOutException.class);

        verify(repository, times(1)).decrementQuantity(TEST_CATEGORY_ID);
        verify(repository, never()).findById(any());
    }
}