package com.hieu.controllers;

import com.hieu.models.UserRole;
import com.hieu.services.AuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.ViewNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Điều khiển màn hình xác thực (Đăng nhập / Đăng ký).
 * - Chế độ Đăng nhập: không cần chọn vai trò (vai trò do máy chủ phản hồi).
 * - Chế độ Đăng ký: cho phép chọn vai trò Người đấu giá hoặc Người bán (không có Admin).
 * - Ô mật khẩu luôn để trống ban đầu, không gán giá trị mặc định.
 */
public class AuthController {

    @FXML private Label lblFormTitle;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private VBox registerFieldsBox;

    @FXML private RadioButton rbBidder;
    @FXML private RadioButton rbSeller;
    @FXML private ToggleGroup roleToggleGroup;

    @FXML private Button btnSubmit;
    @FXML private Button btnSwitchMode;

    @FXML private HBox alertBox;
    @FXML private Label lblAlertMessage;

    private boolean isRegisterMode = false;
    private final AuctionClientService auctionService = ServiceRegistry.getService();

    @FXML
    public void initialize() {
        // Đảm bảo các ô mật khẩu và tài khoản hoàn toàn trống
        txtUsername.clear();
        txtPassword.clear();
        rbBidder.setSelected(true);
        updateModeUI();
        hideAlert();
    }

    private void updateModeUI() {
        hideAlert();
        txtPassword.clear();
        if (isRegisterMode) {
            lblFormTitle.setText("Đăng ký tài khoản");
            btnSubmit.setText("Tạo tài khoản");
            btnSwitchMode.setText("Đã có tài khoản? Đăng nhập tại đây");
            registerFieldsBox.setVisible(true);
            registerFieldsBox.setManaged(true);
        } else {
            lblFormTitle.setText("Đăng nhập");
            btnSubmit.setText("Đăng nhập");
            btnSwitchMode.setText("Chưa có tài khoản? Đăng ký ngay");
            registerFieldsBox.setVisible(false);
            registerFieldsBox.setManaged(false);
        }
    }

    @FXML
    private void handleSwitchMode() {
        isRegisterMode = !isRegisterMode;
        updateModeUI();
    }

    @FXML
    private void handleSubmit() {
        hideAlert();
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showAlert("Vui lòng điền đầy đủ tên đăng nhập và mật khẩu.", true);
            return;
        }

        if (isRegisterMode) {
            String fullName = txtFullName.getText();
            String email = txtEmail.getText();
            if (fullName == null || fullName.trim().isEmpty()) {
                showAlert("Vui lòng nhập họ và tên của bạn.", true);
                return;
            }

            UserRole selectedRole = rbSeller.isSelected() ? UserRole.SELLER : UserRole.BIDDER;
            boolean ok = auctionService.register(username.trim(), password.trim(), fullName.trim(),
                    email != null ? email.trim() : "", selectedRole);
            if (ok) {
                showAlert("Đăng ký thành công. Đang chuyển hướng vào sàn đấu giá...", false);
                navigateToDashboard();
            } else {
                showAlert("Đăng ký không thành công. Tên đăng nhập có thể đã được sử dụng.", true);
            }
        } else {
            // Đăng nhập: không gửi vai trò, vai trò do server trả về
            boolean ok = auctionService.login(username.trim(), password.trim(), null);
            if (ok) {
                showAlert("Đăng nhập thành công. Đang chuyển hướng...", false);
                navigateToDashboard();
            } else {
                showAlert("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin tài khoản.", true);
            }
        }
    }

    private void navigateToDashboard() {
        try {
            ViewNavigator.loadView("/com/hieu/views/dashboard-view.fxml", "Sàn đấu giá trực tuyến");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi mở danh sách đấu giá: " + e.getMessage(), true);
        }
    }

    private void showAlert(String message, boolean isError) {
        lblAlertMessage.setText(message);
        alertBox.setVisible(true);
        alertBox.setManaged(true);
        alertBox.getStyleClass().removeAll("alert-error", "alert-success");
        alertBox.getStyleClass().add(isError ? "alert-error" : "alert-success");
    }

    private void hideAlert() {
        alertBox.setVisible(false);
        alertBox.setManaged(false);
    }
}
