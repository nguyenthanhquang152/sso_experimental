package com.demo.sso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Error response DTO. The JSON field is {"error"} for all error responses. */
public record ErrorResponse(@JsonProperty("error") String message) implements AuthApiResponse {
}
