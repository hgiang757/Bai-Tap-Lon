package com.auction.client.controller;

import com.auction.client.model.Transaction;
import com.auction.client.model.TransactionType;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.service.WalletService;
import com.auction.client.ui.AppFooter;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.util.FormatUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Điều khiển trang ví mô phỏng. */
public class WalletController {
    @FXML private ScrollPane scroll;
    @FXML private VBox pageContent;
    @FXML private FlowPane statistics;
    @FXML private ComboBox<String> typeFilter;
    @FXML private TextField search;
    @FXML private TableView<Transaction> table;
    private final WalletService wallet = ServiceRegistry.getWalletService();
    private List<Transaction> transactions = List.of();
    private String username;
    private long balance, available;
    private final Label[] values = new Label[6];

    /** Khởi tạo bố cục và tải dữ liệu ví. */
    @FXML public void initialize() {
        pageContent.minHeightProperty().bind(Bindings.createDoubleBinding(() -> scroll.getViewportBounds().getHeight(), scroll.viewportBoundsProperty()));
        SmoothScroll.apply(scroll, 3.0); AppFooter.install(scroll, pageContent);
        var user = ServiceRegistry.getService().getCurrentUser();
        if (user == null) { table.setPlaceholder(new Label("Vui lòng đăng nhập để xem ví.")); return; }
        username = user.getUsername();
        String[] headings = {"Số dư", "Khả dụng", "Đang giữ", "Tổng đã nạp", "Tổng đã rút", "Số giao dịch"};
        for (int i = 0; i < headings.length; i++) {
            Label heading = new Label(headings[i]); heading.getStyleClass().add("text-secondary");
            values[i] = new Label("—"); values[i].getStyleClass().add("t-h3");
            VBox card = new VBox(8, heading, values[i]); card.getStyleClass().add("card-panel"); card.setPrefWidth(170); statistics.getChildren().add(card);
        }
        typeFilter.getItems().add("Tất cả loại"); for (var t : TransactionType.values()) typeFilter.getItems().add(t.getLabel()); typeFilter.getSelectionModel().selectFirst();
        typeFilter.valueProperty().addListener((o,a,b) -> filter()); search.textProperty().addListener((o,a,b) -> filter());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có giao dịch nào."));
        column("Thời gian", t -> t.timestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        var type = column("Loại", t -> t.type().getLabel());
        type.setCellFactory(c -> new TableCell<>() { @Override protected void updateItem(String v, boolean empty) { super.updateItem(v, empty); setText(null); setGraphic(null); if (!empty && v != null) { Label badge = new Label(v); badge.getStyleClass().add("badge-status"); setGraphic(badge); } } });
        var amount = column("Số tiền", t -> (incoming(t.type()) ? "+" : "−") + FormatUtil.formatCurrency(t.amount()));
        amount.setCellFactory(c -> new TableCell<>() { @Override protected void updateItem(String v, boolean empty) { super.updateItem(v, empty); setText(empty ? null : v); getStyleClass().removeAll("field-success", "field-error"); if (!empty && v != null) getStyleClass().add(v.startsWith("+") ? "field-success" : "field-error"); } });
        column("Số dư sau", t -> FormatUtil.formatCurrency(t.balanceAfter()));
        column("Ghi chú/phiên", t -> (t.note() == null ? "" : t.note()) + (t.auctionId() == null ? "" : t.auctionId()));
        reload();
    }
    private boolean incoming(TransactionType type) { return type == TransactionType.DEPOSIT || type == TransactionType.BID_RELEASE || type == TransactionType.REFUND; }
    private TableColumn<Transaction,String> column(String name, java.util.function.Function<Transaction,String> f) {
        TableColumn<Transaction,String> c = new TableColumn<>(name); c.setCellValueFactory(v -> new SimpleStringProperty(f.apply(v.getValue()))); table.getColumns().add(c); return c;
    }
    private record Snapshot(long balance, long held, long available, List<Transaction> transactions) { }
    private void reload() {
        if (username == null) return;
        run(() -> new Snapshot(wallet.getBalance(username), wallet.getHeldAmount(username), wallet.getAvailable(username), wallet.getTransactions(username)), data -> {
            balance = data.balance(); available = data.available(); transactions = data.transactions();
            long deposited = transactions.stream().filter(t -> t.type() == TransactionType.DEPOSIT).mapToLong(Transaction::amount).sum();
            long withdrawn = transactions.stream().filter(t -> t.type() == TransactionType.WITHDRAW).mapToLong(Transaction::amount).sum();
            long[] numbers = {balance, available, data.held(), deposited, withdrawn};
            for (int i = 0; i < numbers.length; i++) values[i].setText(FormatUtil.formatCurrency(numbers[i]));
            values[5].setText(Integer.toString(transactions.size())); filter();
        }, error -> showError(error.getMessage()));
    }
    private void filter() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        table.setItems(FXCollections.observableArrayList(transactions.stream().filter(t -> (typeFilter.getSelectionModel().getSelectedIndex() <= 0 || t.type().getLabel().equals(typeFilter.getValue())) && ((t.note() == null ? "" : t.note()) + " " + (t.auctionId() == null ? "" : t.auctionId())).toLowerCase(Locale.ROOT).contains(q)).toList()));
    }
    @FXML private void deposit() { showTransactionDialog(true); }
    @FXML private void withdraw() { showTransactionDialog(false); }
    private void showTransactionDialog(boolean deposit) {
        if (username == null) return;
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(deposit ? "Nạp tiền" : "Rút tiền");
        ButtonType confirm = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE), cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, cancel); ViewNavigator.applyAppStylesheet(dialog.getDialogPane());
        TextField input = new TextField(); input.setPromptText("VD: 500000");
        Label format = new Label(), after = new Label(), error = new Label(), simulation = new Label("Đây là tính năng mô phỏng, không phát sinh tiền thật.");
        error.getStyleClass().add("field-error"); error.setWrapText(true); simulation.getStyleClass().add("text-secondary"); simulation.setWrapText(true);
        FlowPane quick = new FlowPane(6, 6); for (long v : new long[]{100_000, 500_000, 1_000_000, 5_000_000}) { Button b = new Button(FormatUtil.formatCurrency(v)); b.setOnAction(e -> input.setText(Long.toString(v))); quick.getChildren().add(b); }
        input.textProperty().addListener((o,a,b) -> { try { long n = Long.parseLong(b.replaceAll("[,. ]", "")); format.setText(FormatUtil.formatCurrency(n)); after.setText("Số dư sau giao dịch: " + FormatUtil.formatCurrency(deposit ? Math.addExact(balance,n) : balance-n)); } catch (Exception ex) { format.setText(""); after.setText("Số dư sau giao dịch: —"); } error.setText(""); });
        VBox content = new VBox(10, new Label("Số tiền (₫)"), input, format, quick, after, error, simulation); content.setPrefWidth(540); dialog.getDialogPane().setContent(content);
        Button submit = (Button) dialog.getDialogPane().lookupButton(confirm);
        submit.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume(); long n;
            try { String text = input.getText().replaceAll("[,. ]", ""); if (!text.matches("[0-9]+")) throw new NumberFormatException(); n = Long.parseLong(text); }
            catch (Exception ex) { error.setText("Số tiền phải là số nguyên dương."); return; }
            submit.setDisable(true);
            run(() -> { if (deposit) wallet.deposit(username,n); else wallet.withdraw(username,n); return null; }, ignored -> { dialog.close(); reload(); }, failure -> { submit.setDisable(false); error.setText(failure.getMessage()); });
        });
        dialog.showAndWait();
    }
    private <T> void run(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(e -> success.accept(task.getValue())); task.setOnFailed(e -> failure.accept(task.getException()));
        Thread thread = new Thread(task, "wallet-service"); thread.setDaemon(true); thread.start();
    }
    private void showError(String text) { Alert alert = new Alert(Alert.AlertType.ERROR, text); ViewNavigator.applyAppStylesheet(alert.getDialogPane()); alert.show(); }
    @FXML private void back() { try { if (!ViewNavigator.back()) ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD); } catch (Exception e) { showError("Không thể quay lại."); } }
}
