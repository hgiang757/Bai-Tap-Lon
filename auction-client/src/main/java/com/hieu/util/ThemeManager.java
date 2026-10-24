package com.hieu.util;

import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.prefs.Preferences;

/**
 * Quản lý theme Sáng/Tối: gắn stylesheet looked-up colors và nhớ lựa chọn.
 */
public final class ThemeManager {

    public enum Theme {
        LIGHT,
        DARK
    }

    private static final String PREF_KEY = "theme";
    private static final String BASE_CSS = "/com/hieu/css/base.css";
    private static final String LIGHT_CSS = "/com/hieu/css/theme-light.css";
    private static final String DARK_CSS = "/com/hieu/css/theme-dark.css";
    /** Trùng token -c-bg theme sáng — tô Scene để FadeTransition không lộ nền trắng mặc định. */
    private static final String LIGHT_FILL = "#F5F5F7";
    /** Trùng token -c-bg theme tối. */
    private static final String DARK_FILL = "#11131E";

    private static Theme currentTheme = loadSavedTheme();

    private ThemeManager() {
    }

    public static Theme current() {
        return currentTheme;
    }

    /**
     * Gỡ stylesheet theme cũ, đảm bảo base.css đứng trước, rồi gắn file theme hiện tại.
     */
    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }

        String base = url(BASE_CSS);
        String light = url(LIGHT_CSS);
        String dark = url(DARK_CSS);
        if (base == null || light == null || dark == null) {
            return;
        }

        scene.getStylesheets().remove(light);
        scene.getStylesheets().remove(dark);

        if (!scene.getStylesheets().contains(base)) {
            scene.getStylesheets().add(0, base);
        } else {
            scene.getStylesheets().remove(base);
            scene.getStylesheets().add(0, base);
        }

        String themeSheet = currentTheme == Theme.DARK ? dark : light;
        if (!scene.getStylesheets().contains(themeSheet)) {
            scene.getStylesheets().add(themeSheet);
        }

        scene.setFill(Color.web(currentTheme == Theme.DARK ? DARK_FILL : LIGHT_FILL));
    }

    public static void toggle(Scene scene) {
        currentTheme = currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK;
        saveTheme(currentTheme);
        apply(scene);
    }

    private static Theme loadSavedTheme() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            String stored = prefs.get(PREF_KEY, null);
            if (stored == null || stored.isBlank()) {
                return Theme.LIGHT;
            }
            return Theme.valueOf(stored);
        } catch (Exception ex) {
            return Theme.LIGHT;
        }
    }

    private static void saveTheme(Theme theme) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_KEY, theme.name());
            prefs.flush();
        } catch (Exception ignored) {
            // Giữ theme trong bộ nhớ phiên này; lần chạy sau sẽ về mặc định nếu không ghi được.
        }
    }

    private static String url(String resourcePath) {
        java.net.URL resource = ThemeManager.class.getResource(resourcePath);
        return resource == null ? null : resource.toExternalForm();
    }
}
