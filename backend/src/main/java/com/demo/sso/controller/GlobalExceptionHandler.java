package com.demo.sso.controller;

import com.demo.sso.dto.ErrorResponse;
import com.demo.sso.exception.InvalidAuthCodeException;
import com.demo.sso.exception.InvalidIdentityException;
import com.demo.sso.exception.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        logger.warn("Missing parameter: {}", e.getParameterName());
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Missing required parameter: " + e.getParameterName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException e) {
        logger.warn("Malformed request body: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("Malformed request body"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException e) {
        logger.warn("Invalid token: {}", e.getMessage());
        return ResponseEntity.status(401).body(new ErrorResponse("Invalid or expired token"));
    }

    @ExceptionHandler(InvalidIdentityException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIdentity(InvalidIdentityException e) {
        logger.warn("Invalid identity: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid identity"));
    }

    @ExceptionHandler(InvalidAuthCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAuthCode(InvalidAuthCodeException e) {
        logger.warn("Invalid auth code: {}", e.getMessage());
        return ResponseEntity.status(410).body(new ErrorResponse("Invalid or expired authorization code"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        logger.error("Internal error: {}", e.getMessage());
        return ResponseEntity.internalServerError().body(new ErrorResponse("Internal server error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        logger.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Internal server error"));
    }
}
