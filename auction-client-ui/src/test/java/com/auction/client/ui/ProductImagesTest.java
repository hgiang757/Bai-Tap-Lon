package com.auction.client.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductImagesTest {
    @TempDir Path temporaryDirectory;

    @Test void saoChepAnhVaXuLyDuongDanSai() throws Exception {
        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", temporaryDirectory.toString());
            Path source = temporaryDirectory.resolve("original.png");
            byte[] content = {1, 2, 3};
            Files.write(source, content);
            Path copy = Path.of(ProductImages.importImage(source.toFile(), "auction-1"));
            assertEquals(temporaryDirectory.resolve(".digital-auction/images/auction-1.png").toAbsolutePath(), copy);
            assertArrayEquals(content, Files.readAllBytes(copy));
            assertNull(ProductImages.load(temporaryDirectory.resolve("missing.png").toString(), 48, 48));
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }
}
