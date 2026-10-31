package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ProductService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.util.FormatUtil;
import com.auction.client.ui.ProductImages;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Điều khiển trang quản lý sản phẩm của người bán. */
public class SellerProductsController {
    @FXML private TextField search;
    @FXML private ComboBox<String> status;
    @FXML private ComboBox<String> category;
    @FXML private TableView<Auction> table;
    @FXML private TableColumn<Auction, String> image, nameColumn, categoryColumn, startingPrice, currentPrice, state, startColumn, endColumn, actions;
    @FXML private Button addButton;
    @FXML private Label message;
    @FXML private ProgressIndicator progress;
    private final ProductService service = ServiceRegistry.getProductService();
    private final ObservableList<Auction> rows = FXCollections.observableArrayList();
    private final Map<String, String> images = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private User currentUser;

    /** Khởi tạo bảng và tải sản phẩm người bán. */
    @FXML public void initialize() {
        message.visibleProperty().bind(message.textProperty().isNotEmpty());
        message.managedProperty().bind(message.visibleProperty());
        currentUser = ServiceRegistry.getService().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.SELLER) {
            message.setText("Chỉ người bán được truy cập trang này.");
            table.setDisable(true); addButton.setDisable(true); return;
        }
        status.getItems().add("Tất cả trạng thái");
        for (AuctionStatus value : AuctionStatus.values()) status.getItems().add(value.getDescription());
        status.getSelectionModel().selectFirst();
        category.getItems().add("Tất cả loại");
        for (var c : com.auction.client.model.ItemCategory.values()) category.getItems().add(c.getLabel());
        category.getSelectionModel().selectFirst();

        image.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        image.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty); setGraphic(null);
                String path = images.get(id);
                Image loaded = empty || path == null || path.isBlank() ? null : ProductImages.load(path, 48, 48);
                if (loaded != null) {
                    ImageView view = new ImageView(loaded);
                    setGraphic(view);
                } else if (!empty && getTableRow().getItem() != null) {
                    var icon = com.auction.client.ui.Icons.icon(com.auction.client.model.ItemCategory.from(getTableRow().getItem().getItemCategory()).getIcon(), 24);
                    icon.getStyleClass().add("icon-muted");
                    setGraphic(icon);
                }
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
        column(nameColumn, Auction::getItemName);
        column(categoryColumn, Auction::getItemCategory);
        column(startingPrice, a -> FormatUtil.formatCurrency(a.getStartingPrice()));
        column(currentPrice, a -> FormatUtil.formatCurrency(a.getCurrentPrice()));
        column(state, a -> a.getStatus().name());
        state.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty); setText(null); setGraphic(null);
                if (!empty && value != null) {
                    Label badge = new Label(AuctionStatus.valueOf(value).getDescription()); badge.getStyleClass().addAll("product-status", "status-" + value.toLowerCase()); setGraphic(badge);
                }
            }
        });
        column(startColumn, a -> a.getStartTime().format(formatter));
        column(endColumn, a -> a.getEndTime().format(formatter));
        actions.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        actions.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty); setGraphic(null);
                if (empty || id == null || getTableRow().getItem() == null) return;
                Auction a = getTableRow().getItem();
                Button detail = new Button("Chi tiết"), edit = new Button("Sửa"), delete = new Button("Xóa");
                for (Button button : List.of(detail, edit, delete)) button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                boolean locked = a.getTotalBids() > 0 || a.getStatus() != AuctionStatus.OPEN;
                edit.setDisable(locked); delete.setDisable(locked);
                edit.setTooltip(new Tooltip("Chỉ sửa phiên OPEN chưa có lượt đặt giá."));
                delete.setTooltip(new Tooltip("Chỉ xóa phiên OPEN chưa có lượt đặt giá."));
                detail.setOnAction(e -> detail(a)); edit.setOnAction(e -> form(a));
                delete.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa sản phẩm " + a.getItemName() + "?", ButtonType.OK, ButtonType.CANCEL);
                    ViewNavigator.applyAppStylesheet(confirm.getDialogPane());
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        String imagePath = images.get(id);
                        run(() -> { service.deleteProduct(currentUser.getUsername(), id); ProductImages.deleteImportedImage(imagePath, id); return null; }, ignored -> load());
                    }
                });
                setGraphic(new HBox(5, detail, edit, delete));
            }
        });
        search.textProperty().addListener((o, a, b) -> filter());
        status.valueProperty().addListener((o, a, b) -> filter());
        category.valueProperty().addListener((o, a, b) -> filter());
        load();
    }
    private void column(TableColumn<Auction, String> column, Function<Auction, String> value) {
        column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
    }
    private void filter() {
        String q = search.getText().trim().toLowerCase();
        table.setItems(rows.filtered(a -> a.getItemName().toLowerCase().contains(q)
                && (status.getSelectionModel().getSelectedIndex() <= 0 || a.getStatus().getDescription().equals(status.getValue()))
                && (category.getSelectionModel().getSelectedIndex() <= 0 || a.getItemCategory().equals(category.getValue()))));
    }
    private record Products(List<Auction> rows, Map<String, String> paths) { }
    private void load() {
        run(() -> {
            List<Auction> list = service.listProducts(currentUser.getUsername());
            Map<String, String> paths = new HashMap<>();
            for (Auction a : list) paths.put(a.getId(), service.getImagePath(a.getId()));
            return new Products(list, paths);
        }, result -> { images.clear(); images.putAll(result.paths()); rows.setAll(result.rows()); filter(); table.refresh(); });
    }
    private <T> void run(Callable<T> work, Consumer<T> success) {
        progress.setVisible(true); progress.setManaged(true); table.setDisable(true); addButton.setDisable(true); message.setText("");
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(e -> Platform.runLater(() -> { progress.setVisible(false); progress.setManaged(false); table.setDisable(false); addButton.setDisable(false); success.accept(task.getValue()); }));
        task.setOnFailed(e -> Platform.runLater(() -> { progress.setVisible(false); progress.setManaged(false); table.setDisable(false); addButton.setDisable(false); message.setText("Không thể thực hiện: " + task.getException().getMessage()); }));
        Thread worker = new Thread(task, "seller-service"); worker.setDaemon(true); worker.start();
    }
    @FXML private void add() { form(null); }
    private void detail(Auction auction) {
        run(() -> service.getBidHistory(auction.getId()), history -> {
            String bids = history.isEmpty() ? "Chưa có dữ liệu"
                    : history.stream().map(Object::toString).collect(java.util.stream.Collectors.joining("\n"));
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    auction.getItem().getDescription() + "\n\nLịch sử đặt giá:\n" + bids);
            alert.setHeaderText(auction.getItemName());
            ViewNavigator.applyAppStylesheet(alert.getDialogPane()); alert.show();
        });
    }
    private void form(Auction original) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(original == null ? "Thêm sản phẩm" : "Sửa sản phẩm");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ViewNavigator.applyAppStylesheet(dialog.getDialogPane());
        TextField name = new TextField(), price = new TextField(), step = new TextField();
        price.setPromptText("VD: 5000000"); step.setPromptText("VD: 5000000");
        TextArea description = new TextArea(); description.setPrefRowCount(3);
        ComboBox<String> type = new ComboBox<>();
        for (var c : com.auction.client.model.ItemCategory.values()) type.getItems().add(c.getLabel());
        DatePicker start = new DatePicker(), end = new DatePicker();
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT);
        for (DatePicker day : List.of(start, end)) {
            day.setPromptText("dd/mm/yyyy (VD: 30/10/2026)"); day.setEditable(true);
            day.setConverter(new javafx.util.StringConverter<>() {
                public String toString(java.time.LocalDate value) { return value == null ? "" : dayFormat.format(value); }
                public java.time.LocalDate fromString(String text) { try { return text == null || text.isBlank() ? null : java.time.LocalDate.parse(text, dayFormat); } catch (java.time.DateTimeException ex) { return null; } }
            });
        }
        TextField startTime = new TextField(), endTime = new TextField();
        startTime.setPromptText("HH:mm (VD: 14:30)"); endTime.setPromptText("HH:mm (VD: 14:30)");
        CheckBox now = new CheckBox("Bắt đầu ngay");
        ComboBox<String> duration = new ComboBox<>(FXCollections.observableArrayList("30 phút", "1 giờ", "6 giờ", "1 ngày", "3 ngày", "7 ngày", "Tùy chọn")); duration.setValue("Tùy chọn");
        ImageView preview = new ImageView(); preview.setFitWidth(110); preview.setFitHeight(75); preview.setPreserveRatio(true);
        String storedPath = original == null ? null : images.get(original.getId());
        String savedPath = storedPath == null || storedPath.isBlank() ? null : storedPath; String[] path = {savedPath};
        Button choose = new Button("Chọn ảnh"), remove = new Button("Bỏ ảnh"); Label filename = new Label(), imageState = new Label(), imageError = new Label();
        filename.setMaxWidth(220); imageError.getStyleClass().add("field-error"); imageError.setWrapText(true);
        Runnable refreshImage = () -> {
            remove.setVisible(path[0] != null); remove.setManaged(path[0] != null);
            filename.setText(path[0] == null ? "" : new File(path[0]).getName()); filename.setTooltip(path[0] == null ? null : new Tooltip(path[0]));
            imageState.setText(path[0] == null ? "Chưa chọn ảnh" : path[0].equals(savedPath) ? "Đã lưu" : "Đã chọn · chưa lưu");
            preview.setImage(path[0] == null ? null : ProductImages.load(path[0], 110, 75));
        };
        choose.setOnAction(e -> { FileChooser chooser = new FileChooser(); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")); File file = chooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow()); if (file != null) {
            String lower = file.getName().toLowerCase(java.util.Locale.ROOT); Image candidate = new Image(file.toURI().toString());
            if (!file.isFile() || file.length() >= 5 * 1024 * 1024 || !(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".bmp")) || candidate.isError()) imageError.setText("Ảnh phải là PNG/JPG/GIF và nhỏ hơn 5 MB.");
            else { imageError.setText(""); path[0] = file.getAbsolutePath(); refreshImage.run(); }
        }});
        remove.setOnAction(e -> { path[0] = null; imageError.setText(""); refreshImage.run(); }); refreshImage.run();
        if (original != null) {
            name.setText(original.getItemName()); description.setText(original.getItem().getDescription()); type.setValue(original.getItemCategory()); price.setText(String.valueOf(original.getStartingPrice())); step.setText(String.valueOf(original.getMinBidStep()));
            start.setValue(original.getStartTime().toLocalDate()); end.setValue(original.getEndTime().toLocalDate()); startTime.setText(original.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))); endTime.setText(original.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        Label dates = new Label(), summary = new Label(), missing = new Label(), priceHint = new Label(), stepHint = new Label();
        dates.getStyleClass().add("field-error"); summary.getStyleClass().add("field-hint"); missing.getStyleClass().add("field-hint");
        for (Label label : List.of(dates, summary, missing, imageError)) { label.setWrapText(true); label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE); }
        VBox body = new VBox(7, new Label("Tên sản phẩm *"), name, new Label("Mô tả *"), description, new Label("Loại *"), type, new Label("Giá khởi điểm *"), price, priceHint, new Label("Bước giá *"), step, stepHint, new HBox(12, now, new Label("Thời lượng"), duration), new Label("Bắt đầu *"), new HBox(8, start, startTime), new Label("Kết thúc *"), new HBox(8, end, endTime), dates, summary, new Label("Ảnh"), new HBox(8, choose, remove, filename), imageState, imageError, preview, missing);
        ScrollPane scroll = new ScrollPane(body); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(560); scroll.setPrefViewportWidth(520); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); dialog.getDialogPane().setContent(scroll);
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK); save.setText("Lưu"); ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Hủy");
        boolean[] touched = {false}, updating = {false};
        Runnable validate = () -> {
            LocalDateTime a = date(start, startTime), b = date(end, endTime);
            boolean validDates = com.auction.client.util.AuctionTime.valid(a, b, LocalDateTime.now());
            String error = a == null || b == null ? "Giờ theo dạng HH:mm, ví dụ 14:30." : a.isBefore(LocalDateTime.now()) ? "Thời gian bắt đầu phải từ bây giờ trở đi." : !b.isAfter(a) ? "Thời gian kết thúc phải sau thời gian bắt đầu." : "";
            dates.setText(touched[0] && !validDates ? error : "");
            summary.setText(a != null && b != null && b.isAfter(a) ? com.auction.client.util.AuctionTime.summary(a, b) : "");
            java.util.List<String> absent = new java.util.ArrayList<>(); if (name.getText().isBlank()) absent.add("tên"); if (description.getText().isBlank()) absent.add("mô tả"); if (type.getValue() == null) absent.add("loại"); if (!positive(price.getText())) absent.add("giá khởi điểm"); if (!positive(step.getText())) absent.add("bước giá"); if (!validDates) absent.add("thời gian hợp lệ");
            missing.setText(absent.isEmpty() ? "" : "Còn thiếu: " + String.join(", ", absent) + "."); save.setDisable(!absent.isEmpty());
            priceHint.setText(positive(price.getText()) ? FormatUtil.formatCurrency(Double.parseDouble(price.getText())) : ""); stepHint.setText(positive(step.getText()) ? FormatUtil.formatCurrency(Double.parseDouble(step.getText())) : "");
        };
        Runnable applyDuration = () -> { if (updating[0]) return; LocalDateTime a = date(start, startTime); if (a == null || duration.getValue() == null || duration.getValue().equals("Tùy chọn")) { validate.run(); return; }
            LocalDateTime b = switch(duration.getValue()) { case "30 phút" -> a.plusMinutes(30); case "1 giờ" -> a.plusHours(1); case "6 giờ" -> a.plusHours(6); case "1 ngày" -> a.plusDays(1); case "3 ngày" -> a.plusDays(3); default -> a.plusDays(7); };
            updating[0] = true; end.setValue(b.toLocalDate()); endTime.setText(b.format(DateTimeFormatter.ofPattern("HH:mm"))); updating[0] = false; validate.run(); };
        now.selectedProperty().addListener((o,x,v) -> { start.setDisable(v); startTime.setDisable(v); if (v) { LocalDateTime a = LocalDateTime.now().plusMinutes(1).withSecond(0).withNano(0); start.setValue(a.toLocalDate()); startTime.setText(a.format(DateTimeFormatter.ofPattern("HH:mm"))); } applyDuration.run(); });
        duration.valueProperty().addListener((o,x,v) -> applyDuration.run()); start.valueProperty().addListener((o,x,v) -> applyDuration.run()); startTime.textProperty().addListener((o,x,v) -> applyDuration.run());
        end.valueProperty().addListener((o,x,v) -> validate.run()); endTime.textProperty().addListener((o,x,v) -> validate.run());
        for (TextField field : List.of(name, price, step)) field.textProperty().addListener((o,x,v) -> validate.run()); description.textProperty().addListener((o,x,v) -> validate.run()); type.valueProperty().addListener((o,x,v) -> validate.run());
        for (javafx.scene.Node field : List.of(start, end, startTime, endTime)) field.focusedProperty().addListener((o,was,focused) -> { if (was && !focused) { touched[0] = true; validate.run(); } });
        validate.run(); dialog.setOnShown(e -> scroll.setVvalue(0));
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            LocalDateTime a = date(start, startTime), b = date(end, endTime); if (a == null || b == null || a.isBefore(LocalDateTime.now()) || !b.isAfter(a)) { message.setText("Thời gian phiên không hợp lệ."); return; }
            String id = original == null ? UUID.randomUUID().toString() : original.getId();
            Item item = new Item(original == null ? UUID.randomUUID().toString() : original.getItem().getId(), name.getText().trim(), description.getText().trim(), type.getValue(), Double.parseDouble(price.getText()), currentUser.getUsername());
            Auction auction = new Auction(id, item, currentUser.getUsername(), item.getStartingPrice(), null, AuctionStatus.OPEN, a, b, Double.parseDouble(step.getText()));
            run(() -> {
                String imagePath = path[0];
                if (imagePath != null && !imagePath.equals(savedPath)) imagePath = ProductImages.importImage(new File(imagePath), id);
                service.saveProduct(auction, imagePath == null ? "" : imagePath);
                if (savedPath != null && !savedPath.equals(imagePath)) ProductImages.deleteImportedImage(savedPath, id);
                return null;
            }, ignored -> load());
        }
    }
    private boolean positive(String text) { try { double value = Double.parseDouble(text); return Double.isFinite(value) && value > 0; } catch (NumberFormatException e) { return false; } }
    private LocalDateTime date(DatePicker day, TextField time) { LocalTime value = com.auction.client.util.AuctionTime.parse(time.getText()); return day.getValue() == null || value == null ? null : LocalDateTime.of(day.getValue(), value); }
    @FXML private void handleBackToHome() { try { ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD); } catch (IOException e) { message.setText("Không thể mở trang chủ."); } }
}
