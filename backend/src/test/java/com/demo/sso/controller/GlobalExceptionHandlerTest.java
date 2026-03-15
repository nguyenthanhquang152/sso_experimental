package com.demo.sso.controller;

import com.demo.sso.controller.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleMissingParameter_returns400WithParameterName() throws Exception {
        MissingServletRequestParameterException ex =
            new MissingServletRequestParameterException("token", "String");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Missing required parameter: token", response.getBody().message());
    }

    @Test
    void handleUnreadableMessage_returns400WithMessage() {
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("Could not read JSON");

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Malformed request body", response.getBody().message());
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("bad input");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Invalid request", response.getBody().message());
    }

    @Test
    void handleIllegalState_returns500WithGenericMessage() {
        IllegalStateException ex = new IllegalStateException("something broke");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().message());
    }

    @Test
    void handleUnexpected_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("unexpected failure");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().message());
    }
}
