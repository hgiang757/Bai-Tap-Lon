package com.auction.client.controller;

import com.auction.client.navigation.ViewNavigator;
import com.auction.client.ui.AppFooter;
import com.auction.client.ui.SmoothScroll;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

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
        SmoothScroll.apply(contactScroll, 3.0);
        buildPage();
    }

    private void buildPage() {
        for (TeamMember member : loadMembers()) {
            pageContent.getChildren().add(memberCard(member));
        }
        AppFooter.install(contactScroll, pageContent);
    }

    private List<TeamMember> loadMembers() {
        Properties properties = new Properties();
        try (InputStream input =
                ContactController.class.getResourceAsStream(
                        "/com/auction/client/contacts.properties")) {
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
                throw new IllegalStateException(
                        "Thiếu dữ liệu thành viên số " + i + " trong contacts.properties.");
            }
            members.add(new TeamMember(name, email));
        }
        return members;
    }

    private VBox memberCard(TeamMember member) {
        Label name = new Label(member.name());
        name.getStyleClass().add("contact-member-name");
        name.setWrapText(true);
        Label email = new Label(member.email());
        email.getStyleClass().add("contact-member-email");
        email.setWrapText(true);
        Label feedback = new Label();
        feedback.getStyleClass().add("field-success");
        feedback.setWrapText(true);
        feedback.managedProperty().bind(feedback.textProperty().isNotEmpty());
        Button copy = new Button("Sao chép email");
        copy.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(member.email());
            Clipboard.getSystemClipboard().setContent(content);
            feedback.setText("Đã sao chép email");
        });
        VBox card = new VBox(8, name, email, copy, feedback);
        card.getStyleClass().add("card-panel");
        return card;
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

    private record TeamMember(String name, String email) {}
}
