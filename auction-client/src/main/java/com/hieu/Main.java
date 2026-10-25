package com.hieu;

import com.hieu.util.ViewNavigator;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;

/**
 * Entry point chuẩn khởi chạy ứng dụng JavaFX Online Auction Client.
 * Thiết lập Stage chính, nạp giao diện Auth và áp dụng hệ thống CSS đồng bộ.
 */
public class Main extends Application {
    private static HostServices hostServices;

    public static HostServices getHostServicesInstance() {
        return hostServices;
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            hostServices = getHostServices();
            // Đăng ký primaryStage vào tiện ích ViewNavigator để chuyển đổi màn hình linh hoạt
            ViewNavigator.setPrimaryStage(primaryStage);

            // Cấu hình các thông số cơ bản cho cửa sổ ứng dụng
            primaryStage.setMinWidth(640);
            primaryStage.setMinHeight(640);

            // Nạp màn hình đầu tiên: AuthView (Login/Register)
            ViewNavigator.loadView("/com/hieu/views/auth-view.fxml", "Đăng nhập / Đăng ký");

        } catch (Exception e) {
            System.err.println("Không thể khởi động ứng dụng JavaFX:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
