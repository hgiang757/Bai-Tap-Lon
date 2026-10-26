package com.auction.client.ui;

import com.auction.client.navigation.ViewNavigator;
import com.auction.client.util.AppInfo;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

public final class AppFooter extends VBox {
    public AppFooter() {
        getStyleClass().add("app-footer");
        setAlignment(Pos.CENTER);
        VBox content = new VBox(20);
        content.getStyleClass().add("app-footer-content");
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(900);
        Label intro = new Label(AppInfo.NAME + " — Sàn đấu giá trực tuyến theo thời gian thực. Người bán đăng sản phẩm và đặt giá khởi điểm, người đấu giá theo dõi đồng hồ đếm ngược và đặt giá; máy chủ là bên xác nhận mọi lượt đặt giá nên kết quả luôn nhất quán và minh bạch.");
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
