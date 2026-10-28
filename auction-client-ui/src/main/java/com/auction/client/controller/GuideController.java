package com.auction.client.controller;

import com.auction.client.ui.AppFooter;
import com.auction.client.ui.UserMenu;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.navigation.ViewNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class GuideController {
    @FXML private ScrollPane guideScroll;
    @FXML private VBox pageContent;

    private VBox sections;

    @FXML
    public void initialize() {
        pageContent.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> guideScroll.getViewportBounds().getHeight(), guideScroll.viewportBoundsProperty()));
        SmoothScroll.apply(guideScroll, 3.0);
        buildPage();
    }

    private void buildPage() {
        HBox columns = new HBox(24);
        VBox tableOfContents = new VBox(8);
        tableOfContents.getStyleClass().add("guide-toc");
        Label tocTitle = new Label("Mục lục");
        tocTitle.getStyleClass().add("guide-toc-title");
        tableOfContents.getChildren().add(tocTitle);

        sections = new VBox(16);
        sections.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sections, Priority.ALWAYS);
        addSection("Bắt đầu", "guide-start",
                "Đăng ký tài khoản và chọn Người mua hoặc Người bán. Khi đăng nhập, vai trò do máy chủ trả về.",
        "Ứng dụng dùng một bảng màu sáng thống nhất trên mọi màn hình.",
                "Để đăng xuất, mở menu tài khoản ở góc trên và chọn Đăng xuất.");
        addSection("Dành cho người đấu giá", "guide-bidder",
                "Trang chủ hiển thị các phiên đấu giá; có thể tìm theo tên, danh mục hoặc người bán và lọc nhanh theo danh mục. Ứng dụng không có bộ lọc trạng thái riêng.",
                "Sắp diễn ra là phiên chưa tới giờ bắt đầu; Đang diễn ra là phiên có thể nhận giá; Đã kết thúc là phiên hết thời gian.",
                "Mở phòng đấu giá, nhập giá rồi bấm Đặt giá. Giá phải là số nguyên dương và lớn hơn hoặc bằng giá hiện tại cộng bước giá tối thiểu của phiên.",
                "Phòng đấu giá hiển thị đồng hồ đếm ngược, lịch sử đặt giá và biểu đồ biến động giá. Máy chủ xác nhận lượt đặt giá; khi hết giờ ứng dụng khóa lượt đặt mới. App hiện chưa có luồng xác nhận kết quả người thắng hoặc thanh toán riêng.");
        addSection("Dành cho người bán", "guide-seller",
                "Mở menu tài khoản và chọn Chế độ bán hàng để vào Quản lý sản phẩm. Đây chỉ là điều hướng; vai trò và quyền tài khoản không thay đổi.",
                "Tạo sản phẩm bằng cách điền tên, danh mục, mô tả, giá khởi điểm, thời lượng và bước giá. Ảnh là tùy chọn. Dịch vụ hiện tạo phiên đấu giá cùng sản phẩm.",
                "Có thể sửa thông tin sản phẩm hoặc xóa sản phẩm. Dịch vụ từ chối xóa sản phẩm đang thuộc phiên đấu giá diễn ra. Lưu sửa cập nhật các trường sản phẩm, không cập nhật thời lượng hoặc bước giá của phiên hiện có.");
        addSection("Câu hỏi thường gặp", "guide-faq",
                "Vì sao đặt giá bị từ chối? Phiên có thể chưa diễn ra/đã kết thúc, giá không phải số nguyên dương hoặc thấp hơn giá tối thiểu (giá hiện tại cộng bước giá).",
                "Vì sao không thấy Chế độ bán hàng? Mục này chỉ xuất hiện với tài khoản có vai trò Người bán do máy chủ trả về.",
                "Nếu mất kết nối thì làm gì? Thử tải lại màn hình hoặc đăng nhập lại. Ứng dụng hiện chưa cung cấp cơ chế tự khôi phục kết nối hay gửi lại lượt đặt giá.");

        int index = 0;
        for (javafx.scene.Node node : sections.getChildren()) {
            VBox section = (VBox) node;
            Button jump = new Button((++index) + ". " + ((Label) section.getChildren().get(0)).getText());
            jump.setMaxWidth(Double.MAX_VALUE);
            jump.setWrapText(true);
            jump.setAccessibleText("Đi tới mục " + ((Label) section.getChildren().get(0)).getText());
            jump.setOnAction(event -> scrollTo(section));
            tableOfContents.getChildren().add(jump);
        }
        columns.getChildren().addAll(tableOfContents, sections);
        pageContent.getChildren().add(columns);

        Button contact = new Button("Cần hỗ trợ? Liên hệ");
        contact.getStyleClass().add("btn-primary");
        contact.setOnAction(event -> navigate(ViewNavigator.Route.CONTACT));
        HBox supportRow = new HBox(contact);
        supportRow.setAlignment(javafx.geometry.Pos.CENTER);
        pageContent.getChildren().add(supportRow);
        AppFooter.install(guideScroll, pageContent);
    }

    private void addSection(String title, String id, String... paragraphs) {
        VBox card = new VBox(10);
        card.setId(id);
        card.getStyleClass().addAll("card-panel", "guide-card");
        Label heading = new Label(title);
        heading.getStyleClass().add("section-header");
        card.getChildren().add(heading);
        for (String paragraph : paragraphs) {
            Label text = new Label(paragraph);
            text.getStyleClass().add("guide-copy");
            text.setWrapText(true);
            text.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(text);
        }
        sections.getChildren().add(card);
    }

    private void scrollTo(javafx.scene.Node target) {
        guideScroll.applyCss();
        guideScroll.layout();
        pageContent.applyCss();
        pageContent.layout();
        double max = pageContent.getBoundsInLocal().getHeight() - guideScroll.getViewportBounds().getHeight();
        if (max <= 0) {
            return;
        }
        Bounds targetBounds = pageContent.sceneToLocal(target.localToScene(target.getBoundsInLocal()));
        double targetValue = Math.max(0, Math.min(1, targetBounds.getMinY() / max));
        double start = guideScroll.getVvalue();
        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(guideScroll.vvalueProperty(), start)),
                new KeyFrame(Duration.millis(220), new KeyValue(guideScroll.vvalueProperty(), targetValue)));
        animation.play();
    }

    @FXML
    private void handleBack() {
        try {
            ViewNavigator.back();
        } catch (IOException exception) {
            showNavigationError(exception);
        }
    }

    @FXML
    private void handleLogin() {
        navigate(ViewNavigator.Route.AUTH);
    }

    private void navigate(ViewNavigator.Route route) {
        try {
            ViewNavigator.navigate(route);
        } catch (IOException exception) {
            showNavigationError(exception);
        }
    }

    private void showNavigationError(IOException exception) {
        exception.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể chuyển màn hình. Vui lòng thử lại.");
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
        alert.setHeaderText("Lỗi giao diện");
        alert.show();
    }
}
