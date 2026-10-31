package com.auction.client.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.image.Image;

/** L?u v? n?p ?nh s?n ph?m. */
public final class ProductImages {
    private static final Logger LOGGER = Logger.getLogger(ProductImages.class.getName());
    private ProductImages() { }

    public static String importImage(File source, String auctionId) throws IOException {
        try {
            String name = source.getName();
            int dot = name.lastIndexOf('.');
            if (dot < 0 || auctionId == null || !auctionId.matches("[a-zA-Z0-9_-]+")) {
                throw new IOException("T?n ?nh ho?c m? phi?n kh?ng h?p l?.");
            }
            Path directory = imageDirectory();
            Files.createDirectories(directory);
            Path target = directory.resolve(auctionId + name.substring(dot)).toAbsolutePath();
            if (!source.toPath().toAbsolutePath().normalize().equals(target.normalize())) {
                Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target.toString();
        } catch (IOException | RuntimeException ex) {
            throw new IOException("Kh?ng th? sao ch?p ?nh s?n ph?m: " + ex.getMessage(), ex);
        }
    }

    public static Image load(String path, double width, double height) {
        try {
            if (path == null || path.isBlank()) throw new IOException("???ng d?n ?nh tr?ng.");
            String url;
            if (path.startsWith("http://") || path.startsWith("https://")) {
                url = path;
            } else {
                Path file = path.startsWith("file:") ? Path.of(URI.create(path)) : Path.of(path);
                if (!Files.isRegularFile(file)) throw new IOException("Kh?ng t?m th?y t?p ?nh.");
                url = file.toUri().toString();
            }
            Image image = new Image(url, width, height, true, true);
            if (image.isError()) throw new IOException("Kh?ng th? ??c ?nh.", image.getException());
            return image;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Kh?ng th? n?p ?nh s?n ph?m: " + path, ex);
            return null;
        }
    }

    /** Ch? x?a b?n sao do ?ng d?ng qu?n l?. */
    public static void deleteImportedImage(String path, String auctionId) throws IOException {
        if (path == null || path.isBlank() || auctionId == null) return;
        try {
            Path file = Path.of(path).toAbsolutePath().normalize();
            Path directory = imageDirectory().toAbsolutePath().normalize();
            if (directory.equals(file.getParent()) && file.getFileName().toString().startsWith(auctionId + ".")) {
                Files.deleteIfExists(file);
            }
        } catch (java.nio.file.InvalidPathException ex) {
            LOGGER.log(Level.WARNING, "???ng d?n ?nh kh?ng h?p l?: " + path, ex);
        }
    }

    private static Path imageDirectory() {
        return Path.of(System.getProperty("user.home"), ".digital-auction", "images");
    }
}
