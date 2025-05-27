package com.br.recomendation.recomendation.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "events")
public class EventData {
    
    @Id
    private UUID eventid;
    private Double latitude;
    private Double longitude;

    public EventData(UUID eventid, Double latitude, Double longitude) {
        this.eventid = eventid;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public EventData() {
    }
    
    public UUID getEventid() {
        return eventid;
    }

    public void setEventid(UUID eventid) {
        this.eventid = eventid;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
}
