package com.auction.client.dev;

import com.auction.client.controller.AuthController;
import com.auction.client.controller.AuctionRoomController;
import com.auction.client.model.Auction;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.navigation.ViewNavigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class SnapshotApp extends Application {

    private static final File OUTPUT_DIR = new File(System.getProperty("snapshot.outputDir",
            System.getenv().getOrDefault("SNAPSHOT_OUTPUT_DIR", "docs/ui/screenshots")));

    @Override
    public void start(Stage stage) throws Exception {
        ViewNavigator.setPrimaryStage(stage);
        OUTPUT_DIR.mkdirs();
        stage.setWidth(1080);
        stage.setHeight(720);

        try {
            // 1. Auth Login
            capture(stage, "/com/auction/client/views/auth-view.fxml", "01_auth_login.png", null);

            // 2. Auth Register
            FXMLLoader authLoader = new FXMLLoader(getClass().getResource("/com/auction/client/views/auth-view.fxml"));
            Parent authRoot = authLoader.load();
            AuthController authController = authLoader.getController();
            Method switchMethod = AuthController.class.getDeclaredMethod("handleSwitchMode");
            switchMethod.setAccessible(true);
            switchMethod.invoke(authController);
            renderAndSave(stage, authRoot, "02_auth_register.png");

            // 3. Trang chủ
            capture(stage, "/com/auction/client/views/home-view.fxml", "03_dashboard.png", null);

            // 4. Live Auction (RUNNING)
            AuctionClientService service = ServiceRegistry.getService();
            Auction runningAuction = service.getAuctionById("AUC-101");
            FXMLLoader liveLoader = new FXMLLoader(getClass().getResource("/com/auction/client/views/auction-room-view.fxml"));
            Parent liveRoot = liveLoader.load();
            AuctionRoomController liveController = liveLoader.getController();
            liveController.setAuction(runningAuction);
            renderAndSave(stage, liveRoot, "04_realtime_bidding_running.png");

            // 5. Live Auction (FINISHED - Locked inputs)
            Auction finishedAuction = service.getAuctionById("AUC-104");
            FXMLLoader finLoader = new FXMLLoader(getClass().getResource("/com/auction/client/views/auction-room-view.fxml"));
            Parent finRoot = finLoader.load();
            AuctionRoomController finController = finLoader.getController();
            finController.setAuction(finishedAuction);
            renderAndSave(stage, finRoot, "05_realtime_bidding_finished.png");

            // 6. Product Management
            capture(stage, "/com/auction/client/views/seller-products-view.fxml", "06_product_management.png", null);

            // 7. UI Kit and icon gallery
            capture(stage, "/com/auction/client/dev/uikit-view.fxml", "07_uikit_showcase.png", null);
            captureIconGallery(stage);
            capture(stage, "/com/auction/client/views/guide-view.fxml", "09_guide.png", null);
            capture(stage, "/com/auction/client/views/contact-view.fxml", "10_contact.png", null);

            System.out.println("SNAPSHOTS_SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stage.close();
            Platform.exit();
            System.exit(0);
        }
    }

    private void captureIconGallery(Stage stage) throws IOException {
        Parent root = stage.getScene().getRoot();
        root.applyCss();
        root.layout();
        Node gallery = root.lookup("#iconGallery");
        if (gallery == null) {
            throw new IllegalStateException("Icon gallery is missing from the UI Kit.");
        }

        saveSnapshot(gallery.snapshot(null, null), "08_icon_gallery.png");
    }

    private void capture(Stage stage, String fxml, String filename, java.util.function.Consumer<Object> init) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Parent root = loader.load();
        if (init != null) {
            init.accept(loader.getController());
        }
        renderAndSave(stage, root, filename);
    }

    private void renderAndSave(Stage stage, Parent root, String filename) throws IOException {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1080, 720);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        ViewNavigator.applyAppStylesheet(scene);
        stage.show();
        root.applyCss();
        root.layout();

        WritableImage img = root.snapshot(null, null);
        saveSnapshot(img, filename);
    }

    private void saveSnapshot(WritableImage img, String filename) throws IOException {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        File dest = new File(OUTPUT_DIR, filename);
        if (!ImageIO.write(bi, "PNG", dest)) {
            throw new IOException("No PNG writer is available for " + dest.getName());
        }
        System.out.println("SAVED: " + dest.getName() + " (" + w + "x" + h + ")");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
