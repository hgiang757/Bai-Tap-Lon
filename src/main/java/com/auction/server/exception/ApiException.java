package com.auction.server.exception;

/** Stable error codes are part of the client/server contract. */
public class ApiException extends RuntimeException {
  private final String code;

  /** Creates a business rejection with a stable protocol error code. */
  public ApiException(String code, String message) {
    super(message);
    this.code = code;
  }

  /** Returns the stable error code used by the client or protocol. */
  public String code() {
    return code;
  }
}
