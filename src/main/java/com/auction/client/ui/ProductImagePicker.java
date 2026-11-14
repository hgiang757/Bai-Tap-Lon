package com.auction.client.ui;

import com.auction.client.navigation.ViewTasks;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Chọn/kiểm tra/preview ảnh trên scope riêng; không nhập ảnh vào kho hoặc gọi service. */
public final class ProductImagePicker extends VBox implements AutoCloseable {
  private final ViewTasks tasks = ViewTasks.images();
  private final Button choose = new Button("Chọn ảnh");
  private final Button remove = new Button("Bỏ ảnh");
  private final Label filename = new Label();
  private final ImageView preview = new ImageView();
  private final String savedPath;
  private String path;
  private String error = "";
  private boolean loading;
  private Runnable changed = () -> {};

  /** Builds image selection controls and preserves the original saved image path. */
  public ProductImagePicker(String storedPath) {
    savedPath = storedPath == null || storedPath.isBlank() ? null : storedPath;
    path = savedPath;
    setSpacing(8);
    Label label = new Label("Ảnh sản phẩm");
    label.getStyleClass().add("seller-section-title");
    filename.getStyleClass().add("field-hint");
    filename.setWrapText(true);
    filename.setMinWidth(0);
    choose.setId("productChooseImage");
    remove.setId("productRemoveImage");
    preview.setId("productImage");
    preview.setFitWidth(140);
    preview.setFitHeight(84);
    preview.setPreserveRatio(true);
    preview.visibleProperty().bind(preview.imageProperty().isNotNull());
    preview.managedProperty().bind(preview.visibleProperty());
    choose.setOnAction(
        event -> {
          FileChooser chooser = new FileChooser();
          chooser.setTitle("Chọn ảnh sản phẩm");
          chooser
              .getExtensionFilters()
              .add(
                  new FileChooser.ExtensionFilter(
                      "Ảnh PNG/JPG/GIF/BMP (< 5 MB)",
                      "*.png",
                      "*.jpg",
                      "*.jpeg",
                      "*.gif",
                      "*.bmp"));
          File file = chooser.showOpenDialog(getScene().getWindow());
          if (file != null) {
            select(file);
          }
        });
    remove.setOnAction(
        event -> {
          tasks.cancelAll();
          path = null;
          loading = false;
          error = "";
          preview.setImage(null);
          refresh();
          changed.run();
        });
    getChildren().addAll(label, new FlowPane(8, 8, choose, remove), filename, preview);
    refresh();
    if (path != null) {
      loadPreview(new File(path), true);
    }
  }

  public void setOnChange(Runnable changed) {
    this.changed = changed;
  }

  /** Returns the selected image path, or null when no image is selected. */
  public String path() {
    return path;
  }

  /** Returns the most recent image validation error. */
  public String error() {
    return error;
  }

  public boolean isLoading() {
    return loading;
  }

  public boolean isClosed() {
    return tasks.isClosed();
  }

  /** Cũng cho phép test chọn file tạm qua cùng đường kiểm tra với native FileChooser. */
  public void select(File file) {
    loadPreview(file, false);
  }

  private void loadPreview(File file, boolean existing) {
    if (tasks.isClosed()) {
      return;
    }
    loading = true;
    error = "";
    changed.run();
    tasks.submit(
        "preview",
        () -> {
          String lower = file.getName().toLowerCase(Locale.ROOT);
          if (!file.isFile()
              || file.length() >= 5 * 1024 * 1024
              || (!existing
                  && !(lower.endsWith(".png")
                      || lower.endsWith(".jpg")
                      || lower.endsWith(".jpeg")
                      || lower.endsWith(".gif")
                      || lower.endsWith(".bmp")))) {
            throw new IOException("Ảnh phải là PNG/JPG/GIF/BMP và nhỏ hơn 5 MB.");
          }
          var candidate = ProductImages.load(file.getAbsolutePath(), 140, 84);
          if (candidate == null) {
            throw new IOException("Không đọc được ảnh. Chọn lại hoặc bỏ ảnh.");
          }
          return candidate;
        },
        image -> {
          path = file.getAbsolutePath();
          preview.setImage(image);
          loading = false;
          refresh();
          changed.run();
        },
        failure -> {
          loading = false;
          error = failure.getMessage();
          refresh();
          changed.run();
        });
  }

  private void refresh() {
    remove.setVisible(path != null || !error.isEmpty());
    remove.setManaged(remove.isVisible());
    filename.setText(
        path == null
            ? "Chưa chọn ảnh · PNG/JPG/GIF/BMP, dưới 5 MB"
            : path.equals(savedPath) ? "Ảnh đã lưu" : new File(path).getName() + " · Chưa lưu");
    filename.setTooltip(
        path == null || path.equals(savedPath) ? null : new Tooltip(new File(path).getName()));
  }

  @Override
  public void close() {
    tasks.close();
    choose.setOnAction(null);
    remove.setOnAction(null);
    changed = () -> {};
  }
}
