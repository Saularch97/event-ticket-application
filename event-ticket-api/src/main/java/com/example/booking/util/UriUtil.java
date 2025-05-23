package com.example.booking.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

public class UriUtil {
    public static URI getUriLocation(String idName, UUID id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("{" + idName + "}")
                .buildAndExpand(id)
                .toUri();
    }
}
