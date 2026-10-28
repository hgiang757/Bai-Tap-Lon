package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.util.FormatUtil;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.ui.SmoothScroll;
import com.auction.client.ui.Icons;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class SellerProductsController {

    @FXML private javafx.scene.control.ListView<Item> tblProducts;
    @FXML private ScrollPane productScroll;
    @FXML private VBox pageContent;
    @FXML private Label lblProductSearchIcon;
    @FXML private Label lblFormTitle;
    @FXML private Label lblImageName;
    @FXML private TextField txtSearch;
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtDuration;
    @FXML private TextField txtMinStep;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private HBox alertBox;
    @FXML private Label lblAlertMessage;

    private final AuctionClientService auctionService = ServiceRegistry.getService();
    private final ObservableList<Item> productList = FXCollections.observableArrayList();
    private final FilteredList<Item> filteredProducts = new FilteredList<>(productList, item -> true);
    private Item selectedItem;

    @FXML
    public void initialize() {
        pageContent.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> productScroll.getViewportBounds().getHeight(), productScroll.viewportBoundsProperty()));
        com.auction.client.ui.AppFooter.install(productScroll, pageContent);
        javafx.scene.layout.Region searchIcon = Icons.icon("search", 16);
        searchIcon.getStyleClass().add("icon-muted");
        lblProductSearchIcon.setGraphic(searchIcon);
        SmoothScroll.apply(productScroll, 3.0);
        setupCategories();
        setupTableColumns();
        tblProducts.setItems(filteredProducts);
        tblProducts.setPlaceholder(new Label("Chưa có sản phẩm"));
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applySearch(newValue));
        tblProducts.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, item) -> {
            if (item != null) {
                populateForm(item);
            }
        });
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
        loadProducts();
        hideAlert();
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

    private void setupTableColumns() {
        tblProducts.setCellFactory(list -> new javafx.scene.control.ListCell<Item>() {
            @Override protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                Label name = new Label(item.getName()); name.getStyleClass().add("product-title");
                Label category = new Label(item.getCategory());
                Label price = new Label(FormatUtil.formatCurrency(item.getStartingPrice()));
                Label status = new Label(statusFor(item)); status.getStyleClass().add("product-status");
                VBox details = new VBox(5, name, category, price, status);
                HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
                Button edit = new Button("Sửa");
                edit.setOnAction(e -> { tblProducts.getSelectionModel().select(item); populateForm(item); });
                Button delete = new Button("Xóa"); delete.getStyleClass().add("btn-danger");
                delete.setOnAction(e -> confirmDelete(item));
                HBox card = new HBox(10, details, edit, delete); card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.getStyleClass().add("product-card");
                card.prefWidthProperty().bind(list.widthProperty().subtract  (30));
                setGraphic(card);
            }
        });
    }

    private String statusFor(Item item) {
        return auctionService.getAuctions(null).stream()
                .filter(auction -> auction.getItem() != null
                        && auction.getItem().getId().equals(item.getId()))
                .map(Auction::getStatus)
                .findFirst()
                .map(AuctionStatus::getDescription)
                .orElse("Chưa mở phiên");
    }

    private void loadProducts() {
        User user = auctionService.getCurrentUser();
        String seller = user == null ? null : user.getUsername();
        List<Item> items = auctionService.getSellerItems(seller);
        productList.setAll(items);
        applySearch(txtSearch.getText());
    }

    private void applySearch(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        filteredProducts.setPredicate(item -> normalized.isEmpty()
                || item.getName().toLowerCase().contains(normalized)
                || item.getCategory().toLowerCase().contains(normalized));
    }

    private void populateForm(Item item) {
        selectedItem = item;
        lblFormTitle.setText("Chỉnh sửa sản phẩm");
        txtName.setText(item.getName());
        cbCategory.setValue(item.getCategory());
        txtDescription.setText(item.getDescription());
        txtStartingPrice.setText(String.format("%.0f", item.getStartingPrice()));
        auctionService.getAuctions(null).stream()
                .filter(auction -> auction.getItem() != null
                        && auction.getItem().getId().equals(item.getId()))
                .findFirst()
                .ifPresent(auction -> {
                    txtMinStep.setText(String.format("%.0f", auction.getMinBidStep()));
                    if (auction.getStartTime() != null && auction.getEndTime() != null) {
                        txtDuration.setText(String.valueOf(Math.max(1,
                                ChronoUnit.MINUTES.between(auction.getStartTime(), auction.getEndTime()))));
                    }
                });
        btnAdd.setVisible(false);
        btnAdd.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
        lblImageName.setText("Ảnh minh họa đang dùng placeholder");
    }

    @FXML
    private void handleAdd() {
        hideAlert();
        if (!validateInputs()) return;

        User user = auctionService.getCurrentUser();
        if (user == null) {
            showAlert("Vui lòng đăng nhập bằng tài khoản người bán.", true);
            return;
        }
        String id = "IT" + (System.currentTimeMillis() % 100000);
        Item item = new Item(id, txtName.getText().trim(), txtDescription.getText().trim(),
                cbCategory.getValue(), parseAmount(txtStartingPrice.getText()), user.getUsername());
        boolean success = auctionService.createItemAndAuction(item,
                Integer.parseInt(txtDuration.getText().trim()), parseAmount(txtMinStep.getText()));
        if (success) {
            showAlert("Đã tạo sản phẩm và phiên đấu giá thành công.", false);
            loadProducts();
            clearForm();
        } else {
            showAlert("Dịch vụ không thể tạo sản phẩm. Vui lòng thử lại.", true);
        }
    }

    @FXML
    private void handleUpdate() {
        hideAlert();
        if (selectedItem == null || !validateInputs()) return;
        String previousName = selectedItem.getName();
        String previousCategory = selectedItem.getCategory();
        String previousDescription = selectedItem.getDescription();
        double previousPrice = selectedItem.getStartingPrice();
        selectedItem.setName(txtName.getText().trim());
        selectedItem.setCategory(cbCategory.getValue());
        selectedItem.setDescription(txtDescription.getText().trim());
        selectedItem.setStartingPrice(parseAmount(txtStartingPrice.getText()));
        if (auctionService.updateItem(selectedItem)) {
            showAlert("Cập nhật sản phẩm thành công.", false);
            loadProducts();
            tblProducts.refresh();
        } else {
            selectedItem.setName(previousName);
            selectedItem.setCategory(previousCategory);
            selectedItem.setDescription(previousDescription);
            selectedItem.setStartingPrice(previousPrice);
            showAlert("Không thể cập nhật sản phẩm. Vui lòng thử lại.", true);
        }
    }

    private void confirmDelete(Item item) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        ViewNavigator.applyAppStylesheet(confirmation.getDialogPane());
        confirmation.setTitle("Xóa sản phẩm");
        confirmation.setHeaderText("Bạn có chắc muốn xóa \"" + item.getName() + "\"?");
        confirmation.setContentText("Sản phẩm thuộc phiên đang diễn ra sẽ không thể xóa.");
        Optional<ButtonType> answer = confirmation.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            if (auctionService.deleteItem(item.getId())) {
                showAlert("Đã xóa sản phẩm.", false);
                if (selectedItem == item) clearForm();
                loadProducts();
            } else {
                showAlert("Không thể xóa sản phẩm đang có phiên đấu giá diễn ra.", true);
            }
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh sản phẩm");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Tệp ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selected = chooser.showOpenDialog(ViewNavigator.getPrimaryStage());
        if (selected != null) {
            lblImageName.setText(selected.getName());
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
        hideAlert();
    }

    private void clearForm() {
        selectedItem = null;
        txtName.clear();
        txtDescription.clear();
        txtStartingPrice.clear();
        txtDuration.setText("60");
        txtMinStep.setText("500000");
        cbCategory.getSelectionModel().selectFirst();
        lblImageName.setText("Chưa chọn ảnh");
        tblProducts.getSelectionModel().clearSelection();
        lblFormTitle.setText("Thêm sản phẩm mới");
        btnAdd.setVisible(true);
        btnAdd.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInputs() {
        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            showAlert("Vui lòng nhập tên sản phẩm.", true);
            return false;
        }
        if (cbCategory.getValue() == null || cbCategory.getValue().trim().isEmpty()) {
            showAlert("Vui lòng chọn danh mục sản phẩm.", true);
            return false;
        }
        try {
            if (parseAmount(txtStartingPrice.getText()) <= 0) {
                showAlert("Giá khởi điểm phải là số nguyên dương.", true);
                return false;
            }
        } catch (NumberFormatException exception) {
            showAlert("Giá khởi điểm không hợp lệ.", true);
            return false;
        }
        try {
            if (Integer.parseInt(txtDuration.getText().trim()) <= 0) {
                showAlert("Thời lượng đấu giá phải lớn hơn 0 phút.", true);
                return false;
            }
        } catch (NumberFormatException exception) {
            showAlert("Thời lượng đấu giá phải là số nguyên dương.", true);
            return false;
        }
        try {
            if (parseAmount(txtMinStep.getText()) <= 0) {
                showAlert("Bước giá phải là số nguyên dương.", true);
                return false;
            }
        } catch (NumberFormatException exception) {
            showAlert("Bước giá không hợp lệ.", true);
            return false;
        }
        return true;
    }

    private long parseAmount(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("[,.\\s]", "");
        if (!normalized.matches("[0-9]+")) {
            throw new NumberFormatException("Số tiền không hợp lệ");
        }
        return Long.parseLong(normalized);
    }

    @FXML
    private void handleBackToHome() {
        try {
            ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD);
        } catch (IOException exception) {
            showAlert("Không thể quay lại danh sách phiên đấu giá.", true);
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
