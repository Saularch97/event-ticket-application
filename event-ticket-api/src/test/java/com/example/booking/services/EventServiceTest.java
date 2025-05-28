package com.example.booking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.booking.controller.dto.CityDataDto;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.services.intefaces.GeoService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.messaging.EventRequestProducer;
import com.example.booking.repository.EventRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {
    
    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private EventRequestProducer eventRequestProducer;

    @Mock
    private GeoService geoService;

    @InjectMocks
    private EventsServiceImpl eventsService;

    private Event event;
    private CreateEventRequest createEventRequest;
    private EventItemDto eventItemDto;
    private User user;
    private String fakeToken;

    @BeforeEach
    void setUp() {

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formatedDate = now.format(formatter);

        CreateTicketCategoryRequest category = new CreateTicketCategoryRequest(
               "category",
               300.0,
               100
        );

        createEventRequest = new CreateEventRequest("event",
                formatedDate,
                12,
                12,
                "National Stadium",
                120.0,
                List.of(category));

        event = new Event();
        event.setEventName("event");
        event.setEventLocation("National Stadium");
        event.setEventDate(now);
        event.setEventTicketPrice(120.0);

        List<TicketCategory> ticketCategories = new ArrayList<>();
        TicketCategory ticketCategory = new TicketCategory();
        ticketCategory.setName("Pista");
        ticketCategory.setPrice(150.0);
        ticketCategory.setAvailableCategoryTickets(500);
        ticketCategory.setEvent(event);
        ticketCategories.add(ticketCategory);

        event.setTicketCategories(ticketCategories);
        event.setAvailableTickets(500);
        event.setTrending(true);
        event.setTicketsEmittedInTrendingPeriod(0L);

        user = new User();
        user.setUserName("adminUser");
        user.setRoles(Set.of(new Role(ERole.ROLE_ADMIN)));
        event.setEventOwner(user);

        fakeToken = "booking-jwt=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc0ODQ1MDg5MCwiZXhwIjoxNzQ4NTM3MjkwfQ.pbW5gfMGSE6zdl8KCJwrkhlz6HU6LMZ1u7CYzFA3Qp0; Path=/; Max-Age=86400; Expires=Thu, 29 May 2025 16:48:10 GMT; Secure; HttpOnly; SameSite=None; booking-jwt-refresh=a72a0908-8212-4946-a29a-d8725aa0357d; Path=/api/auth/refreshtoken; Max-Age=86400; Expires=Thu, 29 May 2025 16:48:10 GMT; Secure; HttpOnly; SameSite=None";

        eventItemDto = new EventItemDto(event.getEventId(),
                event.getEventName(),
                event.getEventDate().toString(),
                event.getEventDate().getHour(),
                event.getEventDate().getMinute(),
                event.getEventTicketPrice(),
                event.getAvailableTickets(),
                event.getTicketCategories().stream().map(TicketCategory::toTicketCategoryDto).toList());
    }

    @Test
    void createEvent_ShouldReturnAnEventItemDto_WhenRequestIsValid() {
        // Arrange
        CityDataDto cityDataDto = new CityDataDto(10.0, 20.0);

        when(userService.findEntityByUserName("adminUser")).thenReturn(user);
        when(jwtUtils.getUserNameFromJwtToken(anyString())).thenReturn("adminUser");
        when(geoService.searchForCityData(event.getEventLocation())).thenReturn(cityDataDto);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // Act
        EventItemDto result = eventsService.createEvent(createEventRequest, fakeToken);

        // Assert
        verify(eventRepository).save(any(Event.class));
        assertNotNull(result);
        assertEquals(eventItemDto, result);
    }

    @Test
    void createEvent_ShouldReturnAnError_WhenUserIsNotFound() {
        // Arrange
        when(userService.findEntityByUserName("adminUser"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));

        when(jwtUtils.getUserNameFromJwtToken(anyString()))
                .thenReturn("adminUser");

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventsService.createEvent(createEventRequest, fakeToken)
        );


        verify(eventRepository, never()).save(any(Event.class));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found!", exception.getReason());
    }

    @Test
    void createEvent_ShouldReturnAnError_WhenTokenIsInvalid() {
        // Arrange
        when(jwtUtils.getUserNameFromJwtToken(anyString()))
                .thenThrow(new MalformedJwtException("Invalid token"));

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventsService.createEvent(createEventRequest, fakeToken)
        );

        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    void createEvent_ShouldReturnAnEventItemDtoWithMoreThanOneTicketCategory_WhenMoreThanOneTicketCategoryIsProvided() {
        // TODO decouple ticketCategory creation from eventServiceImpl
        // Arrange
        CityDataDto cityDataDto = new CityDataDto(10.0, 20.0);

        when(userService.findEntityByUserName("adminUser")).thenReturn(user);
        when(jwtUtils.getUserNameFromJwtToken(anyString())).thenReturn("adminUser");
        when(geoService.searchForCityData(event.getEventLocation())).thenReturn(cityDataDto);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // Act
        EventItemDto result = eventsService.createEvent(createEventRequest, fakeToken);

        // Assert
        verify(eventRepository).save(any(Event.class));
        assertNotNull(result);
        assertEquals(eventItemDto, result);
    }
}
