package com.auction.client.ui;

import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import java.io.IOException;
import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Toggles an account menu inside the main scene, without a separate popup window. */
public final class UserMenu extends HBox {
  private static UserMenu activeMenu;

  private final AuctionClientService service = ServiceRegistry.getService();
  private final ScrollPane menuPane = new ScrollPane();
  private final VBox menuContent = new VBox();
  private final Button trigger = new Button();

  /** Builds the account button and its in-scene menu. */
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
          if (isOpen()) {
            hideMenu();
          } else {
            openMenu();
          }
        });
    getChildren().add(trigger);
    createMenu(user, displayName);
    menuPane.getStyleClass().add("user-menu-pane");
    menuPane.setContent(menuContent);
    menuPane.setFitToWidth(true);
    menuPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    menuPane.setPrefWidth(300);
    menuPane.setMaxWidth(300);
    menuPane.setMaxHeight(Region.USE_PREF_SIZE);
    sceneProperty()
        .addListener(
            (o, before, scene) -> {
              if (before != scene) {
                hideMenu();
              }
            });
  }

  /** Closes the account menu before navigation, logout or window shutdown. */
  public static void closeActive() {
    if (activeMenu != null) {
      activeMenu.hideMenu();
    }
  }

  private void createMenu(User user, String displayName) {
    menuContent.getStyleClass().add("user-menu-content");

    VBox account = new VBox(5);
    account.getStyleClass().add("user-menu-account");
    Label accountName = new Label(displayName);
    accountName.getStyleClass().add("user-menu-account-name");
    accountName.setWrapText(true);
    String email = user == null ? "" : user.getEmail();
    String secondary =
        email == null || email.trim().isEmpty() ? (user == null ? "" : user.getUsername()) : email;
    Label accountEmail = new Label(secondary == null ? "" : secondary);
    accountEmail.getStyleClass().add("user-menu-account-secondary");
    accountEmail.setWrapText(true);
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
    Button button = new Button(text);
    button.getStyleClass().add("user-menu-item");
    button.setGraphic(icon(iconName, 18, "icon-muted"));
    button.setMaxWidth(Double.MAX_VALUE);
    button.setAccessibleText(text);
    if (selected) {
      button.getStyleClass().add("user-menu-item-selected");
      button.setAccessibleText(text + ", chế độ hiện tại");
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

  private boolean isOpen() {
    return menuPane.getParent() != null;
  }

  private void openMenu() {
    if (getScene() == null || !(getScene().getRoot() instanceof StackPane host)) {
      return;
    }
    closeActive();
    Bounds bounds = localToScene(getBoundsInLocal());
    double top = bounds.getMaxY() + 8;
    StackPane.setAlignment(menuPane, Pos.TOP_RIGHT);
    StackPane.setMargin(menuPane, new Insets(top, 16, 16, 16));
    host.getChildren().add(menuPane);
    host.applyCss();
    // Scroll only when the account details and actions cannot fit in the current window.
    menuPane
        .prefHeightProperty()
        .bind(
            Bindings.min(
                menuContent.prefHeight(280) + 2,
                Bindings.max(0, host.heightProperty().subtract(top + 16))));
    activeMenu = this;
  }

  private void hideMenu() {
    if (menuPane.getParent() instanceof StackPane host) {
      host.getChildren().remove(menuPane);
    }
    menuPane.prefHeightProperty().unbind();
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
                ViewNavigator.Route.AUTH.getPath(), ViewNavigator.Route.AUTH.getTitle(), false);
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
    Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể chuyển màn hình. Vui lòng thử lại.");
    ViewNavigator.prepareDialog(alert);
    alert.setHeaderText("Lỗi giao diện");
    alert.show();
  }

  private String safeName(User user) {
    String name = user.getUsername();
    return name == null || name.isBlank() ? "Tài khoản" : name;
  }
}
