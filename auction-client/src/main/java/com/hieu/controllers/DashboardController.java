package com.hieu.controllers;

import com.hieu.models.Auction;
import com.hieu.models.AuctionStatus;
import com.hieu.models.User;
import com.hieu.models.UserRole;
import com.hieu.services.AuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.ViewNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.List;

/**
 * Điều khiển màn hình Danh sách phiên đấu giá (Dashboard).
 * Quản lý tìm kiếm, lọc theo trạng thái và chuyển hướng vào phòng đấu giá.
 */
public class DashboardController {

    @FXML private Label lblWelcomeUser;
    @FXML private Label lblUserRole;
    @FXML private Button btnProductManagement;
    @FXML private Button btnLogout;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Button btnRefresh;
    @FXML private Button btnEnterAuction;

    @FXML private TableView<Auction> tblAuctions;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colCategory;
    @FXML private TableColumn<Auction, String> colStartingPrice;
    @FXML private TableColumn<Auction, String> colCurrentPrice;
    @FXML private TableColumn<Auction, String> colHighestBidder;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colEndTime;

    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private final ObservableList<Auction> auctionMasterData = FXCollections.observableArrayList();
    private FilteredList<Auction> filteredData;

    @FXML
    public void initialize() {
        setupUserInfo();
        setupTableColumns();
        setupFilterControls();
        setupPlaceholder();
        loadAuctions();

        // Kích đúp vào một dòng để chuyển ngay sang phòng đấu giá trực tiếp
        tblAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openAuctionDetail(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupUserInfo() {
        User user = auctionService.getCurrentUser();
        if (user != null) {
            lblWelcomeUser.setText("Xin chào, " + user.getFullName());
            lblUserRole.setText(user.getRole().getDisplayName());

            boolean isSeller = user.getRole() == UserRole.SELLER;
            btnProductManagement.setVisible(isSeller);
            btnProductManagement.setManaged(isSeller);
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("itemCategory"));
        colStartingPrice.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedStartingPrice()));
        colCurrentPrice.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedCurrentPrice()));
        colHighestBidder.setCellValueFactory(new PropertyValueFactory<>("highestBidderUsername"));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().getDescription()));
        colEndTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedEndTime()));
    }

    private void setupPlaceholder() {
        Label placeholder = new Label("Không có phiên đấu giá nào phù hợp");
        placeholder.getStyleClass().add("empty-placeholder");
        tblAuctions.setPlaceholder(placeholder);
    }

    private void setupFilterControls() {
        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc"));
        cbStatusFilter.getSelectionModel().selectFirst();

        filteredData = new FilteredList<>(auctionMasterData, p -> true);
        tblAuctions.setItems(filteredData);

        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void applyFilter() {
        String selected = cbStatusFilter.getValue();
        String searchText = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";

        filteredData.setPredicate(auction -> {
            boolean statusMatches = true;
            if (selected != null && !selected.equals("Tất cả")) {
                if (selected.equals("Đang diễn ra")) {
                    statusMatches = auction.getStatus() == AuctionStatus.RUNNING;
                } else if (selected.equals("Sắp diễn ra")) {
                    statusMatches = auction.getStatus() == AuctionStatus.OPEN;
                } else if (selected.equals("Đã kết thúc")) {
                    statusMatches = auction.getStatus() == AuctionStatus.FINISHED;
                }
            }

            boolean searchMatches = true;
            if (!searchText.isEmpty()) {
                searchMatches = auction.getItemName().toLowerCase().contains(searchText) ||
                        auction.getItemCategory().toLowerCase().contains(searchText) ||
                        auction.getSellerUsername().toLowerCase().contains(searchText);
            }

            return statusMatches && searchMatches;
        });
    }

    @FXML
    public void loadAuctions() {
        List<Auction> list = auctionService.getAuctions(null);
        auctionMasterData.setAll(list);
        applyFilter();
    }

    @FXML
    private void handleEnterAuction() {
        Auction selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openAuctionDetail(selected);
        }
    }

    private void openAuctionDetail(Auction auction) {
        try {
            RealtimeBiddingController controller = ViewNavigator.loadView(
                    "/com/hieu/views/realtime-bidding-view.fxml",
                    "Phòng đấu giá: " + auction.getItemName());
            controller.setAuction(auction);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenProductManagement() {
        try {
            ViewNavigator.loadView("/com/hieu/views/product-management-view.fxml", "Quản lý sản phẩm");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        auctionService.logout();
        try {
            ViewNavigator.loadView("/com/hieu/views/auth-view.fxml", "Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
