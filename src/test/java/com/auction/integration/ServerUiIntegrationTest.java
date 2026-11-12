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
                                    stage.getScene().lookup("#overviewTable")
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
}
