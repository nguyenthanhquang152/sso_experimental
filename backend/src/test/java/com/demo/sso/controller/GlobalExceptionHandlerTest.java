package com.demo.sso.controller;

import com.demo.sso.controller.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("bad input");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("bad input", response.getBody().error());
    }

    @Test
    void handleIllegalState_returns500WithGenericMessage() {
        IllegalStateException ex = new IllegalStateException("something broke");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().error());
    }

    @Test
    void handleUnexpected_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("unexpected failure");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().error());
    }
}
