package com.auction.server.dao;

import com.google.gson.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** One durable snapshot is the transaction boundary for money and auctions. */
public final class AuctionRepository implements AutoCloseable {
    private final Path file;
    private final FileChannel lockChannel;
    private final FileLock lock;

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

    public JsonObject load() throws IOException {
        if (!Files.exists(file)) return null;
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Invalid state file; refusing to overwrite existing data", e);
        }
    }

    public void save(JsonObject state) throws IOException {
        Path temporary = file.resolveSibling("state.json.tmp");
        byte[] bytes = new Gson().toJson(state).getBytes(StandardCharsets.UTF_8);
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
        // Fail rather than silently replace atomic transactions with a non-atomic write.
        Files.move(
                temporary,
                file,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            lockChannel.close();
        }
    }
}
