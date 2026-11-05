package com.auction.client.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

class ServiceRegistryTest {
    @Test
    void unsupportedModeDoesNotFallBackToMock() {
        String previous = System.getProperty("auction.mode");
        try {
            System.setProperty("auction.mode", "unknown");
            assertThrows(IllegalStateException.class, ServiceRegistry::getService);
            assertThrows(IllegalStateException.class, ServiceRegistry::getWalletService);
        } finally {
            if (previous == null) System.clearProperty("auction.mode");
            else System.setProperty("auction.mode", previous);
        }
    }

    @Test
    void replacingOnlyAuctionCannotSilentlyKeepOtherMocks() {
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
