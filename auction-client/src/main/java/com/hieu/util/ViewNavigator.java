package com.hieu.util;

import com.hieu.ui.UserMenu;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ViewNavigator {
    public enum Route {
        AUTH("/com/hieu/views/auth-view.fxml", "Đăng nhập"),
        DASHBOARD("/com/hieu/views/dashboard-view.fxml", "Sàn đấu giá trực tuyến"),
        PRODUCTS("/com/hieu/views/product-management-view.fxml", "Quản lý sản phẩm"),
        GUIDE("/com/hieu/views/guide-view.fxml", "Hướng dẫn sử dụng"),
        CONTACT("/com/hieu/views/contact-view.fxml", "Liên hệ");

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
        if (recordHistory && currentView != null
                && (!currentView.path.equals(fxmlPath) || !currentView.title.equals(windowTitle))) {
            history.addLast(currentView);
        }

        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1024, 680);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        currentView = new View(fxmlPath, windowTitle);
        ThemeManager.apply(scene);
        primaryStage.setTitle(AppInfo.NAME + " - " + windowTitle);
        primaryStage.show();
        return loader.getController();
    }

    public static boolean back() throws IOException {
        if (history.isEmpty()) {
            return false;
        }
        View previous = history.removeLast();
        loadView(previous.path, previous.title, false);
        return true;
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

        private View(String path, String title) {
            this.path = path;
            this.title = title;
        }
    }
}
