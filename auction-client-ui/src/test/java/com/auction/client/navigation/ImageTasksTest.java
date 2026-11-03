package com.auction.client.navigation;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ImageTasksTest {
    @Test void blockedAndFullImageQueueDoesNotBlockBusinessCommands() throws Exception {
        var images = ViewTasks.pool("test-image", 1, 1);
        var business = ViewTasks.pool("test-business", 1, 1);
        var ui = new ConcurrentLinkedQueue<Runnable>();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var bidRan = new CountDownLatch(1);
        var failures = new ArrayList<Throwable>();
        var imageView = new ViewTasks(images, ui::add, true);
        var serviceView = new ViewTasks(business, ui::add);
        try {
            imageView.submit("running", () -> { entered.countDown(); release.await(); return 1; }, x -> {}, failures::add);
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            assertTrue(imageView.submit("queued", () -> 2, x -> {}, failures::add));
            assertFalse(imageView.submit("rejected", () -> fail("Rejected work must not run"), x -> {}, failures::add));
            serviceView.submit("bid", () -> { bidRan.countDown(); return 1; }, x -> {}, failures::add);
            assertTrue(bidRan.await(3, TimeUnit.SECONDS), "Bid must run while image remains blocked");
            Runnable callback; while ((callback = ui.poll()) != null) callback.run();
            assertEquals(1, failures.size()); assertInstanceOf(ViewTasks.NotSubmittedException.class, failures.getFirst());
            assertEquals(1, images.getQueue().size());
        } finally { imageView.close(); serviceView.close(); release.countDown(); images.shutdownNow(); business.shutdownNow(); }
    }

    @Test void closingImageScopeRemovesQueuedWorkAndInterruptsOnlyItsRunningRead() throws Exception {
        var executor = ViewTasks.pool("test-images", 1, 3);
        var ui = new ConcurrentLinkedQueue<Runnable>();
        var started = new CountDownLatch(1); var interrupted = new CountDownLatch(1); var otherRan = new CountDownLatch(1);
        AtomicInteger obsolete = new AtomicInteger();
        var oldView = new ViewTasks(executor, ui::add, true);
        var otherView = new ViewTasks(executor, ui::add, true);
        try {
            oldView.submit("running", () -> {
                started.countDown();
                try { new CountDownLatch(1).await(); } catch (InterruptedException error) { interrupted.countDown(); throw error; }
                return 1;
            }, x -> fail("Disposed image reached UI"), e -> fail("Disposed error reached UI"));
            assertTrue(started.await(3, TimeUnit.SECONDS));
            oldView.submit("obsolete", obsolete::incrementAndGet, x -> fail(), e -> fail(e));
            otherView.submit("other", () -> { otherRan.countDown(); return 1; }, x -> {}, e -> fail(e));
            oldView.close();
            assertTrue(interrupted.await(3, TimeUnit.SECONDS));
            assertTrue(otherRan.await(3, TimeUnit.SECONDS));
            assertEquals(0, obsolete.get());
            Runnable callback; while ((callback = ui.poll()) != null) callback.run();
        } finally { oldView.close(); otherView.close(); executor.shutdownNow(); }
    }

    @Test void changingFilterOrReusingCellDropsOldQueuedAndAlreadyDeliveredImages() {
        var workers = new ArrayList<Runnable>(); var ui = new ArrayList<Runnable>(); var shown = new ArrayList<String>();
        var view = new ViewTasks(workers::add, ui::add, true);
        view.submit("cell", () -> "old", shown::add, e -> fail(e));
        workers.getFirst().run(); // Kết quả đã xếp vào hàng đợi UI.
        view.cancelAll();
        view.submit("cell", () -> "new", shown::add, e -> fail(e));
        workers.get(1).run(); ui.forEach(Runnable::run);
        assertEquals(List.of("new"), shown);
        view.submit("card", () -> fail("Obsolete queued image decoded"), x -> {}, e -> fail(e));
        view.cancelAll(); workers.get(2).run();
        assertEquals(List.of("new"), shown);
    }

    @Test void oneFailedImageDoesNotDisableLaterLoads() {
        var workers = new ArrayList<Runnable>(); var ui = new ArrayList<Runnable>(); var shown = new ArrayList<String>();
        var view = new ViewTasks(workers::add, ui::add, true);
        view.<String>submit("image", () -> { throw new IllegalStateException("bad image"); }, shown::add, e -> shown.add(e.getMessage()));
        workers.getFirst().run(); ui.removeFirst().run();
        view.submit("image", () -> "next image", shown::add, e -> fail(e));
        workers.get(1).run(); ui.removeFirst().run();
        assertEquals(List.of("bad image", "next image"), shown);
    }

    @Test void rejectedBusinessWorkReportsNotSentAndAcceptedMutationSurvivesClose() throws Exception {
        var executor = ViewTasks.pool("test-business", 1, 1); var ui = new ConcurrentLinkedQueue<Runnable>();
        var started = new CountDownLatch(1); var release = new CountDownLatch(1); var acceptedRan = new CountDownLatch(1);
        var failures = new ArrayList<Throwable>(); var view = new ViewTasks(executor, ui::add);
        try {
            executor.execute(() -> { started.countDown(); try { release.await(); } catch (InterruptedException error) { Thread.currentThread().interrupt(); } });
            assertTrue(started.await(3, TimeUnit.SECONDS));
            view.submit("accepted", () -> { acceptedRan.countDown(); return 1; }, x -> fail("Closed callback"), failures::add);
            assertFalse(view.submit("rejected", () -> fail("Queue rejected mutation ran"), x -> {}, failures::add));
            ui.remove().run();
            assertInstanceOf(ViewTasks.NotSubmittedException.class, failures.getFirst());
            view.close(); release.countDown();
            assertTrue(acceptedRan.await(3, TimeUnit.SECONDS));
        } finally { release.countDown(); view.close(); executor.shutdownNow(); }
    }
}
