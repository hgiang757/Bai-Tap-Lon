package com.auction.client.dev;

import com.auction.client.controller.*;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ServiceRegistry;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Công cụ chụp giao diện bằng mock; chờ dữ liệu nền và dùng vòng đời điều hướng thật. */
public class SnapshotApp extends Application {
    private static final File OUTPUT_DIR = new File(System.getProperty("snapshot.outputDir",
            System.getenv().getOrDefault("SNAPSHOT_OUTPUT_DIR", "target/snapshots")));

    @Override public void start(Stage stage) {
        Platform.setImplicitExit(false);
        ViewNavigator.setPrimaryStage(stage);
        int width = Integer.parseInt(System.getProperty("snapshot.width",
                System.getenv().getOrDefault("SNAPSHOT_WIDTH", "1200")));
        stage.setScene(new Scene(new StackPane(), width, 800));
        stage.show();
        new Thread(() -> runSnapshots(stage), "snapshot-sequence").start();
    }

    private void runSnapshots(Stage stage) {
        int exitCode = 0;
        try {
            ServiceRegistry.validateMode();
            if (!OUTPUT_DIR.isDirectory() && !OUTPUT_DIR.mkdirs()) throw new java.io.IOException("Không thể tạo " + OUTPUT_DIR);
            capture(stage, ViewNavigator.Route.AUTH.getPath(), "01_auth_login.png", null);
            capture(stage, ViewNavigator.Route.AUTH.getPath(), "02_auth_register.png", controller -> {
                try {
                    Method method = AuthController.class.getDeclaredMethod("handleSwitchMode");
                    method.setAccessible(true); method.invoke(controller);
                } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            });
            if (!ServiceRegistry.getService().login("bidder_hieu", "123456", null)) throw new IllegalStateException("Mock login failed");
            capture(stage, ViewNavigator.Route.DASHBOARD.getPath(), "03_dashboard.png", null);
            capture(stage, "/com/auction/client/views/auction-room-view.fxml", "04_realtime_bidding_running.png",
                    controller -> ((AuctionRoomController) controller).loadAuction("AUC-103"));
            capture(stage, "/com/auction/client/views/auction-room-view.fxml", "05_realtime_bidding_finished.png",
                    controller -> ((AuctionRoomController) controller).loadAuction("AUC-104"));
            if (!ServiceRegistry.getService().login("seller_hieu", "123456", null)) throw new IllegalStateException("Mock seller login failed");
            capture(stage, ViewNavigator.Route.PRODUCTS.getPath(), "06_product_management.png", null);
            capture(stage, "/com/auction/client/dev/uikit-view.fxml", "07_uikit_showcase.png", null);
            fx(() -> {
                Node gallery = stage.getScene().getRoot().lookup("#iconGallery");
                if (gallery == null) throw new IllegalStateException("Icon gallery is missing");
                saveSnapshot(gallery, "08_icon_gallery.png"); return null;
            });
            if (!ServiceRegistry.getService().register("snapshot_wallet", "Testpass123", "Ví kiểm thử",
                    "snapshot_wallet@example.com", com.auction.client.model.UserRole.BIDDER)) {
                throw new IllegalStateException("Mock wallet registration failed");
            }
            ServiceRegistry.getWalletService().deposit("snapshot_wallet", 9_123_456_739_012_345L);
            capture(stage, ViewNavigator.Route.WALLET.getPath(), "11_wallet_long_amount.png", null);
            capture(stage, ViewNavigator.Route.GUIDE.getPath(), "09_guide.png", null);
            capture(stage, ViewNavigator.Route.CONTACT.getPath(), "10_contact.png", null);
            System.out.println("SNAPSHOTS_SUCCESS (mock only)");
        } catch (Exception error) {
            exitCode = 1; error.printStackTrace();
        } finally {
            try { fx(() -> { ViewNavigator.disposeCurrentView(); stage.close(); return null; }); }
            catch (Exception error) { exitCode = 1; error.printStackTrace(); }
            Platform.exit();
            System.exit(exitCode);
        }
    }

    private void capture(Stage stage, String fxml, String filename, Consumer<Object> init) throws Exception {
        Object controller = fx(() -> {
            Object loaded = ViewNavigator.loadView(fxml, filename, false);
            if (init != null) init.accept(loaded);
            return loaded;
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!fx(() -> ready(controller))) {
            if (System.nanoTime() > deadline) throw new IllegalStateException("Chưa tải xong: " + fxml);
            Thread.sleep(30);
        }
        CompletableFuture<Void> layoutReady = new CompletableFuture<>();
        fx(() -> {
            var pulse = new javafx.animation.AnimationTimer() {
                private int frames;
                @Override public void handle(long now) {
                    if (++frames >= 3) { stop(); layoutReady.complete(null); }
                }
            };
            pulse.start(); return null;
        });
        layoutReady.get(10, TimeUnit.SECONDS);
        fx(() -> { saveSnapshot(stage.getScene().getRoot(), filename); return null; });
    }

    private boolean ready(Object controller) {
        if (controller instanceof HomeController home) return !home.isLoading();
        if (controller instanceof AuctionRoomController room) return room.isReady();
        if (controller instanceof SellerProductsController seller) return !seller.isLoading();
        if (controller instanceof WalletController wallet) return !wallet.isLoading();
        return true;
    }

    private static <T> T fx(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private void saveSnapshot(Node node, String filename) throws java.io.IOException {
        node.applyCss();
        if (node instanceof javafx.scene.Parent parent) parent.layout();
        File file = new File(OUTPUT_DIR, filename);
        if (!ImageIO.write(SwingFXUtils.fromFXImage(node.snapshot(null, null), null), "PNG", file)) {
            throw new java.io.IOException("No PNG writer for " + filename);
        }
        System.out.println("SAVED: " + file);
    }

    public static void main(String[] args) { launch(args); }
}
