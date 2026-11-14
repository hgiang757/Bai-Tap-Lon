package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Bid;
import com.auction.client.navigation.DisposableView;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.BidSubmissionTracker;
import com.auction.client.service.BidSubmissionTracker.Phase;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.ui.AuctionBidHistoryView;
import com.auction.client.ui.Icons;
import com.auction.client.ui.ProductImages;
import com.auction.client.ui.RoundedClip;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.util.AppInfo;
import com.auction.client.util.FormatUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Điều khiển màn hình chi tiết sản phẩm và đấu giá trực tiếp. Quản lý biểu đồ biến động giá, kiểm
 * soát khóa nhập khi phiên kết thúc, và hiển thị dữ liệu thời gian thực thông qua Observer pattern.
 */
public class AuctionRoomController implements DisposableView {
  private ViewTasks tasks = new ViewTasks();
  private ViewTasks imageTasks = ViewTasks.images();
  private final BidSubmissionTracker submissions = ServiceRegistry.getBidSubmissions();
  private BidSubmissionTracker.Key bidKey;
  private boolean disposed;
  private boolean bidBusy;
  private boolean refreshing;
  private boolean refreshAgain;
  private boolean auctionReady;
  private String requestedAuctionId;

  public boolean isReady() {
    return auctionReady && !refreshing;
  }

  private final java.util.Set<String> notifiedBids = new java.util.HashSet<>();

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
  @FXML private Label lblBidState;
  @FXML private VBox autoBidCard;
  @FXML private Label lblAutoTitle;
  @FXML private Label lblAutoMaxFormat;
  @FXML private Label lblAutoStepFormat;
  @FXML private Label lblAutoError;
  @FXML private Label lblAutoReason;
  @FXML private Label lblAutoStatus;
  @FXML private TextField txtAutoMax;
  @FXML private TextField txtAutoIncrement;
  @FXML private Button btnAutoBid;

  @FXML private TextField txtBidAmount;
  @FXML private Button btnPlaceBid;
  @FXML private Button btnQuickAdd1;
  @FXML private Button btnQuickAdd2;
  @FXML private Button btnQuickAdd3;

  @FXML private HBox alertBox;
  @FXML private Label lblAlertMessage;

  @FXML private ScrollPane detailScroll;
  @FXML private VBox pageContent;

  @FXML private LineChart<Number, Number> priceChart;
  @FXML private NumberAxis timeAxis;
  @FXML private NumberAxis priceAxis;
  @FXML private Label lblNoBids;
  @FXML private StackPane chartContainer;
  @FXML private ListView<String> lvRecentBids;

  private final AuctionClientService auctionService = ServiceRegistry.getService();
  private Auction currentAuction;
  private AuctionBidHistoryView historyView;
  private Consumer<Bid> realtimeBidConsumer;
  private Timeline countdownTimer;
  private com.auction.client.model.AutoBid myAutoBid;
  private boolean autoBusy;
  private final Consumer<String> serverChanges =
      type ->
          tasks.dispatch(
              () -> {
                if (!"BID_PLACED".equals(type) && requestedAuctionId != null) {
                  refreshAuction();
                  refreshAvailable();
                }
              });

  /** Initializes the controls after FXML injection. */
  @FXML
  public void initialize() {
    lblBrandName.setText(AppInfo.NAME);
    SmoothScroll.apply(detailScroll, 3.0);
    com.auction.client.ui.AppFooter.install(detailScroll, pageContent);
    tasks.dispatch(detailScroll::requestFocus);
    auctionService.addChangeListener(serverChanges);
    historyView =
        new AuctionBidHistoryView(priceChart, timeAxis, priceAxis, lblNoBids, lvRecentBids);
    RoundedClip.apply(chartContainer, 12);
    setupQuickBidButtons();
    txtBidAmount.setOnAction(event -> handlePlaceBid());
    setupAutoBid();
    RoundedClip.apply(productImageContainer, 12);
    hideAlert();
    refreshAvailable();
  }

  private void refreshAvailable() {
    var user = auctionService.getCurrentUser();
    if (user == null) {
      lblAvailableBalance.setText("Số tiền có thể sử dụng: vui lòng đăng nhập.");
      return;
    }
    tasks.submit(
        "balance",
        () -> ServiceRegistry.getWalletService().getAvailable(user.getUsername()),
        balance ->
            lblAvailableBalance.setText(
                "Số tiền có thể sử dụng: " + FormatUtil.formatCurrency(balance)),
        error -> lblAvailableBalance.setText("Không thể tải số tiền có thể sử dụng."));
  }

  /** Nạp dữ liệu phiên đấu giá vào màn hình. */
  public void setAuction(Auction auction) {
    java.util.Objects.requireNonNull(auction, "auction");
    beginAuction(auction.getId(), auction);
  }

  /** Khôi phục phiên theo ID từ lịch sử điều hướng, tải service trên luồng nền. */
  public void loadAuction(String id) {
    beginAuction(id, null);
  }

  private void beginAuction(String id, Auction auction) {
    if (disposed) {
      return;
    }
    detachAuction();
    tasks.close();
    imageTasks.close();
    tasks = new ViewTasks();
    imageTasks = ViewTasks.images();
    requestedAuctionId = java.util.Objects.requireNonNull(id);
    var user = auctionService.getCurrentUser();
    bidKey = new BidSubmissionTracker.Key(user == null ? "" : user.getUsername(), id);
    currentAuction = auction;
    bidBusy = autoBusy = refreshing = refreshAgain = auctionReady = false;
    myAutoBid = null;
    notifiedBids.clear();
    productImage.setImage(null);
    productImage.setVisible(false);
    productImage.setManaged(false);
    lblProductImageIcon.setVisible(true);
    lblProductImageIcon.setManaged(true);
    lblCountdown.getStyleClass().remove("countdown-label-urgent");
    if (!lblCountdown.getStyleClass().contains("countdown-label")) {
      lblCountdown.getStyleClass().add("countdown-label");
    }
    historyView.clear();
    ViewNavigator.setCurrentAuctionId(id);
    renderAuctionDetails();
    showAlert("Đang tải dữ liệu phiên…", false);
    registerRealtimeObserver();
    refreshAuction();
    imageTasks.submit(
        "image",
        () -> ProductImages.load(ServiceRegistry.getProductService().getImagePath(id), 468, 200),
        image -> {
          productImage.setImage(image);
          productImage.setVisible(image != null);
          productImage.setManaged(image != null);
          lblProductImageIcon.setVisible(image == null);
          lblProductImageIcon.setManaged(image == null);
        },
        error -> {
          /* Giữ icon thay ảnh. */
        });
    refreshAvailable();
    startCountdown();
  }

  private record RoomData(
      Auction auction, List<Bid> history, BidSubmissionTracker.ReadStamp stamp) {}

  private void refreshAuction() {
    if (disposed || requestedAuctionId == null) {
      return;
    }
    if (refreshing) {
      refreshAgain = true;
      return;
    }
    refreshing = true;
    renderAuctionDetails();
    String id = requestedAuctionId;
    var key = bidKey;
    var stamp = submissions.reading(key);
    tasks.submit(
        "room",
        () -> {
          if (submissions.phase(key) == Phase.UNKNOWN && auctionService.recoverPendingCommands()) {
            submissions.reconciled(key);
          }
          var readStamp = submissions.reading(key);
          var snapshot = auctionService.getRoomSnapshot(id);
          Auction auction = snapshot.auction();
          if (auction == null || !id.equals(auction.getId())) {
            throw new IllegalStateException("Không tìm thấy đúng phiên đấu giá " + id);
          }
          return new RoomData(auction, snapshot.history(), readStamp);
        },
        data -> {
          if (!id.equals(requestedAuctionId)) {
            return;
          }
          // Lệnh hoặc phản hồi đã đổi trong lúc đọc: bỏ snapshot cũ và đọc lại sau đó.
          if (!submissions.isCurrent(data.stamp())) {
            refreshing = false;
            refreshAgain = false;
            refreshAuction();
            return;
          }
          currentAuction = data.auction();
          auctionReady = true;
          historyView.render(currentAuction, data.history(), auctionService.serverNow());
          updateCountdown();
          submissions.refreshed(data.stamp());
          refreshing = false;
          renderAuctionDetails();
          if ("Đang tải dữ liệu phiên…".equals(lblAlertMessage.getText())) {
            hideAlert();
          }
          refreshAutoBid();
          if (refreshAgain) {
            refreshAgain = false;
            refreshAuction();
          }
        },
        error -> {
          if (!id.equals(requestedAuctionId)) {
            return;
          }
          if (!submissions.isCurrent(stamp)) {
            refreshing = false;
            refreshAgain = false;
            refreshAuction();
            return;
          }
          refreshing = false;
          refreshAgain = false;
          auctionReady = false;
          renderAuctionDetails();
          showAlert(
              "Không thể tải phiên: "
                  + error.getMessage()
                  + ". Bấm Tải lại để thử lại; các thao tác giá vẫn khóa.",
              true);
        });
  }

  private Phase bidPhase() {
    return bidKey == null ? Phase.READY : submissions.phase(bidKey);
  }

  private boolean canChangePrice() {
    return !disposed
        && auctionReady
        && !refreshing
        && !bidBusy
        && !autoBusy
        && bidPhase() == Phase.READY;
  }

  private String priceLockReason() {
    return switch (bidPhase()) {
      case SENDING ->
          autoBusy
              ? "Đang cập nhật auto-bid; vui lòng chờ phản hồi."
              : "Đang gửi lệnh đặt giá; vui lòng chờ phản hồi.";
      case AWAITING_REFRESH ->
          "Đã nhận phản hồi đặt giá; cần tải lại dữ liệu sau phản hồi trước khi đặt" + " tiếp.";
      case UNKNOWN ->
          "Kết quả lượt đặt giá trước chưa xác định. Tải lại chỉ cập nhật lịch sử, không"
              + " chứng minh lệnh đã hoàn tất. Bấm Tải lại để đối soát với máy chủ trước"
              + " khi đặt tiếp.";
      case READY ->
          refreshing
              ? "Đang tải lại dữ liệu phiên…"
              : !auctionReady
                  ? "Chưa có dữ liệu mới. Bấm Tải lại để thử lại."
                  : autoBusy ? "Đang cập nhật auto-bid…" : "";
    };
  }

  @FXML
  private void handleReload() {
    refreshAuction();
    refreshAvailable();
  }

  private void setProductImageIcon(String category) {
    String iconName = com.auction.client.model.ItemCategory.from(category).getIcon();
    javafx.scene.layout.Region icon = Icons.icon(iconName, 72);
    icon.getStyleClass().add("icon-primary");
    lblProductImageIcon.setGraphic(icon);
    lblProductImageIcon.setContentDisplay(ContentDisplay.TOP);
  }

  private void renderAuctionDetails() {
    if (currentAuction == null) {
      txtBidAmount.setDisable(true);
      btnPlaceBid.setDisable(true);
      btnQuickAdd1.setDisable(true);
      btnQuickAdd2.setDisable(true);
      btnQuickAdd3.setDisable(true);
      txtAutoMax.setDisable(true);
      txtAutoIncrement.setDisable(true);
      btnAutoBid.setDisable(true);
      lblBidState.setText(priceLockReason());
      lblBidState.setVisible(true);
      lblBidState.setManaged(true);
      return;
    }

    lblItemName.setText(currentAuction.getItemName());
    lblItemName.setWrapText(true);
    lblCategory.setText(currentAuction.getItemCategory());
    lblSeller.setText("Người bán: " + currentAuction.getSellerUsername());
    lblDescription.setText(
        currentAuction.getItem() != null ? currentAuction.getItem().getDescription() : "");
    setProductImageIcon(currentAuction.getItemCategory());
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
      lblStatus
          .getStyleClass()
          .add(currentAuction.getStatus() == AuctionStatus.OPEN ? "badge-open" : "badge-finished");
      if (currentAuction.getStatus() == AuctionStatus.FINISHED) {
        lblStatusExplanation.setText("Phiên đấu giá đã kết thúc. Không thể đặt thêm giá.");
      } else if (currentAuction.getStatus() == AuctionStatus.CANCELED) {
        lblStatusExplanation.setText(
            "Phiên đã bị hủy. Tiền tạm giữ được giải phóng. "
                + currentAuction.getCancellationReason());
      } else if (currentAuction.getStatus() == AuctionStatus.PAID) {
        lblStatusExplanation.setText(
            "Người thắng: "
                + currentAuction.getHighestBidderUsername()
                + ". Đã thanh toán bằng ví giả lập cho người bán.");
      } else {
        lblStatusExplanation.setText("Phiên đấu giá chưa mở. Vui lòng chờ đến giờ bắt đầu.");
      }
      if (!lblStatusExplanation.getStyleClass().contains("notice-disabled")) {
        lblStatusExplanation.getStyleClass().add("notice-disabled");
      }
    }

    var user = auctionService.getCurrentUser();
    boolean seller =
        user != null && user.getUsername().equalsIgnoreCase(currentAuction.getSellerUsername());
    boolean canBid =
        canChangePrice()
            && isRunning
            && user != null
            && !seller
            && currentAuction.getEndTime() != null
            && auctionService.serverNow().isBefore(currentAuction.getEndTime());
    if (isRunning && user == null) {
      lblStatusExplanation.setText("Vui lòng đăng nhập để đặt giá.");
      if (!lblStatusExplanation.getStyleClass().contains("notice-disabled")) {
        lblStatusExplanation.getStyleClass().add("notice-disabled");
      }
    } else if (isRunning && seller) {
      lblStatusExplanation.setText("Bạn không thể đặt giá cho sản phẩm của mình.");
      if (!lblStatusExplanation.getStyleClass().contains("notice-disabled")) {
        lblStatusExplanation.getStyleClass().add("notice-disabled");
      }
    }
    txtBidAmount.setDisable(!canBid);
    btnPlaceBid.setDisable(!canBid);
    btnPlaceBid.setText(bidPhase() == Phase.SENDING ? "Đang gửi giá…" : "Đặt giá");
    lblBidState.setText(priceLockReason());
    lblBidState.setVisible(!lblBidState.getText().isEmpty());
    lblBidState.setManaged(lblBidState.isVisible());
    btnQuickAdd1.setDisable(!canBid);
    btnQuickAdd2.setDisable(!canBid);
    btnQuickAdd3.setDisable(!canBid);

    updateAutoAvailability();
    // Đề xuất mức giá tối thiểu vào ô nhập
    if (txtBidAmount.getText().isBlank() || !txtBidAmount.isFocused()) {
      txtBidAmount.setText(String.format("%.0f", currentAuction.getMinimumNextBid()));
    }
  }

  private void registerRealtimeObserver() {
    var scope = tasks;
    realtimeBidConsumer = bid -> scope.dispatch(() -> applyBidNotification(bid));
    auctionService.registerBidListener(requestedAuctionId, realtimeBidConsumer);
  }

  // ======= Countdown Timer =======

  /** Khởi động countdown timer đếm ngược theo giây đến thời điểm kết thúc phiên. */
  private void startCountdown() {
    if (countdownTimer != null) {
      countdownTimer.stop();
    }
    countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
    countdownTimer.setCycleCount(Timeline.INDEFINITE);
    countdownTimer.play();
  }

  /** Cập nhật label đếm ngược mỗi giây. Chuyển sang màu đỏ khi còn dưới 5 phút. */
  private void updateCountdown() {
    if (currentAuction == null || currentAuction.getEndTime() == null) {
      return;
    }
    LocalDateTime now = auctionService.serverNow();
    if (currentAuction.getStatus() != AuctionStatus.RUNNING
        && currentAuction.getStatus() != AuctionStatus.OPEN) {
      lblCountdown.setText(currentAuction.getStatus().getDescription());
      countdownTimer.stop();
      return;
    }
    boolean upcoming = currentAuction.getStatus() == AuctionStatus.OPEN;
    LocalDateTime end =
        upcoming && currentAuction.getStartTime() != null
            ? currentAuction.getStartTime()
            : currentAuction.getEndTime();
    if (!now.isBefore(end)) {
      lblCountdown.setText("Đang cập nhật trạng thái…");
      txtBidAmount.setDisable(true);
      btnPlaceBid.setDisable(true);
      if (!refreshing) {
        refreshAuction();
      }
      return;
    }
    long seconds = ChronoUnit.SECONDS.between(now, end);
    long hours = seconds / 3600;
    long mins = (seconds % 3600) / 60;
    long secs = seconds % 60;
    String text = String.format("%02d:%02d:%02d", hours, mins, secs);
    lblCountdown.setText(upcoming ? "Mở sau " + text : text);
    // Màu đỏ khi còn < 5 phút
    lblCountdown.getStyleClass().removeAll("countdown-label", "countdown-label-urgent");
    lblCountdown
        .getStyleClass()
        .add(!upcoming && seconds < 300 ? "countdown-label-urgent" : "countdown-label");
  }

  /** Observer của service có thể gọi từ luồng nền; callback cũ bị chặn sau dispose. */
  public void onBidReceived(Bid bid) {
    tasks.dispatch(() -> applyBidNotification(bid));
  }

  private void applyBidNotification(Bid bid) {
    if (disposed
        || bid == null
        || !java.util.Objects.equals(requestedAuctionId, bid.getAuctionId())) {
      return;
    }
    if (bid.getId() != null && !notifiedBids.add(bid.getId())) {
      return;
    }
    refreshAuction();
    refreshAvailable();
    if (bidPhase() != Phase.UNKNOWN) {
      showAlert(
          "Lượt trả giá mới: "
              + FormatUtil.maskUsername(bid.getBidderUsername())
              + " vừa đặt "
              + FormatUtil.formatCurrency(bid.getAmount()),
          false);
    }
  }

  @FXML
  private void handlePlaceBid() {
    if (!canChangePrice()) {
      return;
    }
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
      showAlert(
          "Mức đặt tối thiểu là "
              + FormatUtil.formatCurrency(currentAuction.getMinimumNextBid())
              + ".",
          true);
      return;
    }

    String id = currentAuction.getId();
    var ticket = submissions.begin(bidKey);
    bidBusy = true;
    renderAuctionDetails();
    boolean accepted =
        tasks.submit(
            "bid",
            () -> {
              // Theo dõi kết quả ngoài callback UI: đóng màn hình không làm quên lệnh
              // đang gửi.
              try {
                var result =
                    java.util.Objects.requireNonNull(
                        auctionService.placeBid(id, bidAmount), "Thiếu phản hồi đặt giá");
                submissions.answered(ticket);
                return result;
              } catch (Exception error) {
                submissions.uncertain(ticket);
                throw error;
              }
            },
            result -> {
              bidBusy = false;
              refreshAuction();
              refreshAvailable();
              if (result.isSuccess()) {
                showAlert(
                    "Đặt giá thành công: " + FormatUtil.formatCurrency((double) bidAmount), false);
              } else {
                showAlert(result.getMessage(), true);
              }
            },
            error -> {
              bidBusy = false;
              if (error instanceof ViewTasks.NotSubmittedException) {
                submissions.notSubmitted(ticket);
                renderAuctionDetails();
                showAlert(error.getMessage(), true);
              } else {
                refreshAuction();
                showAlert(
                    "Kết quả đặt giá chưa xác định; lệnh không được tự gửi lại."
                        + " Đọc lại lịch sử không thay thế việc tra trạng thái"
                        + " lệnh.",
                    true);
              }
            });
    // Rejection xảy ra trước khi work chạy, kể cả callback bị chặn sau khi rời màn hình.
    if (!accepted) {
      submissions.notSubmitted(ticket);
      bidBusy = false;
      renderAuctionDetails();
    }
  }

  private void setupAutoBid() {
    var icon = Icons.icon("bolt", 20);
    icon.getStyleClass().add("icon-primary");
    lblAutoTitle.setGraphic(icon);
    txtAutoMax.textProperty().addListener((o, a, b) -> formatAutoInput(b, lblAutoMaxFormat));
    txtAutoIncrement.textProperty().addListener((o, a, b) -> formatAutoInput(b, lblAutoStepFormat));
  }

  private void formatAutoInput(String text, Label label) {
    try {
      label.setText(FormatUtil.formatCurrency(Long.parseLong(text.replaceAll("[,. ]", ""))));
    } catch (RuntimeException ex) {
      label.setText("");
    }
    lblAutoError.setText("");
  }

  private void updateAutoAvailability() {
    if (currentAuction == null) {
      return;
    }
    var user = auctionService.getCurrentUser();
    String reason =
        currentAuction.getStatus() != AuctionStatus.RUNNING
            ? "Phiên chưa diễn ra hoặc đã kết thúc."
            : user == null
                ? "Vui lòng đăng nhập để đặt giá."
                : user.getUsername().equalsIgnoreCase(currentAuction.getSellerUsername())
                    ? "Bạn không thể đặt giá cho sản phẩm của mình."
                    : "";
    if (reason.isEmpty() && !canChangePrice()) {
      reason = priceLockReason();
    }
    lblAutoReason.setText(reason);
    boolean disabled = !reason.isEmpty() || !canChangePrice();
    txtAutoMax.setDisable(disabled);
    txtAutoIncrement.setDisable(disabled);
    btnAutoBid.setDisable(disabled);
  }

  private void refreshAutoBid() {
    if (currentAuction == null || auctionService.getCurrentUser() == null) {
      myAutoBid = null;
      updateAutoAvailability();
      return;
    }
    String id = currentAuction.getId();
    tasks.submit(
        "auto-read",
        () -> ServiceRegistry.getAutoBidService().getMyAutoBid(id),
        value -> {
          myAutoBid = value;
          renderAutoBid();
        },
        error -> {
          lblAutoError.setText("Không thể tải cấu hình auto-bid: " + error.getMessage());
        });
  }

  private void renderAutoBid() {
    boolean active = myAutoBid != null && myAutoBid.isActive();
    btnAutoBid.setText(active ? "Tắt" : "Bật đặt giá tự động");
    lblAutoStatus.getStyleClass().removeAll("alert-success", "alert-error");
    lblAutoStatus.getStyleClass().add(active ? "alert-success" : "alert-error");
    if (active) {
      lblAutoStatus.setText(
          "Đang bật · tối đa "
              + FormatUtil.formatCurrency(myAutoBid.getMaxBid())
              + " · bước "
              + FormatUtil.formatCurrency(myAutoBid.getIncrement())
              + " · đã tự đặt "
              + myAutoBid.getAutoCount()
              + " lần");
    } else if (myAutoBid != null && myAutoBid.isLimitReached()) {
      lblAutoStatus.setText("Đã đạt giá tối đa — bạn không còn dẫn đầu nếu có người trả cao hơn.");
    } else {
      lblAutoStatus.setText("");
    }
    lblAutoStatus.setVisible(!lblAutoStatus.getText().isEmpty());
    lblAutoStatus.setManaged(lblAutoStatus.isVisible());
    updateAutoAvailability();
  }

  @FXML
  private void handleAutoBid() {
    if (currentAuction == null || !canChangePrice()) {
      return;
    }
    final String id = currentAuction.getId();
    boolean cancel = myAutoBid != null && myAutoBid.isActive();
    long max = 0;
    long step = 0;
    if (!cancel) {
      try {
        String m = txtAutoMax.getText().replaceAll("[,. ]", "");
        String i = txtAutoIncrement.getText().replaceAll("[,. ]", "");
        if (!m.matches("[0-9]+") || !i.matches("[0-9]+")) {
          throw new NumberFormatException();
        }
        max = Long.parseLong(m);
        step = Long.parseLong(i);
      } catch (RuntimeException ex) {
        lblAutoError.setText("Giá và bước giá phải là số nguyên dương.");
        return;
      }
    }
    final long maxValue = max;
    final long stepValue = step;
    final var ticket = submissions.begin(bidKey);
    final var autoService = ServiceRegistry.getAutoBidService();
    autoBusy = true;
    renderAuctionDetails();
    lblAutoError.setText("");
    boolean accepted =
        tasks.submit(
            "auto-write",
            () -> {
              try {
                if (cancel) {
                  autoService.cancelAutoBid(id);
                } else {
                  autoService.registerAutoBid(id, maxValue, stepValue);
                }
                submissions.answered(ticket);
                return null;
              } catch (Exception error) {
                // AutoBidException chỉ mô tả từ chối cấu hình trước khi ghi.
                if (error instanceof com.auction.client.service.server.ServerError
                    || error instanceof com.auction.client.service.AutoBidException) {
                  submissions.answered(ticket);
                } else {
                  submissions.uncertain(ticket);
                }
                throw error;
              }
            },
            ignored -> {
              autoBusy = false;
              refreshAutoBid();
              refreshAvailable();
              refreshAuction();
            },
            error -> {
              autoBusy = false;
              if (error instanceof ViewTasks.NotSubmittedException) {
                submissions.notSubmitted(ticket);
              }
              lblAutoError.setText(error.getMessage());
              renderAuctionDetails();
              if (!(error instanceof ViewTasks.NotSubmittedException)) {
                refreshAuction();
              }
            });
    if (!accepted) {
      submissions.notSubmitted(ticket);
      autoBusy = false;
      renderAuctionDetails();
    }
  }

  private void setupQuickBidButtons() {
    btnQuickAdd1.setOnAction(e -> addQuickBid(1_000_000));
    btnQuickAdd2.setOnAction(e -> addQuickBid(5_000_000));
    btnQuickAdd3.setOnAction(e -> addQuickBid(10_000_000));
  }

  private void addQuickBid(double delta) {
    if (currentAuction == null || !canChangePrice()) {
      return;
    }
    try {
      long current = Long.parseLong(txtBidAmount.getText().trim().replaceAll("[,.]", ""));
      txtBidAmount.setText(String.format("%.0f", current + delta));
    } catch (NumberFormatException ex) {
      txtBidAmount.setText(String.format("%.0f", currentAuction.getMinimumNextBid() + delta));
    }
  }

  private void detachAuction() {
    if (countdownTimer != null) {
      countdownTimer.stop();
    }
    if (requestedAuctionId != null && realtimeBidConsumer != null) {
      auctionService.unregisterBidListener(requestedAuctionId, realtimeBidConsumer);
      realtimeBidConsumer = null;
    }
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    auctionService.removeChangeListener(serverChanges);
    tasks.close();
    imageTasks.close();
    detachAuction();
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
    ViewNavigator.prepareDialog(alert);
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
