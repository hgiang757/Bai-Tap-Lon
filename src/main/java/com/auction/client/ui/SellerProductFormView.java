package com.auction.client.ui;

import com.auction.client.model.Auction;
import com.auction.client.model.ItemCategory;
import com.auction.client.util.FormatUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** View của bản nháp sản phẩm: binding/validation và cleanup; Controller chịu trách nhiệm lưu. */
public final class SellerProductFormView extends VBox implements AutoCloseable {
  private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
  @FXML private Label title;
  @FXML private Label validation;
  @FXML private Label priceHint;
  @FXML private Label stepHint;
  @FXML private TextField name;
  @FXML private TextField price;
  @FXML private TextField step;
  @FXML private TextArea description;
  @FXML private ComboBox<String> category;
  @FXML private Button back;
  @FXML private Button cancel;
  @FXML private Button save;
  @FXML private ScrollPane scroll;
  @FXML private FlowPane columns;
  @FXML private FlowPane prices;
  @FXML private VBox card;
  @FXML private VBox information;
  @FXML private VBox scheduling;
  @FXML private VBox priceGroup;
  @FXML private VBox stepGroup;
  @FXML private HBox footer;
  private final AuctionScheduleInput schedule;
  private final ProductImagePicker imagePicker;
  private boolean busy;
  private boolean interacted;
  private String saveError = "";

  /** Contains product form values without performing any authoritative server action. */
  public record Draft(
      String name,
      String description,
      String category,
      long startingPrice,
      long minBidStep,
      AuctionScheduleInput.Selection schedule,
      String imagePath) {}

  /** Loads the product form and connects its draft, validation and callbacks. */
  public SellerProductFormView(
      Auction original,
      String imagePath,
      Supplier<LocalDateTime> clock,
      Consumer<Draft> submit,
      Runnable onCancel)
      throws IOException {
    FXMLLoader loader =
        new FXMLLoader(
            getClass().getResource("/com/auction/client/views/seller-product-form.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    for (Control field : List.of(name, description, category, price, step)) {
      Label label = (Label) ((VBox) field.getParent()).getChildren().getFirst();
      label.setLabelFor(field);
    }
    schedule = new AuctionScheduleInput(clock, original);
    imagePicker = new ProductImagePicker(imagePath);
    scheduling.getChildren().addAll(schedule, imagePicker);
    title.setText(original == null ? "Thêm sản phẩm" : "Sửa sản phẩm");
    for (ItemCategory value : ItemCategory.values()) {
      category.getItems().add(value.getLabel());
    }
    if (original != null) {
      name.setText(original.getItemName());
      description.setText(original.getItem().getDescription());
      category.setValue(original.getItemCategory());
      price.setText(Long.toString((long) original.getStartingPrice()));
      step.setText(Long.toString((long) original.getMinBidStep()));
    }
    setMaxHeight(Region.USE_PREF_SIZE);
    footer.setMinHeight(Region.USE_PREF_SIZE);
    for (Label hint : List.of(priceHint, stepHint)) {
      hint.visibleProperty().bind(hint.textProperty().isNotEmpty());
      hint.managedProperty().bind(hint.visibleProperty());
    }
    scroll.prefViewportHeightProperty().bind(card.heightProperty().add(2));
    columns.prefWrapLengthProperty().bind(columns.widthProperty());
    for (VBox column : List.of(information, scheduling)) {
      column
          .prefWidthProperty()
          .bind(
              Bindings.createDoubleBinding(
                  () -> {
                    double available = Math.max(0, columns.getWidth());
                    return getWidth() >= 820
                        ? Math.max(0, Math.floor((available - 28) / 2) - 1)
                        : available;
                  },
                  columns.widthProperty(),
                  widthProperty()));
      column.maxWidthProperty().bind(column.prefWidthProperty());
    }
    prices.prefWrapLengthProperty().bind(prices.widthProperty());
    for (VBox group : List.of(priceGroup, stepGroup)) {
      group
          .prefWidthProperty()
          .bind(
              Bindings.createDoubleBinding(
                  () ->
                      getWidth() >= 820
                          ? Math.max(0, Math.floor((prices.getWidth() - 16) / 2) - 1)
                          : Math.max(0, prices.getWidth()),
                  prices.widthProperty(),
                  widthProperty()));
      group.maxWidthProperty().bind(group.prefWidthProperty());
    }
    for (TextField field : List.of(name, price, step)) {
      field.textProperty().addListener((o, before, value) -> changed());
    }
    description.textProperty().addListener((o, before, value) -> changed());
    category.valueProperty().addListener((o, before, value) -> changed());
    schedule.setOnChange(this::changed);
    imagePicker.setOnChange(this::changed);
    back.setOnAction(
        event -> {
          if (!busy) {
            onCancel.run();
          }
        });
    cancel.setOnAction(
        event -> {
          if (!busy) {
            onCancel.run();
          }
        });
    save.setOnAction(
        event -> {
          if (busy || isClosed()) {
            return;
          }
          interacted = true;
          refresh(); // Recheck a schedule that may have expired while the form was open.
          if (!save.isDisabled()) {
            submit.accept(
                new Draft(
                    name.getText().strip(),
                    description.getText().strip(),
                    category.getValue(),
                    amount(price.getText()),
                    amount(step.getText()),
                    schedule.selection(),
                    imagePicker.path()));
          }
        });
    save.setDefaultButton(true);
    cancel.setCancelButton(true);
    setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ESCAPE && !busy && !event.isConsumed()) {
            onCancel.run();
            event.consume();
          }
        });
    refresh();
    javafx.application.Platform.runLater(
        () -> {
          if (!isClosed() && getScene() != null) {
            name.requestFocus();
          }
        });
  }

  private void changed() {
    interacted = true;
    saveError = "";
    refresh();
  }

  private String validationMessage() {
    if (!imagePicker.error().isEmpty()) {
      return imagePicker.error();
    }
    if (name.getText().isBlank()) {
      return "Nhập tên sản phẩm.";
    }
    if (name.getText().strip().length() > 200) {
      return "Tên sản phẩm tối đa 200 ký tự.";
    }
    if (description.getText().isBlank()) {
      return "Nhập mô tả sản phẩm.";
    }
    if (description.getText().strip().length() > 10_000) {
      return "Mô tả tối đa 10.000 ký tự.";
    }
    if (category.getValue() == null) {
      return "Chọn loại sản phẩm.";
    }
    if (amount(price.getText()) == null) {
      return "Giá khởi điểm cần là số nguyên từ 1 đến 9.000 tỷ đồng.";
    }
    if (amount(step.getText()) == null) {
      return "Nhập bước giá nguyên dương, tối đa 9.000 tỷ đồng.";
    }
    String dates = schedule.validationMessage();
    if (!dates.isEmpty()) {
      return dates;
    }
    return imagePicker.error();
  }

  private void refresh() {
    String invalid = validationMessage();
    save.setDisable(busy || imagePicker.isLoading() || !invalid.isEmpty());
    String message = !saveError.isEmpty() ? saveError : interacted ? invalid : "";
    validation.setText(
        busy
            ? "Đang xử lý…"
            : imagePicker.isLoading()
                ? "Đang kiểm tra ảnh…"
                : message.isEmpty() && !invalid.isEmpty()
                    ? "Điền các trường có dấu * để lưu sản phẩm."
                    : message);
    validation.pseudoClassStateChanged(
        ERROR, !busy && !imagePicker.isLoading() && !message.isEmpty());
    priceHint.setText(
        amount(price.getText()) == null ? "" : FormatUtil.formatCurrency(amount(price.getText())));
    stepHint.setText(
        amount(step.getText()) == null ? "" : FormatUtil.formatCurrency(amount(step.getText())));
  }

  static Long amount(String text) {
    try {
      long value = new BigDecimal(text.strip()).longValueExact();
      return value > 0 && value <= 9_000_000_000_000L ? value : null;
    } catch (NumberFormatException | ArithmeticException | NullPointerException error) {
      return null;
    }
  }

  /** Disables form actions while a product save is in progress. */
  public void setBusy(boolean busy) {
    this.busy = busy;
    card.setDisable(busy);
    back.setDisable(busy);
    cancel.setDisable(busy);
    save.setText(busy ? "Đang lưu…" : "Lưu");
    refresh();
  }

  /** Displays a save error while preserving the product draft. */
  public void showSaveError(String error) {
    saveError = error == null || error.isBlank() ? "Không thể lưu. Vui lòng thử lại." : error;
    refresh();
  }

  public boolean isClosed() {
    return imagePicker.isClosed();
  }

  @Override
  public void close() {
    imagePicker.close();
    save.setDefaultButton(false);
    cancel.setCancelButton(false);
    save.setOnAction(null);
    back.setOnAction(null);
    cancel.setOnAction(null);
    setOnKeyPressed(null);
  }
}
