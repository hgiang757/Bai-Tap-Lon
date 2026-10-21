package com.hieu.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Tiện ích chuyển đổi màn hình (Scene Navigation) trong ứng dụng JavaFX.
 * Tự động gắn stylesheet và quản lý kích thước màn hình.
 */
public class ViewNavigator {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Chuyển màn hình với đường dẫn FXML và tiêu đề cửa sổ.
     */
    public static <T> T loadView(String fxmlPath, String windowTitle) throws IOException {
        FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
        Parent root = loader.load();

        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1024, 680);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        // Đảm bảo gắn style.css
        String css = ViewNavigator.class.getResource("/com/hieu/css/style.css").toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }

        primaryStage.setTitle("Online Auction System - " + windowTitle);
        primaryStage.show();

        return loader.getController();
    }
}
