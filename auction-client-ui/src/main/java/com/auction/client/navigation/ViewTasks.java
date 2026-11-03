package com.auction.client.navigation;

import javafx.application.Platform;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Tác vụ nền thuộc một màn hình. Chỉ kết quả mới nhất của mỗi khóa được hiển thị.
 * Scope nghiệp vụ không hủy hay gửi lại lệnh đã nhận khi đóng màn hình.
 * Scope ảnh dùng pool riêng và hủy công việc lỗi thời khi đóng/cancelAll.
 * Gọi submit/close trên JavaFX Application Thread; dispatch dùng được từ observer.
 */
public final class ViewTasks implements AutoCloseable {
    private static final Executor WORKERS = pool("client-service", 4, 64);
    private static final Executor IMAGES = pool("client-image", 2, 32);
    private final Executor workers;
    private final Consumer<Runnable> ui;
    private final Map<String, Long> versions = new HashMap<>();
    private final Map<String, FutureTask<Void>> pending = new HashMap<>();
    private final boolean cancellable;
    private long sequence;
    private volatile boolean closed;

    public ViewTasks() { this(WORKERS, Platform::runLater); }

    ViewTasks(Executor workers, Consumer<Runnable> ui) {
        this(workers, ui, false);
    }

    ViewTasks(Executor workers, Consumer<Runnable> ui, boolean cancellable) {
        this.workers = workers;
        this.ui = ui;
        this.cancellable = cancellable;
    }

    /** Scope đọc ảnh: dùng hàng đợi riêng, cho phép hủy công việc lỗi thời. */
    public static ViewTasks images() { return new ViewTasks(IMAGES, Platform::runLater, true); }

    static ThreadPoolExecutor pool(String name, int threads, int capacity) {
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), runnable -> {
                    Thread thread = new Thread(runnable, name);
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    /** Chỉ lỗi này chứng minh công việc chưa được executor nhận, khác lỗi sau khi gửi lệnh. */
    public static final class NotSubmittedException extends RejectedExecutionException {
        NotSubmittedException(RejectedExecutionException cause) {
            super("Hàng đợi đang đầy. Tác vụ chưa được gửi; vui lòng thử lại.", cause);
        }
    }

    /** Trả false nếu scope đóng hoặc hàng đợi từ chối; rejection của scope mở vẫn có callback lỗi. */
    public <T> boolean submit(String key, Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        if (closed) return false;
        if (cancellable) cancel(key);
        long version = ++sequence;
        versions.put(key, version);
        FutureTask<Void> task = new FutureTask<>(() -> {
            T value;
            try { value = work.call(); }
            catch (Exception error) {
                deliver(key, version, () -> failure.accept(error));
                return null;
            }
            deliver(key, version, () -> success.accept(value));
            return null;
        });
        pending.put(key, task);
        try { workers.execute(task); return true; }
        catch (RejectedExecutionException error) {
            pending.remove(key);
            deliver(key, version, () -> failure.accept(new NotSubmittedException(error)));
            return false;
        }
    }

    private void deliver(String key, long version, Runnable action) {
        dispatch(() -> {
            if (!java.util.Objects.equals(versions.get(key), version)) return;
            versions.remove(key); pending.remove(key);
            action.run();
        });
    }

    private void cancel(String key) {
        versions.remove(key);
        FutureTask<Void> task = pending.remove(key);
        if (task != null) {
            task.cancel(true);
            if (workers instanceof ThreadPoolExecutor pool) pool.remove(task);
        }
    }

    /** Chỉ dùng cho ảnh; không được hủy lệnh nghiệp vụ qua API này. */
    public void cancelAll() {
        if (!cancellable) throw new IllegalStateException("Không hủy lệnh nghiệp vụ đã nhận.");
        for (String key : java.util.List.copyOf(versions.keySet())) cancel(key);
    }

    public void dispatch(Runnable action) {
        if (!closed) ui.accept(() -> { if (!closed) action.run(); });
    }

    public boolean isClosed() { return closed; }

    @Override public void close() {
        closed = true;
        if (cancellable) cancelAll();
    }
}
