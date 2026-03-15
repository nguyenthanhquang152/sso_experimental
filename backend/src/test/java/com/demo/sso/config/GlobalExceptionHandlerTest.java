package com.demo.sso.config;

import com.demo.sso.controller.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentReturns400WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Bad request", response.getBody().error());
        assertEquals(400, response.getBody().status());
    }

    @Test
    void handleIllegalStateReturns500WithSanitizedMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalState(new IllegalStateException("conflict detected"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().error());
        assertEquals(500, response.getBody().status());
    }

    @Test
    void handleGenericExceptionReturns500WithSanitizedMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGeneric(new RuntimeException("sensitive stack trace details"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().error());
        assertEquals(500, response.getBody().status());
    }
}
