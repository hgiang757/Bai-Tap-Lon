package com.auction.shared;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Limits uploads and verifies the actual image format and pixel data. */
public final class ProductImage {
  public static final int MAX_BYTES = 5 * 1024 * 1024;
  private static final long MAX_PIXELS = 40_000_000;
  private static final Set<String> FORMATS = Set.of("png", "jpeg", "gif", "bmp");

  private ProductImage() {}

  /** Reads at most the upload limit, even if the file grows while it is read. */
  public static byte[] read(Path file) throws IOException {
    try (var input = Files.newInputStream(file)) {
      byte[] bytes = input.readNBytes(MAX_BYTES);
      if (bytes.length >= MAX_BYTES) {
        throw new IOException("Image must be smaller than 5 MB");
      }
      return bytes;
    }
  }

  /** Returns the recognized format after decoding the image, or throws for an invalid upload. */
  public static String format(byte[] bytes) throws IOException {
    if (bytes.length >= MAX_BYTES) {
      throw new IOException("Image must be smaller than 5 MB");
    }
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new IOException("Unsupported image");
      }
      var reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!FORMATS.contains(format)) {
          throw new IOException("Unsupported image");
        }
        reader.setInput(input);
        if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
          throw new IOException("Image dimensions too large");
        }
        boolean[] warning = {false};
        reader.addIIOReadWarningListener((source, message) -> warning[0] = true);
        reader.read(0);
        if (warning[0]) {
          throw new IIOException("Image decoder reported damaged data");
        }
        return format;
      } finally {
        reader.dispose();
      }
    }
  }
}
