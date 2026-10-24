package com.hieu.util;

import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Nút chuyển theme Sáng/Tối, biểu tượng mặt trời / mặt trăng vẽ bằng SVGPath.
 */
public class ThemeToggle extends Button {

    private static final Duration HALF = Duration.millis(90);

    private final SVGPath glyph = new SVGPath();
    private boolean switching;

    public ThemeToggle() {
        getStyleClass().add("theme-toggle");
        setFocusTraversable(true);
        setDefaultButton(false);
        setCancelButton(false);
        setTooltip(new Tooltip("Chế độ sáng/tối"));
        glyph.setStrokeWidth(1.6);
        setGraphic(glyph);
        refreshGlyph();
        setOnAction(e -> toggleAnimated());
    }

    private void toggleAnimated() {
        Scene scene = getScene();
        if (scene == null || switching) {
            return;
        }
        Parent root = scene.getRoot();
        switching = true;

        FadeTransition fadeOut = new FadeTransition(HALF, root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(ev -> {
            ThemeManager.toggle(scene);
            refreshGlyph();
            FadeTransition fadeIn = new FadeTransition(HALF, root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(done -> switching = false);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void refreshGlyph() {
        if (ThemeManager.current() == ThemeManager.Theme.DARK) {
            // Đang tối → hiện mặt trời (chuyển sang sáng)
            glyph.setContent("M12 8a4 4 0 1 0 0.01 0z M12 2v2 M12 20v2 M4.93 4.93l1.41 1.41 M17.66 17.66l1.41 1.41 M2 12h2 M20 12h2 M4.93 19.07l1.41-1.41 M17.66 6.34l1.41-1.41");
            glyph.getStyleClass().setAll("theme-toggle-glyph");
        } else {
            // Đang sáng → hiện mặt trăng (chuyển sang tối)
            glyph.setContent("M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z");
            glyph.getStyleClass().setAll("theme-toggle-glyph-fill");
        }
    }
}
