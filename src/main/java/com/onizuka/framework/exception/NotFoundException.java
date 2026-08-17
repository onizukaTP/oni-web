package com.onizuka.framework.exception;

public class NotFoundException extends HttpException {
    public NotFoundException(String message) {
        super(404, message);
    }
}
