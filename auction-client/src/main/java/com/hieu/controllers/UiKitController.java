package com.hieu.controllers;

import com.hieu.ui.Icons;
import com.hieu.util.ViewNavigator;
import com.hieu.util.SmoothScroll;
import com.hieu.util.AppInfo;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
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
    @FXML private ScrollPane uiKitScroll;
    @FXML private FlowPane iconGallery;
    @FXML private Label lblPageTitle;
    @FXML private Label lblBrandName;

    @FXML
    public void initialize() {
        lblPageTitle.setText(AppInfo.NAME + " — Thư viện UI");
        lblBrandName.setText(AppInfo.NAME);
        SmoothScroll.apply(uiKitScroll, 3.0);
        buildIconGallery();
    }

    private void buildIconGallery() {
        for (String name : Icons.names()) {
            VBox item = new VBox(8.0);
            item.getStyleClass().add("icon-gallery-item");
            item.setAlignment(Pos.CENTER);

            Label title = new Label(name);
            title.getStyleClass().add("text-bold");
            item.getChildren().add(title);

            HBox sizes = new HBox(12.0);
            sizes.setAlignment(Pos.CENTER);
            for (double size : new double[]{16, 24, 48}) {
                VBox sample = new VBox(4.0);
                sample.setAlignment(Pos.CENTER);
                Label sizeLabel = new Label(Integer.toString((int) size));
                sizeLabel.getStyleClass().add("text-secondary");
                Region icon = Icons.icon(name, size);
                icon.getStyleClass().add("icon-primary");
                sample.getChildren().addAll(sizeLabel, icon);
                sizes.getChildren().add(sample);
            }
            item.getChildren().add(sizes);
            iconGallery.getChildren().add(item);
        }
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
