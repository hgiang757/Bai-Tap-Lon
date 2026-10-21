package com.hieu.services;

/**
 * Service Locator / Registry quản lý instance của AuctionClientService.
 * Giúp các Controller hoàn toàn tách rời (loose-coupling) với việc khởi tạo Service,
 * cho phép hoán đổi linh hoạt giữa MockService và SocketService/NetworkService thực tế.
 */
public class ServiceRegistry {
    private static AuctionClientService instance;

    private ServiceRegistry() {
    }

    public static synchronized AuctionClientService getService() {
        if (instance == null) {
            instance = new MockAuctionClientService();
        }
        return instance;
    }

    public static synchronized void setService(AuctionClientService service) {
        instance = service;
    }
}
