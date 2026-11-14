package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.navigation.DisposableView;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.navigation.ViewTasks;
import com.auction.client.service.ProductService;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.ui.ProductImages;
import com.auction.client.ui.SellerProductFormView;
import com.auction.client.util.FormatUtil;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Điều khiển trang quản lý sản phẩm của người bán. */
public class SellerProductsController implements DisposableView {
  private final ViewTasks tasks = new ViewTasks();
  private boolean operationBusy;
  private boolean loadAgain;
  private final Consumer<String> serverChanges =
      type ->
          tasks.dispatch(
              () -> {
                if (type.startsWith("AUCTION_")
                    || type.equals("BID_PLACED")
                    || type.equals("RECONNECTED")) {
                  load();
                }
              });

  public boolean isLoading() {
    return progress.isVisible();
  }

  @Override
  public void dispose() {
    closeForm();
    ServiceRegistry.getService().removeChangeListener(serverChanges);
    tasks.close();
    imageScopes.forEach(ViewTasks::close);
    imageScopes.clear();
  }

  @FXML private TextField search;
  @FXML private ComboBox<String> status;
  @FXML private ComboBox<String> category;
  @FXML private TableView<Auction> table;
  @FXML private TableColumn<Auction, String> image;
  @FXML private TableColumn<Auction, String> nameColumn;
  @FXML private TableColumn<Auction, String> categoryColumn;
  @FXML private TableColumn<Auction, String> startingPrice;
  @FXML private TableColumn<Auction, String> currentPrice;
  @FXML private TableColumn<Auction, String> state;
  @FXML private TableColumn<Auction, String> startColumn;
  @FXML private TableColumn<Auction, String> endColumn;
  @FXML private TableColumn<Auction, String> actions;
  @FXML private Button addButton;
  @FXML private VBox listPane;
  @FXML private BorderPane editorHost;
  private SellerProductFormView activeForm;
  @FXML private Label message;
  @FXML private ProgressIndicator progress;
  private final ProductService service = ServiceRegistry.getProductService();
  private final ObservableList<Auction> rows = FXCollections.observableArrayList();
  private final Map<String, String> images = new HashMap<>();
  private final java.util.List<ViewTasks> imageScopes = new java.util.ArrayList<>();
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private User currentUser;

  /** Khởi tạo bảng và tải sản phẩm người bán. */
  @FXML
  public void initialize() {
    message
        .visibleProperty()
        .bind(message.textProperty().isNotEmpty().and(editorHost.visibleProperty().not()));
    message.managedProperty().bind(message.visibleProperty());
    currentUser = ServiceRegistry.getService().getCurrentUser();
    if (currentUser == null || currentUser.getRole() != UserRole.SELLER) {
      message.setText("Chỉ người bán được truy cập trang này.");
      table.setDisable(true);
      addButton.setDisable(true);
      return;
    }
    ServiceRegistry.getService().addChangeListener(serverChanges);
    status.getItems().add("Tất cả trạng thái");
    for (AuctionStatus value : AuctionStatus.values()) {
      status.getItems().add(value.getDescription());
    }
    status.getSelectionModel().selectFirst();
    category.getItems().add("Tất cả loại");
    for (var c : com.auction.client.model.ItemCategory.values()) {
      category.getItems().add(c.getLabel());
    }
    category.getSelectionModel().selectFirst();

    image.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
    image.setCellFactory(
        c ->
            new TableCell<>() {
              private final ViewTasks cellImages = ViewTasks.images();

              {
                imageScopes.add(cellImages);
                sceneProperty()
                    .addListener(
                        (o, oldScene, scene) -> {
                          if (scene == null) {
                            cellImages.cancelAll();
                          }
                        });
              }

              @Override
              protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty);
                cellImages.cancelAll();
                setGraphic(null);
                if (empty || id == null) {
                  return;
                }
                if (getTableRow().getItem() != null) {
                  var icon =
                      com.auction.client.ui.Icons.icon(
                          com.auction.client.model.ItemCategory.from(
                                  getTableRow().getItem().getItemCategory())
                              .getIcon(),
                          24);
                  icon.getStyleClass().add("icon-muted");
                  setGraphic(icon);
                }
                String path = images.get(id);
                cellImages.submit(
                    "thumbnail",
                    () -> ProductImages.load(path, 48, 48),
                    loaded -> {
                      if (loaded != null && !isEmpty() && id.equals(getItem())) {
                        setGraphic(new ImageView(loaded));
                      }
                    },
                    error -> {
                      /* Giữ icon; lỗi/hàng đợi đầy không khóa bảng. */
                    });
                setAlignment(javafx.geometry.Pos.CENTER);
              }
            });
    column(nameColumn, Auction::getItemName);
    column(categoryColumn, Auction::getItemCategory);
    column(startingPrice, a -> FormatUtil.formatCurrency(a.getStartingPrice()));
    column(currentPrice, a -> FormatUtil.formatCurrency(a.getCurrentPrice()));
    column(state, a -> a.getStatus().getDescription());
    column(startColumn, a -> a.getStartTime().format(formatter));
    column(endColumn, a -> a.getEndTime().format(formatter));
    actions.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
    actions.setCellFactory(
        c ->
            new TableCell<>() {
              @Override
              protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty);
                setGraphic(null);
                if (empty || id == null || getTableRow().getItem() == null) {
                  return;
                }
                Auction a = getTableRow().getItem();
                Button detail = new Button("Chi tiết");
                Button edit = new Button("Sửa");
                Button delete = new Button("Xóa");
                for (Button button : List.of(detail, edit, delete)) {
                  button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                }
                boolean locked = a.getTotalBids() > 0 || a.getStatus() != AuctionStatus.OPEN;
                edit.setDisable(locked);
                delete.setDisable(locked);
                edit.setTooltip(new Tooltip("Chỉ sửa phiên OPEN chưa có lượt đặt giá."));
                delete.setTooltip(new Tooltip("Chỉ xóa phiên OPEN chưa có lượt đặt giá."));
                detail.setOnAction(e -> detail(a));
                edit.setOnAction(e -> form(a));
                delete.setOnAction(
                    e -> {
                      Alert confirm =
                          new Alert(
                              Alert.AlertType.CONFIRMATION,
                              "Xóa sản phẩm " + a.getItemName() + "?",
                              ButtonType.OK,
                              ButtonType.CANCEL);
                      ViewNavigator.prepareDialog(confirm);
                      if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        String imagePath = images.get(id);
                        run(
                            () -> {
                              service.deleteProduct(currentUser.getUsername(), id);
                              ProductImages.deleteImportedImage(imagePath, id);
                              return null;
                            },
                            ignored -> load());
                      }
                    });
                setGraphic(new HBox(5, detail, edit, delete));
              }
            });
    search.textProperty().addListener((o, a, b) -> filter());
    status.valueProperty().addListener((o, a, b) -> filter());
    category.valueProperty().addListener((o, a, b) -> filter());
    load();
  }

  private void column(TableColumn<Auction, String> column, Function<Auction, String> value) {
    column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
  }

  private void filter() {
    String q = search.getText().trim().toLowerCase();
    table.setItems(
        rows.filtered(
            a ->
                a.getItemName().toLowerCase().contains(q)
                    && (status.getSelectionModel().getSelectedIndex() <= 0
                        || a.getStatus().getDescription().equals(status.getValue()))
                    && (category.getSelectionModel().getSelectedIndex() <= 0
                        || a.getItemCategory().equals(category.getValue()))));
  }

  private record Products(List<Auction> rows, Map<String, String> paths) {}

  private void load() {
    if (operationBusy) {
      loadAgain = true;
      return;
    }
    run(
        () -> {
          List<Auction> list = service.listProducts(currentUser.getUsername());
          Map<String, String> paths = new HashMap<>();
          for (Auction a : list) {
            String path = service.getImagePath(a.getId());
            paths.put(a.getId(), path);
          }
          return new Products(list, paths);
        },
        result -> {
          images.clear();
          images.putAll(result.paths());
          rows.setAll(result.rows());
          filter();
          table.refresh();
        });
  }

  private <T> void run(Callable<T> work, Consumer<T> success) {
    operationBusy = true;
    progress.setVisible(true);
    progress.setManaged(true);
    table.setDisable(true);
    addButton.setDisable(true);
    if (activeForm != null) {
      activeForm.setBusy(true);
    }
    message.setText("");
    tasks.submit(
        "seller",
        work,
        value -> {
          progress.setVisible(false);
          progress.setManaged(false);
          table.setDisable(false);
          addButton.setDisable(false);
          if (activeForm != null) {
            activeForm.setBusy(false);
          }
          operationBusy = false;
          success.accept(value);
          if (loadAgain && !operationBusy) {
            loadAgain = false;
            load();
          }
        },
        error -> {
          progress.setVisible(false);
          progress.setManaged(false);
          table.setDisable(false);
          addButton.setDisable(false);
          if (activeForm != null) {
            activeForm.setBusy(false);
          }
          operationBusy = false;
          loadAgain = false;
          message.setText("Không thể thực hiện: " + error.getMessage());
          if (activeForm != null) {
            activeForm.showSaveError(message.getText());
          }
        });
  }

  @FXML
  private void add() {
    form(null);
  }

  private void detail(Auction auction) {
    run(
        () -> service.getBidHistory(auction.getId()),
        history -> {
          String bids =
              history.isEmpty()
                  ? "Chưa có dữ liệu"
                  : history.stream()
                      .map(Object::toString)
                      .collect(java.util.stream.Collectors.joining("\n"));
          Alert alert =
              new Alert(
                  Alert.AlertType.INFORMATION,
                  auction.getItem().getDescription() + "\n\nLịch sử đặt giá:\n" + bids);
          alert.setHeaderText(auction.getItemName());
          ViewNavigator.prepareDialog(alert);
          alert.show();
        });
  }

  private void form(Auction original) {
    if (operationBusy || currentUser == null || currentUser.getRole() != UserRole.SELLER) {
      return;
    }
    if (original != null
        && (original.getTotalBids() > 0
            || original.getStatus() != AuctionStatus.OPEN
            || !currentUser.getUsername().equals(original.getSellerUsername()))) {
      message.setText("Chỉ sửa sản phẩm của mình khi phiên OPEN chưa có lượt đặt giá.");
      return;
    }
    closeForm();
    try {
      activeForm =
          new SellerProductFormView(
              original,
              original == null ? null : images.get(original.getId()),
              ServiceRegistry.getService()::serverNow,
              draft -> saveProduct(original, draft),
              this::closeForm);
      editorHost.setCenter(activeForm);
      BorderPane.setAlignment(activeForm, javafx.geometry.Pos.TOP_CENTER);
      editorHost.setVisible(true);
      editorHost.setManaged(true);
      listPane.setVisible(false);
      listPane.setManaged(false);
      message.setText("");
    } catch (IOException error) {
      closeForm();
      message.setText("Không thể mở biểu mẫu sản phẩm: " + error.getMessage());
    }
  }

  private void saveProduct(Auction original, SellerProductFormView.Draft draft) {
    String id = original == null ? UUID.randomUUID().toString() : original.getId();
    String itemId = original == null ? UUID.randomUUID().toString() : original.getItem().getId();
    String savedPath = original == null ? null : images.get(id);
    run(
        () -> {
          String imagePath = draft.imagePath();
          if (imagePath != null && !imagePath.equals(savedPath)) {
            imagePath = ProductImages.importImage(new File(imagePath), id);
          }
          // Refresh immediate start after image I/O and any executor queue delay.
          LocalDateTime start =
              draft.schedule().startNow()
                  ? ServiceRegistry.getService().serverNow()
                  : draft.schedule().start();
          Item item =
              new Item(
                  itemId,
                  draft.name(),
                  draft.description(),
                  draft.category(),
                  draft.startingPrice(),
                  currentUser.getUsername());
          Auction auction =
              new Auction(
                  id,
                  item,
                  currentUser.getUsername(),
                  draft.startingPrice(),
                  null,
                  AuctionStatus.OPEN,
                  start,
                  draft.schedule().end(),
                  draft.minBidStep());
          service.saveProduct(auction, imagePath == null ? "" : imagePath);
          if (savedPath != null && !savedPath.equals(imagePath)) {
            ProductImages.deleteImportedImage(savedPath, id);
          }
          return null;
        },
        ignored -> {
          closeForm();
          load();
        });
  }

  private void closeForm() {
    if (activeForm != null) {
      activeForm.close();
      activeForm = null;
    }
    if (editorHost == null) {
      return;
    }
    editorHost.setVisible(false);
    editorHost.setManaged(false);
    editorHost.setCenter(null);
    listPane.setVisible(true);
    listPane.setManaged(true);
    addButton.requestFocus();
  }

  @FXML
  private void handleBackToHome() {
    try {
      ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD);
    } catch (IOException e) {
      message.setText("Không thể mở trang chủ.");
    }
  }
}
