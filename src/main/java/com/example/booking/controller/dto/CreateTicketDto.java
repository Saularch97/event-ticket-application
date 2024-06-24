package com.example.booking.controller.dto;

public record CreateTicketDto(String ticketName, Double ticketPrice, String ticketDate, Integer eventHour,String eventLocation, Integer eventMinute) {
}
