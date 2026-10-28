package com.auction.client.ui;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/** Kết nối hai ô cùng một giá trị với nút hiển thị mật khẩu. */
public final class PasswordFieldWithToggle {
    private PasswordFieldWithToggle() { }

    public static void install(PasswordField hidden, TextField visible, Button toggle) {
        visible.textProperty().bindBidirectional(hidden.textProperty());
        updateButton(toggle, false);
        toggle.setOnAction(event -> {
            TextField current = visible.isVisible() ? visible : hidden;
            int anchor = current.getAnchor();
            int caret = current.getCaretPosition();
            boolean show = !visible.isVisible();
            visible.setVisible(show);
            visible.setManaged(show);
            hidden.setVisible(!show);
            hidden.setManaged(!show);
            TextField target = show ? visible : hidden;
            target.requestFocus();
            target.selectRange(anchor, caret);
            updateButton(toggle, show);
        });
    }

    private static void updateButton(Button button, boolean shown) {
        String description = shown ? "Ẩn mật khẩu" : "Hiện mật khẩu";
        Region icon = Icons.icon(shown ? "eye-off" : "eye", 20);
        icon.getStyleClass().add("icon-muted");
        button.setGraphic(icon);
        button.setAccessibleText(description);
        button.setTooltip(new Tooltip(description));
    }
}
