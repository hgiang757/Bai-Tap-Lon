package com.auction.server.network;

import com.auction.server.dao.*;
import com.auction.server.event.AuctionEventPublisher;
import com.auction.server.event.UserNotificationPublisher;
import com.auction.server.factory.ArtCreator;
import com.auction.server.factory.ElectronicsCreator;
import com.auction.server.factory.ItemFactory;
import com.auction.server.factory.VehicleCreator;
import com.auction.server.service.*;
import com.auction.server.strategy.antisniping.AntiSnipingStrategy;
import com.auction.server.strategy.antisniping.ConfigurableAntiSnipingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
    private static final Logger logger = LoggerFactory.getLogger(AuctionServer.class);
    private static final int PORT = 8080;
    private static final int MAX_CLIENT_THREADS = 100;

    private boolean isRunning = true;
    private ExecutorService clientThreadPool;
    private ServerSocket serverSocket;

    private UserService userService;
    private ItemService itemService;
    private AuctionBiddingService auctionService;
    private CheckoutService checkoutService;
    private AuctionSellerService auctionManagementService;
    private AdminFacadeService adminFacadeService;
    private AuctionSchedulerService schedulerService;
    private AutoBidService autoBidService;
    private UserNotificationPublisher userNotificationPublisher;

    public AuctionServer() {
        registerItemCreators();

        UserDAO userDAO = new FileUserDAOImpl();
        AuctionDAO auctionDAO = new FileAuctionDAOImpl();
        ItemDAO itemDAO = new FileItemDAOImpl();

        seedDefaultAdmin(userDAO);

        AuctionEventPublisher auctionEventPublisher = new AuctionEventPublisher();
        this.userNotificationPublisher = new UserNotificationPublisher();

        AntiSnipingStrategy snipingStrategy = new ConfigurableAntiSnipingStrategy(1, 2);

        this.userService = new UserService(userDAO, userNotificationPublisher);
        this.itemService = new ItemService(itemDAO);
        this.autoBidService = new AutoBidService(this.userService, userNotificationPublisher);

        this.auctionService = new AuctionBiddingService(auctionDAO, auctionEventPublisher, snipingStrategy,
                this.userService);
        this.auctionService.setAutoBidService(this.autoBidService);

        this.checkoutService = new CheckoutService(auctionDAO, this.userService);
        this.auctionManagementService = new AuctionSellerService(auctionDAO, this.autoBidService);

        AdminUserService adminUserService = new AdminUserService(userDAO);
        AdminAuctionService adminAuctionService = new AdminAuctionService(
                auctionDAO, auctionEventPublisher, this.autoBidService);
        this.adminFacadeService = new AdminFacadeService(adminUserService, adminAuctionService);

        this.schedulerService = new AuctionSchedulerService(
                auctionDAO, this.userService, auctionEventPublisher, this.autoBidService);

        this.clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENT_THREADS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void start() {
        schedulerService.start();

        try {
            serverSocket = new ServerSocket(PORT);
            logger.info("=========================================");
            logger.info("Auction Server da khoi dong tai cong: {}", PORT);
            logger.info("=========================================");

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client moi ket noi: {}", clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(
                        clientSocket,
                        userService,
                        itemService,
                        auctionService,
                        checkoutService,
                        auctionManagementService,
                        adminFacadeService,
                        autoBidService,
                        userNotificationPublisher);
                clientThreadPool.submit(handler);
            }
        } catch (IOException e) {
            if (isRunning) {
                logger.error("Loi khoi chay Server: {}", e.getMessage(), e);
            }
        }
    }

    public void shutdown() {
        logger.info("Dang tien hanh tat Server...");
        isRunning = false;

        try {
            if (schedulerService != null) {
                schedulerService.stop();
            }
            if (autoBidService != null) {
                autoBidService.shutdown();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (clientThreadPool != null) {
                clientThreadPool.shutdown();
                if (!clientThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientThreadPool.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
             logger.error("Interrupted during shutdown: {}", e.getMessage());
             Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Loi khi tat Server: {}", e.getMessage());
        }
    }

    private void registerItemCreators() {
        ItemFactory factory = ItemFactory.getInstance();
        factory.registerCreator(ItemFactory.ItemType.ART, new ArtCreator());
        factory.registerCreator(ItemFactory.ItemType.ELECTRONICS, new ElectronicsCreator());
        factory.registerCreator(ItemFactory.ItemType.VEHICLE, new VehicleCreator());
    }

    private void seedDefaultAdmin(UserDAO userDAO) {
        if (userDAO.findByUsername("admin") == null) {
            com.auction.shared.model.Admin defaultAdmin = new com.auction.shared.model.Admin("admin", "admin123",
                    "admin@auction.com");
            userDAO.save(defaultAdmin);
            logger.info("Da tao tai khoan Admin mac dinh: admin / admin123");
        }
    }

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        server.start();
    }
}
