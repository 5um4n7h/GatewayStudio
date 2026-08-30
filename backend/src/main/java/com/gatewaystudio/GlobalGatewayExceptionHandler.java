package com.gatewaystudio;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.http.HttpTimeoutException;

@RestControllerAdvice
public class GlobalGatewayExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    public org.springframework.http.ResponseEntity<String> handleResourceAccessException(
            ResourceAccessException ex) {

        if (isTimeout(ex)) {
            return org.springframework.http.ResponseEntity
                    .status(HttpStatus.GATEWAY_TIMEOUT)
                    .body("Upstream request timed out");
        }

        throw ex;
    }

    private boolean isTimeout(Throwable throwable) {

        while (throwable != null) {

            if (throwable instanceof HttpTimeoutException) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }
}
