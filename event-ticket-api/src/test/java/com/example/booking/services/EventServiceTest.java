package com.example.booking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.example.booking.builders.EventBuilder;
import com.example.booking.builders.TicketCategoryBuilder;

import com.example.booking.controller.request.event.UpdateEventRequest;
import com.example.booking.dto.CityDataDto;
import com.example.booking.dto.EventsDto;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.dto.EventSummaryDto;
import com.example.booking.exception.EventNotFoundException;
import com.example.booking.services.intefaces.GeoService;
import com.example.booking.services.intefaces.TicketCategoryService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.booking.dto.EventItemDto;
import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.messaging.interfaces.EventRequestProducer;
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

    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final LocalDateTime COMMON_EVENT_DATE = LocalDateTime.of(2025, 8, 15, 20, 30);

    private static final String SETUP_CAT1_NAME = "category";
    private static final BigDecimal SETUP_CAT1_PRICE = BigDecimal.valueOf(300.0);
    private static final Integer SETUP_CAT1_TICKETS = 100;
    private static final String SETUP_CAT2_NAME = "category2";
    private static final BigDecimal SETUP_CAT2_PRICE = BigDecimal.valueOf(200.0);
    private static final Integer SETUP_CAT2_TICKETS = 1500;
    private static final String SETUP_EVENT_NAME = "event";
    private static final Integer SETUP_EVENT_HOUR = 12;
    private static final Integer SETUP_EVENT_MINUTE = 12;
    private static final String SETUP_EVENT_LOCATION = "National Stadium";
    private static final BigDecimal SETUP_EVENT_PRICE = BigDecimal.valueOf(120.0);
    private static final Integer SAMPLE_EVENT_TICKETS = 500;
    private static final String SAMPLE_CAT_NAME = "Pista";
    private static final BigDecimal SAMPLE_CAT_PRICE = BigDecimal.valueOf(150.0);
    private static final String ADMIN_USERNAME = "adminUser";
    private static final String SIMPLE_EVENT_NAME = "Show de Rock";
    private static final Long DEFAULT_TRENDING_TICKETS = 0L;
    private static final Double TEST_LAT = 10.0;
    private static final Double TEST_LON = 20.0;

    private static final String MSG_USER_NOT_FOUND = "User not found!";
    private static final String MSG_INVALID_TOKEN = "Invalid token";
    private static final String MSG_EVENT_NOT_FOUND_PREFIX = "Event not found with ID:";

    private static final long TEST_CAT_ID_1 = 1L;
    private static final long TEST_CAT_ID_2 = 2L;
    private static final String PRIME_CAT_NAME = "prime_ticket";
    private static final BigDecimal PRIME_CAT_PRICE = BigDecimal.valueOf(20.0);
    private static final int PRIME_CAT_TICKETS = 50;
    private static final String ULTRA_CAT_NAME = "ultra_ticket";
    private static final BigDecimal ULTRA_CAT_PRICE = BigDecimal.valueOf(40.0);
    private static final int ULTRA_CAT_TICKETS = 50;
    private static final String SUMMER_FEST_NAME = "Festival de Verão";
    private static final String SUMMER_FEST_DATE_STR = "25/12/2025";
    private static final int SUMMER_FEST_HOUR = 20;
    private static final int SUMMER_FEST_MINUTE = 30;
    private static final String SUMMER_FEST_LOCATION = "São Paulo - SP";
    private static final BigDecimal SUMMER_FEST_PRICE = BigDecimal.valueOf(100.0);
    private static final int SUMMER_FEST_TOTAL_TICKETS = 100;

    private static final int PAGE_0 = 0;
    private static final int PAGE_SIZE_2 = 2;
    private static final int PAGE_SIZE_10 = 10;
    private static final String LOCATION_ALFENAS = "Alfenas";
    private static final String LOCATION_BOTELHOS = "Botelhos";
    private static final int TICKET_COUNT_1000 = 1000;
    private static final int TICKET_COUNT_2000 = 2000;
    private static final long TOTAL_ELEMENTS_5 = 5L;
    private static final int TOTAL_PAGES_3 = 3;
    private static final int RESULT_SIZE_2 = 2;
    private static final int RESULT_SIZE_1 = 1;
    private static final int RESULT_SIZE_0 = 0;
    private static final String SORT_BY_EVENT_DATE = "eventDate";

    private static final String SEARCH_NAME = "Show";
    private static final String SEARCH_LOCATION_VARGINHA = "Varginha";
    private static final String SEARCH_NAME_PAGODE = "Show de Pagode";
    private static final int SEARCH_TICKETS_150 = 150;
    private static final int SEARCH_TICKETS_200 = 200;
    private static final long SEARCH_DAYS_AHEAD = 3L;

    private static final String UPDATE_EVENT_NAME = "Evento Atualizado";
    private static final String UPDATE_EVENT_LOCATION = "Nova Localização";
    private static final String UPDATE_DUMMY_NAME = "Nome";
    private static final String UPDATE_DUMMY_LOCATION = "Local";

    private Event event;
    private CreateEventRequest createEventRequest;
    private EventItemDto eventItemDto;
    private User user;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        String formatedDate = now.format(formatter);

        CreateTicketCategoryRequest category1 = new CreateTicketCategoryRequest(
                SETUP_CAT1_NAME,
                SETUP_CAT1_PRICE,
                SETUP_CAT1_TICKETS
        );

        CreateTicketCategoryRequest category2 = new CreateTicketCategoryRequest(
                SETUP_CAT2_NAME,
                SETUP_CAT2_PRICE,
                SETUP_CAT2_TICKETS
        );

        createEventRequest = new CreateEventRequest(SETUP_EVENT_NAME,
                formatedDate,
                SETUP_EVENT_HOUR,
                SETUP_EVENT_MINUTE,
                SETUP_EVENT_LOCATION,
                SETUP_EVENT_PRICE,
                List.of(category1, category2)
        );

        event = createSampleEvent(now);
        user = createAdminUser();
        event.setEventOwner(user);

        eventItemDto = new EventItemDto(event.getEventId(),
                event.getEventName(),
                event.getEventDate().toString(),
                event.getEventDate().getHour(),
                event.getEventDate().getMinute(),
                event.getAvailableTickets(),
                event.getTicketCategories().stream().map(TicketCategory::toTicketCategoryDto).toList());
    }

    private Event createSampleEvent(LocalDateTime date) {
        Event event = EventBuilder.anEvent()
                .withEventName(SETUP_EVENT_NAME)
                .withEventLocation(SETUP_EVENT_LOCATION)
                .withEventDate(date)
                .withAvailableTickets(SAMPLE_EVENT_TICKETS)
                .withIsTrending(true)
                .withTicketsEmittedInTrendingPeriod(DEFAULT_TRENDING_TICKETS)
                .build();

        TicketCategory ticketCategory = TicketCategoryBuilder.aTicketCategory()
                .withName(SAMPLE_CAT_NAME)
                .withPrice(SAMPLE_CAT_PRICE)
                .withAvailableCategoryTickets(SAMPLE_EVENT_TICKETS)
                .withEvent(event)
                .build();

        event.setTicketCategories(List.of(ticketCategory));

        return event;
    }

    private User createAdminUser() {
        User user = new User();
        user.setUserName(ADMIN_USERNAME);
        user.setRoles(Set.of(new Role(ERole.ROLE_ADMIN)));
        return user;
    }

    private Event createSimpleEvent(String location, LocalDateTime date,
                                    User owner, int tickets, boolean trending) {
        return EventBuilder.anEvent()
                .withEventId(UUID.randomUUID())
                .withEventLocation(location)
                .withEventName(SIMPLE_EVENT_NAME)
                .withEventDate(date)
                .withEventOwner(owner)
                .withAvailableTickets(tickets)
                .withIsTrending(trending)
                .withTicketsEmittedInTrendingPeriod(DEFAULT_TRENDING_TICKETS)
                .build();
    }

    @Test
    void createEvent_ShouldReturnAnEventItemDto_WhenRequestIsValid() {
        CityDataDto cityDataDto = new CityDataDto(TEST_LAT, TEST_LON);

        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(geoService.searchForCityData(event.getEventLocation())).thenReturn(Optional.of(cityDataDto));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventItemDto result = eventsService.createEvent(createEventRequest);

        verify(eventRepository).save(any(Event.class));
        assertEquals(eventItemDto, result);
    }

    @Test
    void createEvent_ShouldReturnAnError_WhenUserIsNotFound() {
        when(userService.findUserEntityById(user.getUserId()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND));

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventsService.createEvent(createEventRequest)
        );

        verify(eventRepository, never()).save(any(Event.class));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteEvent_ShouldDeleteEventCorrectly() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(any())).thenReturn(true);

        eventsService.deleteEvent(eventId);

        verify(eventRepository).deleteById(any(UUID.class));
    }

    @Test
    void deleteEvent_ShouldThrowAnExceptionWhenEventIsNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(any())).thenReturn(false);

        EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> eventsService.deleteEvent(eventId));

        assertInstanceOf(EventNotFoundException.class, exception);
    }

    @Test
    void createEvent_ShouldReturnAnError_WhenTokenIsInvalid() {
        when(jwtUtils.getAuthenticatedUserId())
                .thenThrow(new MalformedJwtException(MSG_INVALID_TOKEN));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventsService.createEvent(createEventRequest)
        );

        assertInstanceOf(MalformedJwtException.class, exception);
    }

    @Test
    void createEvent_ShouldReturnAnEventItemDtoWithMoreThanOneTicketCategory_WhenMoreThanOneTicketCategoryIsProvided() {
        CityDataDto cityDataDto = new CityDataDto(TEST_LAT, TEST_LON);

        CreateTicketCategoryRequest req1 = createEventRequest.ticketCategories().getFirst();
        TicketCategory cat1 = TicketCategoryBuilder
                .aTicketCategory()
                .withAvailableCategoryTickets(req1.availableCategoryTickets())
                .withEvent(event)
                .withPrice(req1.price())
                .withName(req1.name())
                .withTicketCategoryId(TEST_CAT_ID_1)
                .build();

        CreateTicketCategoryRequest req2 = createEventRequest.ticketCategories().getLast();
        TicketCategory cat2 = TicketCategoryBuilder
                .aTicketCategory()
                .withAvailableCategoryTickets(req2.availableCategoryTickets())
                .withEvent(event)
                .withPrice(req2.price())
                .withName(req2.name())
                .withTicketCategoryId(TEST_CAT_ID_1)
                .build();

        List<TicketCategory> ticketCategories = List.of(cat1, cat2);

        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(geoService.searchForCityData(any())).thenReturn(Optional.of(cityDataDto));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setTicketCategories(ticketCategories);
            return e;
        });

        EventItemDto result = eventsService.createEvent(createEventRequest);

        verify(eventRepository).save(any(Event.class));
        assertAll("EventItemDto validation",
                () -> assertNotNull(result),
                () -> assertEquals(createEventRequest.eventName(), result.eventName()),
                () -> assertEquals(createEventRequest.ticketCategories().size(), result.ticketCategories().size()),
                () -> assertEquals(createEventRequest.ticketCategories().getFirst().name(), result.ticketCategories().getFirst().name()),
                () -> assertEquals(createEventRequest.ticketCategories().getLast().name(), result.ticketCategories().getLast().name())
        );
    }

    @Test
    void createEvent_ShouldHaveTheRightAmountOfAvailableAndOriginalTickets_WhenCreateEventWithAGivenAmountOfTicketCategories() {
        CityDataDto cityDataDto = new CityDataDto(TEST_LAT, TEST_LON);

        List<TicketCategory> ticketCategories = List.of(
                TicketCategoryBuilder.aTicketCategory()
                        .withAvailableCategoryTickets(PRIME_CAT_TICKETS)
                        .withEvent(event)
                        .withPrice(PRIME_CAT_PRICE)
                        .withName(PRIME_CAT_NAME)
                        .withTicketCategoryId(TEST_CAT_ID_1)
                        .build(),
                TicketCategoryBuilder.aTicketCategory()
                        .withAvailableCategoryTickets(ULTRA_CAT_TICKETS)
                        .withEvent(event)
                        .withPrice(ULTRA_CAT_PRICE)
                        .withName(ULTRA_CAT_NAME)
                        .withTicketCategoryId(TEST_CAT_ID_2)
                        .build()
        );

        CreateEventRequest createRequest = new CreateEventRequest(
                SUMMER_FEST_NAME,
                SUMMER_FEST_DATE_STR,
                SUMMER_FEST_HOUR,
                SUMMER_FEST_MINUTE,
                SUMMER_FEST_LOCATION,
                SUMMER_FEST_PRICE,
                List.of(
                        new CreateTicketCategoryRequest(PRIME_CAT_NAME, PRIME_CAT_PRICE, PRIME_CAT_TICKETS),
                        new CreateTicketCategoryRequest(ULTRA_CAT_NAME, ULTRA_CAT_PRICE, ULTRA_CAT_TICKETS)
                )
        );

        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(geoService.searchForCityData(any())).thenReturn(Optional.of(cityDataDto));
        when(ticketCategoryService.createTicketCategoriesForEvent(any(), any())).thenReturn(ticketCategories);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setAvailableTickets(SUMMER_FEST_TOTAL_TICKETS);
            e.setOriginalAmountOfTickets(SUMMER_FEST_TOTAL_TICKETS);
            e.setTicketCategories(ticketCategories);
            return e;
        });

        EventItemDto result = eventsService.createEvent(createRequest);

        verify(eventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();

        assertAll("event ticket count consistency",
                () -> assertNotNull(result),
                () -> assertEquals(SUMMER_FEST_TOTAL_TICKETS, result.availableTickets()),
                () -> assertEquals(SUMMER_FEST_TOTAL_TICKETS, savedEvent.getOriginalAmountOfTickets())
        );
    }

    @Test
    void listAllEvents_ShouldReturnPaginatedEvents() {
        LocalDateTime eventDate = COMMON_EVENT_DATE;
        Event event1 = createSimpleEvent(LOCATION_ALFENAS, eventDate, null, TICKET_COUNT_1000, false);
        Event event2 = createSimpleEvent(LOCATION_BOTELHOS, eventDate, null, TICKET_COUNT_1000, false);

        List<Event> eventList = List.of(event1, event2);
        Page<Event> eventPage = new PageImpl<>(eventList, PageRequest.of(PAGE_0, PAGE_SIZE_2), TOTAL_ELEMENTS_5);

        when(eventRepository.findAll(any(PageRequest.class))).thenReturn(eventPage);

        EventsDto result = eventsService.listAllEvents(PAGE_0, PAGE_SIZE_2);

        assertAll("Paginated events result",
                () -> assertNotNull(result),
                () -> assertEquals(PAGE_0, result.page()),
                () -> assertEquals(PAGE_SIZE_2, result.pageSize()),
                () -> assertEquals(TOTAL_ELEMENTS_5, result.totalElements()),
                () -> assertEquals(TOTAL_PAGES_3, result.totalPages()),
                () -> assertEquals(RESULT_SIZE_2, result.events().size())
        );
    }

    @Test
    void listAllEvents_ShouldUseCorrectPageRequestParameters() {
        when(eventRepository.findAll(any(PageRequest.class)))
                .thenReturn(Page.empty());

        eventsService.listAllEvents(PAGE_0, PAGE_SIZE_2);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(eventRepository).findAll(captor.capture());

        PageRequest pr = captor.getValue();

        assertAll("PageRequest configuration",
                () -> assertEquals(PAGE_0, pr.getPageNumber()),
                () -> assertEquals(PAGE_SIZE_2, pr.getPageSize()),
                () -> assertEquals(Sort.Direction.DESC,
                        Objects.requireNonNull(pr.getSort().getOrderFor(SORT_BY_EVENT_DATE)).getDirection())
        );
    }


    @Test
    void listAllUserEvents_ShouldReturnPaginatedEvents() {
        LocalDateTime eventDate = COMMON_EVENT_DATE;
        Event event1 = createSimpleEvent(LOCATION_ALFENAS, eventDate, user, TICKET_COUNT_1000, false);
        Event event2 = createSimpleEvent(LOCATION_BOTELHOS, eventDate, user, TICKET_COUNT_1000, false);

        List<Event> eventList = List.of(event1, event2);
        Page<Event> eventPage = new PageImpl<>(eventList, PageRequest.of(PAGE_0, PAGE_SIZE_2), TOTAL_ELEMENTS_5);

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(eventRepository.findAllEventsByUserId(eq(user.getUserId()), any(PageRequest.class)))
                .thenReturn(eventPage);

        EventsDto result = eventsService.listAllUserEvents(PAGE_0, PAGE_SIZE_2);

        assertAll("Paginated user events result",
                () -> assertNotNull(result),
                () -> assertEquals(PAGE_0, result.page()),
                () -> assertEquals(PAGE_SIZE_2, result.pageSize()),
                () -> assertEquals(TOTAL_ELEMENTS_5, result.totalElements()),
                () -> assertEquals(TOTAL_PAGES_3, result.totalPages()),
                () -> assertEquals(RESULT_SIZE_2, result.events().size())
        );
    }

    @Test
    void listAllUserEvents_ShouldUseCorrectPageRequestParameters() {
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(eventRepository.findAllEventsByUserId(eq(user.getUserId()), any(PageRequest.class)))
                .thenReturn(Page.empty());

        eventsService.listAllUserEvents(PAGE_0, PAGE_SIZE_2);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(eventRepository).findAllEventsByUserId(eq(user.getUserId()), captor.capture());

        PageRequest pr = captor.getValue();

        assertAll("PageRequest configuration for user events",
                () -> assertEquals(PAGE_0, pr.getPageNumber()),
                () -> assertEquals(PAGE_SIZE_2, pr.getPageSize()),
                () -> assertEquals(Sort.Direction.DESC,
                        Objects.requireNonNull(pr.getSort().getOrderFor(SORT_BY_EVENT_DATE)).getDirection())
        );
    }


    @Test
    void getTopTrendingEvents_ShouldReturnTrendingEventsCorrectly() {
        LocalDateTime eventDate = COMMON_EVENT_DATE;
        Event event1 = createSimpleEvent(LOCATION_ALFENAS, eventDate, null, TICKET_COUNT_1000, false);
        Event event2 = createSimpleEvent(LOCATION_BOTELHOS, eventDate, null, TICKET_COUNT_1000, true);

        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<EventItemDto> trendingEvents = eventsService.getTopTrendingEvents();

        assertEquals(RESULT_SIZE_1, trendingEvents.size());
        assertEquals(event2.getEventName(), trendingEvents.getFirst().eventName());
    }

    @Test
    void getTopTrendingEvents_ShouldReturnAnEmptyListWhenNoEventsAreTrending() {
        LocalDateTime eventDate = COMMON_EVENT_DATE;
        Event event1 = createSimpleEvent(LOCATION_ALFENAS, eventDate, null, TICKET_COUNT_1000, false);
        Event event2 = createSimpleEvent(LOCATION_BOTELHOS, eventDate, null, TICKET_COUNT_1000, false);

        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<EventItemDto> trendingEvents = eventsService.getTopTrendingEvents();

        assertEquals(RESULT_SIZE_0, trendingEvents.size());
    }

    @Test
    void findEventById_ShouldReturnAnEventGivenHisId() {
        UUID id = UUID.randomUUID();
        Event event = createSimpleEvent(LOCATION_ALFENAS,
                COMMON_EVENT_DATE,
                null, TICKET_COUNT_1000, false);
        event.setEventId(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        var res = eventsService.findEventEntityById(id);

        assertEquals(event, res);
    }

    @Test
    void findEventById_ShouldThrowAnExceptionWhenEventNotFound() {
        when(eventRepository.findById(any(UUID.class))).thenThrow(EventNotFoundException.class);
        var exception = assertThrows(EventNotFoundException.class, () -> eventsService.findEventEntityById(UUID.randomUUID()));
        assertInstanceOf(EventNotFoundException.class, exception);
    }

    @Test
    void listAllAvailableUserEvents_ShouldReturnPaginatedAvailableUserEvents() {
        LocalDateTime eventDate = COMMON_EVENT_DATE;
        Event event1 = createSimpleEvent(LOCATION_ALFENAS, eventDate, user, TICKET_COUNT_1000, false);
        Event event2 = createSimpleEvent(LOCATION_BOTELHOS, eventDate, user, TICKET_COUNT_2000, false);

        List<EventSummaryDto> eventList = List.of(
                new EventSummaryDto(event1.getEventId(), event1.getEventName(), event1.getEventLocation(),
                        event1.getAvailableTickets(), event1.getEventDate()),
                new EventSummaryDto(event2.getEventId(), event2.getEventName(), event2.getEventLocation(),
                        event2.getAvailableTickets(), event2.getEventDate())
        );

        Page<EventSummaryDto> eventPage = new PageImpl<>(eventList, PageRequest.of(PAGE_0, PAGE_SIZE_2), TOTAL_ELEMENTS_5);

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(eventRepository.findAvailableEventsByOwner(eq(user.getUserId()), any(PageRequest.class)))
                .thenReturn(eventPage);

        EventsDto result = eventsService.listAllAvailableUserEvents(PAGE_0, PAGE_SIZE_2);

        assertAll("Paginated available user events result",
                () -> assertNotNull(result),
                () -> assertEquals(PAGE_0, result.page()),
                () -> assertEquals(PAGE_SIZE_2, result.pageSize()),
                () -> assertEquals(TOTAL_ELEMENTS_5, result.totalElements()),
                () -> assertEquals(TOTAL_PAGES_3, result.totalPages()),
                () -> assertEquals(RESULT_SIZE_2, result.events().size()),
                () -> assertEquals(TICKET_COUNT_1000, result.events().getFirst().availableTickets()),
                () -> assertEquals(TICKET_COUNT_2000, result.events().getLast().availableTickets())
        );
    }


    @Test
    void listAllAvailableUserEvents_ShouldUseCorrectPageRequestParameters() {
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(user.getUserId());
        when(userService.findUserEntityById(user.getUserId())).thenReturn(user);
        when(eventRepository.findAvailableEventsByOwner(eq(user.getUserId()), any(PageRequest.class)))
                .thenReturn(Page.empty());

        eventsService.listAllAvailableUserEvents(PAGE_0, PAGE_SIZE_2);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(eventRepository).findAvailableEventsByOwner(eq(user.getUserId()), captor.capture());

        PageRequest pr = captor.getValue();

        assertAll("PageRequest configuration for available user events",
                () -> assertEquals(PAGE_0, pr.getPageNumber()),
                () -> assertEquals(PAGE_SIZE_2, pr.getPageSize()),
                () -> assertEquals(Sort.Direction.DESC,
                        Objects.requireNonNull(pr.getSort().getOrderFor(SORT_BY_EVENT_DATE)).getDirection())
        );
    }

    @Test
    void testSearchEventsShouldCallRepositoryWithCorrectParameters() {
        String name = SEARCH_NAME;
        String location = LOCATION_ALFENAS;
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(SEARCH_DAYS_AHEAD);
        Pageable pageRequest = PageRequest.of(PAGE_0, PAGE_SIZE_10);
        when(eventRepository.findByCriteria(name, location, startDate, endDate, pageRequest))
                .thenReturn(Page.empty());

        eventsService.searchEvents(name, location, startDate, endDate, PAGE_0, PAGE_SIZE_10);

        verify(eventRepository, times(1))
                .findByCriteria(name, location, startDate, endDate, pageRequest);
    }

    @Test
    void testSearchEventsShouldReturnCorrectDto() {
        String name = SEARCH_NAME;
        String location = LOCATION_ALFENAS;
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(SEARCH_DAYS_AHEAD);
        Pageable pageRequest = PageRequest.of(PAGE_0, PAGE_SIZE_10);

        List<EventSummaryDto> fakeEventList = List.of(
                new EventSummaryDto(UUID.randomUUID(), SIMPLE_EVENT_NAME, LOCATION_ALFENAS, SEARCH_TICKETS_150, startDate),
                new EventSummaryDto(UUID.randomUUID(), SEARCH_NAME_PAGODE, SEARCH_LOCATION_VARGINHA, SEARCH_TICKETS_200, startDate)
        );
        Page<EventSummaryDto> fakePage = new PageImpl<>(fakeEventList, pageRequest, fakeEventList.size());

        when(eventRepository.findByCriteria(name, location, startDate, endDate, pageRequest))
                .thenReturn(fakePage);

        EventsDto result = eventsService.searchEvents(name, location, startDate, endDate, PAGE_0, PAGE_SIZE_10);

        assertAll("Validação do DTO retornado",
                () -> assertNotNull(result, "O resultado não deveria ser nulo"),
                () -> assertEquals(RESULT_SIZE_2, result.events().size(), "Quantidade de eventos incorreta"),
                () -> assertEquals(PAGE_0, result.page(), "Página incorreta"),
                () -> assertEquals(PAGE_SIZE_10, result.pageSize(), "Tamanho da página incorreto"),
                () -> assertEquals(RESULT_SIZE_1, result.totalPages(), "Total de páginas incorreto"),
                () -> assertEquals(RESULT_SIZE_2, result.totalElements(), "Total de elementos incorreto"),
                () -> assertEquals(SIMPLE_EVENT_NAME, result.events().getFirst().name(), "Nome do primeiro evento incorreto"),
                () -> assertEquals(SEARCH_NAME_PAGODE, result.events().get(1).name(), "Nome do segundo evento incorreto")
        );
    }



    @Test
    void updateEvent_shouldUpdateEventFieldsAndCategories_whenEventExists() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime newDate = LocalDateTime.now().plusMonths(1);



        UpdateEventRequest updateRequest = new UpdateEventRequest(
                UPDATE_EVENT_NAME,
                newDate,
                UPDATE_EVENT_LOCATION
        );

        TicketCategory oldCategory = TicketCategoryBuilder.aTicketCategory().build();
        Event existingEvent = EventBuilder.anEvent()
                .withTicketCategories(new ArrayList<>(List.of(oldCategory)))
                .build();


        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

        eventsService.updateEvent(eventId, updateRequest);

        verify(eventRepository, times(1)).findById(eventId);

        assertAll("Validação dos campos atualizados do evento",
                () -> assertEquals(UPDATE_EVENT_NAME, existingEvent.getEventName(), "Nome do evento incorreto"),
                () -> assertEquals(newDate, existingEvent.getEventDate(), "Data do evento incorreta"),
                () -> assertEquals(UPDATE_EVENT_LOCATION, existingEvent.getEventLocation(), "Localização incorreta")
        );
    }

    @Test
    void updateEvent_shouldThrowEntityNotFoundException_whenEventDoesNotExist() {
        UUID nonExistentEventId = UUID.randomUUID();
        UpdateEventRequest updateRequest = new UpdateEventRequest(
                UPDATE_DUMMY_NAME,
                LocalDateTime.now(),
                UPDATE_DUMMY_LOCATION
        );

        when(eventRepository.findById(nonExistentEventId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> eventsService.updateEvent(nonExistentEventId, updateRequest)
        );

        assertTrue(exception.getMessage().contains(MSG_EVENT_NOT_FOUND_PREFIX));
    }
}