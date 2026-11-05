package com.auction.server.network;

import com.auction.server.event.AuctionObserver;
import com.auction.server.event.AutoBidNotificationEvent;
import com.auction.server.event.UserNotificationEvent;
import com.auction.server.event.UserNotificationObserver;
import com.auction.server.event.UserNotificationPublisher;
import com.auction.server.event.WalletChangedEvent;
import com.auction.server.service.AdminFacadeService;
import com.auction.server.service.AuctionBiddingService;
import com.auction.server.service.AuctionSellerService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.CheckoutService;
import com.auction.server.service.ItemService;
import com.auction.server.service.UserService;
import com.auction.shared.exception.AuthenticationException;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.User;
import com.auction.shared.network.AuctionEndedResponse;
import com.auction.shared.network.AutoBidUpdateResponse;
import com.auction.shared.network.BidUpdateResponse;
import com.auction.shared.network.GenericResponse;
import com.auction.shared.network.NetworkResponse;
import com.auction.shared.network.WalletTransactionResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable, ClientConnection, AuctionObserver, UserNotificationObserver {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final String SERIALIZATION_FILTER = String.join(
            ";",
            "com.auction.shared.model.*",
            "com.auction.shared.network.*",
            "com.auction.shared.exception.*",
            "java.util.*",
            "java.lang.*",
            "java.time.*",
            "!*"
    );

    public static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    private final Socket socket;
    private final UserService userService;
    private final UserNotificationPublisher notificationPublisher;
    private final ServerRequestDispatcher dispatcher;

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private volatile boolean running = true;
    private volatile User currentUser;

    public ClientHandler(
            Socket socket,
            UserService userService,
            ItemService itemService,
            AuctionBiddingService auctionService,
            CheckoutService checkoutService,
            AuctionSellerService sellerService,
            AdminFacadeService adminFacadeService,
            AutoBidService autoBidService,
            UserNotificationPublisher notificationPublisher) {
        this.socket = socket;
        this.userService = userService;
        this.notificationPublisher = notificationPublisher;
        this.dispatcher = new ServerRequestDispatcher(
                userService,
                itemService,
                auctionService,
                checkoutService,
                sellerService,
                adminFacadeService,
                autoBidService,
                ClientHandler::broadcastToAll,
                ClientHandler::forceLogoutUser);
        activeClients.add(this);
        this.notificationPublisher.subscribe(this);
    }

    @Override
    public void run() {
        try {
            openStreams();
            listen();
        } catch (EOFException e) {
            logger.info("Client đã ngắt kết nối.");
        } catch (IOException | ClassNotFoundException e) {
            logger.warn("Kết nối client kết thúc: {}", e.getMessage());
        } finally {
            close();
        }
    }

    public static void broadcastToAll(NetworkResponse response) {
        for (ClientHandler client : activeClients) {
            CompletableFuture.runAsync(() -> client.send(response));
        }
    }

    public static void forceLogoutUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        for (ClientHandler client : activeClients) {
            User user = client.currentUser;
            if (user != null && username.equals(user.getUsername())) {
                CompletableFuture.runAsync(() -> client.forceLogout("Tài khoản của bạn đã bị khóa bởi Admin."));
            }
        }
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @Override
    public void refreshCurrentUser(String username) {
        User user = currentUser;
        if (user == null) {
            return;
        }

        try {
            currentUser = userService.login(username, user.getPassword());
        } catch (AuthenticationException e) {
            logger.warn("Không thể đồng bộ user '{}': {}", username, e.getMessage());
        }
    }

    @Override
    public synchronized void send(NetworkResponse response) {
        try {
            if (output == null || socket.isClosed()) {
                return;
            }
            output.writeObject(response);
            output.flush();
            output.reset();
        } catch (IOException e) {
            logger.warn("Không thể gửi dữ liệu đến client: {}", e.getMessage());
        }
    }

    @Override
    public void onNewBid(BidTransaction transaction, LocalDateTime newEndTime) {
        send(new BidUpdateResponse(transaction.getAuctionId(), transaction, newEndTime));
    }

    @Override
    public void onAuctionEnded(Auction auction, Bidder winner) {
        String winnerName = winner == null ? null : winner.getUsername();
        String winnerId = winner == null ? null : winner.getId();
        send(new AuctionEndedResponse(auction.getId(), winnerName, winnerId, auction.getCurrentHighestBid()));
        broadcastToAll(new GenericResponse(true, "SYS_REFRESH_AUCTIONS"));
    }

    @Override
    public void onAuctionCanceled(String auctionId, String reason) {
        send(new GenericResponse(false, "Phiên đấu giá đã bị hủy: " + reason));
    }

    @Override
    public void onSystemMessage(String auctionId, String message) {
        send(new GenericResponse(true, "SYS_MSG: " + message));
    }

    @Override
    public void onUserNotification(String userId, UserNotificationEvent event) {
        User user = currentUser;
        if (user == null || !user.getId().equals(userId)) {
            return;
        }

        if (event instanceof WalletChangedEvent) {
            sendWalletChanged((WalletChangedEvent) event);
        } else if (event instanceof AutoBidNotificationEvent) {
            sendAutoBidChanged((AutoBidNotificationEvent) event);
        }
    }

    private void openStreams() throws IOException {
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
        input.setObjectInputFilter(ObjectInputFilter.Config.createFilter(SERIALIZATION_FILTER));
    }

    private void listen() throws IOException, ClassNotFoundException {
        while (running) {
            dispatcher.dispatch(input.readObject(), this);
        }
    }

    private void forceLogout(String reason) {
        send(new GenericResponse(false, "BAN_ACCOUNT:" + reason));
        currentUser = null;
    }

    private void sendWalletChanged(WalletChangedEvent event) {
        send(new WalletTransactionResponse(
                true,
                event.getMessage(),
                event.getNewBalance(),
                event.getTransaction()));
    }

    private void sendAutoBidChanged(AutoBidNotificationEvent event) {
        send(new AutoBidUpdateResponse(
                event.getAuctionId(),
                event.getAutoBidAmount(),
                event.getMaxBidAmount(),
                event.isMaxReached()));
    }

    private void close() {
        running = false;
        activeClients.remove(this);
        notificationPublisher.unsubscribe(this);
        closeQuietly(input);
        closeQuietly(output);
        closeSocket();
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            logger.debug("Bỏ qua lỗi đóng tài nguyên: {}", e.getMessage());
        }
    }

    private void closeSocket() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.debug("Bỏ qua lỗi đóng socket: {}", e.getMessage());
        }
    }
}
