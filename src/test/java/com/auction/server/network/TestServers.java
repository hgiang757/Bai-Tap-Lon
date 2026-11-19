package com.auction.server.network;

import com.auction.server.assistant.AuctionAssistant;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

/** Test-only construction: isolated ports/data without a production Singleton bypass. */
public final class TestServers {
  private TestServers() {}

  /**
   * Creates an isolated test server through the private constructor without a production factory.
   */
  public static Server create(String host, int port, Path data, boolean demo) throws IOException {
    // Existing suites do not need a provider.
    return create(host, port, data, demo, null);
  }

  /** Creates an isolated TCP server with a supplied test assistant. */
  public static Server create(
      String host, int port, Path data, boolean demo, AuctionAssistant assistant)
      throws IOException {
    try {
      if (assistant != null) {
        var constructor =
            Server.class.getDeclaredConstructor(
                String.class, int.class, Path.class, boolean.class, AuctionAssistant.class);
        constructor.setAccessible(true);
        return constructor.newInstance(host, port, data, demo, assistant);
      }
      var constructor =
          Server.class.getDeclaredConstructor(String.class, int.class, Path.class, boolean.class);
      constructor.setAccessible(true);
      return constructor.newInstance(host, port, data, demo);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof IOException io) {
        throw io;
      }
      if (e.getCause() instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new AssertionError("Cannot construct test server", e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Cannot access test server constructor", e);
    }
  }
}
