package com.auction.client.ui;

import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.navigation.ViewNavigator;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.KeyCode;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;

public final class UserMenu extends HBox {
    private static UserMenu activeMenu;

    private final AuctionClientService service = ServiceRegistry.getService();
    private final Popup popup = new Popup();
    private final VBox menuContent = new VBox();
    private final Button trigger = new Button();
    private Button firstMenuItem;
    private boolean closing;
    private final EventHandler<MouseEvent> outsideClickHandler = this::handleOutsideClick;
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
        trigger.setOnAction(event -> {
            if (popup.isShowing()) {
                closeMenu();
            } else {
                openMenu();
            }
        });
        getChildren().add(trigger);
        createMenu(user, displayName);
        popup.getContent().add(menuContent);
        popup.setAutoHide(false);
        popup.setOnHidden(event -> removeSceneHandlers());
    }

    public static void closeActive() {
        if (activeMenu != null) {
            activeMenu.closeMenu();
        }
    }

    private void createMenu(User user, String displayName) {
        menuContent.getStyleClass().add("user-menu-popup");
        menuContent.setFocusTraversable(true);
        menuContent.setPrefWidth(260);
        menuContent.setMinWidth(260);

        VBox account = new VBox(5);
        account.getStyleClass().add("user-menu-account");
        Label accountName = new Label(displayName);
        accountName.getStyleClass().add("user-menu-account-name");
        String email = user == null ? "" : user.getEmail();
        String secondary = email == null || email.trim().isEmpty()
                ? (user == null ? "" : user.getUsername())
                : email;
        Label accountEmail = new Label(secondary == null ? "" : secondary);
        accountEmail.getStyleClass().add("user-menu-account-secondary");
        Label role = new Label(user == null ? "" : (user.getRole() == UserRole.SELLER ? "Người bán" : "Người mua"));
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
            Label note = new Label("Chỉ đổi cách xem, không đổi quyền tài khoản.");
            note.getStyleClass().add("user-menu-note");
            menuContent.getChildren().addAll(auctionMode, salesMode, note, divider());
        }

        if (user != null && (user.getRole() == UserRole.BIDDER || user.getRole() == UserRole.SELLER)) {
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
        Button button = new Button();
        button.getStyleClass().add("user-menu-item");
        if (selected) {
            button.getStyleClass().add("user-menu-item-selected");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        button.setAccessibleText(text);
        HBox row = new HBox(12);
        row.setPrefWidth(212);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(icon(iconName, 20, "icon-muted"));
        Label label = new Label(text);
        label.setMinWidth(0);
        label.setPrefWidth(150);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().add(label);
        if (selected) {
            row.getChildren().add(icon("check", 16, "icon-primary"));
            button.setAccessibleText(text + ", chế độ hiện tại");
        }
        button.setGraphic(row);
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
        activeMenu = this;
        popup.show(owner, bounds.getMaxX() - 260, bounds.getMaxY() + 8);
        Scene popupScene = popup.getScene();
        ViewNavigator.applyAppStylesheet(popupScene);
        popupScene.addEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        if (trigger.getScene() != null) {
            trigger.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
            trigger.getScene().addEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        }
        menuContent.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(120), menuContent);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        boolean keyboardNavigation = trigger.getScene() != null
                && trigger.getScene().getRoot().getStyleClass().contains("keyboard-nav");
        if (!keyboardNavigation) {
            popupScene.getRoot().getStyleClass().remove("keyboard-nav");
        }
        Platform.runLater(() -> {
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
            closeMenu();
            trigger.requestFocus();
        }
    }

    private void handleOutsideClick(MouseEvent event) {
        if (!popup.isShowing() || event.getTarget() == trigger || isDescendant(trigger, event.getTarget())) {
            return;
        }
        Bounds bounds = menuContent.localToScreen(menuContent.getBoundsInLocal());
        if (bounds == null || !bounds.contains(event.getScreenX(), event.getScreenY())) {
            closeMenu();
        }
    }

    private boolean isDescendant(javafx.scene.Node ancestor, Object candidate) {
        if (!(candidate instanceof javafx.scene.Node)) {
            return false;
        }
        for (javafx.scene.Node node = (javafx.scene.Node) candidate; node != null; node = node.getParent()) {
            if (node == ancestor) {
                return true;
            }
        }
        return false;
    }

    private void closeMenu() {
        if (!popup.isShowing() || closing) {
            return;
        }
        closing = true;
        FadeTransition fade = new FadeTransition(Duration.millis(120), menuContent);
        fade.setFromValue(menuContent.getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(event -> {
            popup.hide();
            menuContent.setOpacity(1);
            closing = false;
            if (activeMenu == this) {
                activeMenu = null;
            }
        });
        fade.play();
    }

    private void removeSceneHandlers() {
        if (popup.getScene() != null) {
            popup.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        }
        if (trigger.getScene() != null) {
            trigger.getScene().removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
            trigger.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        }
        if (activeMenu == this) {
            activeMenu = null;
        }
    }

    private void navigate(ViewNavigator.Route route) {
        closeMenu();
        try {
            ViewNavigator.navigate(route);
        } catch (IOException exception) {
            showNavigationError(exception);
        }
    }

    private void logout() {
        closeMenu();
        service.logout();
        ViewNavigator.clearHistory();
        try {
            ViewNavigator.loadView(ViewNavigator.Route.AUTH.getPath(), ViewNavigator.Route.AUTH.getTitle(), false);
        } catch (IOException exception) {
            showNavigationError(exception);
        }
    }

    private void showNavigationError(IOException exception) {
        exception.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể chuyển màn hình. Vui lòng thử lại.");
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
        alert.setHeaderText("Lỗi giao diện");
        alert.show();
    }

    private String safeName(User user) {
        String name = user.getUsername();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        String username = user.getUsername();
        return username == null || username.trim().isEmpty() ? "Tài khoản" : username;
    }
}
