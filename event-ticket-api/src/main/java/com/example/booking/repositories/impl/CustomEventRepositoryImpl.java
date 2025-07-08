package com.example.booking.repositories.impl;

import com.example.booking.domain.entities.Event;
import com.example.booking.dto.EventSummaryDto;
import com.example.booking.repositories.CustomEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CustomEventRepositoryImpl implements CustomEventRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<EventSummaryDto> findByCriteria(String name, String location, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EventSummaryDto> query = cb.createQuery(EventSummaryDto.class);
        Root<Event> event = query.from(Event.class);

        List<Predicate> predicates = createPredicates(cb, event, name, location, startDate, endDate);

        query.select(cb.construct(
                EventSummaryDto.class,
                event.get("eventId"),
                event.get("eventName"),
                event.get("eventLocation"),
                event.get("availableTickets"),
                event.get("eventDate")
        )).where(predicates.toArray(new Predicate[0]));

        if (pageable.getSort().isSorted()) {
            query.orderBy(cb.asc(event.get(pageable.getSort().get().findFirst().get().getProperty())));
        }

        TypedQuery<EventSummaryDto> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<EventSummaryDto> resultList = typedQuery.getResultList();

        long totalRows = countByCriteria(name, location, startDate, endDate);

        return new PageImpl<>(resultList, pageable, totalRows);
    }

    private long countByCriteria(String name, String location, LocalDateTime startDate, LocalDateTime endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Event> event = countQuery.from(Event.class);

        List<Predicate> predicates = createPredicates(cb, event, name, location, startDate, endDate);

        countQuery.select(cb.count(event)).where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> createPredicates(CriteriaBuilder cb, Root<Event> event, String name, String location, LocalDateTime startDate, LocalDateTime endDate) {
        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(cb.lower(event.get("eventName")), "%" + name.toLowerCase() + "%"));
        }
        if (location != null && !location.isEmpty()) {
            predicates.add(cb.equal(event.get("eventLocation"), location));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(event.get("eventDate"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(event.get("eventDate"), endDate));
        }

        return predicates;
    }
}