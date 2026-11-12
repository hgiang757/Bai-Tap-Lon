package com.auction.client.ui;

import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.IOException;

public final class UserMenu extends HBox {
    private static UserMenu activeMenu;

    private final AuctionClientService service = ServiceRegistry.getService();
    private final Popup popup = new Popup();
    private final VBox menuContent = new VBox();
    private final Button trigger = new Button();
    private Button firstMenuItem;
    private Scene menuScene;
    private Window menuOwner;
    private final ChangeListener<Boolean> focusListener = (o, before, focused) -> {
        if (!focused) Platform.runLater(() -> {
            if (popup.isShowing() && menuOwner != null
                    && !menuOwner.isFocused() && !popup.isFocused()) hideMenu();
        });
    };
    private final ChangeListener<Boolean> showingListener = (o, before, showing) -> {
        if (!showing) hideMenu();
    };
    private final EventHandler<KeyEvent> escapeHandler = this::handleEscape;

    public UserMenu() {
        getStyleClass().add("user-menu-host");
        setAlignment(Pos.CENTER);
        User user = service.getCurrentUser();
        String displayName = user == null ? "Tài khoản" : safeName(user);
        String initial = displayName.isEmpty() ? "V" : displayName.substring(0, 1).toUpperCase();

        Label avatar = new Label(initial);
        avatar.getStyleClass().add("user-menu-avatar");
        Label name = new Label(displayName);
        name.getStyleClass().add("user-menu-trigger-name");
        name.setMaxWidth(140);
        name.setWrapText(false);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setTooltip(new Tooltip(displayName));
        Region chevron = icon("chevron-down", 12, "icon-muted");
        HBox contents = new HBox(9, avatar, name, chevron);
        contents.setAlignment(Pos.CENTER);
        trigger.setGraphic(contents);
        trigger.getStyleClass().add("user-menu-trigger");
        trigger.setAccessibleText("Mở menu tài khoản của " + displayName);
        trigger.setTooltip(new Tooltip("Menu tài khoản"));
        trigger.setOnAction(
                event -> {
                    if (popup.isShowing()) {
                        hideMenu();
                    } else {
                        openMenu();
                    }
                });
        getChildren().add(trigger);
        createMenu(user, displayName);
        popup.getContent().add(menuContent);
        popup.setAutoHide(true);
        popup.setConsumeAutoHidingEvents(true);
        popup.setOnHidden(event -> removeSceneHandlers());
        sceneProperty().addListener((o, before, scene) -> {
            if (before != scene) hideMenu();
        });
    }

    public static void closeActive() {
        if (activeMenu != null) {
            activeMenu.hideMenu();
        }
    }

    private void createMenu(User user, String displayName) {
        menuContent.getStyleClass().add("user-menu-popup");
        menuContent.setFocusTraversable(true);
        menuContent.setPrefWidth(300);
        menuContent.setMinWidth(300);

        VBox account = new VBox(5);
        account.getStyleClass().add("user-menu-account");
        Label accountName = new Label(displayName);
        accountName.getStyleClass().add("user-menu-account-name");
        String email = user == null ? "" : user.getEmail();
        String secondary =
                email == null || email.trim().isEmpty()
                        ? (user == null ? "" : user.getUsername())
                        : email;
        Label accountEmail = new Label(secondary == null ? "" : secondary);
        accountEmail.getStyleClass().add("user-menu-account-secondary");
        Label role = new Label(user == null ? "" : user.getRole().getDisplayName());
        role.getStyleClass().add("user-menu-role");
        account.getChildren().addAll(accountName, accountEmail, role);
        menuContent.getChildren().addAll(account, divider());

        if (user != null && user.getRole() == UserRole.SELLER) {
            Label modeTitle = new Label("Chuyển chế độ");
            modeTitle.getStyleClass().add("user-menu-section-title");
            menuContent.getChildren().add(modeTitle);
            boolean sellingMode = isProductsRoute();
            Button auctionMode = menuButton("Chế độ đấu giá", "gavel", !sellingMode);
            Button salesMode = menuButton("Chế độ bán hàng", "bag", sellingMode);
            auctionMode.setOnAction(event -> navigate(ViewNavigator.Route.DASHBOARD));
            salesMode.setOnAction(event -> navigate(ViewNavigator.Route.PRODUCTS));
            menuContent.getChildren().addAll(auctionMode, salesMode, divider());
        }

        if (user != null
                && (user.getRole() == UserRole.BIDDER || user.getRole() == UserRole.SELLER)) {
            Button wallet = menuButton("Ví của tôi", "bag", false);
            wallet.setOnAction(event -> navigate(ViewNavigator.Route.WALLET));
            menuContent.getChildren().add(wallet);
        }
        Button guide = menuButton("Hướng dẫn sử dụng", "book", false);
        guide.setOnAction(event -> navigate(ViewNavigator.Route.GUIDE));
        Button contact = menuButton("Liên hệ", "mail", false);
        contact.setOnAction(event -> navigate(ViewNavigator.Route.CONTACT));
        menuContent.getChildren().addAll(guide, contact, divider());

        Button logout = menuButton("Đăng xuất", "logout", false);
        logout.getStyleClass().add("btn-danger");
        logout.setOnAction(event -> logout());
        menuContent.getChildren().add(logout);
    }

    private Button menuButton(String text, String iconName, boolean selected) {
        Button button = new Button(text);
        button.getStyleClass().add("user-menu-item");
        button.setGraphic(icon(iconName, 18, "icon-muted"));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAccessibleText(text);
        if (selected) {
            button.getStyleClass().add("user-menu-item-selected");
            button.setAccessibleText(text + ", chế độ hiện tại");
        }
        if (firstMenuItem == null) {
            firstMenuItem = button;
        }
        return button;
    }

    private Region icon(String name, double size, String colorClass) {
        Region result = Icons.icon(name, size);
        result.getStyleClass().add(colorClass);
        return result;
    }

    private Region divider() {
        Region line = new Region();
        line.getStyleClass().add("user-menu-divider");
        return line;
    }

    private boolean isProductsRoute() {
        return ViewNavigator.isSellingMode()
                || ViewNavigator.Route.PRODUCTS.getPath().equals(ViewNavigator.getCurrentPath());
    }

    private void openMenu() {
        Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
        Window owner = trigger.getScene() == null ? null : trigger.getScene().getWindow();
        if (bounds == null || owner == null) {
            return;
        }
        if (activeMenu != null && activeMenu != this) activeMenu.hideMenu();
        menuScene = trigger.getScene();
        menuOwner = owner;
        activeMenu = this;
        popup.show(owner, bounds.getMaxX() - 300, bounds.getMaxY() + 8);
        Scene popupScene = popup.getScene();
        ViewNavigator.applyAppStylesheet(popupScene);
        popupScene.addEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        if (trigger.getScene() != null) {
            trigger.getScene().addEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        }
        owner.focusedProperty().addListener(focusListener);
        owner.showingProperty().addListener(showingListener);
        popup.focusedProperty().addListener(focusListener);
        boolean keyboardNavigation =
                trigger.getScene() != null
                        && trigger.getScene().getRoot().getStyleClass().contains("keyboard-nav");
        popupScene.getRoot().getStyleClass().remove("keyboard-nav");
        if (keyboardNavigation) {
            popupScene.getRoot().getStyleClass().add("keyboard-nav");
        }
        Platform.runLater(
                () -> {
                    if (!popup.isShowing()) {
                        return;
                    }
                    if (keyboardNavigation && firstMenuItem != null) {
                        firstMenuItem.requestFocus();
                    } else {
                        menuContent.setFocusTraversable(true);
                        menuContent.requestFocus();
                    }
                });
    }

    private void handleEscape(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE && popup.isShowing()) {
            event.consume();
            hideMenu();
            trigger.requestFocus();
        }
    }

    private void hideMenu() {
        popup.hide();
    }

    private void removeSceneHandlers() {
        if (popup.getScene() != null) {
            popup.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        }
        if (menuScene != null) {
            menuScene.removeEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        }
        if (menuOwner != null) {
            menuOwner.focusedProperty().removeListener(focusListener);
            menuOwner.showingProperty().removeListener(showingListener);
        }
        popup.focusedProperty().removeListener(focusListener);
        menuScene = null;
        menuOwner = null;
        if (activeMenu == this) {
            activeMenu = null;
        }
    }

    private void navigate(ViewNavigator.Route route) {
        hideMenu();
        try {
            ViewNavigator.navigate(route);
        } catch (IOException exception) {
            showNavigationError(exception);
        }
    }

    private void logout() {
        hideMenu();
        trigger.setDisable(true);
        var tasks = new ViewTasks();
        tasks.submit(
                "logout",
                () -> {
                    service.logout();
                    return null;
                },
                ignored -> {
                    tasks.close();
                    ViewNavigator.clearHistory();
                    try {
                        ViewNavigator.loadView(
                                ViewNavigator.Route.AUTH.getPath(),
                                ViewNavigator.Route.AUTH.getTitle(),
                                false);
                    } catch (IOException exception) {
                        trigger.setDisable(false);
                        showNavigationError(exception);
                    }
                },
                error -> {
                    tasks.close();
                    trigger.setDisable(false);
                    showNavigationError(new IOException("Không thể đăng xuất", error));
                });
    }

    private void showNavigationError(IOException exception) {
        exception.printStackTrace();
        Alert alert =
                new Alert(Alert.AlertType.ERROR, "Không thể chuyển màn hình. Vui lòng thử lại.");
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
        alert.setHeaderText("Lỗi giao diện");
        alert.show();
    }

    private String safeName(User user) {
        String name = user.getUsername();
        return name == null || name.isBlank() ? "Tài khoản" : name;
    }
}
