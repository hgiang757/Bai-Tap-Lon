package com.hieu.controllers;

import com.hieu.models.UserRole;
import com.hieu.services.AuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.ViewNavigator;
import com.hieu.util.SmoothScroll;
import com.hieu.ui.Icons;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Điều khiển màn hình xác thực (Đăng nhập / Đăng ký).
 * - Chế độ đăng nhập: không cần chọn vai trò (vai trò do máy chủ phản hồi).
 * - Chế độ đăng ký: cho phép chọn vai trò Người đấu giá hoặc Người bán (không có Admin).
 * - Ô mật khẩu luôn để trống ban đầu, không gán giá trị mặc định.
 */
public class AuthController {

    private static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());
    private static final String REMEMBERED_USERNAME_KEY = "rememberedUsername";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private Label lblFormTitle;
    @FXML private Label lblFormSubtitle;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private VBox registerFieldsBox;
    @FXML private VBox registerRoleBox;

    @FXML private ToggleButton rbBidder;
    @FXML private ToggleButton rbSeller;

    @FXML private Button btnSubmit;
    @FXML private Button btnSwitchMode;

    @FXML private HBox alertBox;
    @FXML private Label lblAlertMessage;

    @FXML private CheckBox chkRemember;
    @FXML private HBox rememberBox;
    @FXML private ScrollPane authScroll;
    @FXML private Label lblBrandIcon;
    @FXML private Label lblBenefitRealtime;
    @FXML private Label lblBenefitTransparent;
    @FXML private Label lblBenefitSafe;

    private boolean isRegisterMode = false;
    private final AuctionClientService auctionService = ServiceRegistry.getService();

    @FXML
    public void initialize() {
        SmoothScroll.apply(authScroll, 3.0);
        setIcon(lblBrandIcon, "trophy", 40, "icon-on-panel");
        setIcon(lblBenefitRealtime, "bolt", 22, "icon-primary");
        setIcon(lblBenefitTransparent, "book", 22, "icon-primary");
        setIcon(lblBenefitSafe, "lock", 22, "icon-primary");
        loadRememberedUsername();
        txtPassword.clear();
        rbBidder.setSelected(true);
        updateModeUI();
        hideAlert();
        txtUsername.requestFocus();
        txtUsername.setOnAction(event -> txtPassword.requestFocus());
        txtPassword.setOnAction(event -> handleSubmit());
    }

    private void setIcon(Label label, String name, double size, String colorClass) {
        Region icon = Icons.icon(name, size);
        icon.getStyleClass().add(colorClass);
        label.setGraphic(icon);
        label.setGraphicTextGap(8);
    }

    private void updateModeUI() {
        hideAlert();
        txtPassword.clear();
        if (isRegisterMode) {
            lblFormTitle.setText("Đăng ký tài khoản");
            lblFormSubtitle.setText("Tham gia VaultAuction ngay hôm nay!");
            btnSubmit.setText("Tạo tài khoản");
            btnSwitchMode.setText("Đã có tài khoản? Đăng nhập tại đây");
            registerFieldsBox.setVisible(true);
            registerFieldsBox.setManaged(true);
            registerRoleBox.setVisible(true);
            registerRoleBox.setManaged(true);
            rememberBox.setVisible(false);
            rememberBox.setManaged(false);
        } else {
            lblFormTitle.setText("Đăng nhập");
            lblFormSubtitle.setText("Chào mừng trở lại!");
            btnSubmit.setText("Đăng nhập");
            btnSwitchMode.setText("Chưa có tài khoản? Đăng ký ngay");
            registerFieldsBox.setVisible(false);
            registerFieldsBox.setManaged(false);
            registerRoleBox.setVisible(false);
            registerRoleBox.setManaged(false);
            rememberBox.setVisible(true);
            rememberBox.setManaged(true);
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

            // Validate email format
            if (email != null && !email.trim().isEmpty()) {
                if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                    showAlert("Địa chỉ email không hợp lệ. Vui lòng kiểm tra lại.", true);
                    return;
                }
            } else {
                showAlert("Vui lòng nhập địa chỉ email.", true);
                return;
            }

            UserRole selectedRole = rbSeller.isSelected() ? UserRole.SELLER : UserRole.BIDDER;
            boolean ok = auctionService.register(username.trim(), password.trim(), fullName.trim(),
                    email.trim(), selectedRole);
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
                saveRememberedUsername(username.trim());
                showAlert("Đăng nhập thành công. Đang chuyển hướng...", false);
                navigateToDashboard();
            } else {
                showAlert("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin tài khoản.", true);
            }

        }
    }

    private void loadRememberedUsername() {
        try {
            Preferences preferences = Preferences.userNodeForPackage(AuthController.class);
            String rememberedUsername = preferences.get(REMEMBERED_USERNAME_KEY, "");
            txtUsername.setText(rememberedUsername);
            chkRemember.setSelected(!rememberedUsername.isEmpty());
        } catch (SecurityException ex) {
            LOGGER.log(Level.WARNING, "Không thể đọc tên đăng nhập đã ghi nhớ.", ex);
        }
    }

    private void saveRememberedUsername(String username) {
        try {
            Preferences preferences = Preferences.userNodeForPackage(AuthController.class);
            if (chkRemember.isSelected()) {
                preferences.put(REMEMBERED_USERNAME_KEY, username);
            } else {
                preferences.remove(REMEMBERED_USERNAME_KEY);
            }
            preferences.flush();
        } catch (SecurityException | BackingStoreException ex) {
            LOGGER.log(Level.WARNING, "Không thể lưu lựa chọn ghi nhớ tên đăng nhập.", ex);
        }
    }

    private void navigateToDashboard() {
        try {
            ViewNavigator.loadView("/com/hieu/views/dashboard-view.fxml", "Sàn đấu giá trực tuyến");
        } catch (IOException e) {
            showAlert("Không thể mở danh sách đấu giá. Vui lòng thử lại.", true);
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
