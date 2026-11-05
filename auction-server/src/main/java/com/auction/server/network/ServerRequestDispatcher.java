package com.auction.server.network;

import com.auction.server.service.*;
import com.auction.shared.network.NetworkRequest;
import com.auction.shared.network.NetworkResponse;
import com.auction.shared.network.GenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ServerRequestDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(ServerRequestDispatcher.class);

    private final UserService userService;
    private final ItemService itemService;
    private final AuctionBiddingService auctionService;
    private final CheckoutService checkoutService;
    private final AuctionSellerService sellerService;
    private final AdminFacadeService adminFacadeService;
    private final AutoBidService autoBidService;
    private final Consumer<NetworkResponse> broadcastCallback;
    private final Consumer<String> forceLogoutCallback;

    public ServerRequestDispatcher(
            UserService userService,
            ItemService itemService,
            AuctionBiddingService auctionService,
            CheckoutService checkoutService,
            AuctionSellerService sellerService,
            AdminFacadeService adminFacadeService,
            AutoBidService autoBidService,
            Consumer<NetworkResponse> broadcastCallback,
            Consumer<String> forceLogoutCallback) {
        this.userService = userService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.checkoutService = checkoutService;
        this.sellerService = sellerService;
        this.adminFacadeService = adminFacadeService;
        this.autoBidService = autoBidService;
        this.broadcastCallback = broadcastCallback;
        this.forceLogoutCallback = forceLogoutCallback;
    }

    public void dispatch(Object requestObj, ClientConnection client) {
        // Kiểm tra xem dữ liệu gửi lên có đúng chuẩn khung NetworkRequest không
        if (!(requestObj instanceof NetworkRequest)) {
            logger.warn("Gói tin không hợp lệ, từ chối xử lý.");
            return;
        }

        NetworkRequest request = (NetworkRequest) requestObj;
        logger.info("Server nhận lệnh: {}", request.getCommand());

        try {
            // Bộ định tuyến rẽ nhánh (Router)
            switch (request.getCommand()) {
                case "LOGIN":
                    // TODO: Gọi userService.login() ở đây và lưu vào client.setCurrentUser(user)
                    client.send(new GenericResponse(true, "Đăng nhập thành công"));
                    break;
                    
                case "BID":
                    // TODO: Gọi auctionService.placeBid() ở đây
                    client.send(new GenericResponse(true, "Ghi nhận giá thầu"));
                    break;
                    
                case "LOGOUT":
                    client.setCurrentUser(null);
                    client.send(new GenericResponse(true, "Đăng xuất thành công"));
                    break;
                    
                default:
                    logger.warn("Lệnh không xác định: {}", request.getCommand());
                    client.send(new GenericResponse(false, "Lệnh không hợp lệ"));
                    break;
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý lệnh {}: {}", request.getCommand(), e.getMessage());
            client.send(new GenericResponse(false, "Lỗi Server: " + e.getMessage()));
        }
    }
}
