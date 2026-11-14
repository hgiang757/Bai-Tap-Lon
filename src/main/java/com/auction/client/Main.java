package com.auction.client;

import com.auction.client.navigation.ViewNavigator;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Entry point chuẩn khởi chạy ứng dụng JavaFX Online Auction Client. Thiết lập Stage chính, nạp
 * giao diện Auth và áp dụng hệ thống CSS đồng bộ.
 */
public class Main extends Application {
  private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

  @Override
  public void start(Stage primaryStage) {
    try {
      com.auction.client.service.ServiceRegistry.validateMode();
      com.auction.client.service.ServiceRegistry.getService()
          .addChangeListener(
              type -> {
                if (type.equals("SESSION_EXPIRED") || type.equals("USER_CHANGED")) {
                  javafx.application.Platform.runLater(
                      () -> {
                        try {
                          ViewNavigator.clearHistory();
                          var user =
                              com.auction.client.service.ServiceRegistry.getService()
                                  .getCurrentUser();
                          ViewNavigator.navigate(
                              user == null
                                  ? ViewNavigator.Route.AUTH
                                  : user.getRole() == com.auction.client.model.UserRole.ADMIN
                                      ? ViewNavigator.Route.ADMIN
                                      : ViewNavigator.Route.DASHBOARD);
                        } catch (java.io.IOException e) {
                          LOGGER.warning(e.getMessage());
                        }
                      });
                }
              });
      String fontFamily = Font.getFamilies().contains("Segoe UI") ? "Segoe UI" : "System";
      LOGGER.info("Phông chữ giao diện đang dùng: " + fontFamily);
      // Đăng ký primaryStage vào tiện ích ViewNavigator để chuyển đổi màn hình linh hoạt
      ViewNavigator.setPrimaryStage(primaryStage);

      // Cấu hình các thông số cơ bản cho cửa sổ ứng dụng
      primaryStage.setMinWidth(640);
      primaryStage.setMinHeight(640);

      // Nạp màn hình đầu tiên: AuthView (Login/Register)
      ViewNavigator.loadView("/com/auction/client/views/auth-view.fxml", "Đăng nhập / Đăng ký");

    } catch (Exception e) {
      System.err.println("Không thể khởi động ứng dụng JavaFX:");
      e.printStackTrace();
      new javafx.scene.control.Alert(
              javafx.scene.control.Alert.AlertType.ERROR, "Không thể khởi động: " + e.getMessage())
          .showAndWait();
      javafx.application.Platform.exit();
    }
  }

  @Override
  public void stop() {
    ViewNavigator.disposeCurrentView();
    com.auction.client.service.ServiceRegistry.close();
  }

  /** Starts the JavaFX auction client. */
  public static void main(String[] args) {
    launch(args);
  }
}
