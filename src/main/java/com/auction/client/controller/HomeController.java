package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.ItemCategory;
import com.auction.client.navigation.DisposableView;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.ui.Icons;
import com.auction.client.ui.ProductImages;
import com.auction.client.ui.RoundedClip;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.util.AppInfo;
import com.auction.client.util.FormatUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Danh sách phiên đấu giá, tìm kiếm và lọc theo danh mục. */
public class HomeController implements DisposableView {
  private final ViewTasks tasks = new ViewTasks();
  private final ViewTasks imageTasks = ViewTasks.images();
  private boolean loading;
  private boolean loadAgain;
  private final java.util.function.Consumer<String> serverChanges =
      type -> tasks.dispatch(this::loadAuctions);
  private boolean loadFailed;
  private long cardsVersion;

  public boolean isLoading() {
    return loading;
  }

  /* ======= FXML Bindings ======= */
  @FXML private TextField txtSearch;
  @FXML private ScrollPane dashboardScroll;
  @FXML private VBox pageContent;
  @FXML private Label lblLogoIcon;
  @FXML private Label lblBrandName;
  @FXML private Label lblSearchIcon;
  @FXML private Button chipWatch;
  @FXML private Button chipArt;
  @FXML private Button chipHome;
  @FXML private Button chipCar;
  @FXML private Button chipLaptop;
  @FXML private Button chipOther;
  @FXML private Label lblRunningHeader;

  // Grid & sidebar
  @FXML private FlowPane auctionGrid;
  @FXML private FlowPane upcomingGrid;
  @FXML private FlowPane finishedGrid;
  @FXML private Label lblFinishedEmpty;
  @FXML private Label lblNoResults;
  @FXML private Label lblUpcomingEmpty;
  @FXML private VBox runningPanel;
  @FXML private VBox upcomingPanel;
  @FXML private VBox finishedPanel;
  @FXML private ToggleButton activeTab;
  @FXML private ToggleButton historyTab;
  @FXML private ToggleGroup homeTabs;
  @FXML private ComboBox<String> homeStatus;

  // Category chips
  @FXML private Button chipAll;

  /* ======= Internal state ======= */
  private final AuctionClientService auctionService = ServiceRegistry.getService();
  private List<Auction> allAuctions = new ArrayList<>();
  private String activeCategory = "all";
  private Timeline masterCountdownTimer;
  private final List<CountdownEntry> countdownEntries = new ArrayList<>();

  /* ======= Initialization ======= */

  /** Initializes the controls after FXML injection. */
  @FXML
  public void initialize() {
    auctionService.addChangeListener(serverChanges);
    configureVisuals();
    setupSearchListener();
    homeStatus.getItems().add("Tất cả trạng thái");
    for (AuctionStatus status : AuctionStatus.values()) {
      homeStatus.getItems().add(status.getDescription());
    }
    homeStatus.getSelectionModel().selectFirst();
    homeStatus
        .valueProperty()
        .addListener(
            (o, before, selected) -> {
              int index = homeStatus.getSelectionModel().getSelectedIndex() - 1;
              if (index >= 0) {
                AuctionStatus status = AuctionStatus.values()[index];
                homeTabs.selectToggle(
                    status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING
                        ? activeTab
                        : historyTab);
                homeStatus.setValue(selected);
              }
              applyFilters();
            });
    homeTabs
        .selectedToggleProperty()
        .addListener(
            (o, before, selected) -> {
              if (selected == null) {
                homeTabs.selectToggle(before);
                return;
              }
              homeStatus.getSelectionModel().selectFirst();
              applyFilters();
            });
    loadAuctions();
    startMasterTimer();
  }

  private void configureVisuals() {
    SmoothScroll.apply(dashboardScroll, 3.0);
    com.auction.client.ui.AppFooter.install(dashboardScroll, pageContent);
    auctionGrid.prefWrapLengthProperty().bind(auctionGrid.widthProperty());
    upcomingGrid.prefWrapLengthProperty().bind(upcomingGrid.widthProperty());
    setIcon(lblLogoIcon, "trophy", 22, "icon-primary");
    lblBrandName.setText(AppInfo.NAME);
    setIcon(lblSearchIcon, "search", 16, "icon-muted");
  }

  private void setIcon(Label label, String name, double size, String colorClass) {
    Region icon = Icons.icon(name, size);
    icon.getStyleClass().add(colorClass);
    label.setGraphic(icon);
    label.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
    label.setGraphicTextGap(8);
  }

  private void setupSearchListener() {
    txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
  }

  /* ======= Data Loading ======= */

  /** Loads auctions in the background and updates the current filters. */
  @FXML
  public void loadAuctions() {
    if (tasks.isClosed()) {
      return;
    }
    if (loading) {
      loadAgain = true;
      return;
    }
    loading = true;
    loadFailed = false;
    applyFilters();
    tasks.submit(
        "auctions",
        () -> auctionService.getAuctions(null),
        loaded -> {
          loading = false;
          // Mỗi ID phiên chỉ có một card, kể cả khi nguồn dữ liệu trả lặp.
          var unique = new java.util.LinkedHashMap<String, Auction>();
          if (loaded != null) {
            for (Auction auction : loaded) {
              unique.putIfAbsent(auction.getId(), auction);
            }
          }
          allAuctions = new ArrayList<>(unique.values());
          applyFilters();
          if (loadAgain) {
            loadAgain = false;
            loadAuctions();
          }
        },
        error -> {
          loading = false;
          loadFailed = true;
          applyFilters();
        });
  }

  private void applyFilters() {
    if (tasks.isClosed()) {
      return;
    }
    cardsVersion++;
    imageTasks.cancelAll();
    String search =
        txtSearch.getText() != null
            ? txtSearch.getText().toLowerCase(java.util.Locale.ROOT).trim()
            : "";

    final List<Auction> matching =
        allAuctions.stream()
            .filter(
                a -> {
                  boolean categoryMatch =
                      "all".equals(activeCategory)
                          || ItemCategory.from(a.getItemCategory())
                              == ItemCategory.from(activeCategory);
                  boolean searchMatch =
                      search.isEmpty()
                          || a.getItemName().toLowerCase(java.util.Locale.ROOT).contains(search)
                          || a.getItemCategory().toLowerCase(java.util.Locale.ROOT).contains(search)
                          || a.getSellerUsername()
                              .toLowerCase(java.util.Locale.ROOT)
                              .contains(search);
                  return categoryMatch && searchMatch;
                })
            .collect(Collectors.toList());

    countdownEntries.clear();
    boolean history = historyTab.isSelected();
    for (VBox panel : List.of(runningPanel, upcomingPanel)) {
      panel.setVisible(!history);
      panel.setManaged(!history);
    }
    finishedPanel.setVisible(history);
    finishedPanel.setManaged(history);
    List<Auction> filtered =
        matching.stream()
            .filter(
                a ->
                    homeStatus.getSelectionModel().getSelectedIndex() <= 0
                        || a.getStatus().getDescription().equals(homeStatus.getValue()))
            .toList();
    renderGroup(history ? List.of() : filtered, AuctionStatus.RUNNING, auctionGrid, lblNoResults);
    renderGroup(history ? List.of() : filtered, AuctionStatus.OPEN, upcomingGrid, lblUpcomingEmpty);
    renderGroup(
        history ? filtered : List.of(), AuctionStatus.FINISHED, finishedGrid, lblFinishedEmpty);
  }

  /* ======= Auction Card Grid ======= */

  private void renderGroup(
      List<Auction> filtered, AuctionStatus status, FlowPane grid, Label empty) {
    grid.getChildren().clear();
    if (!loading && !loadFailed) {
      for (Auction auction : filtered) {
        if (inGroup(auction, status)) {
          grid.getChildren().add(buildAuctionCard(auction, grid));
        }
      }
    }
    boolean hasCards = !grid.getChildren().isEmpty();
    if (loading) {
      empty.setText("Đang tải phiên đấu giá…");
    } else if (loadFailed) {
      empty.setText("Không thể tải phiên đấu giá. Bấm Làm mới để thử lại.");
    } else if (allAuctions.stream().noneMatch(a -> inGroup(a, status))) {
      empty.setText(
          status == AuctionStatus.RUNNING
              ? "Chưa có phiên đấu giá đang diễn ra."
              : status == AuctionStatus.OPEN
                  ? "Chưa có phiên đấu giá sắp diễn ra."
                  : "Chưa có phiên kết thúc hoặc bị hủy.");
    } else {
      empty.setText("Không có kết quả phù hợp với tìm kiếm hoặc danh mục đã chọn.");
    }
    empty.setVisible(!hasCards);
    empty.setManaged(!hasCards);
  }

  private boolean inGroup(Auction a, AuctionStatus status) {
    return status == AuctionStatus.FINISHED
        ? a.getStatus() != AuctionStatus.RUNNING && a.getStatus() != AuctionStatus.OPEN
        : a.getStatus() == status;
  }

  private VBox buildAuctionCard(Auction auction, FlowPane grid) {
    VBox card = new VBox(8.0);
    card.getStyleClass().add("auction-card");
    card.setUserData(auction.getId());
    card.setMinWidth(0);
    card.setMinHeight(Region.USE_PREF_SIZE);
    card.prefWidthProperty()
        .bind(
            Bindings.createDoubleBinding(
                () -> {
                  double width = Math.max(0, grid.getWidth());
                  int columns =
                      Math.max(1, (int) ((width + grid.getHgap()) / (300 + grid.getHgap())));
                  // Chừa 1 px cho làm tròn ở màn hình Windows có tỉ lệ hiển thị
                  // khác 100%.
                  return Math.max(
                      0, Math.floor((width - (columns - 1) * grid.getHgap()) / columns) - 1);
                },
                grid.widthProperty(),
                grid.hgapProperty()));
    card.maxWidthProperty().bind(card.prefWidthProperty());

    // Image placeholder
    VBox imgBox = new VBox();
    imgBox.getStyleClass().add("auction-card-image");
    imgBox.setMinSize(0, 120);
    imgBox.setPrefHeight(120);
    imgBox.setMaxHeight(120);
    RoundedClip.apply(imgBox, 12);
    Label imgLabel = new Label();
    setIcon(imgLabel, getCategoryIconName(auction.getItemCategory()), 64, "icon-on-panel");
    imgBox.getChildren().add(imgLabel);
    imgBox.setAlignment(javafx.geometry.Pos.CENTER);
    long version = cardsVersion;
    imageTasks.submit(
        "image-" + auction.getId(),
        () -> {
          String path = ServiceRegistry.getProductService().getImagePath(auction.getId());
          return ProductImages.load(path, 600, 240);
        },
        image -> {
          if (image == null || version != cardsVersion) {
            return;
          }
          double width = image.getWidth();
          double height = image.getHeight();
          double cropWidth = Math.min(width, height * 2.5);
          double cropHeight = Math.min(height, width / 2.5);
          ImageView view = new ImageView(image);
          view.setViewport(
              new Rectangle2D(
                  (width - cropWidth) / 2, (height - cropHeight) / 2, cropWidth, cropHeight));
          view.fitWidthProperty().bind(imgBox.widthProperty());
          view.setFitHeight(120);
          imgBox.getChildren().setAll(view);
        },
        error -> {
          /* Giữ icon khi ảnh không khả dụng. */
        });

    // Countdown label
    Label lblCountdown = new Label("--:--:--");
    lblCountdown.getStyleClass().add("countdown-label");
    lblCountdown.setMinHeight(Region.USE_PREF_SIZE);
    lblCountdown.setWrapText(true);
    // Register countdown entry
    if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
      countdownEntries.add(new CountdownEntry(auction, lblCountdown));
    }
    LocalDateTime target = countdownTarget(auction);
    boolean active =
        auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING;
    if (active && target != null) {
      lblCountdown.setText(
          formatCountdown(auctionService.serverNow(), target, auction.getStatus()));
    }

    // Status badge
    Label statusBadge = new Label(auction.getStatus().getDescription());
    statusBadge.getStyleClass().addAll("badge-status");
    if (auction.getStatus() == AuctionStatus.RUNNING) {
      statusBadge.getStyleClass().add("badge-running");
    } else if (auction.getStatus() == AuctionStatus.OPEN) {
      statusBadge.getStyleClass().add("badge-open");
    } else {
      statusBadge.getStyleClass().add("badge-finished");
    }

    // Tên sản phẩm
    Label lblName = new Label(auction.getItemName());
    lblName.getStyleClass().add("auction-card-title");
    lblName.getStyleClass().add("t-h3");
    lblName.setWrapText(true);
    lblName.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    lblName.setTooltip(new javafx.scene.control.Tooltip(auction.getItemName()));

    // Giá hiện tại
    Label lblPrice = new Label(FormatUtil.formatCurrency(auction.getCurrentPrice()));
    lblPrice.getStyleClass().add("auction-card-price");
    lblPrice.getStyleClass().add("t-h3");
    lblPrice.setWrapText(true);
    lblPrice.setMinHeight(Region.USE_PREF_SIZE);
    lblPrice.setTooltip(new javafx.scene.control.Tooltip(lblPrice.getText()));

    // Nút đặt giá
    Button btnBid = new Button("Xem chi tiết");
    btnBid.getStyleClass().add("btn-primary");
    btnBid.setMaxWidth(Double.MAX_VALUE);
    btnBid.setOnAction(e -> openAuctionDetail(auction));
    btnBid.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

    // HBox top: badge + countdown
    FlowPane topRow = new FlowPane(6.0, 6.0);
    topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    topRow.getStyleClass().add("auction-card-meta");
    topRow.getChildren().add(statusBadge);
    if (active) {
      topRow.getChildren().add(lblCountdown);
    }

    Region flexibleSpace = new Region();
    VBox.setVgrow(flexibleSpace, javafx.scene.layout.Priority.ALWAYS);
    VBox.setMargin(btnBid, new javafx.geometry.Insets(12, 16, 0, 16));
    card.getChildren().addAll(imgBox, topRow, lblName, lblPrice, flexibleSpace, btnBid);

    // Click toàn card để mở detail.
    card.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 1 && !(e.getTarget() instanceof Button)) {
            openAuctionDetail(auction);
          }
        });

    return card;
  }

  private String getCategoryIconName(String category) {
    return ItemCategory.from(category).getIcon();
  }

  /* ======= Countdown Timer ======= */

  private void startMasterTimer() {
    if (masterCountdownTimer != null) {
      masterCountdownTimer.stop();
    }
    masterCountdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdowns()));
    masterCountdownTimer.setCycleCount(Timeline.INDEFINITE);
    masterCountdownTimer.play();
  }

  private void tickCountdowns() {
    LocalDateTime now = auctionService.serverNow();
    // Chỉ tải nền khi một phiên đi qua mốc bắt đầu/kết thúc.
    boolean boundary =
        allAuctions.stream()
            .anyMatch(
                a ->
                    a.getStatus() == AuctionStatus.OPEN
                            && a.getStartTime() != null
                            && !now.isBefore(a.getStartTime())
                        || a.getStatus() == AuctionStatus.RUNNING
                            && a.getEndTime() != null
                            && !now.isBefore(a.getEndTime()));
    if (boundary && !loading) {
      loadAuctions();
    }

    for (CountdownEntry entry : countdownEntries) {
      LocalDateTime target = countdownTarget(entry.auction);
      if (target == null) {
        continue;
      }
      String cd = formatCountdown(now, target, entry.auction.getStatus());
      entry.label.setText(cd);
      updateCountdownStyle(entry.label, now, target);
    }
  }

  private LocalDateTime countdownTarget(Auction auction) {
    return auction.getStatus() == AuctionStatus.OPEN && auction.getStartTime() != null
        ? auction.getStartTime()
        : auction.getEndTime();
  }

  private String formatCountdown(LocalDateTime now, LocalDateTime end, AuctionStatus status) {
    if (status == AuctionStatus.FINISHED) {
      return "Đã kết thúc";
    }
    long seconds = ChronoUnit.SECONDS.between(now, end);
    if (seconds <= 0) {
      return "Đã kết thúc";
    }

    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;

    String remaining;
    if (hours > 24) {
      long days = hours / 24;
      remaining = days + " ngày " + (hours % 24) + " giờ";
    } else {
      remaining = String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    return status == AuctionStatus.OPEN ? "Mở sau " + remaining : remaining;
  }

  private void updateCountdownStyle(Label label, LocalDateTime now, LocalDateTime end) {
    long seconds = ChronoUnit.SECONDS.between(now, end);
    label.getStyleClass().remove("countdown-label-urgent");
    if (seconds < 60 && seconds > 0) {
      label.getStyleClass().add("countdown-label-urgent");
    }
  }

  /* ======= Action Handlers ======= */

  @FXML
  private void handleCategoryFilter(javafx.event.ActionEvent event) {
    Button source = (Button) event.getSource();
    Object userData = source.getUserData();
    activeCategory = userData != null ? userData.toString() : "all";

    // Update chip styles
    for (Button chip :
        List.of(chipAll, chipWatch, chipArt, chipHome, chipCar, chipLaptop, chipOther)) {
      chip.getStyleClass().remove("btn-primary");
    }
    source.getStyleClass().add("btn-primary");

    applyFilters();
  }

  private void openAuctionDetail(Auction auction) {
    try {
      AuctionRoomController controller =
          ViewNavigator.loadView(
              "/com/auction/client/views/auction-room-view.fxml",
              "Phòng đấu giá: " + auction.getItemName());
      controller.setAuction(auction);
    } catch (IOException e) {
      showNavigationError();
    }
  }

  private void showNavigationError() {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    ViewNavigator.prepareDialog(alert);
    alert.setTitle("Lỗi giao diện");
    alert.setHeaderText("Không thể chuyển màn hình");
    alert.setContentText("Vui lòng thử lại. Nếu sự cố tiếp tục, hãy khởi động lại ứng dụng.");
    alert.showAndWait();
  }

  @Override
  public void dispose() {
    auctionService.removeChangeListener(serverChanges);
    tasks.close();
    imageTasks.close();
    if (masterCountdownTimer != null) {
      masterCountdownTimer.stop();
    }
    countdownEntries.clear();
  }

  /* ======= Inner helper ======= */

  /** Gắn kết một Label countdown với một phiên đấu giá. */
  private static class CountdownEntry {
    final Auction auction;
    final Label label;

    CountdownEntry(Auction auction, Label label) {
      this.auction = auction;
      this.label = label;
    }
  }
}
