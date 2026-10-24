package com.hieu.controllers;

import com.hieu.util.ViewNavigator;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller cho màn hình Thư viện UI (UiKit).
 * Dùng để kiểm thử và tham chiếu tất cả component dùng trong ứng dụng.
 */
public class UiKitController {

    @FXML private HBox toastSuccess;
    @FXML private HBox toastError;
    @FXML private HBox toastWarning;
    @FXML private HBox toastInfo;

    @FXML
    public void initialize() {
        // Ẩn tất cả toast ban đầu (chỉ hiện static demo, không hide)
    }

    @FXML
    private void handleBack() {
        try {
            ViewNavigator.loadView("/com/hieu/views/auth-view.fxml", "Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Hiện toast thành công với hiệu ứng fade, tự ẩn sau 3 giây */
    @FXML
    private void showSuccessToast() {
        showToastAnimated(toastSuccess);
    }

    @FXML
    private void showErrorToast() {
        showToastAnimated(toastError);
    }

    @FXML
    private void showWarningToast() {
        showToastAnimated(toastWarning);
    }

    private void showToastAnimated(HBox toast) {
        toast.setVisible(true);
        toast.setManaged(true);
        toast.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            PauseTransition hold = new PauseTransition(Duration.seconds(3));
            hold.setOnFinished(ev -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(done -> {
                    toast.setVisible(false);
                    toast.setManaged(false);
                });
                fadeOut.play();
            });
            hold.play();
        });
        fadeIn.play();
    }
}
