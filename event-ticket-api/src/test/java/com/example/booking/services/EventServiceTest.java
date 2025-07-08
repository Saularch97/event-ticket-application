package com.example.booking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.example.booking.controller.dto.CityDataDto;
import com.example.booking.controller.dto.EventsDto;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.dto.EventSummaryDto;
import com.example.booking.services.intefaces.GeoService;
import com.example.booking.services.intefaces.TicketCategoryService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.messaging.EventRequestProducer;
import com.example.booking.repositories.EventRepository;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import org.springframework.data.domain.*;
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

    @Mock
    private TicketCategoryService ticketCategoryService;

    @InjectMocks
    private EventsServiceImpl eventsService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    private Event event;
    private CreateEventRequest createEventRequest;
    private EventItemDto eventItemDto;
    private User user;

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

        event = createSampleEvent(now, "event", "National Stadium", 120.0, 500, true);
        user = createAdminUser("adminUser");
        event.setEventOwner(user);

        eventItemDto = new EventItemDto(event.getEventId(),
                event.getEventName(),
                event.getEventDate().toString(),
                event.getEventDate().getHour(),
                event.getEventDate().getMinute(),
                event.getEventTicketPrice(),
                event.getAvailableTickets(),
                event.getTicketCategories().stream().map(TicketCategory::toTicketCategoryDto).toList());
    }

    private Event createSampleEvent(LocalDateTime date, String name, String location,
                                    double price, int availableTickets, boolean isTrending) {
        Event event = new Event();
        event.setEventName(name);
        event.setEventLocation(location);
        event.setEventDate(date);
        event.setEventTicketPrice(price);

        List<TicketCategory> ticketCategories = new ArrayList<>();
        TicketCategory ticketCategory = new TicketCategory();
        ticketCategory.setName("Pista");
        ticketCategory.setPrice(150.0);
        ticketCategory.setAvailableCategoryTickets(availableTickets);
        ticketCategory.setEvent(event);
        ticketCategories.add(ticketCategory);

        event.setTicketCategories(ticketCategories);
        event.setAvailableTickets(availableTickets);
        event.setTrending(isTrending);
        event.setTicketsEmittedInTrendingPeriod(0L);

        return event;
    }

    private User createAdminUser(String username) {
        User user = new User();
        user.setUserName(username);
        user.setRoles(Set.of(new Role(ERole.ROLE_ADMIN)));
        return user;
    }

    private Event createSimpleEvent(String name, String location, LocalDateTime date,
                                    User owner, double price, int tickets, boolean trending) {
        return new Event(
                UUID.randomUUID(),
                location,
                name,
                date,
                new HashSet<>(),
                owner,
                price,
                tickets,
                new ArrayList<>(),
                trending,
                0L
        );
    }

    @Test
    void createEvent_ShouldReturnAnEventItemDto_WhenRequestIsValid() {
        CityDataDto cityDataDto = new CityDataDto(10.0, 20.0);

        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(geoService.searchForCityData(event.getEventLocation())).thenReturn(cityDataDto);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(ticketCategoryService.createTicketCategoriesForEvent(any(Event.class), any()))
                .thenReturn(List.of(new TicketCategory(
                        20,
                        event,
                        20.0,
                        "prime_ticket",
                        1))
                );

        EventItemDto result = eventsService.createEvent(createEventRequest);

        verify(eventRepository).save(any(Event.class));
        assertNotNull(result);
        assertEquals(eventItemDto, result);
    }

    @Test
    void createEvent_ShouldReturnAnError_WhenUserIsNotFound() {
        when(userService.findUserEntityById(user.getUserId()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventsService.createEvent(createEventRequest)
        );

        verify(eventRepository, never()).save(any(Event.class));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found!", exception.getReason());
    }

    @Test
    void createEvent_ShouldReturnAnError_WhenTokenIsInvalid() {
        when(jwtUtils.getAuthenticatedUserId())
                .thenThrow(new MalformedJwtException("Invalid token"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventsService.createEvent(createEventRequest)
        );

        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    void createEvent_ShouldReturnAnEventItemDtoWithMoreThanOneTicketCategory_WhenMoreThanOneTicketCategoryIsProvided() {
        CityDataDto cityDataDto = new CityDataDto(10.0, 20.0);
        List<TicketCategory> ticketCategories = List.of(
                new TicketCategory(20, event, 20.0, "prime_ticket", 1),
                new TicketCategory(30, event, 40.0, "ultra_ticket", 2)
        );

        CreateEventRequest createRequest = new CreateEventRequest(
                "Festival de Verão",
                "25/12/2025",
                20,
                30,
                "São Paulo - SP",
                100.0,
                List.of(
                        new CreateTicketCategoryRequest("prime_ticket", 20.0, 1),
                        new CreateTicketCategoryRequest("ultra_ticket", 40.0, 2)
                )
        );

        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(geoService.searchForCityData(any())).thenReturn(cityDataDto);
        when(ticketCategoryService.createTicketCategoriesForEvent(any(Event.class), any())).thenReturn(ticketCategories);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setTicketCategories(ticketCategories);
            return e;
        });

        EventItemDto result = eventsService.createEvent(createRequest);

        verify(eventRepository).save(any(Event.class));
        assertNotNull(result);
        assertEquals("Festival de Verão", result.eventName());
        assertEquals(2, result.ticketCategories().size());
        assertEquals("prime_ticket", result.ticketCategories().get(0).name());
        assertEquals("ultra_ticket", result.ticketCategories().get(1).name());
    }

    @Test
    void createEvent_ShouldHaveTheRightAmountOfAvailableAndOriginalTickets_WhenCreateEventWithAGivenAmountOfTicketCategories() {
        CityDataDto cityDataDto = new CityDataDto(10.0, 20.0);
        List<TicketCategory> ticketCategories = List.of(
                new TicketCategory(50, event, 20.0, "prime_ticket", 1),
                new TicketCategory(50, event, 40.0, "ultra_ticket", 2)
        );

        CreateEventRequest createRequest = new CreateEventRequest(
                "Festival de Verão",
                "25/12/2025",
                20,
                30,
                "São Paulo - SP",
                100.0,
                List.of(
                        new CreateTicketCategoryRequest("prime_ticket", 20.0, 50),
                        new CreateTicketCategoryRequest("ultra_ticket", 40.0, 50)
                )
        );

        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(geoService.searchForCityData(any())).thenReturn(cityDataDto);
        when(ticketCategoryService.createTicketCategoriesForEvent(any(Event.class), any())).thenReturn(ticketCategories);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setTicketCategories(ticketCategories);
            return e;
        });

        EventItemDto result = eventsService.createEvent(createRequest);

        verify(eventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();

        assertNotNull(result);
        assertEquals(100, result.availableTickets());
        assertEquals(100, savedEvent.getOriginalAmountOfTickets());
    }

    @Test
    void listAllEvents_ShouldReturnPaginatedEvents() {
        int page = 0;
        int pageSize = 2;

        LocalDateTime eventDate = LocalDateTime.of(2025, 8, 15, 20, 30);
        Event event1 = createSimpleEvent("Show de Rock", "Alfenas", eventDate, null, 150.00, 1000, false);
        Event event2 = createSimpleEvent("Show de Rock", "Botelhos", eventDate, null, 150.00, 1000, false);

        List<Event> eventList = List.of(event1, event2);
        Page<Event> eventPage = new PageImpl<>(eventList, PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate"), 5);

        when(eventRepository.findAll(any(PageRequest.class))).thenReturn(eventPage);

        EventsDto result = eventsService.listAllEvents(page, pageSize);

        assertNotNull(result);
        assertEquals(page, result.page());
        assertEquals(pageSize, result.pageSize());
        assertEquals(5, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(2, result.events().size());

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(eventRepository).findAll(pageRequestCaptor.capture());

        PageRequest pr = pageRequestCaptor.getValue();
        assertEquals(page, pr.getPageNumber());
        assertEquals(pageSize, pr.getPageSize());
        assertEquals(Sort.Direction.DESC, Objects.requireNonNull(pr.getSort().getOrderFor("eventDate")).getDirection());
    }

    @Test
    void listAllUserEvents_ShouldReturnPaginatedEvents() {
        int page = 0;
        int pageSize = 2;

        LocalDateTime eventDate = LocalDateTime.of(2025, 8, 15, 20, 30);
        Event event1 = createSimpleEvent("Show de Rock", "Alfenas", eventDate, user, 150.00, 1000, false);
        Event event2 = createSimpleEvent("Show de Rock", "Botelhos", eventDate, user, 150.00, 1000, false);

        List<Event> eventList = List.of(event1, event2);
        Page<Event> eventPage = new PageImpl<>(eventList, PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate"), 5);

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(eventRepository.findAllEventsByUserId(eq(user.getUserId()), any(PageRequest.class)))
                .thenReturn(eventPage);

        EventsDto result = eventsService.listAllUserEvents(page, pageSize);

        assertNotNull(result);
        assertEquals(page, result.page());
        assertEquals(pageSize, result.pageSize());
        assertEquals(5, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(2, result.events().size());

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(eventRepository).findAllEventsByUserId(eq(user.getUserId()), pageRequestCaptor.capture());

        PageRequest pr = pageRequestCaptor.getValue();
        assertEquals(page, pr.getPageNumber());
        assertEquals(pageSize, pr.getPageSize());
        assertEquals(Sort.Direction.DESC, Objects.requireNonNull(pr.getSort().getOrderFor("eventDate")).getDirection());
    }

    @Test
    void getTopTrendingEvents_ShouldReturnTrendingEventsCorrectly() {
        LocalDateTime eventDate = LocalDateTime.of(2025, 8, 15, 20, 30);
        Event event1 = createSimpleEvent("Show de Rock", "Alfenas", eventDate, null, 150.00, 1000, false);
        Event event2 = createSimpleEvent("Show de Rock", "Botelhos", eventDate, null, 150.00, 1000, true);

        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<EventItemDto> trendingEvents = eventsService.getTopTrendingEvents();

        assertEquals(1, trendingEvents.size());
        assertEquals(event2.getEventName(), trendingEvents.getFirst().eventName());
    }

    @Test
    void getTopTrendingEvents_ShouldReturnAnEmptyListWhenNoEventsAreTrending() {
        LocalDateTime eventDate = LocalDateTime.of(2025, 8, 15, 20, 30);
        Event event1 = createSimpleEvent("Show de Rock", "Alfenas", eventDate, null, 150.00, 1000, false);
        Event event2 = createSimpleEvent("Show de Rock", "Botelhos", eventDate, null, 150.00, 1000, false);

        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<EventItemDto> trendingEvents = eventsService.getTopTrendingEvents();

        assertEquals(0, trendingEvents.size());
    }

    @Test
    void findEventById_ShouldReturnAnEventGivenHisId() {
        UUID id = UUID.randomUUID();
        Event event = createSimpleEvent("Show de Rock", "Alfenas",
                LocalDateTime.of(2025, 8, 15, 20, 30),
                null, 150.00, 1000, false);
        event.setEventId(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        var res = eventsService.findEventEntityById(id);

        assertEquals(event, res);
    }

    @Test
    void listAllAvailableUserEvents_ShouldReturnAnListOfAvailableUserEvents() {
        int page = 0;
        int pageSize = 2;

        LocalDateTime eventDate = LocalDateTime.of(2025, 8, 15, 20, 30);
        Event event1 = createSimpleEvent("Show de Rock", "Alfenas", eventDate, user, 150.00, 1000, false);
        Event event2 = createSimpleEvent("Show de Rock", "Botelhos", eventDate, user, 150.00, 2000, false);

        List<EventSummaryDto> eventList = List.of(
                new EventSummaryDto(event1.getEventId(), event1.getEventName(), event1.getEventLocation(), event1.getAvailableTickets(), event1.getEventDate()),
                new EventSummaryDto(event2.getEventId(), event2.getEventName(), event2.getEventLocation(), event2.getAvailableTickets(), event.getEventDate())
        );

        Page<EventSummaryDto> eventPage = new PageImpl<>(eventList, PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate"), 5);

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(eventRepository.findAvailableEventsByOwner(eq(user.getUserId()), any(PageRequest.class)))
                .thenReturn(eventPage);

        EventsDto result = eventsService.listAllAvailableUserEvents(page, pageSize);

        assertNotNull(result);
        assertEquals(page, result.page());
        assertEquals(pageSize, result.pageSize());
        assertEquals(5, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(2, result.events().size());
        assertEquals(1000 , result.events().getFirst().availableTickets());
        assertEquals(2000 , result.events().getLast().availableTickets());

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(eventRepository).findAvailableEventsByOwner(eq(user.getUserId()), pageRequestCaptor.capture());

        PageRequest pr = pageRequestCaptor.getValue();
        assertEquals(page, pr.getPageNumber());
        assertEquals(pageSize, pr.getPageSize());
        assertEquals(Sort.Direction.DESC, Objects.requireNonNull(pr.getSort().getOrderFor("eventDate")).getDirection());
    }

    @Test
    void testSearchEventsShouldCallRepositoryAndReturnDto() {

        String name = "Show";
        String location = "Alfenas";
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(3);
        int page = 0;
        int pageSize = 10;
        Pageable pageRequest = PageRequest.of(page, pageSize);

        List<EventSummaryDto> fakeEventList = List.of(
                new EventSummaryDto(UUID.randomUUID(), "Show de Rock", "Alfenas", 150, startDate),
                new EventSummaryDto(UUID.randomUUID(), "Show de Pagode", "Varginha", 200, startDate)
        );
        Page<EventSummaryDto> fakePage = new PageImpl<>(fakeEventList, pageRequest, fakeEventList.size());

        when(eventRepository.findByCriteria(name, location, startDate, endDate, pageRequest))
                .thenReturn(fakePage);

        EventsDto result = eventsService.searchEvents(name, location, startDate, endDate, page, pageSize);

        verify(eventRepository, times(1)).findByCriteria(name, location, startDate, endDate, pageRequest);

        assertNotNull(result);
        assertEquals(2, result.events().size());
        assertEquals(page, result.page());
        assertEquals(pageSize, result.pageSize());
        assertEquals(1, result.totalPages());
        assertEquals(2, result.totalElements());
        assertEquals("Show de Rock", result.events().getFirst().name());
        assertEquals("Show de Pagode", result.events().get(1).name());
    }
}
