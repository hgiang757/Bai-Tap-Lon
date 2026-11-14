package com.auction.client.controller;

import com.auction.client.navigation.ViewNavigator;
import com.auction.client.ui.AppFooter;
import com.auction.client.ui.SmoothScroll;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Initializes the usage guide and its navigation controls. */
public class GuideController {
  @FXML private ScrollPane guideScroll;
  @FXML private VBox pageContent;

  /** Initializes the controls after FXML injection. */
  @FXML
  public void initialize() {
    SmoothScroll.apply(guideScroll, 3.0);
    addSection(
        "Đăng ký và đăng nhập",
        "guide-start",
        "Chọn Đăng ký, điền thông tin và chọn vai trò Người đấu giá hoặc Người bán. "
            + "Sau đó đăng nhập bằng tên tài khoản và mật khẩu đã tạo.");
    addSection(
        "Thêm sản phẩm",
        "guide-seller",
        "Với tài khoản Người bán, mở menu tài khoản và chọn Chế độ bán hàng, rồi bấm Thêm sản phẩm."
            + " Điền thông tin, giá khởi điểm, bước giá và lịch đấu giá, sau đó lưu.");
    addSection(
        "Tham gia đấu giá",
        "guide-bidder",
        "Tìm sản phẩm trên Trang chủ và lọc theo danh mục nếu cần. "
            + "Bấm Xem chi tiết để vào phòng và kiểm tra trạng thái, thời gian còn lại.");
    addSection(
        "Đặt giá và Auto-Bidding",
        "guide-auto-bid",
        "Nhập số tiền ít nhất bằng mức đặt tối thiểu đang hiển thị rồi bấm Đặt giá. "
            + "Để bật Auto-Bidding, nhập giá tối đa và bước giá rồi bấm Bật đặt giá tự động, "
            + "bạn có thể tắt khi cần.");
    addSection(
        "Sử dụng ví",
        "guide-wallet",
        "Mở Ví của tôi trong menu tài khoản để nạp, rút và xem giao dịch mô phỏng. Tiền được tạm"
            + " giữ khi bạn dẫn đầu, được giải phóng khi bị vượt giá hoặc phiên bị hủy.");
    addSection(
        "Xem kết quả",
        "guide-results",
        "Mở Kết quả các phiên trên Trang chủ rồi chọn Xem chi tiết để xem người thắng và lịch sử"
            + " giá. Khi phiên kết thúc có người thắng, hệ thống tự thanh toán từ ví người thắng"
            + " sang ví người bán.");
    AppFooter.install(guideScroll, pageContent);
  }

  private void addSection(String title, String id, String paragraph) {
    VBox section = new VBox(10);
    section.setId(id);
    section.getStyleClass().add("card-panel");
    Label heading = new Label(title);
    heading.getStyleClass().add("section-header");
    heading.setWrapText(true);
    Label text = new Label(paragraph);
    text.getStyleClass().add("guide-copy");
    text.setWrapText(true);
    section.getChildren().addAll(heading, text);
    pageContent.getChildren().add(section);
  }

  @FXML
  private void handleBack() {
    try {
      ViewNavigator.back();
    } catch (IOException exception) {
      Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể quay lại màn hình trước đó.");
      ViewNavigator.prepareDialog(alert);
      alert.setHeaderText("Lỗi giao diện");
      alert.show();
    }
  }
}
