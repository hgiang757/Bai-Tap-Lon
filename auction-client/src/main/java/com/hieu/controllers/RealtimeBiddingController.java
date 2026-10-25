package com.hieu.controllers;

import com.hieu.models.Auction;
import com.hieu.models.AuctionStatus;
import com.hieu.models.Bid;
import com.hieu.services.AuctionClientService;
import com.hieu.services.BidResult;
import com.hieu.services.MockAuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.FormatUtil;
import com.hieu.util.AppInfo;
import com.hieu.util.SmoothScroll;
import com.hieu.util.ViewNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Alert;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

/**
 * Điều khiển màn hình chi tiết sản phẩm và đấu giá trực tiếp.
 * Quản lý biểu đồ biến động giá, kiểm soát khóa nhập khi phiên kết thúc,
 * và hiển thị dữ liệu thời gian thực thông qua Observer pattern.
 */
public class RealtimeBiddingController {

    @FXML private Label lblItemName;
    @FXML private Label lblBrandName;
    @FXML private Label lblCategory;
    @FXML private Label lblSeller;
    @FXML private Label lblDescription;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblHighestBidder;
    @FXML private Label lblMinNextBid;
    @FXML private Label lblStatus;
    @FXML private Label lblEndTime;
    @FXML private Label lblStatusExplanation;
    @FXML private Label lblCountdown;

    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnQuickAdd1;
    @FXML private Button btnQuickAdd2;
    @FXML private Button btnQuickAdd3;

    @FXML private HBox alertBox;
    @FXML private Label lblAlertMessage;

    @FXML private HBox devModeBox;
    @FXML private ScrollPane detailScroll;

    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis xAxisTime;
    @FXML private NumberAxis yAxisPrice;
    @FXML private ListView<String> lvRecentBids;

    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private Auction currentAuction;
    private XYChart.Series<String, Number> priceSeries;
    private Consumer<Bid> realtimeBidConsumer;
    private Timeline countdownTimer;

    @FXML
    public void initialize() {
        lblBrandName.setText(AppInfo.NAME);
        SmoothScroll.apply(detailScroll, 3.0);
        setupChart();
        setupQuickBidButtons();
        setupPlaceholder();
        checkDevMode();
        hideAlert();
    }

    /**
     * Cấu hình trục Y tự co giãn theo dữ liệu và không ép số 0 vào dải hiển thị.
     */
    private void setupChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá trả");

        priceChart.getData().clear();
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(true);
        priceChart.setLegendVisible(false);

        xAxisTime.setLabel("Thời gian");
        yAxisPrice.setLabel("Mức giá (₫)");

        // Sửa lỗi A.1: Trục Y tự động co giãn theo khoảng dữ liệu thực tế
        yAxisPrice.setAutoRanging(true);
        yAxisPrice.setForceZeroInRange(false);
    }

    /**
     * Hiển thị dòng thông báo khi danh sách lịch sử đặt giá trống (Sửa lỗi A.3)
     */
    private void setupPlaceholder() {
        Label placeholder = new Label("Chưa có lượt đặt giá nào");
        placeholder.getStyleClass().add("empty-placeholder");
        lvRecentBids.setPlaceholder(placeholder);
    }

    /**
     * Chỉ hiển thị nút mô phỏng khi đang sử dụng MockAuctionClientService (Sửa lỗi A.4)
     */
    private void checkDevMode() {
        boolean isMock = auctionService instanceof MockAuctionClientService;
        devModeBox.setVisible(isMock);
        devModeBox.setManaged(isMock);
    }

    /**
     * Nạp dữ liệu phiên đấu giá vào màn hình
     */
    public void setAuction(Auction auction) {
        if (currentAuction != null && realtimeBidConsumer != null) {
            auctionService.unregisterBidListener(currentAuction.getId(), realtimeBidConsumer);
        }
        this.currentAuction = auction;
        renderAuctionDetails();
        loadBidHistory();
        registerRealtimeObserver();
        startCountdown();
        updateCountdown();
    }

    private void renderAuctionDetails() {
        if (currentAuction == null) return;

        lblItemName.setText(currentAuction.getItemName());
        lblCategory.setText(currentAuction.getItemCategory());
        lblSeller.setText("Người bán: " + currentAuction.getSellerUsername());
        lblDescription.setText(currentAuction.getItem() != null ? currentAuction.getItem().getDescription() : "");
        lblStartingPrice.setText(currentAuction.getFormattedStartingPrice());
        lblCurrentPrice.setText(currentAuction.getFormattedCurrentPrice());
        lblHighestBidder.setText(currentAuction.getHighestBidderUsername());
        lblMinNextBid.setText("Mức đặt tối thiểu: " + FormatUtil.formatCurrency(currentAuction.getMinimumNextBid()));
        lblEndTime.setText(currentAuction.getFormattedEndTime());

        lblStatus.setText(currentAuction.getStatus().getDescription());
        lblStatus.getStyleClass().removeAll("badge-running", "badge-open", "badge-finished");

        boolean isRunning = currentAuction.getStatus() == AuctionStatus.RUNNING;

        if (isRunning) {
            lblStatus.getStyleClass().add("badge-running");
            lblStatusExplanation.setText("Phiên đấu giá đang diễn ra. Bạn có thể tham gia trả giá.");
            lblStatusExplanation.getStyleClass().removeAll("notice-disabled");
        } else {
            lblStatus.getStyleClass().add(currentAuction.getStatus() == AuctionStatus.OPEN ? "badge-open" : "badge-finished");
            if (currentAuction.getStatus() == AuctionStatus.FINISHED) {
                lblStatusExplanation.setText("Phiên đấu giá đã kết thúc. Không thể đặt thêm giá.");
            } else {
                lblStatusExplanation.setText("Phiên đấu giá chưa mở. Vui lòng chờ đến giờ bắt đầu.");
            }
            lblStatusExplanation.getStyleClass().add("notice-disabled");
        }

        // Sửa lỗi A.2: Khóa toàn bộ các thành phần nhập giá khi phiên không ở trạng thái diễn ra
        txtBidAmount.setDisable(!isRunning);
        btnPlaceBid.setDisable(!isRunning);
        btnQuickAdd1.setDisable(!isRunning);
        btnQuickAdd2.setDisable(!isRunning);
        btnQuickAdd3.setDisable(!isRunning);

        // Đề xuất mức giá tối thiểu vào ô nhập
        txtBidAmount.setText(String.format("%.0f", currentAuction.getMinimumNextBid()));
    }

    private void loadBidHistory() {
        priceSeries.getData().clear();
        lvRecentBids.getItems().clear();

        List<Bid> bids = auctionService.getBidHistory(currentAuction.getId());
        if (bids.isEmpty() && currentAuction.getItem() != null) {
            updatePriceChart("Bắt đầu", currentAuction.getStartingPrice());
        } else {
            for (Bid bid : bids) {
                updatePriceChart(bid.getFormattedTime(), bid.getAmount());
                lvRecentBids.getItems().add(0, formatBid(bid));
            }
        }
    }

    private String formatBid(Bid bid) {
        String username = bid.getBidderUsername();
        String maskedName = username == null || username.isEmpty()
                ? "Người dùng"
                : username.charAt(0) + "***" + (username.length() > 2
                        ? username.substring(username.length() - 1)
                        : "");
        return maskedName + "  •  " + bid.getFormattedTime() + "  •  "
                + FormatUtil.formatCurrency(bid.getAmount());
    }

    private void registerRealtimeObserver() {
        if (currentAuction == null) return;

        realtimeBidConsumer = this::onBidReceived;

        auctionService.registerBidListener(currentAuction.getId(), realtimeBidConsumer);
    }

    // ======= Countdown Timer =======

    /**
     * Khởi động countdown timer đếm ngược theo giây đến thời điểm kết thúc phiên.
     */
    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    /**
     * Cập nhật label đếm ngược mỗi giây. Chuyển sang màu đỏ khi còn dưới 5 phút.
     */
    private void updateCountdown() {
        if (currentAuction == null || currentAuction.getEndTime() == null) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = currentAuction.getEndTime();
        if (!now.isBefore(end)) {
            lblCountdown.setText("Đã kết thúc");
            currentAuction.setStatus(AuctionStatus.FINISHED);
            countdownTimer.stop();
            renderAuctionDetails();
            return;
        }
        if (currentAuction.getStatus() == AuctionStatus.OPEN
                && currentAuction.getStartTime() != null
                && !now.isBefore(currentAuction.getStartTime())) {
            currentAuction.setStatus(AuctionStatus.RUNNING);
            renderAuctionDetails();
        }
        long seconds = ChronoUnit.SECONDS.between(now, end);
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        String text = String.format("%02d:%02d:%02d", hours, mins, secs);
        lblCountdown.setText(text);
        // Màu đỏ khi còn < 5 phút
        if (seconds < 300) {
            lblCountdown.getStyleClass().removeAll("countdown-label");
            if (!lblCountdown.getStyleClass().contains("countdown-label-urgent")) {
                lblCountdown.getStyleClass().add("countdown-label-urgent");
            }
        }
    }

    /**
     * Phương thức cập nhật điểm dữ liệu mới vào biểu đồ LineChart
     */
    public void updatePriceChart(String time, double price) {
        XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(time, price);
        if (Platform.isFxApplicationThread()) {
            priceSeries.getData().add(dataPoint);
        } else {
            Platform.runLater(() -> priceSeries.getData().add(dataPoint));
        }
    }

    /**
     * API entry point for realtime bid events from socket/server.
     * Member 1 (Server Integration) should call this method when a new bid arrives.
     * Must be safe to call from non-JavaFX threads.
     */
    public void onBidReceived(Bid bid) {
        if (bid == null || currentAuction == null || !currentAuction.getId().equals(bid.getAuctionId())) {
            return;
        }
        Platform.runLater(() -> {
            updatePriceChart(bid.getFormattedTime(), bid.getAmount());
            currentAuction.setCurrentPrice(bid.getAmount());
            currentAuction.setHighestBidderUsername(bid.getBidderUsername());
            lblCurrentPrice.setText(FormatUtil.formatCurrency(bid.getAmount()));
            lblHighestBidder.setText(bid.getBidderUsername());
            lblMinNextBid.setText("Mức đặt tối thiểu: " + FormatUtil.formatCurrency(bid.getAmount() + currentAuction.getMinBidStep()));
            txtBidAmount.setText(String.format("%.0f", bid.getAmount() + currentAuction.getMinBidStep()));
            lvRecentBids.getItems().add(0, formatBid(bid));
            showAlert("Lượt trả giá mới: " + bid.getBidderUsername() + " vừa đặt " + FormatUtil.formatCurrency(bid.getAmount()), false);
        });
    }

    @FXML
    private void handlePlaceBid() {
        hideAlert();

        if (currentAuction == null) {
            showAlert("Không tìm thấy thông tin phiên đấu giá.", true);
            return;
        }

        if (currentAuction.getStatus() != AuctionStatus.RUNNING) {
            showAlert("Phiên đấu giá đã kết thúc hoặc chưa bắt đầu.", true);
            return;
        }

        String input = txtBidAmount.getText();
        if (input == null || input.trim().isEmpty()) {
            showAlert("Vui lòng nhập số tiền muốn đặt giá.", true);
            return;
        }

        long bidAmount;
        try {
            String normalized = input.trim().replaceAll("[,.]", "");
            if (!normalized.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            bidAmount = Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            showAlert("Giá đặt phải là số nguyên dương.", true);
            return;
        }
        if (bidAmount <= 0) {
            showAlert("Giá đặt phải là số nguyên dương.", true);
            return;
        }
        if (bidAmount <= currentAuction.getCurrentPrice()) {
            showAlert("Giá đặt phải cao hơn giá hiện tại.", true);
            return;
        }
        if (bidAmount < currentAuction.getMinimumNextBid()) {
            showAlert("Mức đặt tối thiểu là " + FormatUtil.formatCurrency(currentAuction.getMinimumNextBid()) + ".", true);
            return;
        }

        BidResult result = auctionService.placeBid(currentAuction.getId(), bidAmount);
        if (result.isSuccess()) {
            showAlert("Đặt giá thành công: " + FormatUtil.formatCurrency((double) bidAmount), false);
        } else {
            showAlert(result.getMessage(), true);
        }
    }

    private void setupQuickBidButtons() {
        btnQuickAdd1.setOnAction(e -> addQuickBid(1_000_000));
        btnQuickAdd2.setOnAction(e -> addQuickBid(5_000_000));
        btnQuickAdd3.setOnAction(e -> addQuickBid(10_000_000));
    }

    private void addQuickBid(double delta) {
        try {
            long current = Long.parseLong(txtBidAmount.getText().trim().replaceAll("[,.]", ""));
            txtBidAmount.setText(String.format("%.0f", current + delta));
        } catch (NumberFormatException ex) {
            txtBidAmount.setText(String.format("%.0f", currentAuction.getMinimumNextBid() + delta));
        }
    }

    @FXML
    private void handleSimulateIncomingBid() {
        if (currentAuction == null) return;
        double simulatedPrice = currentAuction.getCurrentPrice() + currentAuction.getMinBidStep();
        auctionService.placeBid(currentAuction.getId(), simulatedPrice);
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            if (countdownTimer != null) countdownTimer.stop();
            if (currentAuction != null && realtimeBidConsumer != null) {
                auctionService.unregisterBidListener(currentAuction.getId(), realtimeBidConsumer);
            }
            ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD);
        } catch (IOException e) {
            showDialog("Không thể mở danh sách phiên đấu giá.");
        }
    }

    private void showDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Không thể chuyển màn hình");
        alert.showAndWait();
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
