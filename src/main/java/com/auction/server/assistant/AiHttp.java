package com.auction.server.assistant;

import com.auction.server.exception.ApiException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;

/** Bounds the entire provider exchange, including reading the response body. */
final class AiHttp implements AutoCloseable {
  private static final int MAX_RESPONSE_BYTES = 128 * 1024;
  private final HttpClient http;
  private final Duration timeout;

  AiHttp(Duration timeout) {
    this.timeout = timeout;
    http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  HttpResponse<String> post(HttpRequest request, boolean retryUnavailable)
      throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    var response = exchange(request, deadline);
    // Retry at most once for an explicit temporary rejection, on the original deadline.
    if (retryUnavailable
        && response.statusCode() == 503
        && deadline - System.nanoTime() > Duration.ofSeconds(1).toNanos()) {
      Thread.sleep(250);
      return exchange(request, deadline);
    }
    return response;
  }

  private HttpResponse<String> exchange(HttpRequest request, long deadline)
      throws InterruptedException {
    long remaining = deadline - System.nanoTime();
    if (remaining <= 0) {
      throw timeout(new TimeoutException("Provider deadline expired"));
    }
    var exchange = http.sendAsync(request, HttpResponse.BodyHandlers.limiting(
        HttpResponse.BodyHandlers.ofString(), MAX_RESPONSE_BYTES));
    try {
      return exchange.get(remaining, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      exchange.cancel(true);
      throw e;
    } catch (TimeoutException e) {
      exchange.cancel(true);
      throw timeout(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof HttpTimeoutException) {
        throw timeout(cause);
      }
      if (cause instanceof IOException) {
        throw new ApiException(networkCode(cause), "Cannot connect to the AI provider");
      }
      throw new ApiException("AI_PROVIDER_ERROR", "AI provider exchange failed");
    }
  }

  private static String networkCode(Throwable cause) {
    for (Throwable failure = cause; failure != null; failure = failure.getCause()) {
      if (failure instanceof SSLException) {
        return "AI_TLS_ERROR";
      }
      if (failure instanceof UnknownHostException
          || failure instanceof UnresolvedAddressException) {
        return "AI_DNS_ERROR";
      }
    }
    return "AI_NETWORK_ERROR";
  }

  private static ApiException timeout(Throwable cause) {
    return new ApiException("AI_TIMEOUT", "AI provider deadline exceeded", cause);
  }

  @Override
  public void close() {
    http.shutdownNow();
  }
}
