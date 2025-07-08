package com.example.booking.config.cache;

public final class CacheNames {
    /**
     * Utility class that holds cache name constants used across the application.
     *
     * This class is preferred over an enum because cache names must be string literals
     * in annotations like @Cacheable, and enum methods (like .value()) are not allowed there.
     *
     * This class cannot be instantiated or extended.
     */
    private CacheNames(){}
    public static final String REMAINING_TICKETS = "AVAILABLE_TICKETS_CACHE";
    public static final String ORDERS = "ORDERS_CACHE";
    public static final String TOP_EVENTS = "TOP_EVENTS_CACHE";
}
