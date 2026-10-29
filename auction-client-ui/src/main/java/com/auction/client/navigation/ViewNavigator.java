package com.auction.client.navigation;

import com.auction.client.ui.UserMenu;
import com.auction.client.controller.AuctionRoomController;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.ui.AppBackground;
import com.auction.client.util.AppInfo;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ViewNavigator {
    private static final String APP_STYLESHEET = "/com/auction/client/css/app.css";
    private static final Object KEYBOARD_NAVIGATION_FILTERS = new Object();

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
    public static boolean isSellingMode() { return sellingMode; }

    private ViewNavigator() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static <T> T navigate(Route route) throws IOException {
        return loadView(route.path, route.title);
    }

    public static <T> T loadView(String fxmlPath, String windowTitle) throws IOException {
        return loadView(fxmlPath, windowTitle, true);
    }

    public static <T> T loadView(String fxmlPath, String windowTitle, boolean recordHistory) throws IOException {
        FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
        Parent root;
        loadingPath = fxmlPath;
        try {
            root = loader.load();
        } finally {
            loadingPath = null;
        }

        UserMenu.closeActive();
        if (currentController instanceof AuctionRoomController) {
            ((AuctionRoomController) currentController).dispose();
        }
        if (recordHistory && currentView != null
                && (!currentView.path.equals(fxmlPath) || !currentView.title.equals(windowTitle))) {
            history.addLast(currentView);
        }

        Parent sceneRoot = root;
        if (!Route.AUTH.getPath().equals(fxmlPath)) {
            StackPane appRoot = new StackPane(new AppBackground(), root);
            appRoot.getStyleClass().add("root");
            sceneRoot = appRoot;
        }

        if (Route.PRODUCTS.path.equals(fxmlPath)) sellingMode = true;
        else if (Route.DASHBOARD.path.equals(fxmlPath)) sellingMode = false;
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(sceneRoot, 1024, 680);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(sceneRoot);
        }

        currentView = new View(fxmlPath, windowTitle, null);
        currentController = loader.getController();
        applyAppStylesheet(scene);
        primaryStage.setTitle(AppInfo.NAME + " - " + windowTitle);
        primaryStage.show();
        sceneRoot.setFocusTraversable(true);
        Parent focusRoot = sceneRoot;
        Platform.runLater(focusRoot::requestFocus);
        return loader.getController();
    }

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

    public static void applyAppStylesheet(DialogPane dialogPane) {
        java.net.URL stylesheet = ViewNavigator.class.getResource(APP_STYLESHEET);
        if (stylesheet == null) {
            throw new IllegalStateException("Missing stylesheet: " + APP_STYLESHEET);
        }
        String stylesheetUrl = stylesheet.toExternalForm();
        if (!dialogPane.getStylesheets().contains(stylesheetUrl)) {
            dialogPane.getStylesheets().add(stylesheetUrl);
        }
        dialogPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                applyAppStylesheet(newScene);
            }
        });
        if (dialogPane.getScene() != null) {
            applyAppStylesheet(dialogPane.getScene());
        }
    }

    private static void installNavigationModality(Scene scene) {
        if (scene.getProperties().containsKey(KEYBOARD_NAVIGATION_FILTERS)) {
            return;
        }
        scene.getProperties().put(KEYBOARD_NAVIGATION_FILTERS, Boolean.TRUE);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.TAB || code == KeyCode.UP || code == KeyCode.DOWN
                    || code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                Parent root = scene.getRoot();
                if (!root.getStyleClass().contains("keyboard-nav")) {
                    root.getStyleClass().add("keyboard-nav");
                }
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED,
                event -> scene.getRoot().getStyleClass().remove("keyboard-nav"));
    }

    public static boolean back() throws IOException {
        if (history.isEmpty()) {
            return false;
        }
        View previous = history.removeLast();
        try {
            Object controller = loadView(previous.path, previous.title, false);
            if (previous.auctionId != null && controller instanceof AuctionRoomController) {
                com.auction.client.model.Auction auction = ServiceRegistry.getService().getAuctionById(previous.auctionId);
                if (auction == null) throw new IOException("Không tìm thấy phiên đấu giá " + previous.auctionId);
                ((AuctionRoomController) controller).setAuction(auction);
                setCurrentAuctionId(previous.auctionId);
            }
        } catch (IOException ex) { history.addLast(previous); throw ex; }
        return true;
    }

    public static void setCurrentAuctionId(String auctionId) {
        if (currentView != null) currentView = new View(currentView.path, currentView.title, auctionId);
    }

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
