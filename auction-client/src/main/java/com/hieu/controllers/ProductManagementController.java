package com.hieu.controllers;

import com.hieu.models.Item;
import com.hieu.models.User;
import com.hieu.services.AuctionClientService;
import com.hieu.services.ServiceRegistry;
import com.hieu.util.ViewNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.List;

/**
 * Controller xử lý Màn hình Quản lý Sản phẩm (dành cho Seller).
 * Cung cấp form Thêm / Sửa / Xóa sản phẩm và đưa sản phẩm lên sàn đấu giá.
 */
public class ProductManagementController {

    @FXML private TableView<Item> tblProducts;
    @FXML private TableColumn<Item, String> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colPrice;
    @FXML private TableColumn<Item, String> colDescription;

    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtDuration; // Thời lượng đấu giá (phút)
    @FXML private TextField txtMinStep;   // Bước giá tối thiểu

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;

    @FXML private HBox alertBox;
    @FXML private Label lblAlertMessage;

    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private final ObservableList<Item> productList = FXCollections.observableArrayList();
    private Item selectedItem;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupCategories();
        setupPlaceholder();
        loadProducts();
        hideAlert();

        // Lắng nghe chọn sản phẩm trên bảng để đổ dữ liệu vào form
        tblProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateForm(newVal);
            }
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(cellData -> new SimpleStringProperty(com.hieu.util.FormatUtil.formatCurrency(cellData.getValue().getStartingPrice())));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void setupPlaceholder() {
        Label placeholder = new Label("Chưa có sản phẩm nào trong danh sách");
        placeholder.getStyleClass().add("empty-placeholder");
        tblProducts.setPlaceholder(placeholder);
    }

    private void setupCategories() {
        cbCategory.setItems(FXCollections.observableArrayList(
                "Đồng hồ cao cấp",
                "Nghệ thuật & Cổ vật",
                "Thiết bị công nghệ",
                "Trang sức & Đá quý",
                "Xe cộ & Phụ tùng",
                "Bất động sản & Khác"
        ));
        cbCategory.getSelectionModel().selectFirst();
    }

    private void loadProducts() {
        User user = auctionService.getCurrentUser();
        String seller = user != null ? user.getUsername() : null;
        List<Item> items = auctionService.getSellerItems(seller);
        productList.setAll(items);
        tblProducts.setItems(productList);
    }

    private void populateForm(Item item) {
        this.selectedItem = item;
        txtName.setText(item.getName());
        cbCategory.setValue(item.getCategory());
        txtDescription.setText(item.getDescription());
        txtStartingPrice.setText(String.format("%.0f", item.getStartingPrice()));

        btnUpdate.setDisable(false);
        btnDelete.setDisable(false);
    }

    @FXML
    private void handleAdd() {
        hideAlert();
        if (!validateInputs()) return;

        User user = auctionService.getCurrentUser();
        String seller = user != null ? user.getUsername() : "seller";

        String id = "IT" + (System.currentTimeMillis() % 10000);
        String name = txtName.getText().trim();
        String category = cbCategory.getValue();
        String desc = txtDescription.getText().trim();
        double price = Double.parseDouble(txtStartingPrice.getText().trim().replaceAll("[,.]", ""));
        int duration = Integer.parseInt(txtDuration.getText().trim());
        double minStep = txtMinStep.getText().isEmpty() ? 500_000.0 : Double.parseDouble(txtMinStep.getText().trim().replaceAll("[,.]", ""));

        Item newItem = new Item(id, name, desc, category, price, seller);
        boolean success = auctionService.createItemAndAuction(newItem, duration, minStep);

        if (success) {
            showAlert("Đã tạo sản phẩm và khởi tạo phiên đấu giá thành công!", false);
            loadProducts();
            handleClear();
        } else {
            showAlert("Không thể tạo sản phẩm!", true);
        }
    }

    @FXML
    private void handleUpdate() {
        hideAlert();
        if (selectedItem == null) {
            showAlert("Vui lòng chọn sản phẩm cần sửa từ danh sách!", true);
            return;
        }

        if (!validateInputs()) return;

        selectedItem.setName(txtName.getText().trim());
        selectedItem.setCategory(cbCategory.getValue());
        selectedItem.setDescription(txtDescription.getText().trim());
        selectedItem.setStartingPrice(Double.parseDouble(txtStartingPrice.getText().trim().replaceAll("[,.]", "")));

        boolean ok = auctionService.updateItem(selectedItem);
        if (ok) {
            showAlert("Cập nhật sản phẩm thành công!", false);
            loadProducts();
            tblProducts.refresh();
        } else {
            showAlert("Cập nhật thất bại!", true);
        }
    }

    @FXML
    private void handleDelete() {
        hideAlert();
        if (selectedItem == null) {
            showAlert("Vui lòng chọn sản phẩm cần xóa!", true);
            return;
        }

        boolean ok = auctionService.deleteItem(selectedItem.getId());
        if (ok) {
            showAlert("Đã xóa sản phẩm thành công!", false);
            loadProducts();
            handleClear();
        } else {
            showAlert("Không thể xóa sản phẩm này!", true);
        }
    }

    @FXML
    private void handleClear() {
        selectedItem = null;
        txtName.clear();
        txtDescription.clear();
        txtStartingPrice.clear();
        txtDuration.setText("60");
        txtMinStep.setText("500000");
        tblProducts.getSelectionModel().clearSelection();
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
        hideAlert();
    }

    private boolean validateInputs() {
        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            showAlert("Vui lòng nhập tên sản phẩm!", true);
            return false;
        }

        try {
            double price = Double.parseDouble(txtStartingPrice.getText().trim().replaceAll("[,.]", ""));
            if (price <= 0) {
                showAlert("Giá khởi điểm phải lớn hơn 0!", true);
                return false;
            }
        } catch (Exception e) {
            showAlert("Giá khởi điểm không hợp lệ!", true);
            return false;
        }

        try {
            int duration = Integer.parseInt(txtDuration.getText().trim());
            if (duration <= 0) {
                showAlert("Thời gian đấu giá (phút) phải lớn hơn 0!", true);
                return false;
            }
        } catch (Exception e) {
            showAlert("Thời gian đấu giá phải là số nguyên dương (phút)!", true);
            return false;
        }

        return true;
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            ViewNavigator.loadView("/com/hieu/views/dashboard-view.fxml", "Sàn đấu giá trực tuyến");
        } catch (IOException e) {
            e.printStackTrace();
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
