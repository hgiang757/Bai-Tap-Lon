package com.hieu.controllers;

import com.hieu.models.Auction;
import com.hieu.models.AuctionStatus;
import com.hieu.models.Bid;
import com.hieu.services.AuctionClientService;
import com.hieu.services.BidResult;
import com.hieu.services.MockAuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.FormatUtil;
import com.hieu.util.ViewNavigator;
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
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Điều khiển màn hình chi tiết sản phẩm và đấu giá trực tiếp.
 * Quản lý biểu đồ biến động giá, kiểm soát khóa nhập khi phiên kết thúc,
 * và hiển thị dữ liệu thời gian thực thông qua Observer pattern.
 */
public class RealtimeBiddingController {

    @FXML private Label lblItemName;
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

    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnQuickAdd1;
    @FXML private Button btnQuickAdd2;
    @FXML private Button btnQuickAdd3;

    @FXML private HBox alertBox;
    @FXML private Label lblAlertMessage;

    @FXML private HBox devModeBox;

    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis xAxisTime;
    @FXML private NumberAxis yAxisPrice;
    @FXML private ListView<String> lvRecentBids;

    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private Auction currentAuction;
    private XYChart.Series<String, Number> priceSeries;
    private Consumer<Bid> realtimeBidConsumer;

    @FXML
    public void initialize() {
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
        this.currentAuction = auction;
        renderAuctionDetails();
        loadBidHistory();
        registerRealtimeObserver();
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
                lvRecentBids.getItems().add(0, bid.toString());
            }
        }
    }

    private void registerRealtimeObserver() {
        if (currentAuction == null) return;

        realtimeBidConsumer = bid -> Platform.runLater(() -> {
            updatePriceChart(bid.getFormattedTime(), bid.getAmount());

            lblCurrentPrice.setText(FormatUtil.formatCurrency(bid.getAmount()));
            lblHighestBidder.setText(bid.getBidderUsername());
            lblMinNextBid.setText("Mức đặt tối thiểu: " + FormatUtil.formatCurrency(bid.getAmount() + currentAuction.getMinBidStep()));
            txtBidAmount.setText(String.format("%.0f", bid.getAmount() + currentAuction.getMinBidStep()));

            lvRecentBids.getItems().add(0, bid.toString());
            showAlert("Lượt trả giá mới: " + bid.getBidderUsername() + " vừa đặt " + FormatUtil.formatCurrency(bid.getAmount()), false);
        });

        auctionService.registerBidListener(currentAuction.getId(), realtimeBidConsumer);
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

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(input.trim().replaceAll("[,.]", ""));
        } catch (NumberFormatException e) {
            showAlert("Định dạng số tiền không hợp lệ.", true);
            return;
        }

        BidResult result = auctionService.placeBid(currentAuction.getId(), bidAmount);
        if (result.isSuccess()) {
            showAlert("Đặt giá thành công: " + FormatUtil.formatCurrency(bidAmount), false);
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
            double current = Double.parseDouble(txtBidAmount.getText().trim().replaceAll("[,.]", ""));
            txtBidAmount.setText(String.format("%.0f", current + delta));
        } catch (Exception ex) {
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
            if (currentAuction != null && realtimeBidConsumer != null) {
                auctionService.unregisterBidListener(currentAuction.getId(), realtimeBidConsumer);
            }
            ViewNavigator.loadView("/com/hieu/views/dashboard-view.fxml", "Danh sách phiên đấu giá");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Không thể quay lại danh sách: " + e.getMessage(), true);
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
