package com.auction.client.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServiceRegistryTest {
  @TempDir Path directory;
  private String previousMode;

  @BeforeEach
  void mockFixture() {
    previousMode = System.getProperty("auction.mode");
    System.setProperty("auction.mode", "server");
    ServiceRegistry.close();
  }

  @AfterEach
  void restoreMode() {
    ServiceRegistry.close();
    if (previousMode == null) {
      System.clearProperty("auction.mode");
    } else {
      System.setProperty("auction.mode", previousMode);
    }
  }

  @Test
  void absentModeUsesOnlyServerAdaptersAndFailureNeverEnablesMock() throws Exception {
    System.clearProperty("auction.mode");
    String previousHost = System.getProperty("auction.host");
    String previousPort = System.getProperty("auction.port");
    String previousDirectory = System.getProperty("auction.client.data");
    try (var unavailable = new java.net.ServerSocket(0)) {
      int port = unavailable.getLocalPort();
      unavailable.close();
      System.setProperty("auction.host", "127.0.0.1");
      System.setProperty("auction.port", Integer.toString(port));
      System.setProperty("auction.client.data", directory.toString());
      var adapter = ServiceRegistry.getService();
      assertInstanceOf(com.auction.client.service.server.ServerServices.class, adapter);
      assertSame(adapter, ServiceRegistry.getWalletService());
      assertSame(adapter, ServiceRegistry.getAutoBidService());
      assertSame(adapter, ServiceRegistry.getProductService());
      assertSame(adapter, ServiceRegistry.getUserAdminService());
      assertSame(adapter, ServiceRegistry.getAuctionAdminService());
      assertEquals("SERVER", ServiceRegistry.modeLabel());
      assertThrows(IllegalStateException.class, () -> adapter.getAuctions(null));
      assertSame(adapter, ServiceRegistry.getService());
    } finally {
      restore("auction.host", previousHost);
      restore("auction.port", previousPort);
      restore("auction.client.data", previousDirectory);
    }
  }

  private static void restore(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

  @Test
  void unsupportedModeDoesNotFallBackToMock() {
    String previous = System.getProperty("auction.mode");
    try {
      for (String mode : new String[] {"unknown", "mock"}) {
        System.setProperty("auction.mode", mode);
        assertThrows(IllegalStateException.class, ServiceRegistry::getService);
        assertThrows(IllegalStateException.class, ServiceRegistry::getWalletService);
      }
    } finally {
      if (previous == null) {
        System.clearProperty("auction.mode");
      } else {
        System.setProperty("auction.mode", previous);
      }
    }
  }

  @Test
  void replacingOnlyAuctionCannotSilentlyKeepAnotherTcpSession() {
    AuctionClientService before = ServiceRegistry.getService();
    AuctionClientService adapter =
        (AuctionClientService)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {AuctionClientService.class},
                (proxy, method, args) -> {
                  throw new UnsupportedOperationException();
                });
    assertThrows(IllegalArgumentException.class, () -> ServiceRegistry.setService(adapter));
    assertSame(before, ServiceRegistry.getService());
    assertThrows(NullPointerException.class, () -> ServiceRegistry.setProductService(null));
    assertNotNull(ServiceRegistry.getProductService());
  }
}
