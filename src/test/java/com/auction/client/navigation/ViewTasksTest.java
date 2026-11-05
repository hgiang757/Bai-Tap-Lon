package com.auction.client.navigation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ViewTasksTest {
    private final List<Runnable> workers = new ArrayList<>(), ui = new ArrayList<>();
    private final ViewTasks tasks = new ViewTasks(workers::add, ui::add);
    private final List<String> shown = new ArrayList<>();

    @Test
    void lateReadCannotReplaceNewerResult() {
        tasks.submit("list", () -> "old", shown::add, e -> fail(e));
        tasks.submit("list", () -> "new", shown::add, e -> fail(e));
        workers.get(1).run();
        workers.get(0).run();
        ui.forEach(Runnable::run);
        assertEquals(List.of("new"), shown);
    }

    @Test
    void leavingViewSuppressesAlreadyQueuedCallbacksAndObserverEvents() {
        tasks.submit("read", () -> "read", shown::add, e -> fail(e));
        workers.getFirst().run();
        tasks.dispatch(() -> shown.add("event"));
        tasks.close();
        ui.forEach(Runnable::run);
        assertTrue(shown.isEmpty());
    }

    @Test
    void leavingViewDoesNotCancelOrRetrySubmittedMutation() {
        int[] calls = {0};
        tasks.submit(
                "bid",
                () -> {
                    calls[0]++;
                    return "accepted";
                },
                shown::add,
                e -> fail(e));
        tasks.close();
        workers.getFirst().run();
        ui.forEach(Runnable::run);
        assertEquals(1, calls[0]);
        assertTrue(shown.isEmpty());
    }

    @Test
    void failureGoesToUiAndClosedScopeRejectsNewWork() {
        tasks.<String>submit(
                "read",
                () -> {
                    throw new IllegalStateException("offline");
                },
                shown::add,
                error -> shown.add(error.getMessage()));
        workers.getFirst().run();
        assertTrue(shown.isEmpty());
        ui.getFirst().run();
        assertEquals(List.of("offline"), shown);
        tasks.close();
        tasks.submit("next", () -> "unexpected", shown::add, e -> fail(e));
        assertEquals(1, workers.size());
    }
}
