package com.auction.client.service;

import com.auction.client.service.mock.MockAuctionClientService;
/**
 * Service Locator / Registry quản lý instance của AuctionClientService.
 * Giúp các Controller hoàn toàn tách rời (loose-coupling) với việc khởi tạo Service,
 * cho phép hoán đổi linh hoạt giữa MockService và SocketService/NetworkService thực tế.
 */
public class ServiceRegistry {
    private static AuctionClientService instance;
    private static WalletService walletService = new com.auction.client.service.mock.MockWalletService();
    /** Lấy dịch vụ ví mô phỏng. */
    public static synchronized WalletService getWalletService() { return walletService; }
    /** Thay dịch vụ ví. */
    public static synchronized void setWalletService(WalletService service) { walletService = java.util.Objects.requireNonNull(service); }
    private static final com.auction.client.service.mock.MockManagementService MANAGEMENT =
            new com.auction.client.service.mock.MockManagementService();
    private static ProductService productService = MANAGEMENT;
    private static UserAdminService userAdminService = MANAGEMENT;
    private static AuctionAdminService auctionAdminService = MANAGEMENT;

    /** Lấy dịch vụ sản phẩm. */
    public static ProductService getProductService() { return productService; }
    /** Thay dịch vụ sản phẩm khi kết nối Socket. */
    public static void setProductService(ProductService service) { productService = service; }
    /** Lấy dịch vụ quản trị người dùng. */
    public static UserAdminService getUserAdminService() { return userAdminService; }
    /** Thay dịch vụ quản trị người dùng khi kết nối Socket. */
    public static void setUserAdminService(UserAdminService service) { userAdminService = service; }
    /** Lấy dịch vụ quản trị phiên. */
    public static AuctionAdminService getAuctionAdminService() { return auctionAdminService; }
    /** Thay dịch vụ quản trị phiên khi kết nối Socket. */
    public static void setAuctionAdminService(AuctionAdminService service) { auctionAdminService = service; }

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
