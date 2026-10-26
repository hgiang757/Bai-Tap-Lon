package com.auction.client.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppBackground extends StackPane {
    private static final Logger LOGGER = Logger.getLogger(AppBackground.class.getName());
    private static final String IMAGE_PATH = "/com/auction/client/images/app-bg.jpg";
    private static Image sharedImage;
    private static boolean imageLoadAttempted;

    private final ImageView imageView = new ImageView();
    private final Region overlay = new Region();

    public AppBackground() {
        getStyleClass().add("app-background");
        setMinSize(0, 0);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        imageView.setManaged(false);
        imageView.setMouseTransparent(true);
        imageView.setSmooth(true);
        imageView.setPreserveRatio(false);
        overlay.setMouseTransparent(true);
        overlay.setVisible(false);
        overlay.setManaged(false);
        getChildren().addAll(imageView, overlay);

        Image image = loadImage();
        if (image != null && !image.isError()) {
            imageView.setImage(image);
            overlay.setVisible(true);
            overlay.setManaged(true);
            image.widthProperty().addListener(observable -> requestLayout());
            image.heightProperty().addListener(observable -> requestLayout());
            image.errorProperty().addListener((observable, wasError, isError) -> {
                if (isError) {
                    LOGGER.log(Level.WARNING, "Không thể nạp ảnh nền ứng dụng.", image.getException());
                    imageView.setImage(null);
                    overlay.setVisible(false);
                    overlay.setManaged(false);
                }
                requestLayout();
            });
        }
    }

    private static synchronized Image loadImage() {
        if (imageLoadAttempted) {
            return sharedImage;
        }
        imageLoadAttempted = true;
        URL resource = AppBackground.class.getResource(IMAGE_PATH);
        if (resource == null) {
            LOGGER.warning("Không tìm thấy ảnh nền ứng dụng: " + IMAGE_PATH);
            return null;
        }

        try {
            int width = readImageWidth(resource);
            double requestedWidth = width > 1920 ? 1920 : 0;
            sharedImage = new Image(resource.toExternalForm(), requestedWidth, 0, true, true, true);
            sharedImage.errorProperty().addListener((observable, wasError, isError) -> {
                if (isError) {
                    LOGGER.log(Level.WARNING, "Không thể nạp ảnh nền ứng dụng: " + IMAGE_PATH,
                            sharedImage.getException());
                }
            });
            if (sharedImage.isError()) {
                LOGGER.log(Level.WARNING, "Không thể nạp ảnh nền ứng dụng: " + IMAGE_PATH,
                        sharedImage.getException());
                sharedImage = null;
            }
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Không thể đọc kích thước ảnh nền ứng dụng: " + IMAGE_PATH, exception);
            sharedImage = new Image(resource.toExternalForm(), 0, 0, true, true, true);
            if (sharedImage.isError()) {
                LOGGER.log(Level.WARNING, "Không thể nạp ảnh nền ứng dụng: " + IMAGE_PATH,
                        sharedImage.getException());
                sharedImage = null;
            }
        }
        return sharedImage;
    }

    private static int readImageWidth(URL resource) throws IOException {
        try (InputStream imageData = resource.openStream();
             ImageInputStream input = ImageIO.createImageInputStream(imageData)) {
            if (input == null) {
                throw new IOException("Không thể mở dữ liệu ảnh.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Không tìm thấy bộ đọc định dạng ảnh.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return reader.getWidth(0);
            } finally {
                reader.dispose();
            }
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        Image image = imageView.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        double scale = Math.max(getWidth() / image.getWidth(), getHeight() / image.getHeight());
        double width = image.getWidth() * scale;
        double height = image.getHeight() * scale;
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.relocate((getWidth() - width) / 2, (getHeight() - height) / 2);
    }
}
