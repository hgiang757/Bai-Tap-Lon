package com.auction.server.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/** Lưu ví và phiên đấu giá cùng nhau trong một bản chụp trạng thái. */
public final class AuctionRepository implements AutoCloseable {
  private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
  private final Path file;
  private final FileChannel lockChannel;
  private final FileLock lock;
  private boolean loaded;

  /** Mở thư mục dữ liệu và lấy khóa ghi độc quyền. */
  public AuctionRepository(Path directory) throws IOException {
    Files.createDirectories(directory);
    file = directory.resolve("state.json");
    lockChannel =
        FileChannel.open(
            directory.resolve("server.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    FileLock acquired;
    try {
      acquired = lockChannel.tryLock();
    } catch (OverlappingFileLockException e) {
      lockChannel.close();
      throw new IOException("Data directory is already in use", e);
    } catch (RuntimeException | IOException e) {
      lockChannel.close();
      throw e;
    }
    if (acquired == null) {
      lockChannel.close();
      throw new IOException("Data directory is already in use");
    }
    lock = acquired;
  }

  /** Đọc JSON theo chế độ nghiêm ngặt, từ chối khóa trùng và giữ lại tệp lỗi. */
  public synchronized JsonObject load() throws IOException {
    if (!lock.isValid()) {
      throw new IOException("Repository is closed");
    }
    loaded = false;
    if (Files.notExists(file)) {
      loaded = true;
      return null;
    }
    String contents = Files.readString(file, StandardCharsets.UTF_8);
    try {
      try (JsonReader reader = new JsonReader(new StringReader(contents))) {
        reader.setStrictness(Strictness.STRICT);
        validateKeys(reader);
        if (reader.peek() != JsonToken.END_DOCUMENT) {
          throw new JsonParseException("Trailing data");
        }
      }
      JsonObject snapshot = JSON.fromJson(contents, JsonObject.class);
      if (snapshot == null) {
        throw new JsonParseException("Expected a JSON object");
      }
      loaded = true;
      return snapshot;
    } catch (IOException | JsonParseException | IllegalStateException e) {
      throw new IOException("Invalid state file; refusing to overwrite existing data", e);
    }
  }

  /** Phát hiện khóa trùng trước khi bộ phân tích chỉ giữ lại giá trị cuối. */
  private static void validateKeys(JsonReader reader) throws IOException {
    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
      reader.beginObject();
      Set<String> names = new HashSet<>();
      while (reader.hasNext()) {
        if (!names.add(reader.nextName())) {
          throw new JsonParseException("Duplicate object key at " + reader.getPath());
        }
        validateKeys(reader);
      }
      reader.endObject();
    } else if (reader.peek() == JsonToken.BEGIN_ARRAY) {
      reader.beginArray();
      while (reader.hasNext()) {
        validateKeys(reader);
      }
      reader.endArray();
    } else {
      reader.skipValue();
    }
  }

  /** Ghi và đồng bộ tệp tạm xuống đĩa trước khi thay thế tệp dữ liệu bằng thao tác nguyên tử. */
  public synchronized void save(JsonObject state) throws IOException {
    if (!lock.isValid()) {
      throw new IOException("Repository is closed");
    }
    if (!loaded) {
      throw new IOException("Load state successfully before saving");
    }
    if (state == null) {
      throw new IOException("Cannot save a null state");
    }
    Path temporary = file.resolveSibling("state.json.tmp");
    byte[] bytes = JSON.toJson(state).getBytes(StandardCharsets.UTF_8);
    try (FileChannel channel =
        FileChannel.open(
            temporary,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
    try {
      Files.move(
          temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      // Từ chối giao dịch để bảo toàn tệp trạng thái hợp lệ gần nhất.
      throw new IOException(
          "Atomic replacement is unavailable; use a local data directory supporting"
              + " ATOMIC_MOVE. Existing state.json was not replaced.",
          e);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    lockChannel.close(); // Đóng kênh đồng thời giải phóng khóa tệp.
  }
}
