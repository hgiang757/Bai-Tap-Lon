package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.ui.AppFooter;
import com.auction.client.ui.Icons;
import com.auction.client.ui.UserMenu;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.util.AppInfo;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ContactController {
    @FXML private ScrollPane contactScroll;
    @FXML private VBox pageContent;

    @FXML
    public void initialize() {
        pageContent.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> contactScroll.getViewportBounds().getHeight(), contactScroll.viewportBoundsProperty()));
        SmoothScroll.apply(contactScroll, 3.0);
        buildPage();
    }

    private void buildPage() {
        TilePane members = new TilePane();
        members.getStyleClass().add("contact-grid");
        members.setHgap(20);
        members.setVgap(20);
        members.setPrefColumns(2);
        members.prefTileWidthProperty().bind(pageContent.widthProperty().subtract(84).divide(2));
        for (TeamMember member : loadMembers()) {
            members.getChildren().add(memberCard(member));
        }

        VBox note = new VBox(8);
        note.getStyleClass().addAll("card-panel", "contact-note");
        Label noteText = new Label("Nếu có sai sót gì trong quá trình sử dụng, xin vui lòng liên hệ với nhóm phát triển qua các email ở trên.");
        noteText.setWrapText(true);
        note.getChildren().add(noteText);
        note.setMaxWidth(Double.MAX_VALUE);
        pageContent.getChildren().addAll(members, note);
        AppFooter.install(contactScroll, pageContent);
    }

    private List<TeamMember> loadMembers() {
        Properties properties = new Properties();
        try (InputStream input = ContactController.class.getResourceAsStream("/com/auction/client/contacts.properties")) {
            if (input == null) {
                throw new IllegalStateException("Không tìm thấy tài nguyên contacts.properties.");
            }
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc danh sách liên hệ.", exception);
        }
        List<TeamMember> members = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String name = properties.getProperty("member." + i + ".name");
            String email = properties.getProperty("member." + i + ".email");
            if (name == null || email == null) {
                throw new IllegalStateException("Thiếu dữ liệu thành viên số " + i + " trong contacts.properties.");
            }
            members.add(new TeamMember(name, email));
        }
        return members;
    }

    private VBox memberCard(TeamMember member) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card-panel", "contact-member-card");
        card.setPrefWidth(420);
        HBox identity = new HBox(12);
        identity.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label avatar = new Label(member.name.substring(0, 1));
        avatar.getStyleClass().add("contact-avatar");
        VBox details = new VBox(4);
        Label name = new Label(member.name);
        name.getStyleClass().add("contact-member-name");
        Label email = new Label(member.email);
        email.getStyleClass().add("contact-member-email");
        email.setWrapText(true);
        details.getChildren().addAll(name, email);
        identity.getChildren().addAll(avatar, details);

        HBox actions = new HBox(8);
        Button copy = actionButton("Sao chép email", "copy");
        copy.setOnAction(event -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(member.email);
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            showToast("Đã sao chép email");
        });
        Button emailButton = actionButton("Gửi email", "mail");
        emailButton.setOnAction(event -> sendEmail(member.email));
        copy.setMaxWidth(Double.MAX_VALUE);
        emailButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox.setHgrow(emailButton, Priority.ALWAYS);
        actions.getChildren().addAll(copy, emailButton);
        card.getChildren().addAll(identity, actions);
        return card;
    }

    private Button actionButton(String text, String iconName) {
        Button button = new Button(text);
        Region icon = Icons.icon(iconName, 15);
        icon.getStyleClass().add("icon-current");
        button.setGraphic(icon);
        button.setAccessibleText(text);
        return button;
    }

    private void sendEmail(String email) {
        try {
            if (Main.getHostServicesInstance() == null) {
                throw new IllegalStateException("HostServices chưa sẵn sàng.");
            }
            Main.getHostServicesInstance().showDocument("mailto:" + email);
        } catch (RuntimeException exception) {
            showToast("Không mở được ứng dụng thư, hãy sao chép email");
        }
    }

    private void showToast(String message) {
        Window owner = contactScroll.getScene().getWindow();
        Label toast = new Label(message);
        toast.getStyleClass().add("app-toast");
        Popup popup = new Popup();
        popup.getContent().add(toast);
        popup.setAutoHide(false);
        popup.show(owner, owner.getX() + (owner.getWidth() - 360) / 2,
                owner.getY() + owner.getHeight() - 110);
        ViewNavigator.applyAppStylesheet(popup.getScene());
        toast.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(120), toast);
        fadeIn.setToValue(1);
        fadeIn.play();
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(120), toast);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(done -> popup.hide());
            fadeOut.play();
        });
        pause.play();
    }

    @FXML
    private void handleBack() {
        try {
            ViewNavigator.back();
        } catch (IOException exception) {
            showNavigationError(exception);
        }
    }

    private void showNavigationError(IOException exception) {
        exception.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể quay lại màn hình trước đó.");
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
        alert.setHeaderText("Lỗi điều hướng");
        alert.show();
    }

    @FXML
    private void handleLogin() {
        try {
            ViewNavigator.navigate(ViewNavigator.Route.AUTH);
        } catch (IOException exception) {
            showNavigationError(exception);
        }
    }

    private static final class TeamMember {
        private final String name;
        private final String email;

        private TeamMember(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
