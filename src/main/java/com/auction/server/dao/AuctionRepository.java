package com.auction.server.dao;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

/** One durable snapshot is the transaction boundary for money and auctions. */
public final class AuctionRepository implements AutoCloseable {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final Path file;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private boolean loaded;

    public AuctionRepository(Path directory) throws IOException {
        Files.createDirectories(directory);
        file = directory.resolve("state.json");
        lockChannel =
                FileChannel.open(
                        directory.resolve("server.lock"),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE);
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

    public synchronized JsonObject load() throws IOException {
        if (!lock.isValid()) throw new IOException("Repository is closed");
        loaded = false;
        if (Files.notExists(file)) {
            loaded = true;
            return null;
        }
        try {
            String contents = Files.readString(file, StandardCharsets.UTF_8);
            try (JsonReader reader = new JsonReader(new StringReader(contents))) {
                reader.setStrictness(Strictness.STRICT);
                validateKeys(reader);
                if (reader.peek() != JsonToken.END_DOCUMENT)
                    throw new JsonParseException("Trailing data");
            }
            JsonObject snapshot = JSON.fromJson(contents, JsonObject.class);
            if (snapshot == null) throw new JsonParseException("Expected a JSON object");
            loaded = true;
            return snapshot;
        } catch (RuntimeException e) {
            throw new IOException("Invalid state file; refusing to overwrite existing data", e);
        }
    }

    /** A tree parser otherwise silently keeps only the last occurrence of a duplicate key. */
    private static void validateKeys(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            reader.beginObject();
            Set<String> names = new HashSet<>();
            while (reader.hasNext()) {
                if (!names.add(reader.nextName()))
                    throw new JsonParseException("Duplicate object key at " + reader.getPath());
                validateKeys(reader);
            }
            reader.endObject();
        } else if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            while (reader.hasNext()) validateKeys(reader);
            reader.endArray();
        } else reader.skipValue();
    }

    public synchronized void save(JsonObject state) throws IOException {
        if (!lock.isValid()) throw new IOException("Repository is closed");
        if (!loaded) throw new IOException("Load state successfully before saving");
        if (state == null) throw new IOException("Cannot save a null state");
        Path temporary = file.resolveSibling("state.json.tmp");
        byte[] bytes = JSON.toJson(state).getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel =
                FileChannel.open(
                        temporary,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        try {
            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Refuse the transaction: a non-atomic fallback could destroy the last good state.
            throw new IOException(
                    "Atomic replacement is unavailable; use a local data directory supporting"
                            + " ATOMIC_MOVE. Existing state.json was not replaced.",
                    e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        lockChannel.close(); // Closing the channel also releases its file lock.
    }
}
