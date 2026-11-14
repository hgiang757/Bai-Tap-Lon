package com.auction.server.exception;

/** Rejects invalid login credentials or an unavailable account. */
public final class AuthenticationException extends ApiException {
  /** Creates a failed-login rejection. */
  public AuthenticationException(String message) {
    super("AUTHENTICATION_FAILED", message);
  }
}
