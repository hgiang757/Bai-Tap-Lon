package com.auction.client.ui;

import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.ServiceRegistry;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/** Thanh điều hướng dùng chung, cập nhật tài khoản mỗi khi được hiển thị. */
public final class AppHeader extends HBox {
    private javafx.scene.Node account;
    private ViewTasks tasks;
    private boolean readingBalance, readAgain;
    private Runnable balanceRefresh = () -> {};
    private final javafx.scene.control.Label connectionState =
            new javafx.scene.control.Label(ServiceRegistry.modeLabel());
    private final java.util.function.Consumer<String> serverChanges =
            type -> {
                if (tasks != null)
                    tasks.dispatch(
                            () -> {
                                if (type.equals("DISCONNECTED"))
                                    connectionState.setText("Mất kết nối");
                                else if (type.equals("RECONNECTED"))
                                    connectionState.setText("Đã kết nối");
                                if (type.equals("WALLET_CHANGED") || type.equals("RECONNECTED"))
                                    balanceRefresh.run();
                            });
            };
    private javafx.animation.Timeline walletTimer;
    private final String path = ViewNavigator.getCurrentPath();

    public AppHeader() {
        sceneProperty()
                .addListener(
                        (obs, oldScene, scene) -> {
                            ServiceRegistry.getService().removeChangeListener(serverChanges);
                            if (tasks != null) tasks.close();
                            if (walletTimer != null) walletTimer.stop();
                            readingBalance = false;
                            if (scene != null) {
                                tasks = new ViewTasks();
                                ServiceRegistry.getService().addChangeListener(serverChanges);
                                refreshAccount();
                            }
                        });
    }

    private void refreshAccount() {
        for (javafx.scene.Node child : getChildren()) {
            if (child instanceof Button button)
                button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        }
        if (account != null) getChildren().remove(account);
        if (walletTimer != null) walletTimer.stop();
        if (ServiceRegistry.getService().getCurrentUser() != null) {
            UserMenu menu = new UserMenu();
            menu.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            if (ViewNavigator.Route.DASHBOARD.getPath().equals(path)
                    || "/com/auction/client/views/auction-room-view.fxml".equals(path)) {
                Button balance = new Button("Ví: …");
                balance.visibleProperty().bind(widthProperty().greaterThanOrEqualTo(900));
                balance.managedProperty().bind(balance.visibleProperty());
                balance.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                balance.setOnAction(
                        e -> {
                            try {
                                ViewNavigator.navigate(ViewNavigator.Route.WALLET);
                            } catch (java.io.IOException ex) {
                                throw new IllegalStateException(ex);
                            }
                        });
                HBox group = new HBox(8, connectionState, balance, menu);
                group.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                account = group;
                Runnable update =
                        () -> {
                            var user = ServiceRegistry.getService().getCurrentUser();
                            if (user == null) return;
                            if (readingBalance) {
                                readAgain = true;
                                return;
                            }
                            readingBalance = true;
                            tasks.submit(
                                    "balance",
                                    () ->
                                            ServiceRegistry.getWalletService()
                                                    .getAvailable(user.getUsername()),
                                    value -> {
                                        readingBalance = false;
                                        balance.setText(
                                                "Ví: "
                                                        + com.auction.client.util.FormatUtil
                                                                .formatCurrency(value));
                                        if (readAgain) {
                                            readAgain = false;
                                            balanceRefresh.run();
                                        }
                                    },
                                    error -> {
                                        readingBalance = false;
                                        readAgain = false;
                                        balance.setText("Ví: không thể tải");
                                    });
                        };
                balanceRefresh = update;
                update.run();
                if (ServiceRegistry.modeLabel().equals("MOCK")) {
                    walletTimer =
                            new javafx.animation.Timeline(
                                    new javafx.animation.KeyFrame(
                                            javafx.util.Duration.seconds(2), e -> update.run()));
                    walletTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
                    walletTimer.play();
                }
            } else account = menu;
        } else {
            Button login = new Button("Đăng nhập");
            login.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            login.getStyleClass().add("btn-primary");
            login.setOnAction(
                    e -> {
                        try {
                            ViewNavigator.navigate(ViewNavigator.Route.AUTH);
                        } catch (java.io.IOException ex) {
                            javafx.scene.control.Alert alert =
                                    new javafx.scene.control.Alert(
                                            javafx.scene.control.Alert.AlertType.ERROR,
                                            "Không thể mở màn hình đăng nhập.");
                            ViewNavigator.applyAppStylesheet(alert.getDialogPane());
                            alert.show();
                        }
                    });
            account = login;
        }
        getChildren().add(account);
    }
}
