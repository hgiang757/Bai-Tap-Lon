package com.hieu;

import com.hieu.controllers.AuthController;
import com.hieu.controllers.RealtimeBiddingController;
import com.hieu.models.Auction;
import com.hieu.services.AuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.ViewNavigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    private static final String CSS_PATH = "/com/hieu/css/style.css";
    private static final File OUTPUT_DIR = new File("docs/ui/screenshots");

    @Override
    public void start(Stage stage) throws Exception {
        ViewNavigator.setPrimaryStage(stage);
        OUTPUT_DIR.mkdirs();
        stage.setWidth(1080);
        stage.setHeight(720);

        try {
            // 1. Auth Login
            capture(stage, "/com/hieu/views/auth-view.fxml", "01_auth_login.png", null);

            // 2. Auth Register
            FXMLLoader authLoader = new FXMLLoader(getClass().getResource("/com/hieu/views/auth-view.fxml"));
            Parent authRoot = authLoader.load();
            AuthController authController = authLoader.getController();
            Method switchMethod = AuthController.class.getDeclaredMethod("handleSwitchMode");
            switchMethod.setAccessible(true);
            switchMethod.invoke(authController);
            renderAndSave(stage, authRoot, "02_auth_register.png");

            // 3. Dashboard
            capture(stage, "/com/hieu/views/dashboard-view.fxml", "03_dashboard.png", null);

            // 4. Live Auction (RUNNING)
            AuctionClientService service = ServiceRegistry.getService();
            Auction runningAuction = service.getAuctionById("AUC-101");
            FXMLLoader liveLoader = new FXMLLoader(getClass().getResource("/com/hieu/views/realtime-bidding-view.fxml"));
            Parent liveRoot = liveLoader.load();
            RealtimeBiddingController liveController = liveLoader.getController();
            liveController.setAuction(runningAuction);
            renderAndSave(stage, liveRoot, "04_realtime_bidding_running.png");

            // 5. Live Auction (FINISHED - Locked inputs)
            Auction finishedAuction = service.getAuctionById("AUC-104");
            FXMLLoader finLoader = new FXMLLoader(getClass().getResource("/com/hieu/views/realtime-bidding-view.fxml"));
            Parent finRoot = finLoader.load();
            RealtimeBiddingController finController = finLoader.getController();
            finController.setAuction(finishedAuction);
            renderAndSave(stage, finRoot, "05_realtime_bidding_finished.png");

            // 6. Product Management
            capture(stage, "/com/hieu/views/product-management-view.fxml", "06_product_management.png", null);

            System.out.println("SNAPSHOTS_SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stage.close();
            Platform.exit();
            System.exit(0);
        }
    }

    private void capture(Stage stage, String fxml, String filename, java.util.function.Consumer<Object> init) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Parent root = loader.load();
        if (init != null) {
            init.accept(loader.getController());
        }
        renderAndSave(stage, root, filename);
    }

    private void renderAndSave(Stage stage, Parent root, String filename) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1080, 720);
            scene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
            if (!scene.getStylesheets().contains(getClass().getResource(CSS_PATH).toExternalForm())) {
                scene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());
            }
        }
        stage.show();
        root.applyCss();
        root.layout();

        WritableImage img = root.snapshot(null, null);
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        try {
            File dest = new File(OUTPUT_DIR, filename);
            ImageIO.write(bi, "PNG", dest);
            System.out.println("SAVED: " + dest.getName() + " (" + w + "x" + h + ")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
