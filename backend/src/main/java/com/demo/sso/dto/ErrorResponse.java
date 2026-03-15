package com.demo.sso.dto;

import com.demo.sso.controller.dto.AuthApiResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Error response with {@code {"error": "..."}} JSON shape. */
public record ErrorResponse(@JsonProperty("error") String message) implements AuthApiResponse {
}
