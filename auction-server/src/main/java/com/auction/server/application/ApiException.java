package com.auction.server.application;

/** Stable error codes are part of the client/server contract. */
public final class ApiException extends RuntimeException {
    private final String code;
    public ApiException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
