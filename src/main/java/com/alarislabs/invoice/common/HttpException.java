package com.alarislabs.invoice.common;

import org.springframework.http.HttpStatus;

public class HttpException extends java.lang.Exception {

    private HttpStatus httpStatus;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public HttpException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
