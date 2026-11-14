package com.auction.client.controller;

import com.auction.client.model.UserRole;
import com.auction.client.navigation.DisposableView;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.ui.PasswordFieldWithToggle;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.util.AppInfo;
import com.auction.client.util.Validators;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Điều khiển màn hình xác thực (Đăng nhập / Đăng ký). - Chế độ đăng nhập: không cần chọn vai trò
 * (vai trò do máy chủ phản hồi). - Chế độ đăng ký: cho phép chọn vai trò Người đấu giá hoặc Người
 * bán (không có Admin). - Ô mật khẩu luôn để trống ban đầu, không gán giá trị mặc định.
 */
public class AuthController implements DisposableView {
  private final ViewTasks tasks = new ViewTasks();
  private boolean busy;

  @Override
  public void dispose() {
    tasks.close();
  }

  private static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());
  private static final String REMEMBERED_USERNAME_KEY = "rememberedUsername";
  @FXML private VBox authContent;
  @FXML private Label lblFormTitle;
  @FXML private Label lblFormSubtitle;
  @FXML private Label lblBrandName;
  @FXML private TextField txtUsername;
  @FXML private PasswordField txtPassword;
  @FXML private PasswordField txtConfirm;
  @FXML private TextField visiblePassword;
  @FXML private TextField visibleConfirm;
  @FXML private Button togglePassword;
  @FXML private Button toggleConfirm;
  @FXML private Label errUsername;
  @FXML private Label errEmail;
  @FXML private Label errPassword;
  @FXML private Label errConfirm;
  @FXML private Label passwordHint;
  @FXML private VBox confirmBox;
  private boolean touchedUsername;
  private boolean touchedEmail;
  private boolean touchedPassword;
  private boolean touchedConfirm;
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

  private boolean isRegisterMode = false;
  private final AuctionClientService auctionService = ServiceRegistry.getService();

  /** Initializes the controls after FXML injection. */
  @FXML
  public void initialize() {
    SmoothScroll.apply(authScroll, 3.0);
    lblBrandName.setText(AppInfo.NAME);
    loadRememberedUsername();
    txtPassword.clear();
    rbBidder.setSelected(true);
    updateModeUi();
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
    for (TextField field : new TextField[] {txtUsername, txtEmail, txtPassword, txtConfirm}) {
      field.textProperty().addListener((o, a, b) -> validateFields());
    }
  }

  private void updateModeUi() {
    hideAlert();
    txtUsername.clear();
    txtEmail.clear();
    txtPassword.clear();
    txtConfirm.clear();
    rbBidder.setSelected(true);
    if (!isRegisterMode) {
      loadRememberedUsername();
    }
    tasks.dispatch(txtUsername::requestFocus);
    touchedUsername = touchedEmail = touchedPassword = touchedConfirm = false;
    confirmBox.setVisible(isRegisterMode);
    confirmBox.setManaged(isRegisterMode);
    validateFields();
    if (isRegisterMode) {
      lblFormTitle.setText("Đăng ký tài khoản");
      lblFormSubtitle.setText("Tạo tài khoản trên " + AppInfo.NAME + ".");
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
      lblFormSubtitle.setText("Đăng nhập vào máy chủ đấu giá.");
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
    if (busy) {
      return;
    }
    isRegisterMode = !isRegisterMode;
    updateModeUi();
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
    if (busy || tasks.isClosed()) {
      return;
    }
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
        if (!Validators.username(username)) {
          txtUsername.requestFocus();
        } else if (!Validators.email(txtEmail.getText())) {
          txtEmail.requestFocus();
        } else if (!Validators.password(password)) {
          (visiblePassword.isVisible() ? visiblePassword : txtPassword).requestFocus();
        } else {
          (visibleConfirm.isVisible() ? visibleConfirm : txtConfirm).requestFocus();
        }
        return;
      }
      String email = txtEmail.getText();
      UserRole selectedRole = rbSeller.isSelected() ? UserRole.SELLER : UserRole.BIDDER;
      authenticate(
          () ->
              auctionService.register(
                  username.trim(), password, username.trim(), email.trim(), selectedRole),
          username,
          true);
    } else {
      authenticate(() -> auctionService.login(username.trim(), password, null), username, false);
    }
  }

  private void authenticate(
      java.util.concurrent.Callable<Boolean> operation, String username, boolean register) {
    busy = true;
    btnSubmit.setDisable(true);
    btnSwitchMode.setDisable(true);
    authContent.setDisable(true);
    btnSubmit.setText("Đang xử lý…");
    tasks.submit(
        "auth",
        operation,
        ok -> {
          finishAuthentication(register);
          if (ok) {
            if (!register) {
              saveRememberedUsername(username.trim());
            }
            ViewNavigator.clearHistory();
            navigateToHome();
          } else {
            showAlert(
                register
                    ? "Tên đăng nhập hoặc email đã được sử dụng."
                    : "Sai tên đăng nhập hoặc mật khẩu, hoặc tài khoản đã bị" + " khóa.",
                true);
          }
        },
        error -> {
          finishAuthentication(register);
          showAlert("Không thể xác thực: " + error.getMessage(), true);
        });
  }

  private void finishAuthentication(boolean register) {
    busy = false;
    authContent.setDisable(false);
    btnSubmit.setDisable(false);
    btnSwitchMode.setDisable(false);
    btnSubmit.setText(register ? "Tạo tài khoản" : "Đăng nhập");
  }

  private void watchField(TextField field, Runnable touch) {
    field
        .focusedProperty()
        .addListener(
            (o, was, focused) -> {
              if (was && !focused && isRegisterMode) {
                touch.run();
                validateFields();
              }
            });
  }

  private void fieldError(TextField field, Label label, boolean show, String message) {
    field.getStyleClass().remove("invalid-field");
    if (show) {
      field.getStyleClass().add("invalid-field");
    }
    label.setText(message);
    label.setAccessibleText(message);
    label.setVisible(show);
    label.setManaged(show);
  }

  private boolean validateFields() {
    boolean u = Validators.username(txtUsername.getText());
    boolean e = Validators.email(txtEmail.getText());
    boolean p = Validators.password(txtPassword.getText());
    final boolean c =
        !txtConfirm.getText().isEmpty()
            && Validators.matches(txtPassword.getText(), txtConfirm.getText());
    fieldError(
        txtUsername,
        errUsername,
        isRegisterMode && touchedUsername && !u,
        "Tên đăng nhập dài 3–30 ký tự, gồm chữ, số, dấu cách, _ . - và không bắt đầu/kết"
            + " thúc bằng dấu cách.");
    String emailMessage =
        txtEmail.getText().trim().isEmpty()
            ? "Vui lòng nhập email."
            : "Email chưa đúng định dạng (ví dụ ten@gmail.com).";
    fieldError(txtEmail, errEmail, isRegisterMode && touchedEmail && !e, emailMessage);
    boolean showPasswordError = isRegisterMode && touchedPassword && !p;
    fieldError(
        txtPassword, errPassword, showPasswordError, "Mật khẩu dài 8–128 ký tự, gồm chữ và số.");
    passwordHint.setVisible(isRegisterMode && !showPasswordError);
    passwordHint.setManaged(isRegisterMode && !showPasswordError);
    boolean showConfirm = isRegisterMode && (touchedConfirm || !txtConfirm.getText().isEmpty());
    fieldError(txtConfirm, errConfirm, showConfirm && !c, "Mật khẩu nhập lại chưa khớp");
    if (showConfirm && c) {
      errConfirm.setText("Mật khẩu khớp");
      errConfirm.setAccessibleText("Mật khẩu khớp");
      errConfirm.setVisible(true);
      errConfirm.setManaged(true);
    }
    errConfirm.getStyleClass().remove("field-success");
    if (showConfirm && c) {
      errConfirm.getStyleClass().add("field-success");
    }
    if (isRegisterMode
        && touchedUsername
        && touchedEmail
        && touchedPassword
        && touchedConfirm
        && !(u && e && p && c)) {
      // Focus is assigned by the submit handler, not while typing.
    }
    return u && e && p && c;
  }

  private void loadRememberedUsername() {
    try {
      Preferences preferences = Preferences.userNodeForPackage(AuthController.class);
      String rememberedUsername = preferences.get(REMEMBERED_USERNAME_KEY, "");
      if (java.util.Set.of("admin_hieu", "bidder_hieu", "seller_hieu")
          .contains(rememberedUsername.toLowerCase(java.util.Locale.ROOT))) {
        preferences.remove(REMEMBERED_USERNAME_KEY);
        rememberedUsername = "";
      }
      txtUsername.setText(rememberedUsername);
      chkRemember.setSelected(!rememberedUsername.isEmpty());
    } catch (SecurityException ex) {
      LOGGER.log(Level.WARNING, "Không thể đọc tên đăng nhập đã ghi nhớ.", ex);
    }
  }

  private void saveRememberedUsername(String username) {
    try {
      Preferences preferences = Preferences.userNodeForPackage(AuthController.class);
      if (chkRemember.isSelected()
          && auctionService.getCurrentUser() != null
          && auctionService.getCurrentUser().getRole() != UserRole.ADMIN
          && !java.util.Set.of("admin_hieu", "bidder_hieu", "seller_hieu")
              .contains(username.toLowerCase(java.util.Locale.ROOT))) {
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
      ViewNavigator.navigate(
          auctionService.getCurrentUser().getRole() == UserRole.ADMIN
              ? ViewNavigator.Route.ADMIN
              : ViewNavigator.Route.DASHBOARD);
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
