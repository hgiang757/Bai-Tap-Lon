package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Bid;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.BidResult;
import com.auction.client.service.mock.MockAuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.util.FormatUtil;
import com.auction.client.util.AppInfo;
import com.auction.client.ui.Icons;
import com.auction.client.ui.ProductImages;
import com.auction.client.ui.RoundedClip;
import javafx.scene.image.ImageView;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.navigation.ViewNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Điều khiển màn hình chi tiết sản phẩm và đấu giá trực tiếp.
 * Quản lý biểu đồ biến động giá, kiểm soát khóa nhập khi phiên kết thúc,
 * và hiển thị dữ liệu thời gian thực thông qua Observer pattern.
 */
public class AuctionRoomController {

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
    @FXML private Label lblProductImageIcon;
    @FXML private ImageView productImage;
    @FXML private StackPane productImageContainer;
    @FXML private Label lblAvailableBalance;
    @FXML private VBox autoBidCard;
    @FXML private Label lblAutoTitle, lblAutoMaxFormat, lblAutoStepFormat, lblAutoError, lblAutoReason, lblAutoStatus;
    @FXML private TextField txtAutoMax, txtAutoIncrement;
    @FXML private Button btnAutoBid;

    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnQuickAdd1;
    @FXML private Button btnQuickAdd2;
    @FXML private Button btnQuickAdd3;

    @FXML private HBox alertBox;
    @FXML private Label lblAlertMessage;

    @FXML private HBox devModeBox;
    @FXML private ScrollPane detailScroll;
    @FXML private VBox pageContent;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxisTime;
    @FXML private NumberAxis yAxisPrice;
    @FXML private Label lblNoBids;
    @FXML private StackPane chartContainer;
    @FXML private ListView<String> lvRecentBids;

    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private Auction currentAuction;
    private static final int MAX_CHART_POINTS = 30;
    private XYChart.Series<Number, Number> priceSeries;
    private Consumer<Bid> realtimeBidConsumer;
    private Timeline countdownTimer;
    private com.auction.client.model.AutoBid myAutoBid;
    private boolean autoBusy;
    private long autoReadVersion;

    @FXML
    public void initialize() {
        lblBrandName.setText(AppInfo.NAME);
        pageContent.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> detailScroll.getViewportBounds().getHeight(), detailScroll.viewportBoundsProperty()));
        SmoothScroll.apply(detailScroll, 3.0);
        com.auction.client.ui.AppFooter.install(detailScroll, pageContent);
        Platform.runLater(detailScroll::requestFocus);
        setupChart();
        setupQuickBidButtons();
        setupAutoBid();
        setupPlaceholder();
        RoundedClip.apply(productImageContainer, 12);
        checkDevMode();
        hideAlert();
        refreshAvailable();
    }

    private void refreshAvailable() {
        var user = auctionService.getCurrentUser();
        if (user == null) { lblAvailableBalance.setText("Số dư khả dụng: vui lòng đăng nhập."); return; }
        javafx.concurrent.Task<Long> task = new javafx.concurrent.Task<>() { protected Long call() { return ServiceRegistry.getWalletService().getAvailable(user.getUsername()); } };
        task.setOnSucceeded(e -> lblAvailableBalance.setText("Số dư khả dụng: " + FormatUtil.formatCurrency(task.getValue())));
        Thread thread = new Thread(task, "wallet-room"); thread.setDaemon(true); thread.start();
    }

    private void setupChart() {
        priceSeries = new XYChart.Series<>();
        priceChart.getData().clear();
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        priceChart.setLegendVisible(false);
        priceChart.setTitle(null);
        xAxisTime.setLabel("");
        yAxisPrice.setLabel("");
        xAxisTime.setAnimated(false);
        yAxisPrice.setAnimated(false);
        xAxisTime.setAutoRanging(false);
        yAxisPrice.setAutoRanging(false);
        xAxisTime.setForceZeroInRange(false);
        yAxisPrice.setForceZeroInRange(false);
        xAxisTime.setTickMarkVisible(false);
        yAxisPrice.setTickMarkVisible(false);
        yAxisPrice.setMinorTickVisible(false);
        xAxisTime.setTickLabelRotation(0);
        xAxisTime.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                if (value == null || priceSeries == null || priceSeries.getData().isEmpty()) {
                    return "";
                }
                int index = Math.max(0, Math.min(priceSeries.getData().size() - 1,
                        (int) Math.round(value.doubleValue())));
                Object time = priceSeries.getData().get(index).getExtraValue();
                return time instanceof LocalDateTime
                        ? FormatUtil.formatChartTime((LocalDateTime) time) : "";
            }

            @Override
            public Number fromString(String value) {
                return 0;
            }
        });
        yAxisPrice.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return value == null ? "" : FormatUtil.formatCompactPriceAxis(value.doubleValue());
            }

            @Override
            public Number fromString(String value) {
                return 0;
            }
        });
    }

    /**
     * Hiển thị dòng thông báo khi danh sách lịch sử đặt giá trống (Sửa lỗi A.3)
     */
    private void setupPlaceholder() {
        Label placeholder = new Label("Chưa có lượt đặt giá");
        placeholder.getStyleClass().add("empty-placeholder");
        lvRecentBids.setPlaceholder(placeholder);
        com.auction.client.ui.RoundedClip.apply(lvRecentBids, 12);
        com.auction.client.ui.RoundedClip.apply(chartContainer, 12);
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
        productImage.setImage(null);
        this.currentAuction = auction;
        txtAutoIncrement.setText(String.format("%.0f", auction.getMinBidStep()));
        refreshAutoBid();
        ViewNavigator.setCurrentAuctionId(auction.getId());
        renderAuctionDetails();
        loadBidHistory();
        registerRealtimeObserver();
        startCountdown();
        updateCountdown();
    }

    private void setProductImageIcon(String category) {
        String iconName = com.auction.client.model.ItemCategory.from(category).getIcon();
        javafx.scene.layout.Region icon = Icons.icon(iconName, 72);
        icon.getStyleClass().add("icon-primary");
        lblProductImageIcon.setGraphic(icon);
        lblProductImageIcon.setContentDisplay(ContentDisplay.TOP);
    }

    private void renderAuctionDetails() {
        if (currentAuction == null) return;

        lblItemName.setText(currentAuction.getItemName());
        lblItemName.setWrapText(true);
        lblCategory.setText(currentAuction.getItemCategory());
        lblSeller.setText("Người bán: " + currentAuction.getSellerUsername());
        lblDescription.setText(currentAuction.getItem() != null ? currentAuction.getItem().getDescription() : "");
        setProductImageIcon(currentAuction.getItemCategory());
        var image = ProductImages.load(ServiceRegistry.getProductService().getImagePath(currentAuction.getId()), 468, 200);
        productImage.setImage(image);
        productImage.setVisible(image != null);
        productImage.setManaged(image != null);
        lblProductImageIcon.setVisible(image == null);
        lblProductImageIcon.setManaged(image == null);
        lblStartingPrice.setText(currentAuction.getFormattedStartingPrice());
        lblCurrentPrice.setText(currentAuction.getFormattedCurrentPrice());
        lblHighestBidder.setText(currentAuction.getHighestBidderUsername());
        lblMinNextBid.setText(FormatUtil.formatCurrency(currentAuction.getMinimumNextBid()));
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

        var user = auctionService.getCurrentUser();
        boolean seller = user != null && user.getUsername().equalsIgnoreCase(currentAuction.getSellerUsername());
        boolean canBid = isRunning && user != null && !seller;
        if (isRunning && user == null) {
            lblStatusExplanation.setText("Vui lòng đăng nhập để đặt giá.");
            lblStatusExplanation.getStyleClass().add("notice-disabled");
        } else if (isRunning && seller) {
            lblStatusExplanation.setText("Bạn không thể đặt giá cho sản phẩm của mình.");
            lblStatusExplanation.getStyleClass().add("notice-disabled");
        }
        txtBidAmount.setDisable(!canBid);
        btnPlaceBid.setDisable(!canBid);
        btnQuickAdd1.setDisable(!canBid);
        btnQuickAdd2.setDisable(!canBid);
        btnQuickAdd3.setDisable(!canBid);

        updateAutoAvailability();
        // Đề xuất mức giá tối thiểu vào ô nhập
        txtBidAmount.setText(String.format("%.0f", currentAuction.getMinimumNextBid()));
    }

    private void loadBidHistory() {
        priceSeries.getData().clear();
        lvRecentBids.getItems().clear();

        List<Bid> bids = auctionService.getBidHistory(currentAuction.getId());
        if (bids.isEmpty()) {
            LocalDateTime startTime = currentAuction.getStartTime() == null
                    ? LocalDateTime.now() : currentAuction.getStartTime();
            addPricePoint(startTime, currentAuction.getStartingPrice(), null, false);
            lblNoBids.setVisible(true);
            lblNoBids.setManaged(true);
        } else {
            List<Bid> chronologicalBids = new ArrayList<>(bids);
            chronologicalBids.sort(Comparator.comparing(Bid::getTimestamp,
                    Comparator.nullsFirst(Comparator.naturalOrder())));
            for (Bid bid : chronologicalBids) {
                LocalDateTime time = bid.getTimestamp() == null ? LocalDateTime.now() : bid.getTimestamp();
                addPricePoint(time, bid.getAmount(), bid.getBidderUsername(), bid.isAuto());
                lvRecentBids.getItems().add(0, formatBid(bid));
            }
            lblNoBids.setVisible(false);
            lblNoBids.setManaged(false);
        }
        updateChartAxes();
    }

    private String formatBid(Bid bid) {
        String maskedName = FormatUtil.maskUsername(bid.getBidderUsername());
        return (bid.isAuto() ? "TỰ ĐỘNG · " : "") + maskedName + "  ·  " + bid.getFormattedTime() + "  ·  "
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
            auctionService.getAuctionById(currentAuction.getId());
            countdownTimer.stop();
            renderAuctionDetails();
            return;
        }
        if (currentAuction.getStatus() == AuctionStatus.OPEN
                && currentAuction.getStartTime() != null
                && !now.isBefore(currentAuction.getStartTime())) {
            auctionService.getAuctionById(currentAuction.getId());
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

    private void addPricePoint(LocalDateTime time, double price, String bidder, boolean auto) {
        XYChart.Data<Number, Number> dataPoint =
                new XYChart.Data<>(priceSeries.getData().size(), price, time);
        priceSeries.getData().add(dataPoint);
        attachPointTooltip(dataPoint, time, price, bidder, auto);
        while (priceSeries.getData().size() > MAX_CHART_POINTS) {
            priceSeries.getData().remove(0);
        }
        for (int index = 0; index < priceSeries.getData().size(); index++) {
            priceSeries.getData().get(index).setXValue(index);
        }
        updateChartAxes();
        styleCurrentPoint();
    }

    private void attachPointTooltip(XYChart.Data<Number, Number> dataPoint,
                                    LocalDateTime time, double price, String bidder, boolean auto) {
        dataPoint.nodeProperty().addListener((observable, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.getStyleClass().add("chart-point");
                Tooltip.install(newNode, new Tooltip(FormatUtil.formatChartTime(time) + " · "
                        + FormatUtil.formatCurrency(price) + " · " + FormatUtil.maskUsername(bidder) + (auto ? " · TỰ ĐỘNG" : "")));
                styleCurrentPoint();
            }
        });
        if (dataPoint.getNode() != null) {
            dataPoint.getNode().getStyleClass().add("chart-point");
            Tooltip.install(dataPoint.getNode(), new Tooltip(FormatUtil.formatChartTime(time) + " · "
                    + FormatUtil.formatCurrency(price) + " · " + FormatUtil.maskUsername(bidder) + (auto ? " · TỰ ĐỘNG" : "")));
        }
    }

    private void styleCurrentPoint() {
        List<XYChart.Data<Number, Number>> data = priceSeries.getData();
        for (int index = 0; index < data.size(); index++) {
            Node node = data.get(index).getNode();
            if (node == null) {
                continue;
            }
            node.getStyleClass().remove("chart-point-current");
            if (index == data.size() - 1 && !node.getStyleClass().contains("chart-point-current")) {
                node.getStyleClass().add("chart-point-current");
            }
        }
    }

    private void updateChartAxes() {
        if (priceSeries == null || priceSeries.getData().isEmpty()) {
            return;
        }
        double minPrice = Double.POSITIVE_INFINITY;
        double maxPrice = Double.NEGATIVE_INFINITY;
        for (XYChart.Data<Number, Number> point : priceSeries.getData()) {
            double price = point.getYValue().doubleValue();
            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
        }

        int count = priceSeries.getData().size();
        if (count == 1) {
            xAxisTime.setLowerBound(-1);
            xAxisTime.setUpperBound(1);
            xAxisTime.setTickUnit(2);
        } else {
            xAxisTime.setLowerBound(0);
            xAxisTime.setUpperBound(count - 1);
            xAxisTime.setTickUnit(Math.max(1, Math.ceil((count - 1) / 5.0)));
        }

        double priceRange = maxPrice - minPrice;
        if (priceRange <= 0) {
            priceRange = Math.max(Math.abs(maxPrice) * 0.1, 1);
        }
        double lowerBound = Math.max(0, minPrice - priceRange * 0.05);
        double upperBound = maxPrice + priceRange * 0.1;
        yAxisPrice.setLowerBound(lowerBound);
        yAxisPrice.setUpperBound(upperBound);
        yAxisPrice.setTickUnit((upperBound - lowerBound) / 4);
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
            addPricePoint(bid.getTimestamp() == null ? LocalDateTime.now() : bid.getTimestamp(),
                    bid.getAmount(), bid.getBidderUsername(), bid.isAuto());
            lblNoBids.setVisible(false);
            lblNoBids.setManaged(false);
            lblCurrentPrice.setText(FormatUtil.formatCurrency(bid.getAmount()));
            lblHighestBidder.setText(bid.getBidderUsername());
            lblMinNextBid.setText(FormatUtil.formatCurrency(bid.getAmount() + currentAuction.getMinBidStep()));
            txtBidAmount.setText(String.format("%.0f", bid.getAmount() + currentAuction.getMinBidStep()));
            lvRecentBids.getItems().add(0, formatBid(bid));
            refreshAvailable();
            refreshAutoBid();
            var user = auctionService.getCurrentUser();
            String notification = bid.isAuto() && user != null && user.getUsername().equalsIgnoreCase(bid.getBidderUsername())
                    ? "Auto-bid của bạn vừa đặt " + FormatUtil.formatCurrency(bid.getAmount())
                    : "Lượt trả giá mới: " + bid.getBidderUsername() + " vừa đặt " + FormatUtil.formatCurrency(bid.getAmount());
            showAlert(notification, false);
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

        String id = currentAuction.getId();
        btnPlaceBid.setDisable(true);
        javafx.concurrent.Task<BidResult> task = new javafx.concurrent.Task<>() { protected BidResult call() { return auctionService.placeBid(id, bidAmount); } };
        task.setOnSucceeded(e -> {
            renderAuctionDetails(); refreshAvailable();
            BidResult result = task.getValue();
            if (result.isSuccess()) showAlert("Đặt giá thành công: " + FormatUtil.formatCurrency((double) bidAmount), false);
            else showAlert(result.getMessage(), true);
        });
        task.setOnFailed(e -> { renderAuctionDetails(); showAlert("Không thể đặt giá. Vui lòng thử lại.", true); });
        Thread thread = new Thread(task, "place-bid"); thread.setDaemon(true); thread.start();
    }

    private void setupAutoBid() {
        var icon = Icons.icon("bolt", 20); icon.getStyleClass().add("icon-primary"); lblAutoTitle.setGraphic(icon);
        txtAutoMax.textProperty().addListener((o,a,b) -> formatAutoInput(b, lblAutoMaxFormat));
        txtAutoIncrement.textProperty().addListener((o,a,b) -> formatAutoInput(b, lblAutoStepFormat));
    }

    private void formatAutoInput(String text, Label label) {
        try { label.setText(FormatUtil.formatCurrency(Long.parseLong(text.replaceAll("[,. ]", "")))); }
        catch (RuntimeException ex) { label.setText(""); }
        lblAutoError.setText("");
    }

    private void updateAutoAvailability() {
        if (currentAuction == null) return;
        var user = auctionService.getCurrentUser();
        String reason = currentAuction.getStatus() != AuctionStatus.RUNNING ? "Phiên chưa diễn ra hoặc đã kết thúc."
                : user == null ? "Vui lòng đăng nhập để đặt giá."
                : user.getUsername().equalsIgnoreCase(currentAuction.getSellerUsername()) ? "Bạn không thể đặt giá cho sản phẩm của mình." : "";
        lblAutoReason.setText(reason);
        boolean disabled = !reason.isEmpty() || autoBusy;
        txtAutoMax.setDisable(disabled); txtAutoIncrement.setDisable(disabled); btnAutoBid.setDisable(disabled);
    }

    private void refreshAutoBid() {
        if (currentAuction == null || auctionService.getCurrentUser() == null) { myAutoBid = null; updateAutoAvailability(); return; }
        String id = currentAuction.getId();
        long version = ++autoReadVersion;
        javafx.concurrent.Task<com.auction.client.model.AutoBid> task = new javafx.concurrent.Task<>() {
            protected com.auction.client.model.AutoBid call() { return ServiceRegistry.getAutoBidService().getMyAutoBid(id); }
        };
        task.setOnSucceeded(e -> { if (currentAuction == null || !id.equals(currentAuction.getId()) || version != autoReadVersion) return; myAutoBid=task.getValue(); renderAutoBid(); });
        Thread thread = new Thread(task, "auto-bid-read"); thread.setDaemon(true); thread.start();
    }

    private void renderAutoBid() {
        boolean active = myAutoBid != null && myAutoBid.isActive();
        btnAutoBid.setText(active ? "Tắt" : "Bật đặt giá tự động");
        lblAutoStatus.getStyleClass().removeAll("alert-success", "alert-error");
        lblAutoStatus.getStyleClass().add(active ? "alert-success" : "alert-error");
        if (active) {
            lblAutoStatus.setText("Đang bật · tối đa " + FormatUtil.formatCurrency(myAutoBid.getMaxBid()) + " · bước "
                    + FormatUtil.formatCurrency(myAutoBid.getIncrement()) + " · đã tự đặt " + myAutoBid.getAutoCount() + " lần");
        } else if (myAutoBid != null && myAutoBid.isLimitReached()) {
            lblAutoStatus.setText("Đã đạt giá tối đa — bạn không còn dẫn đầu nếu có người trả cao hơn.");
        } else lblAutoStatus.setText("");
        lblAutoStatus.setVisible(!lblAutoStatus.getText().isEmpty()); lblAutoStatus.setManaged(lblAutoStatus.isVisible());
        updateAutoAvailability();
    }

    @FXML private void handleAutoBid() {
        if (currentAuction == null || autoBusy) return;
        String id=currentAuction.getId(); boolean cancel=myAutoBid != null && myAutoBid.isActive();
        long max=0, step=0;
        if (!cancel) {
            try {
                String m=txtAutoMax.getText().replaceAll("[,. ]", ""), i=txtAutoIncrement.getText().replaceAll("[,. ]", "");
                if (!m.matches("[0-9]+") || !i.matches("[0-9]+")) throw new NumberFormatException();
                max=Long.parseLong(m); step=Long.parseLong(i);
            } catch (RuntimeException ex) { lblAutoError.setText("Giá và bước giá phải là số nguyên dương."); return; }
        }
        final long maxValue=max, stepValue=step;
        autoBusy=true; updateAutoAvailability(); lblAutoError.setText("");
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() { protected Void call() {
            if (cancel) ServiceRegistry.getAutoBidService().cancelAutoBid(id);
            else ServiceRegistry.getAutoBidService().registerAutoBid(id, maxValue, stepValue);
            return null;
        } };
        task.setOnSucceeded(e -> { autoBusy=false; refreshAutoBid(); refreshAvailable(); });
        task.setOnFailed(e -> { autoBusy=false; lblAutoError.setText(task.getException().getMessage()); updateAutoAvailability(); });
        Thread thread = new Thread(task, "auto-bid-update"); thread.setDaemon(true); thread.start();
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

    public void dispose() {
        if (countdownTimer != null) countdownTimer.stop();
        if (currentAuction != null && realtimeBidConsumer != null) {
            auctionService.unregisterBidListener(currentAuction.getId(), realtimeBidConsumer);
            realtimeBidConsumer = null;
        }
    }

    @FXML
    private void handleBackToHome() {
        try {
            ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD);
        } catch (IOException e) {
            showDialog("Không thể mở danh sách phiên đấu giá.");
        }
    }

    private void showDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
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
