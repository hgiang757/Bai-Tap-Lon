package com.auction.client.ui;

import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ServiceRegistry;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/** Thanh điều hướng dùng chung, cập nhật tài khoản mỗi khi được hiển thị. */
public final class AppHeader extends HBox {
    private javafx.scene.Node account;
    public AppHeader() {
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) refreshAccount();
        });
    }
    private void refreshAccount() {
        if (account != null) getChildren().remove(account);
        if (ServiceRegistry.getService().getCurrentUser() != null) {
            account = new UserMenu();
        } else {
            Button login = new Button("Đăng nhập");
            login.getStyleClass().add("btn-primary");
            login.setOnAction(e -> {
                try { ViewNavigator.navigate(ViewNavigator.Route.AUTH); }
                catch (java.io.IOException ex) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Không thể mở màn hình đăng nhập.");
                    ViewNavigator.applyAppStylesheet(alert.getDialogPane());
                    alert.show();
                }
            });
            account = login;
        }
        getChildren().add(account);
    }
}
