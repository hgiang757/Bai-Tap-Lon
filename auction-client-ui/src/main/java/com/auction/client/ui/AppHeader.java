package com.auction.client.ui;

import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ServiceRegistry;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/** Thanh điều hướng dùng chung, cập nhật tài khoản mỗi khi được hiển thị. */
public final class AppHeader extends HBox {
    private javafx.scene.Node account;
    private javafx.animation.Timeline walletTimer;
    private final String path = ViewNavigator.getCurrentPath();
    public AppHeader() {
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) refreshAccount();
            else if (walletTimer != null) walletTimer.stop();
        });
    }
    private void refreshAccount() {
        if (account != null) getChildren().remove(account);
        if (walletTimer != null) walletTimer.stop();
        if (ServiceRegistry.getService().getCurrentUser() != null) {
            UserMenu menu = new UserMenu();
            if (ViewNavigator.Route.DASHBOARD.getPath().equals(path) || "/com/auction/client/views/auction-room-view.fxml".equals(path)) {
                Button balance = new Button("Ví: …");
                balance.setOnAction(e -> { try { ViewNavigator.navigate(ViewNavigator.Route.WALLET); } catch (java.io.IOException ex) { throw new IllegalStateException(ex); } });
                HBox group = new HBox(8, balance, menu);
                group.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                account = group;
                Runnable update = () -> {
                    var user = ServiceRegistry.getService().getCurrentUser();
                    if (user == null) return;
                    javafx.concurrent.Task<Long> task = new javafx.concurrent.Task<>() { protected Long call() { return ServiceRegistry.getWalletService().getAvailable(user.getUsername()); } };
                    task.setOnSucceeded(e -> balance.setText("Ví: " + com.auction.client.util.FormatUtil.formatCurrency(task.getValue())));
                    Thread thread = new Thread(task, "wallet-balance"); thread.setDaemon(true); thread.start();
                };
                update.run();
                walletTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> update.run()));
                walletTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
                walletTimer.play();
            } else account = menu;
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
