package com.hieu.util;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.geometry.Pos;
import com.hieu.ui.Icons;
import javafx.scene.layout.Region;

/**
 * Nút chuyển theme Sáng/Tối, biểu tượng mặt trời / mặt trăng vẽ bằng SVGPath.
 */
public class ThemeToggle extends Button {

    public ThemeToggle() {
        getStyleClass().add("theme-toggle");
        setFocusTraversable(true);
        setDefaultButton(false);
        setCancelButton(false);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);
        setGraphicTextGap(0);
        refreshGlyph();
        setOnAction(e -> {
            Scene scene = getScene();
            if (scene != null) {
                ThemeManager.toggle(scene);
                refreshGlyph();
            }
        });
    }

    private void refreshGlyph() {
        Region glyph;
        if (ThemeManager.current() == ThemeManager.Theme.DARK) {
            glyph = Icons.icon("sun", 20);
            setTooltip(new Tooltip("Chuyển sang giao diện sáng"));
        } else {
            glyph = Icons.icon("moon", 20);
            setTooltip(new Tooltip("Chuyển sang giao diện tối"));
        }
        glyph.getStyleClass().add("icon-current");
        setGraphic(glyph);
        setAccessibleText(getTooltip().getText());
    }
}
