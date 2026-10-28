package com.auction.client.controller;

import com.auction.client.model.UserRole;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.util.AppInfo;
import com.auction.client.util.Validators;
import com.auction.client.ui.Icons;
import com.auction.client.ui.PasswordFieldWithToggle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
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
    private static Image cachedBackgroundImage;
    private static boolean backgroundLoadStarted;
    private static boolean backgroundFailureLogged;
    private static WeakReference<AuthController> activeAuthController = new WeakReference<>(null);

    @FXML private StackPane authRoot;
    @FXML private ImageView backgroundImage;
    @FXML private Region imageOverlay;
    @FXML private VBox authContent;
    @FXML private Label lblFormTitle;
    @FXML private Label lblFormSubtitle;
    @FXML private Label lblBrandName;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirm;
    @FXML private TextField visiblePassword, visibleConfirm;
    @FXML private Button togglePassword, toggleConfirm;
    @FXML private Label errUsername, errEmail, errPassword, errConfirm, passwordHint;
    @FXML private VBox confirmBox;
    private boolean touchedUsername, touchedEmail, touchedPassword, touchedConfirm;
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
    @FXML private HBox authFeatureRow;

    private boolean isRegisterMode = false;
    private final AuctionClientService auctionService = ServiceRegistry.getService();

    @FXML
    public void initialize() {
        SmoothScroll.apply(authScroll, 3.0);
        lblBrandName.setText(AppInfo.NAME);
        setIcon(lblBrandIcon, "trophy", 28, "icon-on-panel");
        setIcon(lblBenefitRealtime, "bolt", 18, "icon-on-primary");
        setIcon(lblBenefitTransparent, "book", 18, "icon-on-primary");
        setIcon(lblBenefitSafe, "lock", 18, "icon-on-primary");
        configureBackground();
        authRoot.widthProperty().addListener((observable, oldValue, newValue) -> updateBackgroundCover());
        authRoot.heightProperty().addListener((observable, oldValue, newValue) -> updateBackgroundCover());
        authRoot.widthProperty().addListener((observable, oldValue, newValue) -> updateFeatureVisibility(newValue.doubleValue()));
        updateFeatureVisibility(authRoot.getWidth());
        loadRememberedUsername();
        txtPassword.clear();
        rbBidder.setSelected(true);
        updateModeUI();
        hideAlert();
        txtUsername.setOnAction(event -> txtPassword.requestFocus());
        txtPassword.setOnAction(event -> handleSubmit());
        PasswordFieldWithToggle.install(txtPassword, visiblePassword, togglePassword);
        PasswordFieldWithToggle.install(txtConfirm, visibleConfirm, toggleConfirm);
        watchField(txtUsername, () -> touchedUsername = true);
        watchField(txtEmail, () -> touchedEmail = true);
        watchField(txtPassword, () -> touchedPassword = true);
        watchField(visiblePassword, () -> touchedPassword = true);
        watchField(txtConfirm, () -> touchedConfirm = true);
        watchField(visibleConfirm, () -> touchedConfirm = true);
        for (TextField field : new TextField[]{txtUsername, txtEmail, txtPassword, txtConfirm}) {
            field.textProperty().addListener((o, a, b) -> validateFields());
        }
    }

    private void configureBackground() {
        activeAuthController = new WeakReference<>(this);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(authRoot.widthProperty());
        clip.heightProperty().bind(authRoot.heightProperty());
        authRoot.setClip(clip);

        if (!backgroundLoadStarted) {
            backgroundLoadStarted = true;
            URL resource = AuthController.class.getResource("/com/auction/client/images/login-bg.jpg");
            if (resource == null) {
                showBackgroundFallback("Không tìm thấy ảnh nền /com/auction/client/images/login-bg.jpg.", null);
            } else {
                cachedBackgroundImage = new Image(resource.toExternalForm(), 1600, 0, true, true, true);
                cachedBackgroundImage.errorProperty().addListener((observable, wasError, isError) -> {
                    if (isError) {
                        Platform.runLater(() -> {
                            AuthController controller = activeAuthController.get();
                            if (controller != null) {
                                showBackgroundFallback("Không thể nạp ảnh nền đăng nhập.",
                                        cachedBackgroundImage.getException());
                            }
                        });
                    }
                });
                cachedBackgroundImage.widthProperty().addListener((observable, oldValue, newValue) ->
                        refreshActiveBackgroundCover());
                cachedBackgroundImage.heightProperty().addListener((observable, oldValue, newValue) ->
                        refreshActiveBackgroundCover());
                if (cachedBackgroundImage.isError()) {
                    showBackgroundFallback("Không thể nạp ảnh nền đăng nhập.",
                            cachedBackgroundImage.getException());
                }
            }
        }

        if (cachedBackgroundImage != null && !cachedBackgroundImage.isError()) {
            backgroundImage.setImage(cachedBackgroundImage);
            updateBackgroundCover();
        } else {
            showBackgroundFallback("Ảnh nền đăng nhập không khả dụng.", null);
        }
    }

    private static void refreshActiveBackgroundCover() {
        AuthController controller = activeAuthController.get();
        if (controller != null) {
            controller.updateBackgroundCover();
        }
    }

    private void updateBackgroundCover() {
        if (backgroundImage.getImage() == null || authRoot.getWidth() <= 0 || authRoot.getHeight() <= 0) {
            return;
        }
        Image image = backgroundImage.getImage();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }
        double width = Math.max(authRoot.getWidth(), authRoot.getHeight() * imageWidth / imageHeight);
        double height = width * imageHeight / imageWidth;
        backgroundImage.setFitWidth(width);
        backgroundImage.setFitHeight(0);
        backgroundImage.relocate((authRoot.getWidth() - width) / 2,
                (authRoot.getHeight() - height) / 2);
    }

    private void updateFeatureVisibility(double width) {
        boolean showFeatures = width >= 900;
        authFeatureRow.setVisible(showFeatures);
        authFeatureRow.setManaged(showFeatures);
    }

    private static void showBackgroundFallback(String message, Exception exception) {
        if (!backgroundFailureLogged) {
            backgroundFailureLogged = true;
            if (exception == null) {
                LOGGER.warning(message + " Đang dùng nền phẳng dự phòng.");
            } else {
                LOGGER.log(Level.WARNING, message + " Đang dùng nền phẳng dự phòng.", exception);
            }
        }
        AuthController controller = activeAuthController.get();
        if (controller != null) {
            controller.backgroundImage.setVisible(false);
            controller.imageOverlay.setVisible(false);
        }
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
        txtConfirm.clear();
        touchedUsername = touchedEmail = touchedPassword = touchedConfirm = false;
        confirmBox.setVisible(isRegisterMode); confirmBox.setManaged(isRegisterMode);
        validateFields();
        if (isRegisterMode) {
            lblFormTitle.setText("Đăng ký tài khoản");
            lblFormSubtitle.setText("Tham gia " + AppInfo.NAME + " ngay hôm nay!");
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
    private void handleOpenGuide() {
        try {
            ViewNavigator.navigate(ViewNavigator.Route.GUIDE);
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Không thể mở trang hướng dẫn.", exception);
            showAlert("Không thể mở trang hướng dẫn. Vui lòng thử lại.", true);
        }
    }

    @FXML
    private void handleSubmit() {
        hideAlert();
        String username = Validators.normalizeUsername(txtUsername.getText());
        String password = txtPassword.getText();

        if (username.isEmpty()) {
            showAlert("Vui lòng nhập tên đăng nhập.", true);
            txtUsername.requestFocus();
            return;
        }

        if (isRegisterMode) {
            touchedUsername = touchedEmail = touchedPassword = touchedConfirm = true;
            if (!validateFields()) {
                if (!Validators.username(username)) txtUsername.requestFocus();
                else if (!Validators.email(txtEmail.getText())) txtEmail.requestFocus();
                else if (!Validators.password(password)) (visiblePassword.isVisible() ? visiblePassword : txtPassword).requestFocus();
                else (visibleConfirm.isVisible() ? visibleConfirm : txtConfirm).requestFocus();
                return;
            }
            String email = txtEmail.getText();
            UserRole selectedRole = rbSeller.isSelected() ? UserRole.SELLER : UserRole.BIDDER;
            boolean ok = auctionService.register(username.trim(), password, username.trim(),
                    email.trim(), selectedRole);
            if (ok) {
                showAlert("Đăng ký thành công. Đang chuyển hướng vào sàn đấu giá...", false);
                navigateToHome();
            } else {
                showAlert("Đăng ký không thành công. Tên đăng nhập có thể đã được sử dụng.", true);
            }
        } else {
            // Đăng nhập: không gửi vai trò, vai trò do server trả về
            boolean ok = auctionService.login(username.trim(), password, null);
            if (ok) {
                saveRememberedUsername(username.trim());
                showAlert("Đăng nhập thành công. Đang chuyển hướng...", false);
                navigateToHome();
            } else {
                showAlert("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin tài khoản.", true);
            }

        }
    }

    private void watchField(TextField field, Runnable touch) {
        field.focusedProperty().addListener((o, was, focused) -> {
            if (was && !focused && isRegisterMode) { touch.run(); validateFields(); }
        });
    }

    private void fieldError(TextField field, Label label, boolean show, String message) {
        field.getStyleClass().remove("invalid-field");
        if (show) field.getStyleClass().add("invalid-field");
        label.setText(message); label.setAccessibleText(message);
        label.setVisible(show); label.setManaged(show);
    }

    private boolean validateFields() {
        boolean u = Validators.username(txtUsername.getText());
        boolean e = Validators.email(txtEmail.getText());
        boolean p = Validators.password(txtPassword.getText());
        boolean c = !txtConfirm.getText().isEmpty() && Validators.matches(txtPassword.getText(), txtConfirm.getText());
        fieldError(txtUsername, errUsername, isRegisterMode && touchedUsername && !u, "Tên đăng nhập dài 3–30 ký tự, gồm chữ, số, dấu cách, _ . - và không bắt đầu/kết thúc bằng dấu cách.");
        String emailMessage = txtEmail.getText().trim().isEmpty()
                ? "Vui lòng nhập email." : "Email chưa đúng định dạng (ví dụ ten@gmail.com).";
        fieldError(txtEmail, errEmail, isRegisterMode && touchedEmail && !e, emailMessage);
        boolean showPasswordError = isRegisterMode && touchedPassword && !p;
        fieldError(txtPassword, errPassword, showPasswordError, "Mật khẩu tối thiểu 8 ký tự, gồm chữ và số.");
        passwordHint.setVisible(isRegisterMode && !showPasswordError);
        passwordHint.setManaged(isRegisterMode && !showPasswordError);
        boolean showConfirm = isRegisterMode && (touchedConfirm || !txtConfirm.getText().isEmpty());
        fieldError(txtConfirm, errConfirm, showConfirm && !c, "Mật khẩu nhập lại chưa khớp");
        if (showConfirm && c) { errConfirm.setText("Mật khẩu khớp"); errConfirm.setAccessibleText("Mật khẩu khớp"); errConfirm.setVisible(true); errConfirm.setManaged(true); }
        errConfirm.getStyleClass().remove("field-success");
        if (showConfirm && c) errConfirm.getStyleClass().add("field-success");
        if (isRegisterMode && touchedUsername && touchedEmail && touchedPassword && touchedConfirm && !(u && e && p && c)) {
            // Focus is assigned by the submit handler, not while typing.
        }
        return u && e && p && c;
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

    private void navigateToHome() {
        try {
            ViewNavigator.loadView("/com/auction/client/views/home-view.fxml", "Sàn đấu giá trực tuyến");
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
