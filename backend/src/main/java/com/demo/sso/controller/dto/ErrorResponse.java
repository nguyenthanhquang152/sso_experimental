package com.demo.sso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Error response with {@code {"error": "..."}} JSON shape. */
public record ErrorResponse(@JsonProperty("error") String message) implements AuthApiResponse {
}
