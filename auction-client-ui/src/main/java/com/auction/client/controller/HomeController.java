package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.util.FormatUtil;
import com.auction.client.util.AppInfo;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.ui.Icons;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.animation.Interpolator;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Điều khiển trang chủ với danh sách phiên đấu giá dạng thẻ.
 * Thay thế TableView cũ bằng FlowPane card động với countdown timer thời gian thực.
 */
public class HomeController {

    /* ======= FXML Bindings ======= */
    @FXML private TextField txtSearch;
    @FXML private ScrollPane dashboardScroll;
    @FXML private VBox pageContent;
    @FXML private VBox mainColumn;
    @FXML private Label lblLogoIcon;
    @FXML private Label lblBrandName;
    @FXML private Label lblSearchIcon;
    @FXML private Button chipWatch;
    @FXML private Button chipArt;
    @FXML private Button chipHome;
    @FXML private Button chipCar;
    @FXML private Button chipLaptop;
    @FXML private Label lblFeaturedHeader;
    @FXML private Label lblRunningHeader;
    @FXML private Label lblStepsHeader;
    @FXML private Label lblUpcomingHeader;
    @FXML private Label lblSellHeader;
    @FXML private Label lblStepOneIcon;
    @FXML private Label lblStepTwoIcon;
    @FXML private Label lblStepThreeIcon;
    @FXML private VBox featuredPane;
    @FXML private ScrollPane featuredCarouselScroll;
    @FXML private HBox featuredCarouselTrack;
    @FXML private Button btnCarouselPrevious;
    @FXML private Button btnCarouselNext;

    // Grid & sidebar
    @FXML private FlowPane auctionGrid;
    @FXML private FlowPane upcomingGrid;
    @FXML private Label lblNoResults;

    // Category chips
    @FXML private Button chipAll;

    /* ======= Internal state ======= */
    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private List<Auction> allAuctions = new ArrayList<>();
    private List<Auction> featuredAuctions = new ArrayList<>();
    private String activeCategory = "all";
    private Timeline masterCountdownTimer;
    private Timeline carouselAnimation;
    private final List<CountdownEntry> countdownEntries = new ArrayList<>();

    /* ======= Initialization ======= */

    @FXML
    public void initialize() {
        configureVisuals();
        setupSearchListener();
        loadAuctions();
        startMasterTimer();
    }

    private void configureVisuals() {
        pageContent.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> dashboardScroll.getViewportBounds().getHeight(), dashboardScroll.viewportBoundsProperty()));
        SmoothScroll.apply(dashboardScroll, 3.0);
        com.auction.client.ui.AppFooter.install(dashboardScroll, pageContent);
        auctionGrid.prefWrapLengthProperty().bind(mainColumn.widthProperty());
        upcomingGrid.prefWrapLengthProperty().bind(mainColumn.widthProperty());
        setIcon(lblLogoIcon, "trophy", 22, "icon-primary");
        lblBrandName.setText(AppInfo.NAME);
        setIcon(lblSearchIcon, "search", 16, "icon-muted");
        setButtonIcon(btnCarouselPrevious, "arrow-left");
        setButtonIcon(btnCarouselNext, "arrow-right");
        setIcon(lblStepOneIcon, "user", 28, "icon-primary");
        setIcon(lblStepTwoIcon, "gavel", 28, "icon-primary");
        setIcon(lblStepThreeIcon, "bag", 28, "icon-primary");
        setButtonIcon(chipWatch, "watch");
        setButtonIcon(chipAll, "search");
        setButtonIcon(chipArt, "image");
        setButtonIcon(chipHome, "home");
        setButtonIcon(chipCar, "car");
        setButtonIcon(chipLaptop, "laptop");
        featuredCarouselScroll.hvalueProperty().addListener((observable, oldValue, newValue) -> updateCarouselButtons());
        featuredCarouselScroll.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> updateCarouselButtons());
        featuredCarouselTrack.widthProperty().addListener((observable, oldValue, newValue) -> updateCarouselButtons());
    }

    private void setIcon(Label label, String name, double size, String colorClass) {
        Region icon = Icons.icon(name, size);
        icon.getStyleClass().add(colorClass);
        label.setGraphic(icon);
        label.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        label.setGraphicTextGap(8);
    }

    private void setButtonIcon(Button button, String name) {
        Region icon = Icons.icon(name, 20);
        icon.getStyleClass().add("icon-current");
        button.setGraphic(icon);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        button.setGraphicTextGap(6);
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /* ======= Data Loading ======= */

    @FXML
    public void loadAuctions() {
        List<Auction> loaded = auctionService.getAuctions(null);
        allAuctions = loaded != null ? loaded : new ArrayList<>();
        applyFilters();
    }

    private void applyFilters() {
        String search = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";

        List<Auction> filtered = allAuctions.stream()
                .filter(a -> {
                    boolean categoryMatch = "all".equals(activeCategory)
                            || a.getItemCategory().toLowerCase().contains(activeCategory.toLowerCase());
                    boolean searchMatch = search.isEmpty()
                            || a.getItemName().toLowerCase().contains(search)
                            || a.getItemCategory().toLowerCase().contains(search)
                            || a.getSellerUsername().toLowerCase().contains(search);
                    return categoryMatch && searchMatch;
                })
                .collect(Collectors.toList());

        countdownEntries.clear();
        renderFeatured(filtered);
        renderAuctionGrid(filtered);
        renderUpcomingGrid(filtered);
    }

    /* ======= Featured Section ======= */

    private void renderFeatured(List<Auction> auctions) {
        featuredAuctions = auctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.RUNNING)
                .limit(6)
                .collect(Collectors.toList());
        if (carouselAnimation != null) {
            carouselAnimation.stop();
        }
        featuredCarouselTrack.getChildren().clear();
        if (featuredAuctions.isEmpty()) {
            featuredPane.setVisible(false);
            featuredPane.setManaged(false);
            updateCarouselButtons();
            return;
        }

        featuredPane.setVisible(true);
        featuredPane.setManaged(true);
        for (Auction auction : featuredAuctions) {
            featuredCarouselTrack.getChildren().add(buildAuctionCard(auction));
        }
        featuredCarouselScroll.setHvalue(0);
        Platform.runLater(this::updateCarouselButtons);
    }

    /* ======= Auction Card Grid ======= */

    private void renderAuctionGrid(List<Auction> auctions) {
        auctionGrid.getChildren().clear();
        List<Auction> running = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .filter(a -> featuredAuctions.stream().noneMatch(featured -> featured.getId().equals(a.getId())))
                .collect(Collectors.toList());

        boolean noResults = running.isEmpty();
        if (lblNoResults != null) {
            lblNoResults.setVisible(noResults);
            lblNoResults.setManaged(noResults);
        }

        for (Auction auction : running) {
            VBox card = buildAuctionCard(auction);
            auctionGrid.getChildren().add(card);
        }
    }

    private void renderUpcomingGrid(List<Auction> auctions) {
        upcomingGrid.getChildren().clear();
        List<Auction> upcoming = auctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.OPEN)
                .collect(Collectors.toList());
        for (Auction auction : upcoming) {
            upcomingGrid.getChildren().add(buildAuctionCard(auction));
        }
        if (upcoming.isEmpty()) {
            Label empty = new Label("Chưa có phiên nào được lên lịch.");
            empty.getStyleClass().add("empty-placeholder");
            upcomingGrid.getChildren().add(empty);
        }
    }

    private VBox buildAuctionCard(Auction auction) {
        VBox card = new VBox(8.0);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(300.0);
        card.setMinWidth(300.0);
        card.setMaxWidth(300.0);
        card.setPrefHeight(340.0);

        // Image placeholder
        VBox imgBox = new VBox();
        imgBox.getStyleClass().add("auction-card-image");
        imgBox.setPrefHeight(120.0);
        Label imgLabel = new Label();
        setIcon(imgLabel, getCategoryIconName(auction.getItemCategory()), 64, "icon-on-panel");
        imgBox.getChildren().add(imgLabel);
        imgBox.setAlignment(javafx.geometry.Pos.CENTER);

        // Countdown label
        Label lblCountdown = new Label("--:--:--");
        lblCountdown.getStyleClass().add("countdown-label");
        // Register countdown entry
        countdownEntries.add(new CountdownEntry(auction, lblCountdown));

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
        lblName.setPrefHeight(44);
        lblName.setMaxHeight(44);
        lblName.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        lblName.setTooltip(new javafx.scene.control.Tooltip(auction.getItemName()));

        // Người bán
        Label lblSeller = new Label(auction.getSellerUsername());
        lblSeller.getStyleClass().add("text-secondary");
        setIcon(lblSeller, "user", 14, "icon-muted");

        // Số bid
        Label lblBids = new Label(auction.getTotalBids() + " lượt đặt giá");
        lblBids.getStyleClass().add("text-secondary");
        setIcon(lblBids, "gavel", 14, "icon-muted");

        // Giá hiện tại
        Label lblPrice = new Label(FormatUtil.formatCurrency(auction.getCurrentPrice()));
        lblPrice.getStyleClass().add("auction-card-price");
        lblPrice.getStyleClass().add("t-h3");

        // Nút đặt giá
        Button btnBid = new Button(auction.getStatus() == AuctionStatus.OPEN ? "Xem phiên" : "Đặt giá");
        btnBid.getStyleClass().add("btn-primary");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        setButtonIcon(btnBid, "arrow-right");
        btnBid.setOnAction(e -> openAuctionDetail(auction));
        btnBid.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        // HBox top: badge + countdown
        HBox topRow = new HBox(6.0);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topRow.getStyleClass().add("auction-card-meta");
        topRow.getChildren().addAll(statusBadge, lblCountdown);

        Region flexibleSpace = new Region();
        VBox.setVgrow(flexibleSpace, javafx.scene.layout.Priority.ALWAYS);
        VBox.setMargin(btnBid, new javafx.geometry.Insets(12, 16, 0, 16));
        card.getChildren().addAll(imgBox, topRow, lblName, lblSeller, lblBids, lblPrice, flexibleSpace, btnBid);

        // Click toàn card để mở detail.
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 && !(e.getTarget() instanceof Button)) {
                openAuctionDetail(auction);
            }
        });

        return card;
    }

    private String getCategoryIconName(String category) {
        if (category == null) return "tag";
        String cat = category.toLowerCase();
        if (cat.contains("đồng hồ") || cat.contains("trang sức")) return "watch";
        if (cat.contains("tranh") || cat.contains("nghệ thuật") || cat.contains("cổ vật")) return "image";
        if (cat.contains("bất động sản")) return "home";
        if (cat.contains("xe")) return "car";
        if (cat.contains("công nghệ") || cat.contains("thiết bị")) return "laptop";
        return "tag";
    }

    /* ======= Countdown Timer ======= */

    private void startMasterTimer() {
        if (masterCountdownTimer != null) {
            masterCountdownTimer.stop();
        }
        masterCountdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> tickCountdowns())
        );
        masterCountdownTimer.setCycleCount(Timeline.INDEFINITE);
        masterCountdownTimer.play();
    }

    private void tickCountdowns() {
        LocalDateTime now = LocalDateTime.now();
        boolean statusChanged = false;
        for (Auction auction : allAuctions) {
            if (auction.getStatus() != AuctionStatus.FINISHED
                    && auction.getEndTime() != null
                    && !now.isBefore(auction.getEndTime())) {
                auction.setStatus(AuctionStatus.FINISHED);
                statusChanged = true;
            } else if (auction.getStatus() == AuctionStatus.OPEN
                    && auction.getStartTime() != null
                    && !now.isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.RUNNING);
                statusChanged = true;
            }
        }
        if (statusChanged) {
            applyFilters();
            return;
        }

        for (CountdownEntry entry : countdownEntries) {
            LocalDateTime target = countdownTarget(entry.auction);
            if (target == null) continue;
            String cd = formatCountdown(now, target, entry.auction.getStatus());
            Platform.runLater(() -> {
                entry.label.setText(cd);
                updateCountdownStyle(entry.label, now, target);
            });
        }
    }

    private LocalDateTime countdownTarget(Auction auction) {
        return auction.getStatus() == AuctionStatus.OPEN && auction.getStartTime() != null
                ? auction.getStartTime()
                : auction.getEndTime();
    }

    private String formatCountdown(LocalDateTime now, LocalDateTime end, AuctionStatus status) {
        if (status == AuctionStatus.FINISHED) return "Đã kết thúc";
        long seconds = ChronoUnit.SECONDS.between(now, end);
        if (seconds <= 0) return "Đã kết thúc";

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
        for (Button chip : List.of(chipAll, chipWatch, chipArt, chipHome, chipCar, chipLaptop)) {
            chip.getStyleClass().remove("btn-primary");
        }
        source.getStyleClass().add("btn-primary");

        applyFilters();
    }

    @FXML
    private void handleCarouselPrevious() {
        scrollCarousel(-1);
    }

    @FXML
    private void handleCarouselNext() {
        scrollCarousel(1);
    }

    private void scrollCarousel(int direction) {
        double scrollableWidth = featuredCarouselTrack.getBoundsInLocal().getWidth()
                - featuredCarouselScroll.getViewportBounds().getWidth();
        if (scrollableWidth <= 0) {
            updateCarouselButtons();
            return;
        }

        double currentOffset = featuredCarouselScroll.getHvalue() * scrollableWidth;
        double targetOffset = Math.max(0, Math.min(scrollableWidth, currentOffset + direction * 316));
        double targetValue = targetOffset / scrollableWidth;
        if (carouselAnimation != null) {
            carouselAnimation.stop();
        }
        carouselAnimation = new Timeline(new KeyFrame(Duration.millis(250),
                new KeyValue(featuredCarouselScroll.hvalueProperty(), targetValue, Interpolator.EASE_BOTH)));
        carouselAnimation.setOnFinished(event -> updateCarouselButtons());
        carouselAnimation.play();
    }

    private void updateCarouselButtons() {
        if (btnCarouselPrevious == null || btnCarouselNext == null) {
            return;
        }
        double scrollableWidth = featuredCarouselTrack.getBoundsInLocal().getWidth()
                - featuredCarouselScroll.getViewportBounds().getWidth();
        double currentOffset = featuredCarouselScroll.getHvalue() * Math.max(0, scrollableWidth);
        btnCarouselPrevious.setDisable(scrollableWidth <= 1 || currentOffset <= 1);
        btnCarouselNext.setDisable(scrollableWidth <= 1 || currentOffset >= scrollableWidth - 1);
    }

    @FXML
    private void handleListItem() {
        User user = auctionService.getCurrentUser();
        if (user != null && user.getRole() == UserRole.SELLER) {
            try {
                ViewNavigator.loadView("/com/auction/client/views/seller-products-view.fxml", "Quản lý sản phẩm");
            } catch (IOException e) {
                showNavigationError();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            ViewNavigator.applyAppStylesheet(alert.getDialogPane());
            alert.setTitle("Thông báo");
            alert.setHeaderText("Chỉ dành cho người bán");
            alert.setContentText("Bạn cần có tài khoản NGƯỜI BÁN để đăng bán sản phẩm. "
                    + "Vui lòng đăng ký tài khoản mới với vai trò Người bán.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleShowFullSchedule() {
        List<Auction> upcoming = allAuctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.OPEN)
                .sorted((first, second) -> first.getStartTime().compareTo(second.getStartTime()))
                .collect(Collectors.toList());
        String content = upcoming.isEmpty()
                ? "Hiện chưa có phiên nào được lên lịch."
                : upcoming.stream()
                    .map(auction -> auction.getItemName() + " — "
                            + auction.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            + " — Giá khởi điểm " + FormatUtil.formatCurrency(auction.getStartingPrice()))
                    .collect(Collectors.joining("\n"));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
        alert.setTitle("Lịch đấu giá");
        alert.setHeaderText("Các phiên sắp diễn ra");
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openAuctionDetail(Auction auction) {
        try {
            AuctionRoomController controller = ViewNavigator.loadView(
                    "/com/auction/client/views/auction-room-view.fxml",
                    "Phòng đấu giá: " + auction.getItemName());
            controller.setAuction(auction);
        } catch (IOException e) {
            showNavigationError();
        }
    }

    @FXML
    private void handleOpenProductManagement() {
        try {
            ViewNavigator.navigate(ViewNavigator.Route.PRODUCTS);
        } catch (IOException e) {
            showNavigationError();
        }
    }

    private void showNavigationError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        ViewNavigator.applyAppStylesheet(alert.getDialogPane());
        alert.setTitle("Lỗi giao diện");
        alert.setHeaderText("Không thể chuyển màn hình");
        alert.setContentText("Vui lòng thử lại. Nếu sự cố tiếp tục, hãy khởi động lại ứng dụng.");
        alert.showAndWait();
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
