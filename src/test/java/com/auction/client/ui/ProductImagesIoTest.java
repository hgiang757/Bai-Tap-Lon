package com.auction.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

class ProductImagesIoTest {
    @TempDir Path folder;

    @Test
    void canceledAndExpiredLocalReadStopsWithoutSleeping() throws Exception {
        Path file = Files.write(folder.resolve("sample.bin"), new byte[] {1, 2, 3});
        try (var input = ProductImages.open(file.toString(), 100, 100, 0)) {
            assertThrows(InterruptedIOException.class, input::read);
        }
        Thread.currentThread().interrupt();
        try {
            assertThrows(
                    InterruptedIOException.class,
                    () -> ProductImages.open(file.toString(), 100, 100, 1000));
        } finally {
            Thread.interrupted();
        }
        try (var input = ProductImages.open(file.toString(), 100, 100, 1000)) {
            assertEquals(1, input.read());
        }
    }

    @Test
    void stalledHttpBodyHasReadTimeoutAndDoesNotWaitForever() throws Exception {
        var release = new CountDownLatch(1);
        var headersSent = new CountDownLatch(1);
        try (var server = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"));
                var worker = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(2000);
            Future<?> serving =
                    worker.submit(
                            () -> {
                                try (var socket = server.accept()) {
                                    socket.getOutputStream()
                                            .write(
                                                    "HTTP/1.1 200 OK\r\nContent-Length: 1\r\n\r\n"
                                                            .getBytes(StandardCharsets.US_ASCII));
                                    socket.getOutputStream().flush();
                                    headersSent.countDown();
                                    release.await();
                                } catch (Exception error) {
                                    throw new IllegalStateException(error);
                                }
                            });
            try (var input =
                    ProductImages.open(
                            "http://127.0.0.1:" + server.getLocalPort() + "/image",
                            1000,
                            100,
                            2000)) {
                assertTrue(headersSent.await(2, TimeUnit.SECONDS));
                assertThrows(SocketTimeoutException.class, input::read);
            } finally {
                release.countDown();
            }
            serving.get(3, TimeUnit.SECONDS);
        }
    }
}
