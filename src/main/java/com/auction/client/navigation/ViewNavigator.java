package com.auction.client.navigation;

import com.auction.client.controller.AuctionRoomController;
import com.auction.client.ui.UserMenu;
import com.auction.client.util.AppInfo;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/** Loads FXML views and releases the previous view before navigation. */
public final class ViewNavigator {
  private static final String APP_STYLESHEET = "/com/auction/client/css/app.css";
  private static final Object KEYBOARD_NAVIGATION_FILTERS = new Object();
  private static final EventHandler<WindowEvent> CLOSE_MENU = event -> UserMenu.closeActive();

  /** Names the application views and their FXML resources. */
  public enum Route {
    AUTH("/com/auction/client/views/auth-view.fxml", "Đăng nhập"),
    DASHBOARD("/com/auction/client/views/home-view.fxml", "Sàn đấu giá trực tuyến"),
    PRODUCTS("/com/auction/client/views/seller-products-view.fxml", "Quản lý sản phẩm"),
    WALLET("/com/auction/client/views/wallet-view.fxml", "Ví của tôi"),
    ADMIN("/com/auction/client/views/admin-view.fxml", "Quản trị"),
    GUIDE("/com/auction/client/views/guide-view.fxml", "Hướng dẫn sử dụng"),
    CONTACT("/com/auction/client/views/contact-view.fxml", "Liên hệ");

    private final String path;
    private final String title;

    Route(String path, String title) {
      this.path = path;
      this.title = title;
    }

    public String getPath() {
      return path;
    }

    public String getTitle() {
      return title;
    }
  }

  private static final Deque<View> history = new ArrayDeque<>();
  private static Stage primaryStage;
  private static View currentView;
  private static String loadingPath;
  private static Object currentController;
  private static boolean sellingMode;

  public static boolean isSellingMode() {
    return sellingMode;
  }

  private ViewNavigator() {}

  /** Selects the main window and installs one menu cleanup handler for window hiding. */
  public static void setPrimaryStage(Stage stage) {
    UserMenu.closeActive();
    if (primaryStage != null) {
      primaryStage.removeEventHandler(WindowEvent.WINDOW_HIDDEN, CLOSE_MENU);
    }
    primaryStage = stage;
    stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, CLOSE_MENU);
  }

  public static Stage getPrimaryStage() {
    return primaryStage;
  }

  /** Opens the selected application route. */
  public static <T> T navigate(Route route) throws IOException {
    return loadView(route.path, route.title);
  }

  /** Loads the FXML view, updates navigation state and disposes the previous controller. */
  public static <T> T loadView(String fxmlPath, String windowTitle) throws IOException {
    return loadView(fxmlPath, windowTitle, true);
  }

  /** Loads the FXML view, updates navigation state and disposes the previous controller. */
  public static <T> T loadView(String fxmlPath, String windowTitle, boolean recordHistory)
      throws IOException {
    java.net.URL resource = ViewNavigator.class.getResource(fxmlPath);
    if (resource == null) {
      throw new IOException("Không tìm thấy giao diện: " + fxmlPath);
    }
    FXMLLoader loader = new FXMLLoader(resource);
    Parent root;
    loadingPath = fxmlPath;
    try {
      root = loader.load();
    } catch (IOException | RuntimeException error) {
      if (loader.getController() instanceof DisposableView view) {
        view.dispose();
      }
      throw error;
    } finally {
      loadingPath = null;
    }

    disposeCurrentView();
    if (recordHistory
        && currentView != null
        && (!currentView.path.equals(fxmlPath) || !currentView.title.equals(windowTitle))) {
      history.addLast(currentView);
    }

    if (Route.PRODUCTS.path.equals(fxmlPath)) {
      sellingMode = true;
    } else if (Route.DASHBOARD.path.equals(fxmlPath)) {
      sellingMode = false;
    }
    Scene scene = primaryStage.getScene();
    // The menu is a sibling of the view, so it cannot resize or escape the main window.
    if (root instanceof Region region) {
      region.setMinSize(0, 0);
    }
    StackPane container = new StackPane(root);
    if (scene == null) {
      scene = new Scene(container, 1024, 680);
      primaryStage.setScene(scene);
    } else {
      scene.setRoot(container);
    }

    currentView = new View(fxmlPath, windowTitle, null);
    currentController = loader.getController();
    applyAppStylesheet(scene);
    primaryStage.setTitle(
        AppInfo.NAME
            + " ["
            + com.auction.client.service.ServiceRegistry.modeLabel()
            + "] - "
            + windowTitle);
    primaryStage.show();
    root.setFocusTraversable(true);
    Parent focusRoot = root;
    Platform.runLater(
        () -> {
          if (focusRoot.getScene() == primaryStage.getScene()) {
            focusRoot.requestFocus();
          }
        });
    return loader.getController();
  }

  /** Applies the shared application stylesheet to the supplied UI surface. */
  public static void applyAppStylesheet(Scene scene) {
    Parent root = scene.getRoot();
    if (!root.getStyleClass().contains("root")) {
      root.getStyleClass().add("root");
    }
    installNavigationModality(scene);
    java.net.URL stylesheet = ViewNavigator.class.getResource(APP_STYLESHEET);
    if (stylesheet == null) {
      throw new IllegalStateException("Missing stylesheet: " + APP_STYLESHEET);
    }
    String stylesheetUrl = stylesheet.toExternalForm();
    if (!scene.getStylesheets().contains(stylesheetUrl)) {
      scene.getStylesheets().add(stylesheetUrl);
    }
  }

  /** Applies the shared application stylesheet to the supplied UI surface. */
  public static void applyAppStylesheet(DialogPane dialogPane) {
    java.net.URL stylesheet = ViewNavigator.class.getResource(APP_STYLESHEET);
    if (stylesheet == null) {
      throw new IllegalStateException("Missing stylesheet: " + APP_STYLESHEET);
    }
    String stylesheetUrl = stylesheet.toExternalForm();
    if (!dialogPane.getStylesheets().contains(stylesheetUrl)) {
      dialogPane.getStylesheets().add(stylesheetUrl);
    }
    dialogPane
        .sceneProperty()
        .addListener(
            (observable, oldScene, newScene) -> {
              if (newScene != null) {
                applyAppStylesheet(newScene);
              }
            });
    if (dialogPane.getScene() != null) {
      applyAppStylesheet(dialogPane.getScene());
    }
  }

  /** Dialog thuộc cửa sổ/trang hiện tại; đóng và tháo listener khi owner hoặc trang đổi. */
  public static void prepareDialog(Dialog<?> dialog) {
    applyAppStylesheet(dialog.getDialogPane());
    dialog.getDialogPane().getStyleClass().addAll("form-controls", "app-dialog");
    Stage owner = primaryStage;
    if (owner == null) {
      return;
    }
    dialog.initOwner(owner);
    dialog.initModality(Modality.WINDOW_MODAL);
    Scene scene = owner.getScene();
    ChangeListener<Boolean> visibility =
        (o, before, showing) -> {
          if (!showing) {
            dialog.close();
          }
        };
    ChangeListener<Parent> navigation = (o, before, root) -> dialog.close();
    ChangeListener<Scene> sceneChange = (o, before, current) -> dialog.close();
    dialog
        .showingProperty()
        .addListener(
            (o, before, showing) -> {
              if (showing) {
                owner.showingProperty().addListener(visibility);
                owner.sceneProperty().addListener(sceneChange);
                if (scene != null) {
                  scene.rootProperty().addListener(navigation);
                }
              } else {
                owner.showingProperty().removeListener(visibility);
                owner.sceneProperty().removeListener(sceneChange);
                if (scene != null) {
                  scene.rootProperty().removeListener(navigation);
                }
              }
            });
  }

  private static void installNavigationModality(Scene scene) {
    if (scene.getProperties().containsKey(KEYBOARD_NAVIGATION_FILTERS)) {
      return;
    }
    scene.getProperties().put(KEYBOARD_NAVIGATION_FILTERS, Boolean.TRUE);
    scene.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          KeyCode code = event.getCode();
          if (code == KeyCode.TAB
              || code == KeyCode.UP
              || code == KeyCode.DOWN
              || code == KeyCode.LEFT
              || code == KeyCode.RIGHT) {
            Parent root = scene.getRoot();
            if (!root.getStyleClass().contains("keyboard-nav")) {
              root.getStyleClass().add("keyboard-nav");
            }
          }
        });
    scene.addEventFilter(
        MouseEvent.MOUSE_PRESSED, event -> scene.getRoot().getStyleClass().remove("keyboard-nav"));
  }

  /** Returns to the previous view when navigation history is available. */
  public static boolean back() throws IOException {
    if (history.isEmpty()) {
      return false;
    }
    View previous = history.removeLast();
    try {
      Object controller = loadView(previous.path, previous.title, false);
      if (previous.auctionId != null && controller instanceof AuctionRoomController) {
        ((AuctionRoomController) controller).loadAuction(previous.auctionId);
        setCurrentAuctionId(previous.auctionId);
      }
    } catch (IOException ex) {
      history.addLast(previous);
      throw ex;
    }
    return true;
  }

  /** Associates the current view with the selected auction for back navigation. */
  public static void setCurrentAuctionId(String auctionId) {
    if (currentView != null) {
      currentView = new View(currentView.path, currentView.title, auctionId);
    }
  }

  /** Disposes the active controller and clears its reference. */
  public static void disposeCurrentView() {
    UserMenu.closeActive();
    if (currentController instanceof DisposableView view) {
      view.dispose();
    }
    currentController = null;
  }

  /** Removes the saved navigation history. */
  public static void clearHistory() {
    history.clear();
  }

  public static String getCurrentPath() {
    return loadingPath != null ? loadingPath : currentView == null ? null : currentView.path;
  }

  public static String getNavigationSourcePath() {
    return currentView == null ? null : currentView.path;
  }

  public static String getPreviousPath() {
    return history.isEmpty() ? null : history.peekLast().path;
  }

  private static final class View {
    private final String path;
    private final String title;

    private final String auctionId;

    private View(String path, String title, String auctionId) {
      this.path = path;
      this.title = title;
      this.auctionId = auctionId;
    }
  }
}
