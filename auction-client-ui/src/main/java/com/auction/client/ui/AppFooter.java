package com.auction.client.ui;

import com.auction.client.navigation.ViewNavigator;
import com.auction.client.util.AppInfo;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public final class AppFooter extends VBox {
    private static final double CONTENT_BOTTOM_GAP = 40;

    public AppFooter() {
        getStyleClass().add("app-footer");
        setAlignment(Pos.CENTER);
        VBox content = new VBox(20);
        content.getStyleClass().add("app-footer-content");
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(1200);
        Label intro = new Label(AppInfo.NAME + " — Sàn đấu giá trực tuyến theo thời gian thực, minh bạch và thuận tiện.");
        intro.getStyleClass().add("app-footer-intro");
        intro.setWrapText(true);

        HBox links = new HBox(12);
        links.setAlignment(Pos.CENTER);
        Button guide = linkButton("Hướng dẫn sử dụng", "book");
        guide.setOnAction(event -> navigate(ViewNavigator.Route.GUIDE));
        Button contact = linkButton("Liên hệ", "mail");
        contact.setOnAction(event -> navigate(ViewNavigator.Route.CONTACT));
        links.getChildren().addAll(guide, contact);
        content.getChildren().addAll(intro, links);
        getChildren().add(content);
        VBox.setVgrow(content, Priority.NEVER);
    }

    public static void install(ScrollPane scroll, VBox content) {
        content.minHeightProperty().unbind();
        content.getChildren().removeIf(node -> node instanceof AppFooter ||
                (node instanceof Region && VBox.getVgrow(node) == Priority.ALWAYS && node.getClass() == Region.class));
        scroll.setContent(null);
        if (content.getParent() instanceof StackPane oldParent) oldParent.getChildren().remove(content);
        StackPane centered = new StackPane(content);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setPadding(new Insets(0, 0, CONTENT_BOTTOM_GAP, 0));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox page = new VBox(centered, spacer, new AppFooter());
        page.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> scroll.getViewportBounds().getHeight(), scroll.viewportBoundsProperty()));
        scroll.setContent(page);
    }

    private Button linkButton(String text, String iconName) {
        Button button = new Button(text);
        Region graphic = Icons.icon(iconName, 16);
        graphic.getStyleClass().add("icon-current");
        button.setGraphic(graphic);
        button.setAccessibleText(text);
        return button;
    }

    private void navigate(ViewNavigator.Route route) {
        try {
            ViewNavigator.navigate(route);
        } catch (IOException exception) {
            exception.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể mở trang. Vui lòng thử lại.");
            ViewNavigator.applyAppStylesheet(alert.getDialogPane());
            alert.setHeaderText("Lỗi giao diện");
            alert.show();
        }
    }
}
