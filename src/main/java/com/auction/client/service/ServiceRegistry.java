package com.auction.client.service;

import com.auction.client.service.server.ServerServices;
import java.util.Objects;

/** Một bộ adapter TCP dùng chung; setServices hỗ trợ dependency injection trong test. */
public final class ServiceRegistry {
  private static Services services;
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
      boolean server =
          auction instanceof ServerServices
              || wallet instanceof ServerServices
              || autoBid instanceof ServerServices
              || products instanceof ServerServices
              || users instanceof ServerServices
              || auctions instanceof ServerServices;
      if (server
          && !(auction == wallet
              && auction == autoBid
              && auction == products
              && auction == users
              && auction == auctions)) {
        throw new IllegalArgumentException(
            "Các dịch vụ TCP phải dùng cùng một ServerServices. Hãy dùng setServices.");
      }
    }
  }

  /** Requires server mode and lazily initializes the shared TCP adapter. */
  public static synchronized void validateMode() {
    String mode = System.getProperty("auction.mode", "server").trim();
    if (!"server".equalsIgnoreCase(mode)) {
      throw new IllegalStateException("Chế độ không hợp lệ: " + mode);
    }
    if (services == null) {
      var adapter = new ServerServices();
      services = new Services(adapter, adapter, adapter, adapter, adapter, adapter);
      bidSubmissions = new BidSubmissionTracker();
    }
  }

  /** Closes the configured TCP adapter and clears the shared services. */
  public static synchronized void close() {
    if (services != null
        && services.auction() instanceof com.auction.client.service.server.ServerServices adapter) {
      adapter.close();
    }
    services = null;
  }

  /** Returns the display label of the supported connection mode. */
  public static String modeLabel() {
    return "SERVER";
  }

  /** Returns the shared auction service, initializing the TCP adapter if needed. */
  public static synchronized AuctionClientService getService() {
    validateMode();
    return services.auction();
  }

  /** Returns the wallet service from the configured service set. */
  public static synchronized WalletService getWalletService() {
    validateMode();
    return services.wallet();
  }

  /** Returns the automatic bidding service from the configured service set. */
  public static synchronized AutoBidService getAutoBidService() {
    validateMode();
    return services.autoBid();
  }

  /** Returns the product service from the configured service set. */
  public static synchronized ProductService getProductService() {
    validateMode();
    return services.products();
  }

  /** Returns the user administration service from the configured service set. */
  public static synchronized UserAdminService getUserAdminService() {
    validateMode();
    return services.users();
  }

  /** Returns the auction administration service from the configured service set. */
  public static synchronized AuctionAdminService getAuctionAdminService() {
    validateMode();
    return services.auctions();
  }

  /** Returns the submission tracker shared across view changes. */
  public static synchronized BidSubmissionTracker getBidSubmissions() {
    validateMode();
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
    if (services == null || services.auction() != auction) {
      close();
      bidSubmissions = new BidSubmissionTracker();
    }
    services = next;
  }

  // Các setter giữ khả năng tiêm test double; không trộn các phiên TCP khác nhau.
  /** Replaces the auction service while validating that TCP sessions are not mixed. */
  public static synchronized void setService(AuctionClientService service) {
    validateMode();
    setServices(
        service,
        services.wallet(),
        services.autoBid(),
        services.products(),
        services.users(),
        services.auctions());
  }

  /** Replaces the wallet service while validating the complete service set. */
  public static synchronized void setWalletService(WalletService service) {
    validateMode();
    setServices(
        services.auction(),
        service,
        services.autoBid(),
        services.products(),
        services.users(),
        services.auctions());
  }

  /** Replaces the automatic bidding service while validating the complete service set. */
  public static synchronized void setAutoBidService(AutoBidService service) {
    validateMode();
    setServices(
        services.auction(),
        services.wallet(),
        service,
        services.products(),
        services.users(),
        services.auctions());
  }

  /** Replaces the product service while validating the complete service set. */
  public static synchronized void setProductService(ProductService service) {
    validateMode();
    setServices(
        services.auction(),
        services.wallet(),
        services.autoBid(),
        service,
        services.users(),
        services.auctions());
  }

  /** Replaces the user administration service while validating the complete service set. */
  public static synchronized void setUserAdminService(UserAdminService service) {
    validateMode();
    setServices(
        services.auction(),
        services.wallet(),
        services.autoBid(),
        services.products(),
        service,
        services.auctions());
  }

  /** Replaces the auction administration service while validating the complete service set. */
  public static synchronized void setAuctionAdminService(AuctionAdminService service) {
    validateMode();
    setServices(
        services.auction(),
        services.wallet(),
        services.autoBid(),
        services.products(),
        services.users(),
        service);
  }
}
