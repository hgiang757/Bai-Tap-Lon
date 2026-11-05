package com.auction.server.network;

/** Structured failure at the transport/business boundary. */
public final class NetworkException extends RuntimeException {
    private final String code;
    public NetworkException(String code, String message) { super(message); this.code=code; }
    public String code() { return code; }
}
