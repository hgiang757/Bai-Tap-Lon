package com.auction.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.client.controller.*;
import com.auction.client.model.*;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.service.server.ServerServices;
import com.auction.server.network.Server;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

@EnabledIfSystemProperty(named = "auction.ui.tests", matches = "true")
class ServerUiIntegrationTest {
    @TempDir Path directory;
    static Stage stage;

    static <T> T fx(Callable<T> work) throws Exception {
        FutureTask<T> f = new FutureTask<>(work);
        Platform.runLater(f);
        return f.get(15, TimeUnit.SECONDS);
    }

    static void await(BooleanSupplier ready) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!fx(ready::getAsBoolean)) {
            if (System.nanoTime() > deadline) fail("UI timeout");
            Thread.sleep(30);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T field(Object o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeAll
    static void toolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
        assertTrue(started.await(15, TimeUnit.SECONDS));
        fx(
                () -> {
                    stage = new Stage();
                    stage.setWidth(1200);
                    stage.setHeight(800);
                    ViewNavigator.setPrimaryStage(stage);
                    return null;
                });
    }

    @AfterAll
    static void exit() throws Exception {
        fx(
                () -> {
                    ViewNavigator.disposeCurrentView();
                    stage.close();
                    return null;
                });
        Platform.exit();
    }

    void login(String username, String password) throws Exception {
        AuthController auth = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.AUTH));
        fx(
                () -> {
                    ((TextField) field(auth, "txtUsername")).setText(username);
                    ((TextField) field(auth, "txtPassword")).setText(password);
                    ((Button) field(auth, "btnSubmit")).fire();
                    return null;
                });
        await(() -> !ViewNavigator.Route.AUTH.getPath().equals(ViewNavigator.getCurrentPath()));
    }

    void snapshot(String name) throws Exception {
        fx(
                () -> {
                    Path file = Path.of("target", "qa", name + ".png");
                    Files.createDirectories(file.getParent());
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    ImageIO.write(
                            SwingFXUtils.fromFXImage(
                                    stage.getScene().getRoot().snapshot(null, null), null),
                            "png",
                            file.toFile());
                    return null;
                });
    }

    static DialogPane productDialog() {
        return javafx.stage.Window.getWindows().stream()
                .filter(w -> w.isShowing() && w != stage)
                .map(w -> w.getScene().getRoot())
                .filter(DialogPane.class::isInstance).map(DialogPane.class::cast)
                .findFirst().orElse(null);
    }

    @Test
    void javafxScreensOperateOnTheRealServer() throws Exception {
        try (Server server = new Server("127.0.0.1", 0, directory.resolve("server"), true)) {
            server.start();
            try (ServerServices adapter =
                            new ServerServices(
                                    "127.0.0.1", server.port(), directory.resolve("ui"));
                    ServerServices rival =
                            new ServerServices(
                                    "127.0.0.1", server.port(), directory.resolve("rival"))) {
                System.setProperty("auction.mode", "server");
                ServiceRegistry.setServices(adapter, adapter, adapter, adapter, adapter, adapter);
                try {
                    login("bidder_hieu", "Bidder12345");
                    await(() -> stage.getScene().lookup(".auction-card") != null);
                    snapshot("server-home");
                    String id = adapter.getAuctions(AuctionStatus.RUNNING).get(0).getId();
                    AuctionRoomController room =
                            fx(
                                    () -> {
                                        AuctionRoomController r =
                                                ViewNavigator.loadView(
                                                        "/com/auction/client/views/auction-room-view.fxml",
                                                        "Phòng đấu giá");
                                        r.loadAuction(id);
                                        return r;
                                    });
                    await(room::isReady);
                    fx(
                            () -> {
                                ((TextField) field(room, "txtBidAmount")).setText("2000000");
                                ((Button) field(room, "btnPlaceBid")).fire();
                                return null;
                            });
                    await(
                            () ->
                                    ((Label) field(room, "lblHighestBidder"))
                                                    .getText()
                                                    .equals("bidder_hieu")
                                            && room.isReady());
                    assertTrue(rival.login("bidder_vip99", "Bidder12345", null));
                    assertTrue(rival.placeBid(id, 3_000_000).isSuccess());
                    await(
                            () ->
                                    ((Label) field(room, "lblHighestBidder"))
                                            .getText()
                                            .equals("bidder_vip99"));
                    snapshot("server-room");
                    WalletController wallet =
                            fx(() -> ViewNavigator.navigate(ViewNavigator.Route.WALLET));
                    await(() -> !wallet.isLoading());
                    snapshot("server-wallet");
                    adapter.logout();
                    login("seller_hieu", "Seller12345");
                    SellerProductsController products =
                            fx(() -> ViewNavigator.navigate(ViewNavigator.Route.PRODUCTS));
                    await(() -> !products.isLoading());
                    assertFalse(
                            fx(
                                    () ->
                                            ((TableView<?>) field(products, "table"))
                                                    .getItems()
                                                    .isEmpty()));
                    snapshot("server-seller");
                    fx(() -> {
                        Platform.runLater(() -> ((Button) field(products, "addButton")).fire());
                        return null;
                    });
                    await(() -> productDialog() != null);
                    fx(() -> {
                        DialogPane form = productDialog();
                        form.applyCss();
                        form.layout();
                        ((TextField) form.lookup("#productName")).setText("Sản phẩm từ form thật");
                        ((TextArea) form.lookup("#productDescription")).setText("Mô tả kiểm thử qua TCP");
                        ((ComboBox<?>) form.lookup("#productCategory")).getSelectionModel().selectFirst();
                        ((TextField) form.lookup("#productPrice")).setText("500000");
                        ((TextField) form.lookup("#productStep")).setText("10000");
                        ((DatePicker) form.lookup("#productStart")).setValue(adapter.serverNow().toLocalDate().plusDays(1));
                        ((DatePicker) form.lookup("#productEnd")).setValue(adapter.serverNow().toLocalDate().plusDays(2));
                        ((TextField) form.lookup("#productStartTime")).setText("10:00");
                        ((TextField) form.lookup("#productEndTime")).setText("10:00");
                        Button save = (Button) form.lookupButton(ButtonType.OK);
                        assertFalse(save.isDisabled());
                        save.fire();
                        return null;
                    });
                    await(() -> !products.isLoading()
                            && ((TableView<Auction>) field(products, "table")).getItems().stream()
                            .anyMatch(a -> a.getItemName().equals("Sản phẩm từ form thật")));
                    assertTrue(adapter.listProducts("seller_hieu").stream()
                            .anyMatch(a -> a.getItemName().equals("Sản phẩm từ form thật")));
                    adapter.logout();
                    login("admin_hieu", "Admin12345");
                    await(
                            () ->
                                    stage.getScene().lookup("#auctionTable")
                                                    instanceof TableView<?> table
                                            && !table.getItems().isEmpty());
                    snapshot("server-admin");
                } finally {
                    fx(
                            () -> {
                                ViewNavigator.disposeCurrentView();
                                return null;
                            });
                    System.setProperty("auction.mode", "mock");
                    ServiceRegistry.getService();
                }
            }
        }
    }

    void registerThroughForm(String username, boolean seller) throws Exception {
        AuthController auth = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.AUTH));
        fx(() -> {
            ((Button) field(auth, "btnSwitchMode")).fire();
            ((TextField) field(auth, "txtUsername")).setText(username);
            ((TextField) field(auth, "txtEmail")).setText(username + "@example.com");
            ((TextField) field(auth, "txtPassword")).setText("UiFixture123");
            ((TextField) field(auth, "txtConfirm")).setText("UiFixture123");
            ((ToggleButton) field(auth, seller ? "rbSeller" : "rbBidder")).setSelected(true);
            ((Button) field(auth, "btnSubmit")).fire();
            return null;
        });
        await(() -> ViewNavigator.Route.DASHBOARD.getPath().equals(ViewNavigator.getCurrentPath()));
        assertEquals(username, ServiceRegistry.getService().getCurrentUser().getUsername());
    }

    void transferThroughForm(String action, String amount) throws Exception {
        WalletController wallet = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.WALLET));
        await(() -> !wallet.isLoading());
        fx(() -> {
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            Button deposit = stage.getScene().getRoot().lookupAll(".button").stream()
                    .filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(b -> b.getText().equals(action)).findFirst().orElseThrow();
            Platform.runLater(deposit::fire);
            return null;
        });
        await(() -> productDialog() != null);
        fx(() -> {
            DialogPane form = productDialog();
            ((TextField) form.lookup(".text-field")).setText(amount);
            ButtonType confirm = form.getButtonTypes().stream()
                    .filter(t -> t.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    .findFirst().orElseThrow();
            ((Button) form.lookupButton(confirm)).fire();
            return null;
        });
        try {
            await(() -> productDialog() == null && !wallet.isLoading());
        } catch (AssertionError error) {
            snapshot("non-demo-deposit-failure");
            System.err.println(fx(() -> productDialog() == null ? "Dialog closed; loading=" + wallet.isLoading()
                    : productDialog().lookupAll(".label").stream().map(n -> ((Label) n).getText()).toList().toString()));
            throw error;
        }
    }

    void createThroughForm(SellerProductsController products, String name,
            java.time.LocalDateTime start, java.time.LocalDateTime end) throws Exception {
        fx(() -> {
            Platform.runLater(() -> ((Button) field(products, "addButton")).fire());
            return null;
        });
        await(() -> productDialog() != null);
        fx(() -> {
            DialogPane form = productDialog();
            form.applyCss();
            form.layout();
            ((TextField) form.lookup("#productName")).setText(name);
            ((TextArea) form.lookup("#productDescription")).setText("Created through JavaFX on an empty server");
            ((ComboBox<?>) form.lookup("#productCategory")).getSelectionModel().selectFirst();
            ((TextField) form.lookup("#productPrice")).setText("500000");
            ((TextField) form.lookup("#productStep")).setText("10000");
            ((DatePicker) form.lookup("#productStart")).setValue(start.toLocalDate());
            ((DatePicker) form.lookup("#productEnd")).setValue(end.toLocalDate());
            var format = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            ((TextField) form.lookup("#productStartTime")).setText(start.format(format));
            ((TextField) form.lookup("#productEndTime")).setText(end.format(format));
            Button save = (Button) form.lookupButton(ButtonType.OK);
            assertFalse(save.isDisabled());
            save.fire();
            return null;
        });
        await(() -> productDialog() == null && !products.isLoading()
                && ((TableView<Auction>) field(products, "table")).getItems().stream()
                .anyMatch(a -> a.getItemName().equals(name)));
    }

    @Test
    void uiCreatedAccountsProductsBidsWalletAndImageSurviveNonDemoRestart() throws Exception {
        Path storage = directory.resolve("non-demo-server");
        Files.createDirectories(storage);
        // Empty JUnit-only fixture avoids environment passwords; no users/products are seeded.
        Files.writeString(storage.resolve("state.json"), """
                {"schemaVersion":1,"version":0,"users":{},"auctions":{},
                 "rules":{},"commands":{},"transactions":[]}
                """);
        String previousMode = System.getProperty("auction.mode");
        Server server = new Server("127.0.0.1", 0, storage, false);
        server.start();
        int port = server.port();
        try (ServerServices adapter = new ServerServices("127.0.0.1", port, directory.resolve("new-ui"));
                ServerServices rival = new ServerServices("127.0.0.1", port, directory.resolve("new-rival"))) {
            System.setProperty("auction.mode", "server");
            ServiceRegistry.setServices(adapter, adapter, adapter, adapter, adapter, adapter);
            assertTrue(adapter.getAuctions(null).isEmpty());
            registerThroughForm("ui_seller", true);
            adapter.logout();
            registerThroughForm("ui_bidder_a", false);
            transferThroughForm("Nạp tiền", "5000000");
            transferThroughForm("Rút tiền", "10000");
            transferThroughForm("Nạp tiền", "10000");
            adapter.logout();
            registerThroughForm("ui_bidder_b", false);
            transferThroughForm("Nạp tiền", "5000000");
            adapter.logout();
            login("ui_seller", "UiFixture123");
            SellerProductsController products = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.PRODUCTS));
            await(() -> !products.isLoading());
            var now = adapter.serverNow();
            // The real form accepts HH:mm. Allow >=10 seconds to save, then observe that minute.
            var start = now.plusMinutes(now.getSecond() > 49 ? 2 : 1).withSecond(0).withNano(0);
            createThroughForm(products, "UI live product", start, start.plusDays(1));
            createThroughForm(products, "UI future product", start.plusDays(1), start.plusDays(2));
            Auction created = adapter.listProducts("ui_seller").stream()
                    .filter(a -> a.getItemName().equals("UI live product")).findFirst().orElseThrow();
            String id = created.getId();
            Path image = directory.resolve("ui-product.png");
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(12, 12,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", image.toFile());
            // Native FileChooser interaction is separate; use the real image upload adapter here.
            adapter.saveProduct(created, image.toString());
            adapter.logout();
            login("ui_bidder_a", "UiFixture123");
            AuctionRoomController room = fx(() -> {
                AuctionRoomController r = ViewNavigator.loadView(
                        "/com/auction/client/views/auction-room-view.fxml", "Non-demo room");
                r.loadAuction(id);
                return r;
            });
            await(room::isReady);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(75);
            while (!fx(() -> !((Button) field(room, "btnPlaceBid")).isDisabled())) {
                if (System.nanoTime() > deadline) fail("Scheduled HH:mm auction did not open: " + start);
                Thread.sleep(100);
            }
            fx(() -> {
                ((TextField) field(room, "txtBidAmount")).setText("600000");
                ((Button) field(room, "btnPlaceBid")).fire();
                return null;
            });
            await(() -> room.isReady() && ((Label) field(room, "lblHighestBidder")).getText().equals("ui_bidder_a"));
            assertTrue(rival.login("ui_bidder_b", "UiFixture123", null));
            fx(() -> {
                ((TextField) field(room, "txtAutoMax")).setText("1000000");
                ((TextField) field(room, "txtAutoIncrement")).setText("10000");
                ((Button) field(room, "btnAutoBid")).fire();
                return null;
            });
            await(() -> room.isReady() && !(boolean) field(room, "autoBusy"));
            assertTrue(adapter.getMyAutoBid(id).isActive());
            assertTrue(rival.placeBid(id, 700000).isSuccess());
            await(() -> room.isReady() && ((ListView<?>) field(room, "lvRecentBids")).getItems().size() == 3);
            var before = adapter.getRoomSnapshot(id);
            var walletBefore = adapter.getSnapshot("ui_bidder_a");
            assertEquals(710000, before.auction().getCurrentPrice());
            assertEquals(710000, walletBefore.held());
            assertEquals(5000000, rival.getAvailable("ui_bidder_b"));
            snapshot("non-demo-before-restart");
            fx(() -> { ViewNavigator.disposeCurrentView(); return null; });
            server.close();
            byte[] saved = Files.readAllBytes(storage.resolve("state.json"));
            server = new Server("127.0.0.1", port, storage, false);
            assertArrayEquals(saved, Files.readAllBytes(storage.resolve("state.json")), "Loading must not reseed or reset");
            server.start();
            assertThrows(com.auction.client.service.server.ServerError.class,
                    () -> adapter.getSnapshot("ui_bidder_a"));
            assertNull(adapter.getCurrentUser());
            login("ui_bidder_a", "UiFixture123");
            assertEquals(walletBefore, adapter.getSnapshot("ui_bidder_a"));
            var restored = adapter.getRoomSnapshot(id);
            assertEquals(before.history().stream().map(Bid::getId).toList(), restored.history().stream().map(Bid::getId).toList());
            assertEquals(before.auction().getCurrentPrice(), restored.auction().getCurrentPrice());
            assertTrue(adapter.getMyAutoBid(id).isActive());
            assertArrayEquals(Files.readAllBytes(image), Files.readAllBytes(Path.of(adapter.getImagePath(id))));
            assertEquals(2, adapter.getAuctions(null).size());
            assertTrue(rival.login("ui_bidder_b", "UiFixture123", null));
            assertEquals(5000000, rival.getAvailable("ui_bidder_b"));
            AuctionRoomController reopened = fx(() -> {
                AuctionRoomController r = ViewNavigator.loadView(
                        "/com/auction/client/views/auction-room-view.fxml", "Restored non-demo room");
                r.loadAuction(id);
                return r;
            });
            await(reopened::isReady);
            assertEquals(3, fx(() -> ((ListView<?>) field(reopened, "lvRecentBids")).getItems().size()));
            snapshot("non-demo-after-restart");
        } finally {
            fx(() -> { ViewNavigator.disposeCurrentView(); return null; });
            server.close();
            ServiceRegistry.close();
            if (previousMode == null) System.clearProperty("auction.mode");
            else System.setProperty("auction.mode", previousMode);
        }
    }
}
