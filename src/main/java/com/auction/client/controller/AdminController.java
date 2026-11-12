package com.auction.client.controller;

import com.auction.client.model.AdminUser;
import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.navigation.DisposableView;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.AuctionAdminService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.service.UserAdminService;
import com.auction.client.util.FormatUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/** Quản lý người dùng và phiên đấu giá. */
public class AdminController implements DisposableView {
    private final ViewTasks tasks = new ViewTasks();
    private boolean working, refreshQueued;
    private final Consumer<String> serverChanges =
            type ->
                    tasks.dispatch(
                            () -> {
                                if (!this.authorized) return;
                                if (working) {
                                    refreshQueued = true;
                                    return;
                                }
                                refreshSelected();
                            });

    private void refreshSelected() {
        if (usersPane.isVisible()) users();
        else auctions();
    }

    @Override
    public void dispose() {
        ServiceRegistry.getService().removeChangeListener(serverChanges);
        tasks.close();
    }

    @FXML private VBox content, usersPane, auctionsPane;
    @FXML private TextField userSearch, auctionSearch;
    @FXML private ComboBox<String> userRole, auctionStatus;
    @FXML private TableView<AdminUser> userTable;
    @FXML private TableView<Auction> auctionTable;
    @FXML
    private TableColumn<AdminUser, String> userName,
            userEmail,
            userRoleColumn,
            userState,
            userCreated,
            userAction;
    @FXML
    private TableColumn<Auction, String> auctionName,
            auctionPrice,
            auctionState,
            auctionAction;
    @FXML private Label title;
    @FXML private Label message;
    @FXML private ProgressIndicator progress;
    private final UserAdminService userService = ServiceRegistry.getUserAdminService();
    private final AuctionAdminService auctionService = ServiceRegistry.getAuctionAdminService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private String actorId;
    private boolean authorized;
    private Runnable userFilter = () -> {}, auctionFilter = () -> {};

    /** Kiểm tra vai trò và tải danh sách phiên. */
    @FXML
    public void initialize() {
        message.visibleProperty().bind(message.textProperty().isNotEmpty());
        message.managedProperty().bind(message.visibleProperty());
        User user = ServiceRegistry.getService().getCurrentUser();
        authorized = user != null && user.getRole() == UserRole.ADMIN;
        if (!authorized) {
            message.setText("Chỉ quản trị viên được truy cập trang này.");
            content.setDisable(true);
            return;
        }
        actorId = user.getId();
        ServiceRegistry.getService().addChangeListener(serverChanges);
        configureTables();
        userSearch.textProperty().addListener((o, a, b) -> userFilter.run());
        userRole.valueProperty().addListener((o, a, b) -> userFilter.run());
        auctionSearch.textProperty().addListener((o, a, b) -> auctionFilter.run());
        auctionStatus.valueProperty().addListener((o, a, b) -> auctionFilter.run());
        auctions();
    }

    private <T> void run(Callable<T> work, Consumer<T> success) {
        if (!authorized) return;
        working = true;
        content.setDisable(true);
        progress.setVisible(true);
        progress.setManaged(true);
        message.setText("");
        tasks.submit(
                "admin",
                work,
                value -> {
                    content.setDisable(false);
                    progress.setVisible(false);
                    progress.setManaged(false);
                    working = false;
                    success.accept(value);
                    if (refreshQueued && !working) {
                        refreshQueued = false;
                        refreshSelected();
                    }
                },
                error -> {
                    content.setDisable(false);
                    progress.setVisible(false);
                    progress.setManaged(false);
                    working = false;
                    refreshQueued = false;
                    message.setText("Không thể thực hiện: " + error.getMessage());
                });
    }

    @FXML
    private void users() {
        if (!authorized) return;
        title.setText("Quản lý người dùng");
        run(
                userService::listUsers,
                data -> {
                    userSearch.clear();
                    userRole.getSelectionModel().selectFirst();
                    userFilter =
                            () ->
                                    userTable.setItems(
                                            FXCollections.observableArrayList(
                                                    data.stream()
                                                            .filter(
                                                                    u ->
                                                                            (u.getUser()
                                                                                                    .getUsername()
                                                                                                    .toLowerCase()
                                                                                                    .contains(
                                                                                                            userSearch
                                                                                                                    .getText()
                                                                                                                    .toLowerCase())
                                                                                            || u.getUser()
                                                                                                    .getEmail()
                                                                                                    .toLowerCase()
                                                                                                    .contains(
                                                                                                            userSearch
                                                                                                                    .getText()
                                                                                                                    .toLowerCase()))
                                                                                    && (userRole.getSelectionModel()
                                                                                                            .getSelectedIndex()
                                                                                                    == 0
                                                                                            || u.getUser()
                                                                                                    .getRole()
                                                                                                    .getDisplayName()
                                                                                                    .equals(
                                                                                                            userRole
                                                                                                                    .getValue())))
                                                            .toList()));
                    userFilter.run();
                    showPane(usersPane);
                });
    }

    private void configureTables() {
        userRole.getItems()
                .setAll(
                        "Tất cả",
                        UserRole.BIDDER.getDisplayName(),
                        UserRole.SELLER.getDisplayName(),
                        UserRole.ADMIN.getDisplayName());
        auctionStatus.getItems().add("Tất cả");
        for (AuctionStatus status : AuctionStatus.values())
            auctionStatus.getItems().add(status.getDescription());
        column(userName, u -> u.getUser().getUsername());
        column(userEmail, u -> u.getUser().getEmail());
        column(userRoleColumn, u -> u.getUser().getRole().getDisplayName());
        column(userState, u -> u.isLocked() ? "Bị khóa" : "Hoạt động");
        column(userCreated, u -> u.getCreatedAt().format(formatter));
        TableColumn<AdminUser, String> action = userAction;
        action.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().getId()));
        action.setCellFactory(
                c ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String id, boolean empty) {
                                super.updateItem(id, empty);
                                setGraphic(null);
                                if (empty || id == null || getTableRow().getItem() == null) return;
                                AdminUser u = getTableRow().getItem();
                                boolean self = id.equals(actorId);
                                Button lock = new Button(u.isLocked() ? "Mở khóa" : "Khóa"),
                                        change = new Button("Đổi vai trò"),
                                        delete = new Button("Xóa");
                                for (Button button : List.of(lock, change, delete))
                                    button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                                lock.setDisable(self);
                                change.setDisable(self);
                                delete.setDisable(self);
                                if (self) {
                                    lock.setTooltip(new Tooltip("Không thể tự khóa."));
                                    delete.setTooltip(new Tooltip("Không thể tự xóa."));
                                    change.setTooltip(new Tooltip("Không thể tự đổi vai trò."));
                                }
                                lock.setOnAction(
                                        e ->
                                                run(
                                                        () -> {
                                                            userService.setLocked(
                                                                    actorId, id, !u.isLocked());
                                                            return null;
                                                        },
                                                        x -> users()));
                                change.setOnAction(
                                        e -> {
                                            ChoiceDialog<UserRole> d =
                                                    new ChoiceDialog<>(
                                                            u.getUser().getRole(),
                                                            UserRole.values());
                                            d.setHeaderText("Chọn vai trò");
                                            ViewNavigator.applyAppStylesheet(d.getDialogPane());
                                            d.showAndWait()
                                                    .ifPresent(
                                                            r ->
                                                                    run(
                                                                            () -> {
                                                                                userService.setRole(
                                                                                        actorId, id,
                                                                                        r);
                                                                                return null;
                                                                            },
                                                                            x -> users()));
                                        });
                                delete.setOnAction(
                                        e -> {
                                            if (confirm(
                                                    "Xóa người dùng "
                                                            + u.getUser().getUsername()
                                                            + "?"))
                                                run(
                                                        () -> {
                                                            userService.deleteUser(actorId, id);
                                                            return null;
                                                        },
                                                        x -> users());
                                        });
                                setGraphic(new HBox(5, lock, change, delete));
                            }
                        });
        column(auctionName, Auction::getItemName);
        column(auctionPrice, a -> FormatUtil.formatCurrency(a.getCurrentPrice()));
        column(auctionState, a -> a.getStatus().getDescription());
        configureAuctionActions();
    }

    private void showPane(VBox selected) {
        for (VBox pane : List.of(usersPane, auctionsPane)) {
            pane.setVisible(pane == selected);
            pane.setManaged(pane == selected);
        }
    }

    @FXML
    private void auctions() {
        if (!authorized) return;
        title.setText("Quản lý phiên đấu giá");
        run(
                auctionService::listAuctions,
                data -> {
                    auctionSearch.clear();
                    auctionStatus.getSelectionModel().selectFirst();
                    auctionFilter =
                            () ->
                                    auctionTable.setItems(
                                            FXCollections.observableArrayList(
                                                    data.stream()
                                                            .filter(
                                                                    a ->
                                                                            a.getItemName()
                                                                                            .toLowerCase()
                                                                                            .contains(
                                                                                                    auctionSearch
                                                                                                            .getText()
                                                                                                            .toLowerCase())
                                                                                    && (auctionStatus
                                                                                                            .getSelectionModel()
                                                                                                            .getSelectedIndex()
                                                                                                    == 0
                                                                                            || a.getStatus()
                                                                                                    .getDescription()
                                                                                                    .equals(
                                                                                                            auctionStatus
                                                                                                                    .getValue())))
                                                            .toList()));
                    auctionFilter.run();
                    showPane(auctionsPane);
                });
    }

    private void configureAuctionActions() {
        TableColumn<Auction, String> action = auctionAction;
        action.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        action.setCellFactory(
                c ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String id, boolean empty) {
                                super.updateItem(id, empty);
                                setGraphic(null);
                                if (empty || id == null || getTableRow().getItem() == null) return;
                                Auction a = getTableRow().getItem();
                                Button detail = new Button("Chi tiết"),
                                        cancel = new Button("Hủy"),
                                        delete = new Button("Xóa");
                                for (Button button : List.of(detail, cancel, delete))
                                    button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                                cancel.setDisable(
                                        a.getStatus() != AuctionStatus.OPEN
                                                && a.getStatus() != AuctionStatus.RUNNING);
                                delete.setDisable(a.getStatus() != AuctionStatus.CANCELED);
                                cancel.setTooltip(new Tooltip("Chỉ hủy phiên OPEN hoặc RUNNING."));
                                delete.setTooltip(new Tooltip("Chỉ xóa phiên đã hủy."));
                                detail.setOnAction(
                                        e -> {
                                            Alert info =
                                                    new Alert(
                                                            Alert.AlertType.INFORMATION,
                                                            "Tên: "
                                                                    + a.getItemName()
                                                                    + "\nNgười bán: "
                                                                    + a.getSellerUsername()
                                                                    + "\nGiá khởi điểm: "
                                                                    + FormatUtil.formatCurrency(
                                                                            a.getStartingPrice())
                                                                    + "\nGiá hiện tại: "
                                                                    + FormatUtil.formatCurrency(
                                                                            a.getCurrentPrice())
                                                                    + "\nNgười dẫn đầu: "
                                                                    + a.getHighestBidderUsername()
                                                                    + "\nSố lượt đặt giá: "
                                                                    + a.getTotalBids()
                                                                    + "\nThời gian: "
                                                                    + a.getStartTime()
                                                                            .format(formatter)
                                                                    + " – "
                                                                    + a.getEndTime()
                                                                            .format(formatter)
                                                                    + "\nMô tả: "
                                                                    + a.getItem().getDescription());
                                            info.setHeaderText(a.getItemName());
                                            ViewNavigator.applyAppStylesheet(info.getDialogPane());
                                            info.show();
                                        });
                                cancel.setOnAction(
                                        e -> {
                                            TextInputDialog d = new TextInputDialog();
                                            d.setHeaderText("Nhập lý do hủy phiên");
                                            ViewNavigator.applyAppStylesheet(d.getDialogPane());
                                            d.showAndWait()
                                                    .filter(s -> !s.isBlank())
                                                    .ifPresent(
                                                            reason ->
                                                                    run(
                                                                            () -> {
                                                                                auctionService
                                                                                        .cancelAuction(
                                                                                                id,
                                                                                                reason);
                                                                                return null;
                                                                            },
                                                                            x -> auctions()));
                                        });
                                delete.setOnAction(
                                        e -> {
                                            if (confirm("Xóa phiên " + a.getItemName() + "?"))
                                                run(
                                                        () -> {
                                                            auctionService.deleteAuction(id);
                                                            return null;
                                                        },
                                                        x -> auctions());
                                        });
                                setGraphic(new HBox(5, detail, cancel, delete));
                            }
                        });
    }

    private <T> void column(TableColumn<T, String> column, Function<T, String> value) {
        column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
    }

    private boolean confirm(String text) {
        Alert alert =
                new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    @FXML
    private void logout() {
        if (!authorized) return;
        run(
                () -> {
                    ServiceRegistry.getService().logout();
                    return null;
                },
                ignored -> {
                    ViewNavigator.clearHistory();
                    try {
                        ViewNavigator.loadView(
                                ViewNavigator.Route.AUTH.getPath(),
                                ViewNavigator.Route.AUTH.getTitle(),
                                false);
                    } catch (IOException e) {
                        message.setText("Không thể đăng xuất.");
                    }
                });
    }
}
