package com.demo.sso.controller;

import com.demo.sso.exception.InvalidAuthCodeException;
import com.demo.sso.exception.InvalidIdentityException;
import com.demo.sso.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test that exercises {@link GlobalExceptionHandler} through the
 * Spring MVC dispatcher (filter chain, content negotiation, exception
 * resolution) rather than calling handler methods directly.
 */
class GlobalExceptionHandlerIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void missingRequiredParam_returns400() throws Exception {
        mockMvc.perform(get("/test/required-param"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Missing required parameter: q"));
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "InvalidTokenException"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid or expired token"));
    }

    @Test
    void invalidAuthCode_returns410() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "InvalidAuthCodeException"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error").value("Invalid or expired authorization code"));
    }

    @Test
    void invalidIdentity_returns400() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "InvalidIdentityException"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid identity"));
    }

    @Test
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "IllegalArgumentException"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void illegalState_returns500() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "IllegalStateException"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    @Test
    void unexpectedException_returns500() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "RuntimeException"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    @Test
    void malformedRequestBody_returns400() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "HttpMessageNotReadableException"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    @Test
    void responseContentType_isJson() throws Exception {
        mockMvc.perform(get("/test/throw")
                .param("type", "InvalidTokenException")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Minimal controller used only inside this test to trigger specific
     * exception types through the MVC dispatcher.
     */
    @RestController
    static class ThrowingController {

        @GetMapping("/test/required-param")
        public String requireParam(@RequestParam String q) {
            return q;
        }

        @GetMapping("/test/throw")
        public String throwByType(@RequestParam String type) {
            switch (type) {
                case "InvalidTokenException" ->
                    throw new InvalidTokenException("bad token");
                case "InvalidAuthCodeException" ->
                    throw new InvalidAuthCodeException("bad code");
                case "InvalidIdentityException" ->
                    throw new InvalidIdentityException("bad identity");
                case "IllegalArgumentException" ->
                    throw new IllegalArgumentException("bad arg");
                case "IllegalStateException" ->
                    throw new IllegalStateException("broken state");
                case "HttpMessageNotReadableException" ->
                    throw new org.springframework.http.converter.HttpMessageNotReadableException("bad body");
                default ->
                    throw new RuntimeException("unexpected");
            }
        }
    }
}
