package com.hieu.controllers;

import com.hieu.models.Auction;
import com.hieu.models.AuctionStatus;
import com.hieu.models.User;
import com.hieu.models.UserRole;
import com.hieu.services.AuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.FormatUtil;
import com.hieu.util.ViewNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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
 * Điều khiển màn hình Dashboard (Danh sách phiên đấu giá) - phiên bản nâng cấp với card grid.
 * Thay thế TableView cũ bằng FlowPane card động với countdown timer thời gian thực.
 */
public class DashboardController {

    /* ======= FXML Bindings ======= */
    @FXML private Label lblWelcomeUser;
    @FXML private Label lblUserRole;
    @FXML private Label lblUserAvatar;
    @FXML private Button btnProductManagement;
    @FXML private Button btnLogout;
    @FXML private TextField txtSearch;

    // Featured section
    @FXML private VBox featuredPane;
    @FXML private Label lblFeaturedName;
    @FXML private Label lblFeaturedPrice;
    @FXML private Label lblFeaturedCountdown;
    @FXML private Label lblFeaturedCode;
    @FXML private Label lblFeaturedStatus;
    @FXML private Label lblFeaturedCategory;
    @FXML private Label lblFeaturedBidCount;
    @FXML private Button btnFeaturedJoin;

    // Grid & sidebar
    @FXML private FlowPane auctionGrid;
    @FXML private VBox upcomingList;
    @FXML private Label lblNoResults;

    // Category chips
    @FXML private Button chipAll;

    /* ======= Internal state ======= */
    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private List<Auction> allAuctions = new ArrayList<>();
    private Auction featuredAuction;
    private String activeCategory = "all";
    private Timeline masterCountdownTimer;
    private final List<CountdownEntry> countdownEntries = new ArrayList<>();

    /* ======= Initialization ======= */

    @FXML
    public void initialize() {
        setupUserInfo();
        setupSearchListener();
        loadAuctions();
        startMasterTimer();
    }

    private void setupUserInfo() {
        User user = auctionService.getCurrentUser();
        if (user != null) {
            lblWelcomeUser.setText("Xin chào, " + user.getFullName());
            lblUserRole.setText(user.getRole().getDisplayName());
            lblUserAvatar.setText(user.getFullName() == null || user.getFullName().isEmpty()
                    ? "V"
                    : user.getFullName().substring(0, 1).toUpperCase());

            boolean isSeller = user.getRole() == UserRole.SELLER;
            btnProductManagement.setVisible(isSeller);
            btnProductManagement.setManaged(isSeller);
        }
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
        renderUpcomingList();
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

        renderFeatured(filtered);
        renderAuctionGrid(filtered);
    }

    /* ======= Featured Section ======= */

    private void renderFeatured(List<Auction> auctions) {
        // Ưu tiên phiên RUNNING đầu tiên
        featuredAuction = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .findFirst()
                .orElse(auctions.isEmpty() ? null : auctions.get(0));

        if (featuredAuction == null) {
            featuredPane.setVisible(false);
            featuredPane.setManaged(false);
            return;
        }

        featuredPane.setVisible(true);
        featuredPane.setManaged(true);

        lblFeaturedName.setText(featuredAuction.getItemName());
        lblFeaturedPrice.setText(FormatUtil.formatCurrency(featuredAuction.getCurrentPrice()));
        lblFeaturedCode.setText("#" + featuredAuction.getId());
        lblFeaturedCategory.setText(featuredAuction.getItemCategory());
        lblFeaturedBidCount.setText(featuredAuction.getTotalBids() + " lượt đặt giá");

        String statusText;
        String statusClass;
        if (featuredAuction.getStatus() == AuctionStatus.RUNNING) {
            statusText = "● ĐANG DIỄN RA";
            statusClass = "badge-running";
        } else if (featuredAuction.getStatus() == AuctionStatus.OPEN) {
            statusText = "○ SẮP DIỄN RA";
            statusClass = "badge-open";
        } else {
            statusText = "✕ ĐÃ KẾT THÚC";
            statusClass = "badge-finished";
        }
        lblFeaturedStatus.setText(statusText);
        lblFeaturedStatus.getStyleClass().removeAll("badge-running", "badge-open", "badge-finished");
        lblFeaturedStatus.getStyleClass().add(statusClass);
    }

    /* ======= Auction Card Grid ======= */

    private void renderAuctionGrid(List<Auction> auctions) {
        auctionGrid.getChildren().clear();
        countdownEntries.clear();

        List<Auction> running = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
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

    private VBox buildAuctionCard(Auction auction) {
        VBox card = new VBox(8.0);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(220.0);

        // Image placeholder
        VBox imgBox = new VBox();
        imgBox.getStyleClass().add("auction-card-image");
        imgBox.setPrefHeight(120.0);
        Label imgLabel = new Label(getCategoryIcon(auction.getItemCategory()));
        imgLabel.getStyleClass().add("auction-card-image-icon");
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
        lblName.setWrapText(true);

        // Người bán
        Label lblSeller = new Label("👤 " + auction.getSellerUsername());
        lblSeller.getStyleClass().add("text-secondary");

        // Số bid
        Label lblBids = new Label("🔥 " + auction.getTotalBids() + " lượt đặt giá");
        lblBids.getStyleClass().add("text-secondary");

        // Giá hiện tại
        Label lblPrice = new Label(FormatUtil.formatCurrency(auction.getCurrentPrice()));
        lblPrice.getStyleClass().add("auction-card-price");

        // Nút đặt giá
        Button btnBid = new Button("Đặt giá →");
        btnBid.getStyleClass().addAll("btn-primary", "btn-pill");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setOnAction(e -> openAuctionDetail(auction));
        btnBid.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        // HBox top: badge + countdown
        HBox topRow = new HBox(6.0);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topRow.getChildren().addAll(statusBadge, lblCountdown);

        card.getChildren().addAll(imgBox, topRow, lblName, lblSeller, lblBids, lblPrice, btnBid);

        // Click toàn card → mở detail
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 && !(e.getTarget() instanceof Button)) {
                openAuctionDetail(auction);
            }
        });

        return card;
    }

    private String getCategoryIcon(String category) {
        if (category == null) return "🏷";
        String cat = category.toLowerCase();
        if (cat.contains("đồng hồ") || cat.contains("trang sức")) return "⌚";
        if (cat.contains("tranh") || cat.contains("nghệ thuật") || cat.contains("cổ vật")) return "🎨";
        if (cat.contains("bất động sản")) return "🏠";
        if (cat.contains("xe")) return "🚗";
        if (cat.contains("công nghệ") || cat.contains("thiết bị")) return "💻";
        return "🏷";
    }

    /* ======= Upcoming Sidebar ======= */

    private void renderUpcomingList() {
        upcomingList.getChildren().clear();

        List<Auction> upcoming = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN)
                .limit(4)
                .collect(Collectors.toList());

        if (upcoming.isEmpty()) {
            Label empty = new Label("Không có phiên sắp diễn ra");
            empty.getStyleClass().add("text-secondary");
            upcomingList.getChildren().add(empty);
            return;
        }

        for (Auction auction : upcoming) {
            VBox item = buildUpcomingItem(auction);
            upcomingList.getChildren().add(item);
        }
    }

    private VBox buildUpcomingItem(Auction auction) {
        VBox item = new VBox(4.0);
        item.getStyleClass().add("upcoming-item");

        Label lblName = new Label(auction.getItemName());
        lblName.getStyleClass().add("text-bold");
        lblName.setWrapText(true);

        Label lblPrice = new Label("Khởi điểm: " + FormatUtil.formatCurrency(auction.getStartingPrice()));
        lblPrice.getStyleClass().add("text-secondary");

        String startTime = auction.getStartTime() == null
                ? auction.getFormattedEndTime()
                : auction.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblTime = new Label("🕐 " + startTime);
        lblTime.getStyleClass().add("text-secondary");

        item.getChildren().addAll(lblName, lblPrice, lblTime);
        return item;
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
            renderUpcomingList();
            return;
        }

        // Featured countdown
        if (featuredAuction != null && featuredAuction.getEndTime() != null) {
            String cd = formatCountdown(now, featuredAuction.getEndTime(), featuredAuction.getStatus());
            Platform.runLater(() -> {
                lblFeaturedCountdown.setText(cd);
                updateCountdownStyle(lblFeaturedCountdown, now, featuredAuction.getEndTime());
            });
        }

        // Card countdowns
        for (CountdownEntry entry : countdownEntries) {
            if (entry.auction.getEndTime() == null) continue;
            String cd = formatCountdown(now, entry.auction.getEndTime(), entry.auction.getStatus());
            boolean urgent = entry.auction.getStatus() == AuctionStatus.RUNNING
                    && ChronoUnit.SECONDS.between(now, entry.auction.getEndTime()) < 60;
            Platform.runLater(() -> {
                entry.label.setText(cd);
                updateCountdownStyle(entry.label, now, entry.auction.getEndTime());
            });
        }
    }

    private String formatCountdown(LocalDateTime now, LocalDateTime end, AuctionStatus status) {
        if (status == AuctionStatus.FINISHED) return "Đã kết thúc";
        long seconds = ChronoUnit.SECONDS.between(now, end);
        if (seconds <= 0) return "Đã kết thúc";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 24) {
            long days = hours / 24;
            return days + " ngày " + (hours % 24) + " giờ";
        }
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
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
        if (auctionGrid.getScene() != null) {
            auctionGrid.getScene().getRoot().lookupAll(".category-chip").forEach(node -> {
                node.getStyleClass().remove("category-chip-active");
            });
        }
        source.getStyleClass().add("category-chip-active");

        applyFilters();
    }

    @FXML
    private void handleJoinFeatured() {
        if (featuredAuction != null) {
            openAuctionDetail(featuredAuction);
        }
    }

    @FXML
    private void handleListItem() {
        User user = auctionService.getCurrentUser();
        if (user != null && user.getRole() == UserRole.SELLER) {
            try {
                ViewNavigator.loadView("/com/hieu/views/product-management-view.fxml", "Quản lý sản phẩm");
            } catch (IOException e) {
                showNavigationError();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
        alert.setTitle("Lịch đấu giá");
        alert.setHeaderText("Các phiên sắp diễn ra");
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openAuctionDetail(Auction auction) {
        try {
            RealtimeBiddingController controller = ViewNavigator.loadView(
                    "/com/hieu/views/realtime-bidding-view.fxml",
                    "Phòng đấu giá: " + auction.getItemName());
            controller.setAuction(auction);
        } catch (IOException e) {
            showNavigationError();
        }
    }

    @FXML
    private void handleOpenProductManagement() {
        try {
            ViewNavigator.loadView("/com/hieu/views/product-management-view.fxml", "Quản lý sản phẩm");
        } catch (IOException e) {
            showNavigationError();
        }
    }

    @FXML
    private void handleLogout() {
        if (masterCountdownTimer != null) {
            masterCountdownTimer.stop();
        }
        auctionService.logout();
        try {
            ViewNavigator.loadView("/com/hieu/views/auth-view.fxml", "Đăng nhập");
        } catch (IOException e) {
            showNavigationError();
        }
    }

    private void showNavigationError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
