package com.auction.client.controller;

import com.auction.client.model.AdminUser;
import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.AuctionAdminService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.service.UserAdminService;
import com.auction.client.util.FormatUtil;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Điều khiển ba màn hình quản trị. */
public class AdminController {
    @FXML private VBox content;
    @FXML private Label title;
    @FXML private Label message;
    @FXML private ProgressIndicator progress;
    private final UserAdminService userService = ServiceRegistry.getUserAdminService();
    private final AuctionAdminService auctionService = ServiceRegistry.getAuctionAdminService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private String actorId;
    private boolean authorized;
    private long generation;

    /** Kiểm tra vai trò và hiển thị tổng quan. */
    @FXML public void initialize() {
        message.visibleProperty().bind(message.textProperty().isNotEmpty());
        message.managedProperty().bind(message.visibleProperty());
        User user = ServiceRegistry.getService().getCurrentUser();
        authorized = user != null && user.getRole() == UserRole.ADMIN;
        if (!authorized) { message.setText("Chỉ quản trị viên được truy cập trang này."); content.setDisable(true); return; }
        actorId = user.getId(); overview();
    }
    private <T> void run(Callable<T> work, Consumer<T> success) {
        if (!authorized) return;
        long request = ++generation;
        content.setDisable(true); progress.setVisible(true); progress.setManaged(true); message.setText("");
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(e -> Platform.runLater(() -> { if (request != generation) return; content.setDisable(false); progress.setVisible(false); progress.setManaged(false); success.accept(task.getValue()); }));
        task.setOnFailed(e -> Platform.runLater(() -> { if (request != generation) return; content.setDisable(false); progress.setVisible(false); progress.setManaged(false); message.setText("Không thể thực hiện: " + task.getException().getMessage()); }));
        Thread worker = new Thread(task, "admin-service"); worker.setDaemon(true); worker.start();
    }
    private record Summary(List<AdminUser> users, List<Auction> auctions) { }
    @FXML private void overview() {
        if (!authorized) return;
        title.setText("Tổng quan");
        run(() -> new Summary(userService.listUsers(), auctionService.listAuctions()), data -> {
            HBox cards = new HBox(16);
            cards.getChildren().addAll(card("Tổng người dùng", data.users().size()), card("Chờ bắt đầu", count(data.auctions(), AuctionStatus.OPEN)), card("Đang diễn ra", count(data.auctions(), AuctionStatus.RUNNING)), card("Đã kết thúc", count(data.auctions(), AuctionStatus.FINISHED)), card("Đã hủy", count(data.auctions(), AuctionStatus.CANCELED)));
            TableView<Auction> table = auctionTable(false);
            table.setItems(FXCollections.observableArrayList(data.auctions().stream().sorted((a,b) -> b.getStartTime().compareTo(a.getStartTime())).limit(10).toList()));
            content.getChildren().setAll(cards, new Label("Các phiên mới nhất"), table); VBox.setVgrow(table, Priority.ALWAYS);
        });
    }
    private long count(List<Auction> list, AuctionStatus status) { return list.stream().filter(a -> a.getStatus() == status).count(); }
    private VBox card(String name, long value) { VBox box = new VBox(8, new Label(name), new Label(Long.toString(value))); box.getStyleClass().add("card-panel"); box.setPrefWidth(180); box.getChildren().get(0).getStyleClass().add("field-hint"); box.getChildren().get(1).getStyleClass().add("page-title"); box.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(box, Priority.ALWAYS); return box; }
    @FXML private void users() {
        if (!authorized) return;
        title.setText("Quản lý người dùng");
        run(userService::listUsers, data -> {
            TextField search = new TextField(); search.setPromptText("Tìm tên đăng nhập hoặc email"); search.setPrefWidth(320); HBox.setHgrow(search, Priority.ALWAYS);
            ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("Tất cả", UserRole.BIDDER.getDisplayName(), UserRole.SELLER.getDisplayName(), UserRole.ADMIN.getDisplayName())); role.getSelectionModel().selectFirst();
            TableView<AdminUser> table = new TableView<>(); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); table.setFixedCellSize(44); table.setPlaceholder(new Label("Chưa có dữ liệu"));
            column(table, "Tên đăng nhập", u -> u.getUser().getUsername());
            column(table, "Email", u -> u.getUser().getEmail());
            column(table, "Vai trò", u -> u.getUser().getRole().getDisplayName());
            column(table, "Trạng thái", u -> u.isLocked() ? "Bị khóa" : "Hoạt động");
            column(table, "Ngày tạo", u -> u.getCreatedAt().format(formatter));
            TableColumn<AdminUser, String> action = new TableColumn<>("Thao tác");
            action.setPrefWidth(300); action.setMinWidth(280);
            action.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().getId()));
            action.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(String id, boolean empty) {
                    super.updateItem(id, empty); setGraphic(null);
                    if (empty || id == null || getTableRow().getItem() == null) return;
                    AdminUser u = getTableRow().getItem(); boolean self = id.equals(actorId);
                    Button lock = new Button(u.isLocked() ? "Mở khóa" : "Khóa"), change = new Button("Đổi vai trò"), delete = new Button("Xóa");
                    for (Button button : List.of(lock, change, delete)) button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    lock.setDisable(self); change.setDisable(self); delete.setDisable(self);
                    if (self) { lock.setTooltip(new Tooltip("Không thể tự khóa.")); delete.setTooltip(new Tooltip("Không thể tự xóa.")); change.setTooltip(new Tooltip("Không thể tự đổi vai trò.")); }
                    lock.setOnAction(e -> run(() -> { userService.setLocked(actorId, id, !u.isLocked()); return null; }, x -> users()));
                    change.setOnAction(e -> { ChoiceDialog<UserRole> d = new ChoiceDialog<>(u.getUser().getRole(), UserRole.values()); d.setHeaderText("Chọn vai trò"); ViewNavigator.applyAppStylesheet(d.getDialogPane()); d.showAndWait().ifPresent(r -> run(() -> { userService.setRole(actorId, id, r); return null; }, x -> users())); });
                    delete.setOnAction(e -> { if (confirm("Xóa người dùng " + u.getUser().getUsername() + "?")) run(() -> { userService.deleteUser(actorId, id); return null; }, x -> users()); });
                    setGraphic(new HBox(5, lock, change, delete));
                }
            });
            table.getColumns().add(action);
            Runnable filter = () -> table.setItems(FXCollections.observableArrayList(data.stream().filter(u -> (u.getUser().getUsername().toLowerCase().contains(search.getText().toLowerCase()) || u.getUser().getEmail().toLowerCase().contains(search.getText().toLowerCase())) && (role.getSelectionModel().getSelectedIndex() == 0 || u.getUser().getRole().getDisplayName().equals(role.getValue()))).toList()));
            search.textProperty().addListener((o,a,b)->filter.run()); role.valueProperty().addListener((o,a,b)->filter.run()); filter.run();
            content.getChildren().setAll(new HBox(10, search, role), table); VBox.setVgrow(table, Priority.ALWAYS);
        });
    }
    @FXML private void auctions() {
        if (!authorized) return;
        title.setText("Quản lý phiên đấu giá");
        run(auctionService::listAuctions, data -> {
            TextField search = new TextField(); search.setPromptText("Tìm tên phiên");
            ComboBox<String> status = new ComboBox<>(); status.getItems().add("Tất cả"); for (AuctionStatus s : AuctionStatus.values()) status.getItems().add(s.getDescription()); status.getSelectionModel().selectFirst();
            TableView<Auction> table = auctionTable(true);
            Runnable filter = () -> table.setItems(FXCollections.observableArrayList(data.stream().filter(a -> a.getItemName().toLowerCase().contains(search.getText().toLowerCase()) && (status.getSelectionModel().getSelectedIndex() == 0 || a.getStatus().getDescription().equals(status.getValue()))).toList()));
            search.textProperty().addListener((o,a,b)->filter.run()); status.valueProperty().addListener((o,a,b)->filter.run()); filter.run();
            content.getChildren().setAll(new HBox(10, search, status), table); VBox.setVgrow(table, Priority.ALWAYS);
        });
    }
    private TableView<Auction> auctionTable(boolean actions) {
        TableView<Auction> table = new TableView<>(); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); table.setFixedCellSize(44); table.setPlaceholder(new Label("Chưa có dữ liệu"));
        column(table, "Tên", Auction::getItemName); column(table, "Người bán", Auction::getSellerUsername);
        column(table, "Giá hiện tại", a -> FormatUtil.formatCurrency(a.getCurrentPrice()));
        column(table, "Người dẫn đầu", Auction::getHighestBidderUsername);
        column(table, "Trạng thái", a -> a.getStatus().getDescription());
        column(table, "Bắt đầu", a -> a.getStartTime().format(formatter));
        column(table, "Kết thúc", a -> a.getEndTime().format(formatter));
        if (actions) {
            TableColumn<Auction, String> action = new TableColumn<>("Thao tác");
            action.setPrefWidth(240); action.setMinWidth(230);
            action.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
            action.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(String id, boolean empty) {
                    super.updateItem(id, empty); setGraphic(null);
                    if (empty || id == null || getTableRow().getItem() == null) return;
                    Auction a = getTableRow().getItem();
                    Button detail = new Button("Chi tiết"), cancel = new Button("Hủy"), delete = new Button("Xóa");
                    for (Button button : List.of(detail, cancel, delete)) button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    cancel.setDisable(a.getStatus() != AuctionStatus.OPEN && a.getStatus() != AuctionStatus.RUNNING);
                    delete.setDisable(a.getStatus() != AuctionStatus.CANCELED);
                    cancel.setTooltip(new Tooltip("Chỉ hủy phiên OPEN hoặc RUNNING.")); delete.setTooltip(new Tooltip("Chỉ xóa phiên đã hủy."));
                    detail.setOnAction(e -> { Alert info = new Alert(Alert.AlertType.INFORMATION, "Tên: " + a.getItemName() + "\nNgười bán: " + a.getSellerUsername() + "\nGiá khởi điểm: " + FormatUtil.formatCurrency(a.getStartingPrice()) + "\nGiá hiện tại: " + FormatUtil.formatCurrency(a.getCurrentPrice()) + "\nNgười dẫn đầu: " + a.getHighestBidderUsername() + "\nSố lượt đặt giá: " + a.getTotalBids() + "\nThời gian: " + a.getStartTime().format(formatter) + " – " + a.getEndTime().format(formatter) + "\nMô tả: " + a.getItem().getDescription()); info.setHeaderText(a.getItemName()); ViewNavigator.applyAppStylesheet(info.getDialogPane()); info.show(); });
                    cancel.setOnAction(e -> { TextInputDialog d = new TextInputDialog(); d.setHeaderText("Nhập lý do hủy phiên"); ViewNavigator.applyAppStylesheet(d.getDialogPane()); d.showAndWait().filter(s -> !s.isBlank()).ifPresent(reason -> run(() -> { auctionService.cancelAuction(id, reason); return null; }, x -> auctions())); });
                    delete.setOnAction(e -> { if (confirm("Xóa phiên " + a.getItemName() + "?")) run(() -> { auctionService.deleteAuction(id); return null; }, x -> auctions()); });
                    setGraphic(new HBox(5, detail, cancel, delete));
                }
            });
            table.getColumns().add(action);
        }
        return table;
    }
    private <T> void column(TableView<T> table, String title, Function<T, String> value) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(145);
        c.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue()))); table.getColumns().add(c);
    }
    private boolean confirm(String text) { Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL); ViewNavigator.applyAppStylesheet(alert.getDialogPane()); return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK; }
    @FXML private void logout() {
        if (!authorized) return;
        run(() -> { ServiceRegistry.getService().logout(); return null; }, ignored -> {
            ViewNavigator.clearHistory();
            try { ViewNavigator.loadView(ViewNavigator.Route.AUTH.getPath(), ViewNavigator.Route.AUTH.getTitle(), false); } catch (IOException e) { message.setText("Không thể đăng xuất."); }
        });
    }
}
