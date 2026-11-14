package com.auction.client.fixture;

import com.auction.client.service.ServiceRegistry;

/** Explicit, isolated dependency injection for GUI tests; never loaded by the application. */
public final class FixtureServices {
  private FixtureServices() {}

  /** Installs a fresh in-memory service set for isolated client tests. */
  public static void install() {
    MockDataStore store = MockDataStore.createForTesting();
    var auction = new MockAuctionClientService(store);
    var management = new MockManagementService(store);
    ServiceRegistry.setServices(
        auction,
        new MockWalletService(store),
        new MockAutoBidService(store, () -> auction),
        management,
        management,
        management);
  }
}
