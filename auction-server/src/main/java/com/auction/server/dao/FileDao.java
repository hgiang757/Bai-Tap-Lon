package com.auction.server.dao;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO tổng quát lưu/tải một danh sách đối tượng ra file bằng Java Serialization.
 * Ghi ra file tạm rồi đổi tên để tránh hỏng file khi đang ghi mà gặp lỗi.
 *
 * @param <T> kiểu đối tượng, bắt buộc implements {@link Serializable}
 */
public class FileDao<T extends Serializable> {

    private final Path file;

    public FileDao(Path file) {
        this.file = file;
    }

    public synchronized void saveAll(List<T> items) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
            out.writeObject(new ArrayList<>(items));
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("unchecked")
    public synchronized List<T> loadAll() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (List<T>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("File dữ liệu không đọc được: " + file, e);
        }
    }
}
