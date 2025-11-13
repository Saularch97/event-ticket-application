package com.example.booking.services;

import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.repositories.TicketCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.tuple;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCategoryServiceTest {

    @Mock
    private TicketCategoryRepository repository;

    @InjectMocks
    TicketCategoryServiceImpl ticketCategoryService;

    @Test
    public void createTicketCategoriesForEvent_ShouldReturnListOfTicketCategories_WhenRequestIsValid() {

        Event mockEvent = new Event();
        mockEvent.setEventId(UUID.randomUUID());

        List<CreateTicketCategoryRequest> requests = List.of(
                new CreateTicketCategoryRequest("VIP", 150.00, 100),
                new CreateTicketCategoryRequest("Pista", 80.00, 500)
        );

        when(repository.save(any(TicketCategory.class)))
                .thenAnswer(invocation -> {
                    TicketCategory arg = invocation.getArgument(0);
                    arg.setTicketCategoryId(ThreadLocalRandom.current().nextLong());
                    return arg;
                });

        List<TicketCategory> res = ticketCategoryService.createTicketCategoriesForEvent(mockEvent, requests);

        assertThat(res)
            .hasSize(2)
            .extracting("name", "price", "availableCategoryTickets")
            .containsExactly(
                    tuple("VIP", 150.00, 100),
                    tuple("Pista", 80.00, 500)
            );

        verify(repository, times(2)).save(any(TicketCategory.class));
    }

}
