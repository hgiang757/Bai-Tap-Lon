package com.auction.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.client.model.*;
import com.auction.client.service.server.*;
import com.auction.server.network.Server;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

class FullStackTest {
    @TempDir Path directory;
    Server server;
    ServerServices seller, buyer, other, admin;

    @BeforeEach
    void start() throws Exception {
        server = new Server("127.0.0.1", 0, directory.resolve("server"), true);
        server.start();
        seller = client("seller");
        buyer = client("buyer");
        other = client("other");
        admin = client("admin");
        assertTrue(seller.login("seller_hieu", "Seller12345", null));
        assertTrue(buyer.login("bidder_hieu", "Bidder12345", null));
        assertTrue(other.login("bidder_vip99", "Bidder12345", null));
        assertTrue(admin.login("admin_hieu", "Admin12345", null));
    }

    ServerServices client(String name) {
        return new ServerServices("127.0.0.1", server.port(), directory.resolve(name));
    }

    @AfterEach
    void stop() throws Exception {
        seller.close();
        buyer.close();
        other.close();
        admin.close();
        server.close();
    }

    @Test
    void sellerUploadsImageBidderReadsItAndAdminManagesProduct() throws Exception {
        Path image = directory.resolve("product.png");
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", image.toFile());
        LocalDateTime start = seller.serverNow().plusMinutes(5);
        Item item = new Item("draft-item", "Ảnh thật", "Mô tả", "ART", 1_000_000, "seller_hieu");
        Auction draft =
                new Auction(
                        "draft",
                        item,
                        "seller_hieu",
                        1_000_000,
                        null,
                        AuctionStatus.OPEN,
                        start,
                        start.plusHours(1),
                        100_000);
        seller.saveProduct(draft, image.toString());
        Auction saved =
                seller.listProducts("seller_hieu").stream()
                        .filter(a -> a.getItemName().equals("Ảnh thật"))
                        .findFirst()
                        .orElseThrow();
        assertNotEquals("draft", saved.getId());
        Path downloaded = Path.of(buyer.getImagePath(saved.getId()));
        assertArrayEquals(Files.readAllBytes(image), Files.readAllBytes(downloaded));
        assertEquals(downloaded, Path.of(buyer.getImagePath(saved.getId())),
                "Refreshing an unchanged image must reuse the cached file");
        saved.getItem().setName("Đã sửa");
        seller.saveProduct(saved, downloaded.toString());
        assertEquals("Đã sửa", buyer.getAuctionById(saved.getId()).getItemName());
        ImageIO.write(new BufferedImage(11, 10, BufferedImage.TYPE_INT_RGB), "png", image.toFile());
        seller.saveProduct(saved, image.toString());
        Path changedImage = Path.of(buyer.getImagePath(saved.getId()));
        assertNotEquals(downloaded, changedImage);
        assertArrayEquals(Files.readAllBytes(image), Files.readAllBytes(changedImage));
        Path sellerImage = Path.of(seller.getImagePath(saved.getId()));
        assertNotEquals(changedImage, sellerImage, "Different clients have separate caches");
        assertArrayEquals(Files.readAllBytes(image), Files.readAllBytes(sellerImage));
        assertThrows(ServerError.class, () -> buyer.deleteProduct("bidder_hieu", saved.getId()));
        admin.cancelAuction(saved.getId(), "Kiểm thử hủy");
        assertEquals(AuctionStatus.CANCELED, buyer.getAuctionById(saved.getId()).getStatus());
        admin.deleteAuction(saved.getId());
        assertThrows(ServerError.class, () -> buyer.getAuctionById(saved.getId()));
    }

    @Test
    void sellerValidationIdentifiesTheRejectedFieldAndLeavesProductsUnchanged() {
        LocalDateTime start = seller.serverNow().plusMinutes(5);
        Item item = new Item("draft-item", "x".repeat(201), "Mô tả", "ART", 1_000_000, "seller_hieu");
        Auction draft = new Auction("draft", item, "seller_hieu", 1_000_000, null,
                AuctionStatus.OPEN, start, start.plusHours(1), 100_000);
        int before = seller.listProducts("seller_hieu").size();
        ServerError nameError = assertThrows(ServerError.class, () -> seller.saveProduct(draft, null));
        assertEquals("VALIDATION_ERROR", nameError.code());
        assertTrue(nameError.getMessage().contains("200 ký tự"));
        item.setName("Tên hợp lệ");
        item.setDescription("x".repeat(10001));
        ServerError descriptionError = assertThrows(ServerError.class, () -> seller.saveProduct(draft, null));
        assertTrue(descriptionError.getMessage().contains("10.000 ký tự"));
        assertEquals(before, seller.listProducts("seller_hieu").size());
    }

    @Test
    void twoAdaptersReceiveLiveBidWalletAndAutoBidWithoutMock() throws Exception {
        String id = buyer.getAuctions(AuctionStatus.RUNNING).get(0).getId();
        CountDownLatch pushed = new CountDownLatch(1);
        other.registerBidListener(id, b -> pushed.countDown());
        assertTrue(buyer.placeBid(id, 2_000_000).isSuccess());
        assertTrue(pushed.await(5, TimeUnit.SECONDS));
        assertEquals(98_000_000, buyer.getSnapshot("bidder_hieu").available());
        other.registerAutoBid(id, 4_000_000, 100_000);
        assertEquals("bidder_vip99", buyer.getAuctionById(id).getHighestBidderUsername());
        assertEquals(100_000_000, buyer.getAvailable("bidder_hieu"));
        assertTrue(buyer.getRoomSnapshot(id).history().stream().anyMatch(Bid::isAuto));
        admin.cancelAuction(id, "Kết thúc kiểm thử");
        assertEquals(300_000_000, other.getAvailable("bidder_vip99"));
        assertFalse(other.getMyAutoBid(id).isActive());
    }

    @Test
    void failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges() throws Exception {
        String id = buyer.getAuctions(AuctionStatus.RUNNING).get(0).getId();
        CountDownLatch bidReceived = new CountDownLatch(1), changeReceived = new CountDownLatch(1);
        java.util.function.Consumer<Bid> broken = bid -> { throw new IllegalStateException("broken view"); };
        java.util.function.Consumer<Bid> healthy = bid -> bidReceived.countDown();
        java.util.function.Consumer<String> changes = event -> {
            if (event.equals("BID_PLACED")) changeReceived.countDown();
        };
        other.registerBidListener(id, broken);
        other.registerBidListener(id, healthy);
        other.addChangeListener(changes);
        assertTrue(buyer.placeBid(id, 1_200_000).isSuccess());
        assertTrue(bidReceived.await(5, TimeUnit.SECONDS));
        assertTrue(changeReceived.await(5, TimeUnit.SECONDS));
        other.unregisterBidListener(id, broken);
        other.unregisterBidListener(id, healthy);
        other.removeChangeListener(changes);
    }

    @Test
    void registrationLoginDepositWithdrawAndAdminLockReachBackend() {
        assertTrue(
                other.register(
                        "new_bidder",
                        "Password123",
                        "Tên mới",
                        "new@example.com",
                        UserRole.BIDDER));
        assertEquals(0, other.getBalance("new_bidder"));
        other.deposit("new_bidder", 100_000);
        other.withdraw("new_bidder", 20_000);
        var snapshot = other.getSnapshot("new_bidder");
        assertEquals(80_000, snapshot.balance());
        assertEquals(2, snapshot.transactions().size());
        assertThrows(ServerError.class, () -> other.getBalance("bidder_hieu"));
        String id = other.getCurrentUser().getId();
        admin.setLocked("ignored", id, true);
        assertThrows(RuntimeException.class, () -> other.withdraw("new_bidder", 10000));
        admin.setLocked("ignored", id, false);
        assertTrue(other.login("new_bidder", "Password123", null));
        other.logout();
        assertNull(other.getCurrentUser());
    }

    @Test
    void pendingCommandSurvivesClientRestartAndIsAppliedOnlyOnce() throws Exception {
        String user = buyer.getCurrentUser().getId();
        Path pending =
                directory
                        .resolve("buyer")
                        .resolve("127_0_0_1-" + server.port())
                        .resolve("pending-" + user + ".json");
        Files.createDirectories(pending.getParent());
        Files.writeString(
                pending,
                "{\"action\":\"DEPOSIT_SIMULATED\",\"commandId\":\"durable-command-001\",\"payload\":{\"amount\":10000}}");
        assertTrue(buyer.recoverPendingCommands());
        assertFalse(Files.exists(pending));
        assertEquals(100_010_000, buyer.getBalance("bidder_hieu"));
        Files.writeString(
                pending,
                "{\"action\":\"DEPOSIT_SIMULATED\",\"commandId\":\"durable-command-001\",\"payload\":{\"amount\":10000}}");
        buyer.close();
        buyer = client("buyer");
        assertTrue(buyer.login("bidder_hieu", "Bidder12345", null));
        assertTrue(buyer.recoverPendingCommands());
        assertEquals(100_010_000, buyer.getBalance("bidder_hieu"));
    }

    @Test
    void unavailableServerDoesNotReturnMockData() throws Exception {
        server.close();
        assertThrows(IllegalStateException.class, () -> buyer.getAuctions(null));
        buyer.logout();
        assertNull(buyer.getCurrentUser());
    }

    @Test
    void reconnectAfterServerRestartRequiresLoginAndRetainsMoney() throws Exception {
        buyer.deposit("bidder_hieu", 10000);
        int port = server.port();
        server.close();
        server = new Server("127.0.0.1", port, directory.resolve("server"), false);
        server.start();
        assertThrows(ServerError.class, () -> buyer.getAvailable("bidder_hieu"));
        assertNull(buyer.getCurrentUser());
        assertTrue(buyer.login("bidder_hieu", "Bidder12345", null));
        assertEquals(100_010_000, buyer.getAvailable("bidder_hieu"));
    }

    @Test
    void coldServerJvmCanAuthenticateWithinTheNormalClientTimeout() throws Exception {
        server.close();
        Path log = directory.resolve("cold-server.log");
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT)
                .contains("win") ? "java.exe" : "java";
        String classpath = Path.of(Server.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()) + java.io.File.pathSeparator
                + Path.of(com.google.gson.Gson.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI());
        Process child = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Dauction.bind=127.0.0.1", "-Dauction.port=0", "-Dauction.demo=false",
                "-Dauction.data=" + directory.resolve("server"),
                "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"),
                "-cp", classpath, Server.class.getName())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            int port = -1;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            var listening = java.util.regex.Pattern.compile(
                    "Auction JSON server listening on .*:(\\d+)");
            while (child.isAlive() && System.nanoTime() < deadline) {
                var match = listening.matcher(Files.readString(log));
                if (match.find()) {
                    port = Integer.parseInt(match.group(1));
                    break;
                }
                Thread.sleep(30);
            }
            assertTrue(port > 0, "Child server did not become ready: " + Files.readString(log));
            try (var coldClient = new ServerServices(
                    "127.0.0.1", port, directory.resolve("cold-client"))) {
                // No raised timeout or retry: this is the first authenticated request in a new JVM.
                assertTrue(coldClient.login("bidder_hieu", "Bidder12345", null));
                assertEquals(100_000_000, coldClient.getAvailable("bidder_hieu"));
            }
        } finally {
            child.destroy();
            if (!child.waitFor(5, TimeUnit.SECONDS)) {
                child.destroyForcibly();
                assertTrue(child.waitFor(5, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void requestTimeoutAlsoClosesAConnectionWhosePeerStopsReading() throws Exception {
        try (var listener = new java.net.ServerSocket()) {
            listener.setReceiveBufferSize(1024);
            listener.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
            try (var workers = Executors.newVirtualThreadPerTaskExecutor();
                    var transport = new JsonTransport(
                            "127.0.0.1", listener.getLocalPort(), 300, event -> {})) {
                var accepted = workers.submit(listener::accept);
                var request = workers.submit(() -> transport.call("TEST", "", "",
                        com.auction.shared.JsonProtocol.obj("body", "x".repeat(6 * 1024 * 1024))));
                try (var stalled = accepted.get(5, TimeUnit.SECONDS)) {
                    // The peer accepts TCP but never drains the request body.
                    ExecutionException failure = assertThrows(ExecutionException.class,
                            () -> request.get(4, TimeUnit.SECONDS));
                    assertInstanceOf(IllegalStateException.class, failure.getCause());
                }
            }
        }
    }

    @Test
    void lostBidAcknowledgementRetriesTheSameCommandNotAnotherBid() throws Exception {
        try (DroppingProxy proxy = new DroppingProxy(server.port());
                ServerServices throughProxy =
                        new ServerServices("127.0.0.1", proxy.port(), directory.resolve("proxy"))) {
            assertTrue(throughProxy.login("bidder_hieu", "Bidder12345", null));
            String id = throughProxy.getAuctions(AuctionStatus.RUNNING).get(0).getId();
            assertTrue(throughProxy.placeBid(id, 2_000_000).isSuccess());
            assertEquals(1, throughProxy.getBidHistory(id).size());
            assertEquals(98_000_000, throughProxy.getAvailable("bidder_hieu"));
            assertTrue(proxy.dropped.get());
        }
    }

    /** TCP fault injection: commits reach the server, first bid acknowledgement is lost. */
    static final class DroppingProxy implements AutoCloseable {
        final java.net.ServerSocket listener;
        final java.util.concurrent.atomic.AtomicBoolean dropped =
                new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.Set<java.net.Socket> sockets =
                java.util.concurrent.ConcurrentHashMap.newKeySet();
        final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

        DroppingProxy(int target) throws Exception {
            listener =
                    new java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"));
            workers.submit(
                    () -> {
                        while (!listener.isClosed())
                            try {
                                java.net.Socket client = listener.accept(),
                                        upstream = new java.net.Socket("127.0.0.1", target);
                                sockets.add(client);
                                sockets.add(upstream);
                                java.util.Set<String> bidRequests =
                                        java.util.concurrent.ConcurrentHashMap.newKeySet();
                                workers.submit(() -> relay(client, upstream, bidRequests, true));
                                workers.submit(() -> relay(upstream, client, bidRequests, false));
                            } catch (java.io.IOException e) {
                                break;
                            }
                    });
        }

        int port() {
            return listener.getLocalPort();
        }

        void relay(
                java.net.Socket from,
                java.net.Socket to,
                java.util.Set<String> ids,
                boolean requests) {
            try {
                var in = new java.io.DataInputStream(from.getInputStream());
                var out = new java.io.DataOutputStream(to.getOutputStream());
                while (true) {
                    var value = Server.readFrame(in);
                    if (requests
                            && value.has("action")
                            && value.get("action").getAsString().equals("PLACE_BID"))
                        ids.add(value.get("requestId").getAsString());
                    if (!requests
                            && value.has("requestId")
                            && ids.contains(value.get("requestId").getAsString())
                            && dropped.compareAndSet(false, true)) break;
                    Server.writeFrame(out, value);
                }
            } catch (java.io.IOException ignored) {
            } finally {
                try {
                    from.close();
                    to.close();
                } catch (java.io.IOException ignored) {
                }
            }
        }

        public void close() throws Exception {
            listener.close();
            for (var s : sockets) s.close();
            workers.shutdownNow();
            workers.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
