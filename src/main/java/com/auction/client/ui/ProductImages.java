package com.auction.client.ui;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.image.Image;

/** Lưu và nạp ảnh sản phẩm. */
public final class ProductImages {
  private static final Logger LOGGER = Logger.getLogger(ProductImages.class.getName());

  private ProductImages() {}

  /** Copies a selected image into the client image directory for its auction. */
  public static String importImage(File source, String auctionId) throws IOException {
    try {
      String name = source.getName();
      int dot = name.lastIndexOf('.');
      if (dot < 0 || auctionId == null || !auctionId.matches("[a-zA-Z0-9_-]+")) {
        throw new IOException("Tên ảnh hoặc mã phiên không hợp lệ.");
      }
      Path directory = imageDirectory();
      Files.createDirectories(directory);
      Path target = directory.resolve(auctionId + name.substring(dot)).toAbsolutePath();
      if (!source.toPath().toAbsolutePath().normalize().equals(target.normalize())) {
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
      }
      return target.toString();
    } catch (IOException | RuntimeException ex) {
      throw new IOException("Không thể sao chép ảnh sản phẩm: " + ex.getMessage(), ex);
    }
  }

  /** Loads an image preview, returning null for an absent or unreadable image. */
  public static Image load(String path, double width, double height) {
    try {
      if (path == null || path.isBlank()) {
        return null;
      }
      try (InputStream input = open(path, 3_000, 3_000, 10_000)) {
        Image image = new Image(input, width, height, true, true);
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedIOException("Đã hủy tải ảnh.");
        }
        if (image.isError()) {
          throw new IOException("Không thể đọc ảnh.", image.getException());
        }
        return image;
      }
    } catch (Exception ex) {
      if (Thread.currentThread().isInterrupted() || ex instanceof InterruptedIOException) {
        return null;
      }
      LOGGER.log(Level.WARNING, "Không thể nạp ảnh sản phẩm: " + path, ex);
      return null;
    }
  }

  /** Timeout cho HTTP; kiểm tra interrupt/deadline giữa các lần đọc và luôn đóng stream. */
  static InputStream open(String path, int connectMillis, int readMillis, long totalMillis)
      throws IOException {
    long deadline =
        System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(totalMillis);
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedIOException("Đã hủy tải ảnh.");
    }
    InputStream input;
    if (path.startsWith("http://") || path.startsWith("https://")) {
      var connection = URI.create(path).toURL().openConnection();
      connection.setConnectTimeout(connectMillis);
      connection.setReadTimeout(readMillis);
      input = connection.getInputStream();
    } else {
      Path file = path.startsWith("file:") ? Path.of(URI.create(path)) : Path.of(path);
      if (!Files.isRegularFile(file)) {
        throw new IOException("Không tìm thấy tệp ảnh.");
      }
      input = Files.newInputStream(file);
    }
    return new FilterInputStream(input) {
      private void check() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) {
          throw new InterruptedIOException("Đã hủy hoặc hết thời gian tải ảnh.");
        }
      }

      @Override
      public int read() throws IOException {
        check();
        int value = in.read();
        check();
        return value;
      }

      @Override
      public int read(byte[] bytes, int offset, int length) throws IOException {
        check();
        int value = in.read(bytes, offset, length);
        check();
        return value;
      }

      @Override
      public long skip(long n) throws IOException {
        check();
        return in.skip(n);
      }
    };
  }

  /** Chỉ xóa bản sao do ứng dụng quản lý. */
  public static void deleteImportedImage(String path, String auctionId) throws IOException {
    if (path == null || path.isBlank() || auctionId == null) {
      return;
    }
    try {
      Path file = Path.of(path).toAbsolutePath().normalize();
      Path directory = imageDirectory().toAbsolutePath().normalize();
      if (directory.equals(file.getParent())
          && file.getFileName().toString().startsWith(auctionId + ".")) {
        Files.deleteIfExists(file);
      }
    } catch (java.nio.file.InvalidPathException ex) {
      LOGGER.log(Level.WARNING, "Đường dẫn ảnh không hợp lệ: " + path, ex);
    }
  }

  private static Path imageDirectory() {
    return Path.of(
        System.getProperty(
            "auction.client.images",
            Path.of(System.getProperty("user.home"), ".digital-auction", "images").toString()));
  }
}
