package com.example.booking.services;

import com.example.booking.builders.EventBuilder;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.repositories.TicketCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCategoryServiceTest {

    @Mock
    private TicketCategoryRepository repository;

    @InjectMocks
    TicketCategoryServiceImpl ticketCategoryService;

    private static final String CAT_NAME_VIP = "VIP";
    private static final double CAT_PRICE_VIP = 150.00;
    private static final int CAT_TICKETS_VIP = 100;

    private static final String CAT_NAME_PISTA = "Pista";
    private static final double CAT_PRICE_PISTA = 80.00;
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
}