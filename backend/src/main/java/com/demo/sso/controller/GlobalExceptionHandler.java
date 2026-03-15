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
        return logAndRespond(400, "Malformed request body", e, "Malformed request body");
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException e) {
        return logAndRespond(401, "Invalid token", e, "Invalid or expired token");
    }

    @ExceptionHandler(InvalidIdentityException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIdentity(InvalidIdentityException e) {
        return logAndRespond(400, "Invalid identity", e, "Invalid identity");
    }

    @ExceptionHandler(InvalidAuthCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAuthCode(InvalidAuthCodeException e) {
        return logAndRespond(410, "Invalid auth code", e, "Invalid or expired authorization code");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return logAndRespond(400, "Bad request", e, "Invalid request");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return logAndRespond(500, "Internal error", e, "Internal server error");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        logger.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Internal server error"));
    }

    private ResponseEntity<ErrorResponse> logAndRespond(int status, String logPrefix, Exception e, String message) {
        if (status >= 500) {
            logger.error("{}: {}", logPrefix, e.getMessage());
        } else {
            logger.warn("{}: {}", logPrefix, e.getMessage());
        }
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
