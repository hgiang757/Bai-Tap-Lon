package com.auction.client.service;

import com.auction.client.service.mock.*;

import java.util.Objects;

/** Bộ dịch vụ dùng chung. Thay cả bộ khi tích hợp adapter để không trộn mock/server. */
public final class ServiceRegistry {
    private static Services services = mockServices();
    private static BidSubmissionTracker bidSubmissions = new BidSubmissionTracker();

    private ServiceRegistry() {}

    private record Services(
            AuctionClientService auction,
            WalletService wallet,
            AutoBidService autoBid,
            ProductService products,
            UserAdminService users,
            AuctionAdminService auctions) {
        Services {
            Objects.requireNonNull(auction);
            Objects.requireNonNull(wallet);
            Objects.requireNonNull(autoBid);
            Objects.requireNonNull(products);
            Objects.requireNonNull(users);
            Objects.requireNonNull(auctions);
            boolean mock = auction instanceof MockAuctionClientService;
            if (mock != (wallet instanceof MockWalletService)
                    || mock != (autoBid instanceof MockAutoBidService)
                    || mock != (products instanceof MockManagementService)
                    || mock != (users instanceof MockManagementService)
                    || mock != (auctions instanceof MockManagementService)) {
                throw new IllegalArgumentException(
                        "Không được trộn dịch vụ mock với dịch vụ server. Hãy dùng setServices.");
            }
        }
    }

    private static Services mockServices() {
        MockDataStore store = MockDataStore.getInstance();
        MockManagementService management = new MockManagementService(store);
        return new Services(
                new MockAuctionClientService(store),
                new MockWalletService(store),
                new MockAutoBidService(store, ServiceRegistry::getService),
                management,
                management,
                management);
    }

    public static synchronized void validateMode() {
        String mode = System.getProperty("auction.mode", "mock").trim();
        if (!"mock".equalsIgnoreCase(mode) && !"server".equalsIgnoreCase(mode))
            throw new IllegalStateException("Chế độ không hợp lệ: " + mode);
        if ("server".equalsIgnoreCase(mode)
                && !(services.auction()
                        instanceof com.auction.client.service.server.ServerServices)) {
            var adapter = new com.auction.client.service.server.ServerServices();
            services = new Services(adapter, adapter, adapter, adapter, adapter, adapter);
            bidSubmissions = new BidSubmissionTracker();
        } else if ("mock".equalsIgnoreCase(mode)
                && services.auction()
                        instanceof com.auction.client.service.server.ServerServices adapter) {
            adapter.close();
            services = mockServices();
            bidSubmissions = new BidSubmissionTracker();
        }
    }

    public static synchronized void close() {
        if (services.auction() instanceof com.auction.client.service.server.ServerServices adapter)
            adapter.close();
    }

    public static String modeLabel() {
        return "server".equalsIgnoreCase(System.getProperty("auction.mode", "mock"))
                ? "SERVER"
                : "MOCK";
    }

    public static synchronized AuctionClientService getService() {
        validateMode();
        return services.auction();
    }

    public static synchronized WalletService getWalletService() {
        validateMode();
        return services.wallet();
    }

    public static synchronized AutoBidService getAutoBidService() {
        validateMode();
        return services.autoBid();
    }

    public static synchronized ProductService getProductService() {
        validateMode();
        return services.products();
    }

    public static synchronized UserAdminService getUserAdminService() {
        validateMode();
        return services.users();
    }

    public static synchronized AuctionAdminService getAuctionAdminService() {
        validateMode();
        return services.auctions();
    }

    public static synchronized BidSubmissionTracker getBidSubmissions() {
        return bidSubmissions;
    }

    /** Thay nguyên bộ dịch vụ, dùng cho cấu hình kết nối và kiểm thử tích hợp. */
    public static synchronized void setServices(
            AuctionClientService auction,
            WalletService wallet,
            AutoBidService autoBid,
            ProductService products,
            UserAdminService users,
            AuctionAdminService auctions) {
        Services next = new Services(auction, wallet, autoBid, products, users, auctions);
        if (services.auction() != auction) bidSubmissions = new BidSubmissionTracker();
        services = next;
    }

    // Giữ tên public cũ để tương thích nơi tích hợp. Việc thay riêng không được trộn mock/server.
    public static synchronized void setService(AuctionClientService service) {
        setServices(
                service,
                services.wallet(),
                services.autoBid(),
                services.products(),
                services.users(),
                services.auctions());
    }

    public static synchronized void setWalletService(WalletService service) {
        setServices(
                services.auction(),
                service,
                services.autoBid(),
                services.products(),
                services.users(),
                services.auctions());
    }

    public static synchronized void setAutoBidService(AutoBidService service) {
        setServices(
                services.auction(),
                services.wallet(),
                service,
                services.products(),
                services.users(),
                services.auctions());
    }

    public static synchronized void setProductService(ProductService service) {
        setServices(
                services.auction(),
                services.wallet(),
                services.autoBid(),
                service,
                services.users(),
                services.auctions());
    }

    public static synchronized void setUserAdminService(UserAdminService service) {
        setServices(
                services.auction(),
                services.wallet(),
                services.autoBid(),
                services.products(),
                service,
                services.auctions());
    }

    public static synchronized void setAuctionAdminService(AuctionAdminService service) {
        setServices(
                services.auction(),
                services.wallet(),
                services.autoBid(),
                services.products(),
                services.users(),
                service);
    }
}
