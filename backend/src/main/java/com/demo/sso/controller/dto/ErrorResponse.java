package com.demo.sso.controller.dto;

public record ErrorResponse(String error, int status) {

    public static ErrorResponse of(String error, int status) {
        return new ErrorResponse(error, status);
    }
}
