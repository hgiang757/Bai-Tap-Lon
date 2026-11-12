package com.auction.client.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.client.model.*;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ServiceRegistry;

import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

/** Kiểm tra FXML và hành vi với toolkit thật. Bật bằng -Dauction.ui.tests=true (cần desktop). */
@EnabledIfSystemProperty(named = "auction.ui.tests", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientUiTest {
    private static Stage stage;

    private static <T> T fx(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private static void await(BooleanSupplier ready) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (!fx(ready::getAsBoolean)) {
            if (System.nanoTime() > deadline) fail("UI did not reach expected state");
            Thread.sleep(30);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object controller, String name) {
        try {
            Field f = controller.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(controller);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void snapshot(String name) throws Exception {
        fx(
                () -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    Path path = Path.of("target", "qa", name + ".png");
                    Files.createDirectories(path.getParent());
                    ImageIO.write(
                            SwingFXUtils.fromFXImage(
                                    stage.getScene().getRoot().snapshot(null, null), null),
                            "png",
                            path.toFile());
                    return null;
                });
    }

    private static int listenerCount(String id) {
        Object store = field(ServiceRegistry.getService(), "store");
        synchronized (store) {
            java.util.Map<String, java.util.List<?>> listeners = field(store, "bidListeners");
            return listeners.getOrDefault(id, java.util.List.of()).size();
        }
    }

    private static DialogPane dialogPane() {
        return javafx.stage.Window.getWindows().stream()
                .filter(w -> w.isShowing() && w != stage)
                .map(w -> w.getScene().getRoot())
                .filter(DialogPane.class::isInstance)
                .map(DialogPane.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static void scheduleRowAction(SellerProductsController seller, String text)
            throws Exception {
        fx(
                () -> {
                    TableView<?> table = field(seller, "table");
                    table.applyCss();
                    table.layout();
                    Button action =
                            table.lookupAll(".button").stream()
                                    .filter(Button.class::isInstance)
                                    .map(Button.class::cast)
                                    .filter(b -> text.equals(b.getText()))
                                    .findFirst()
                                    .orElseThrow();
                    Platform.runLater(action::fire);
                    return null;
                });
        await(() -> dialogPane() != null);
    }

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
        assertTrue(started.await(15, TimeUnit.SECONDS));
        fx(
                () -> {
                    stage = new Stage();
                    stage.setWidth(1200);
                    stage.setHeight(800);
                    ViewNavigator.setPrimaryStage(stage);
                    return null;
                });
    }

    @AfterAll
    static void stopToolkit() throws Exception {
        fx(
                () -> {
                    ViewNavigator.disposeCurrentView();
                    stage.close();
                    return null;
                });
        Platform.exit();
    }

    @Test
    @Order(1)
    void registerLoginSearchAndNavigation() throws Exception {
        AuthController auth = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.AUTH));
        fx(
                () -> {
                    ((Button) field(auth, "btnSwitchMode")).fire();
                    ((TextField) field(auth, "txtUsername")).setText("ui_test_user");
                    ((TextField) field(auth, "txtEmail")).setText("ui_test_user@example.com");
                    ((TextField) field(auth, "txtPassword")).setText("Testpass123");
                    ((TextField) field(auth, "txtConfirm")).setText("Testpass123");
                    ((Button) field(auth, "btnSubmit")).fire();
                    return null;
                });
        await(() -> ViewNavigator.Route.DASHBOARD.getPath().equals(ViewNavigator.getCurrentPath()));
        assertEquals("ui_test_user", ServiceRegistry.getService().getCurrentUser().getUsername());
        ServiceRegistry.getService().logout();
        AuthController login = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.AUTH));
        fx(
                () -> {
                    ((TextField) field(login, "txtUsername")).setText("ui_test_user");
                    ((TextField) field(login, "txtPassword")).setText("Testpass123");
                    ((Button) field(login, "btnSubmit")).fire();
                    return null;
                });
        await(() -> ViewNavigator.Route.DASHBOARD.getPath().equals(ViewNavigator.getCurrentPath()));
        HomeController home = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD));
        await(() -> !(boolean) field(home, "loading"));
        fx(
                () -> {
                    ((TextField) field(home, "txtSearch")).setText("no_match_123456");
                    assertTrue(((FlowPane) field(home, "auctionGrid")).getChildren().isEmpty());
                    assertTrue(((Label) field(home, "lblNoResults")).isVisible());
                    ((TextField) field(home, "txtSearch")).clear();
                    return null;
                });
        snapshot("home");
        fx(
                () -> {
                    ViewNavigator.navigate(ViewNavigator.Route.GUIDE);
                    return null;
                });
        assertEquals(
                Animation.Status.STOPPED,
                fx(() -> ((Animation) field(home, "masterCountdownTimer")).getStatus()));
        fx(
                () -> {
                    assertTrue(ViewNavigator.back());
                    return null;
                });
        assertEquals(ViewNavigator.Route.DASHBOARD.getPath(), ViewNavigator.getCurrentPath());
    }

    @Test
    @Order(2)
    void bidHistoryChartAndRepeatedRoomLifecycle() throws Exception {
        assertTrue(ServiceRegistry.getService().login("bidder_hieu", "123456", null));
        for (int visit = 0; visit < 3; visit++) {
            AuctionRoomController room =
                    fx(
                            () -> {
                                AuctionRoomController c =
                                        ViewNavigator.loadView(
                                                "/com/auction/client/views/auction-room-view.fxml",
                                                "Kiểm thử phiên");
                                c.setAuction(
                                        ServiceRegistry.getService().getAuctionById("AUC-103"));
                                return c;
                            });
            await(() -> (boolean) field(room, "auctionReady"));
            assertEquals(1, listenerCount("AUC-103"));
            int before = ServiceRegistry.getService().getBidHistory("AUC-103").size();
            fx(
                    () -> {
                        Auction auction = field(room, "currentAuction");
                        ((TextField) field(room, "txtBidAmount"))
                                .setText(String.format("%.0f", auction.getMinimumNextBid()));
                        Button bid = field(room, "btnPlaceBid");
                        bid.fire();
                        bid.fire();
                        assertTrue(bid.isDisabled());
                        return null;
                    });
            await(
                    () ->
                            !(boolean) field(room, "bidBusy")
                                    && !(boolean) field(room, "refreshing")
                                    && ((ListView<?>) field(room, "lvRecentBids")).getItems().size()
                                            == before + 1);
            assertEquals(before + 1, ServiceRegistry.getService().getBidHistory("AUC-103").size());
            fx(
                    () -> {
                        LineChart<?, ?> chart = field(room, "priceChart");
                        assertEquals(
                                Math.min(30, before + 1),
                                chart.getData().getFirst().getData().size());
                        return null;
                    });
            if (visit == 0) {
                snapshot("room-wide");
                fx(
                        () -> {
                            stage.setWidth(640);
                            return null;
                        });
                await(() -> stage.getScene().getWidth() < 700);
                snapshot("room-narrow");
                fx(
                        () -> {
                            stage.setWidth(1200);
                            return null;
                        });
                await(() -> stage.getScene().getWidth() > 1000);
            }
            fx(
                    () -> {
                        ViewNavigator.navigate(ViewNavigator.Route.CONTACT);
                        return null;
                    });
            assertEquals(
                    Animation.Status.STOPPED,
                    fx(() -> ((Animation) field(room, "countdownTimer")).getStatus()));
            assertNull(fx(() -> field(room, "realtimeBidConsumer")));
            assertEquals(0, listenerCount("AUC-103"));
            fx(
                    () -> {
                        room.onBidReceived(
                                new Bid(
                                        "late",
                                        "AUC-103",
                                        "late",
                                        1,
                                        java.time.LocalDateTime.now()));
                        return null;
                    });
            fx(
                    () -> {
                        assertTrue(ViewNavigator.back());
                        return null;
                    });
            assertEquals(
                    "/com/auction/client/views/auction-room-view.fxml",
                    ViewNavigator.getCurrentPath());
        }
    }

    @Test
    @Order(3)
    void allRoutesLoadWithAuthorizedRoles() throws Exception {
        assertTrue(ServiceRegistry.getService().login("seller_hieu", "123456", null));
        SellerProductsController seller =
                fx(() -> ViewNavigator.navigate(ViewNavigator.Route.PRODUCTS));
        await(() -> !((ProgressIndicator) field(seller, "progress")).isVisible());
        assertFalse(fx(() -> ((TableView<?>) field(seller, "table")).getItems().isEmpty()));
        fx(
                () -> {
                    Platform.runLater(() -> ((Button) field(seller, "addButton")).fire());
                    return null;
                });
        await(() -> dialogPane() != null);
        fx(
                () -> {
                    DialogPane form = dialogPane();
                    form.applyCss();
                    form.layout();
                    ((TextField) form.lookup("#productName")).setText("UI product created");
                    ((TextArea) form.lookup("#productDescription"))
                            .setText("Sản phẩm kiểm thử giao diện");
                    ((ComboBox<?>) form.lookup("#productCategory"))
                            .getSelectionModel()
                            .selectFirst();
                    ((TextField) form.lookup("#productPrice")).setText("500000");
                    ((TextField) form.lookup("#productStep")).setText("10000");
                    ((DatePicker) form.lookup("#productStart"))
                            .setValue(java.time.LocalDate.now().plusDays(1));
                    ((DatePicker) form.lookup("#productEnd"))
                            .setValue(java.time.LocalDate.now().plusDays(2));
                    ((TextField) form.lookup("#productStartTime")).setText("10:00");
                    ((TextField) form.lookup("#productEndTime")).setText("10:00");
                    Button save = (Button) form.lookupButton(ButtonType.OK);
                    assertFalse(save.isDisabled());
                    ((TextField) form.lookup("#productName")).setText("x".repeat(201));
                    assertTrue(save.isDisabled(), "Names over the server limit must be blocked");
                    ((TextField) form.lookup("#productName")).setText("UI product created");
                    ((TextArea) form.lookup("#productDescription")).setText("x".repeat(10001));
                    assertTrue(save.isDisabled(), "Descriptions over the server limit must be blocked");
                    ((TextArea) form.lookup("#productDescription")).setText("Sản phẩm kiểm thử giao diện");
                    ((DatePicker) form.lookup("#productEnd"))
                            .setValue(java.time.LocalDate.now().plusDays(367));
                    assertTrue(save.isDisabled(), "Auctions longer than 365 days must be blocked");
                    ((DatePicker) form.lookup("#productEnd"))
                            .setValue(java.time.LocalDate.now().plusDays(2));
                    assertFalse(save.isDisabled());
                    save.fire();
                    return null;
                });
        await(
                () ->
                        !((ProgressIndicator) field(seller, "progress")).isVisible()
                                && ((TableView<Auction>) field(seller, "table"))
                                        .getItems().stream()
                                                .anyMatch(
                                                        a ->
                                                                a.getItemName()
                                                                        .equals(
                                                                                "UI product"
                                                                                    + " created")));
        fx(
                () -> {
                    ((TextField) field(seller, "search")).setText("UI product created");
                    return null;
                });
        scheduleRowAction(seller, "Sửa");
        fx(
                () -> {
                    ((TextField) dialogPane().lookup("#productName")).setText("UI product updated");
                    ((Button) dialogPane().lookupButton(ButtonType.OK)).fire();
                    return null;
                });
        await(
                () ->
                        ServiceRegistry.getProductService().listProducts("seller_hieu").stream()
                                        .anyMatch(a -> a.getItemName().equals("UI product updated"))
                                && !((ProgressIndicator) field(seller, "progress")).isVisible());
        fx(
                () -> {
                    ((TextField) field(seller, "search")).setText("UI product updated");
                    return null;
                });
        scheduleRowAction(seller, "Xóa");
        fx(
                () -> {
                    ((Button) dialogPane().lookupButton(ButtonType.OK)).fire();
                    return null;
                });
        await(
                () ->
                        !((ProgressIndicator) field(seller, "progress")).isVisible()
                                && ServiceRegistry.getProductService()
                                        .listProducts("seller_hieu")
                                        .stream()
                                        .noneMatch(
                                                a -> a.getItemName().equals("UI product updated")));
        fx(
                () -> {
                    ((TextField) field(seller, "search")).clear();
                    return null;
                });
        snapshot("products");
        WalletController wallet = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.WALLET));
        await(() -> !((Label[]) field(wallet, "values"))[0].getText().equals("—"));
        snapshot("wallet");
        assertTrue(ServiceRegistry.getService().login("admin_hieu", "123456", null));
        AdminController admin = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.ADMIN));
        await(() -> !((ProgressIndicator) field(admin, "progress")).isVisible());
        assertFalse(fx(() -> ((TableView<?>) field(admin, "auctionTable")).getItems().isEmpty()));
        fx(() -> {
            TableView<Auction> table = field(admin, "auctionTable");
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            assertEquals(4, table.getColumns().size());
            var row = table.lookupAll(".table-row-cell").stream()
                    .filter(TableRow.class::isInstance).map(TableRow.class::cast)
                    .filter(candidate -> candidate.getIndex() == 0)
                    .findFirst().orElseThrow();
            Button detail = row.lookupAll(".button").stream()
                    .filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> button.getText().equals("Chi tiết"))
                    .findFirst().orElseThrow();
            detail.fire();
            Auction auction = table.getItems().getFirst();
            DialogPane pane = dialogPane();
            assertNotNull(pane);
            String copy = pane.getContentText();
            assertTrue(copy.contains(auction.getItemName()));
            assertTrue(copy.contains("Người bán: " + auction.getSellerUsername()));
            assertTrue(copy.contains("Người dẫn đầu: " + auction.getHighestBidderUsername()));
            assertTrue(copy.contains(auction.getStartTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            assertTrue(copy.contains(auction.getEndTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            ((Button) pane.lookupButton(ButtonType.OK)).fire();
            return null;
        });
        snapshot("admin");
        fx(
                () -> {
                    ViewNavigator.navigate(ViewNavigator.Route.GUIDE);
                    ViewNavigator.navigate(ViewNavigator.Route.CONTACT);
                    return null;
                });
    }

    @Test
    @Order(7)
    void mockAutoBidValidationIsKnownRejectionAndCanBeCorrected() throws Exception {
        assertTrue(ServiceRegistry.getService().login("bidder_hieu", "123456", null));
        AuctionRoomController room =
                fx(
                        () -> {
                            AuctionRoomController c =
                                    ViewNavigator.loadView(
                                            "/com/auction/client/views/auction-room-view.fxml",
                                            "Auto-bid test");
                            c.loadAuction("AUC-103");
                            return c;
                        });
        awaitFx(room::isReady);
        fx(
                () -> {
                    ((TextField) field(room, "txtAutoMax")).setText("1");
                    ((TextField) field(room, "txtAutoIncrement")).setText("1");
                    ((Button) field(room, "btnAutoBid")).fire();
                    return null;
                });
        awaitFx(
                () ->
                        room.isReady()
                                && !(boolean) field(room, "autoBusy")
                                && !((Label) field(room, "lblAutoError")).getText().isEmpty());
        fx(
                () -> {
                    assertFalse(((Button) field(room, "btnPlaceBid")).isDisabled());
                    Auction auction = field(room, "currentAuction");
                    ((TextField) field(room, "txtAutoMax"))
                            .setText(String.format("%.0f", auction.getMinimumNextBid()));
                    ((TextField) field(room, "txtAutoIncrement"))
                            .setText(String.format("%.0f", auction.getMinBidStep()));
                    ((Button) field(room, "btnAutoBid")).fire();
                    return null;
                });
        awaitFx(
                () ->
                        room.isReady()
                                && !(boolean) field(room, "autoBusy")
                                && field(room, "myAutoBid") != null
                                && ((AutoBid) field(room, "myAutoBid")).isActive());
        fx(
                () -> {
                    ((Button) field(room, "btnAutoBid")).fire();
                    return null;
                });
        awaitFx(
                () ->
                        room.isReady()
                                && !(boolean) field(room, "autoBusy")
                                && !((AutoBid) field(room, "myAutoBid")).isActive());
    }

    @Test
    @Order(8)
    void queueRejectionRestoresBidUiWithoutPretendingItWasSent() throws Exception {
        var original = ServiceRegistry.getService();
        var fake = new ControlledAuctionService(true);
        try {
            AuctionRoomController room = controlledRoom(fake);
            fx(
                    () -> {
                        com.auction.client.navigation.ViewTasks old = field(room, "tasks");
                        old.close();
                        var constructor =
                                com.auction.client.navigation.ViewTasks.class
                                        .getDeclaredConstructor(
                                                java.util.concurrent.Executor.class,
                                                java.util.function.Consumer.class);
                        constructor.setAccessible(true);
                        java.util.concurrent.Executor reject =
                                work -> {
                                    throw new RejectedExecutionException("full");
                                };
                        java.util.function.Consumer<Runnable> ui = Platform::runLater;
                        Object scope = constructor.newInstance(reject, ui);
                        var tasksField = AuctionRoomController.class.getDeclaredField("tasks");
                        tasksField.setAccessible(true);
                        tasksField.set(room, scope);
                        ((Button) field(room, "btnPlaceBid")).fire();
                        return null;
                    });
            awaitFx(() -> ((Label) field(room, "lblAlertMessage")).getText().contains("Hàng đợi"));
            fx(
                    () -> {
                        assertFalse(((Button) field(room, "btnPlaceBid")).isDisabled());
                        assertFalse((boolean) field(room, "bidBusy"));
                        return null;
                    });
            assertEquals(0, fake.bids.get());
        } finally {
            fake.finish.countDown();
            fx(
                    () -> {
                        ViewNavigator.disposeCurrentView();
                        return null;
                    });
            ServiceRegistry.setService(original);
        }
    }

    // Các ca lỗi mới dùng latch để giữ đúng thời điểm gửi/đọc; không dùng sleep để tạo race.
    private static void awaitFx(BooleanSupplier condition) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        var timer =
                fx(
                        () -> {
                            var pulse =
                                    new javafx.animation.AnimationTimer() {
                                        @Override
                                        public void handle(long now) {
                                            try {
                                                if (condition.getAsBoolean()) {
                                                    stop();
                                                    done.complete(null);
                                                }
                                            } catch (Throwable error) {
                                                stop();
                                                done.completeExceptionally(error);
                                            }
                                        }
                                    };
                            pulse.start();
                            return pulse;
                        });
        try {
            done.get(8, TimeUnit.SECONDS);
        } finally {
            fx(
                    () -> {
                        timer.stop();
                        return null;
                    });
        }
    }

    private static com.auction.client.ui.UserMenu prepareAccountMenu() throws Exception {
        assertTrue(ServiceRegistry.getService().login("seller_hieu", "123456", null));
        HomeController home = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD));
        awaitFx(() -> !home.isLoading());
        return fx(() -> {
            var menu = (com.auction.client.ui.UserMenu)
                    stage.getScene().getRoot().lookup(".user-menu-host");
            assertNotNull(menu);
            return menu;
        });
    }

    /** Keeps native focus, layout and auto-hide evidence in the Surefire XML. */
    private static final class PopupDiagnostics implements AutoCloseable {
        private final com.auction.client.ui.UserMenu menu;
        private final javafx.stage.Popup popup;
        private final javafx.beans.value.ChangeListener<Boolean> ownerFocus =
                (observable, before, focused) -> log("owner.focus=" + focused);
        private final javafx.beans.value.ChangeListener<Boolean> popupFocus =
                (observable, before, focused) -> log("popup.focus=" + focused);
        private final javafx.event.EventHandler<javafx.stage.WindowEvent> lifecycle = event -> {
            log(event.getEventType().getName());
            if (event.getEventType() == javafx.stage.WindowEvent.WINDOW_HIDING) {
                new Exception("Popup hide origin").printStackTrace(System.out);
            }
        };
        private int autoHideCount;
        private final javafx.event.EventHandler<javafx.event.Event> autoHide = event -> {
            autoHideCount++;
            log("AUTO_HIDE");
        };

        PopupDiagnostics(com.auction.client.ui.UserMenu menu) throws Exception {
            this.menu = menu;
            popup = field(menu, "popup");
            fx(() -> {
                stage.focusedProperty().addListener(ownerFocus);
                popup.focusedProperty().addListener(popupFocus);
                popup.addEventHandler(javafx.stage.WindowEvent.ANY, lifecycle);
                popup.setOnAutoHide(autoHide);
                log("attached");
                return null;
            });
        }

        void open() throws Exception {
            fx(() -> {
                stage.toFront();
                stage.requestFocus();
                Parent root = stage.getScene().getRoot();
                root.applyCss();
                root.layout();
                log("focus requested before open");
                return null;
            });
            // requestFocus and toFront return before native focus events are dispatched.
            awaitState("owner focused and header laid out", () -> {
                Button trigger = field(menu, "trigger");
                var bounds = trigger.localToScreen(trigger.getBoundsInLocal());
                return stage.isShowing() && stage.isFocused()
                        && !stage.getScene().getRoot().isNeedsLayout()
                        && bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0;
            });
            fx(() -> {
                assertFalse(popup.isShowing());
                assertTrue(stage.isFocused(), "owner must retain focus until activation");
                Parent root = stage.getScene().getRoot();
                root.applyCss();
                root.layout();
                log("before trigger.fire");
                ((Button) field(menu, "trigger")).fire();
                log("after trigger.fire");
                assertTrue(popup.isShowing(), "Popup.show must open synchronously");
                return null;
            });
            awaitState("popup stays open through a pulse", () -> popup.isShowing() && popup.isFocused());
        }

        private void log(String event) {
            Button trigger = field(menu, "trigger");
            Parent root = stage.getScene().getRoot();
            System.out.println("[account-popup] " + event
                    + " ownerShowing=" + stage.isShowing()
                    + " ownerFocused=" + stage.isFocused()
                    + " needsLayout=" + root.isNeedsLayout()
                    + " triggerBounds=" + trigger.localToScreen(trigger.getBoundsInLocal())
                    + " popupShowing=" + popup.isShowing()
                    + " popupFocused=" + popup.isFocused()
                    + " popupBounds=" + popup.getX() + "," + popup.getY()
                    + "," + popup.getWidth() + "," + popup.getHeight()
                    + " windows=" + javafx.stage.Window.getWindows().stream()
                            .map(window -> window.getClass().getSimpleName()
                                    + "(focused=" + window.isFocused() + ")").toList());
        }

        void awaitState(String description, BooleanSupplier condition) throws Exception {
            try {
                awaitFx(condition);
            } catch (TimeoutException error) {
                fx(() -> { log("TIMEOUT " + description); return null; });
                fail(description, error);
            }
        }

        @Override
        public void close() throws Exception {
            fx(() -> {
                com.auction.client.ui.UserMenu.closeActive();
                stage.focusedProperty().removeListener(ownerFocus);
                popup.focusedProperty().removeListener(popupFocus);
                popup.removeEventHandler(javafx.stage.WindowEvent.ANY, lifecycle);
                popup.setOnAutoHide(null);
                assertFalse(popup.isShowing());
                assertNull(field(menu, "menuOwner"));
                assertNull(field(menu, "menuScene"));
                return null;
            });
        }
    }

    private static void invoke(Object controller, String name) {
        try {
            var method = controller.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(controller);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class ReadGate {
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicBoolean claimed =
                new java.util.concurrent.atomic.AtomicBoolean();
        final boolean fail;

        ReadGate(boolean fail) {
            this.fail = fail;
        }
    }

    private static final class ControlledAuctionService
            extends com.auction.client.service.mock.MockAuctionClientService {
        volatile ReadGate gate;
        final CountDownLatch sent = new CountDownLatch(1), finish = new CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicInteger bids =
                new java.util.concurrent.atomic.AtomicInteger();
        final boolean timeout;

        ControlledAuctionService(boolean timeout) {
            this.timeout = timeout;
        }

        @Override
        public com.auction.client.service.BidResult placeBid(String id, double amount) {
            bids.incrementAndGet();
            sent.countDown();
            waitFor(finish);
            if (timeout) throw new IllegalStateException("timeout: server outcome unknown");
            return com.auction.client.service.BidResult.fail("Explicit rejection for this test");
        }

        @Override
        public Auction getAuctionById(String id) {
            ReadGate read = gate;
            if (read != null && read.claimed.compareAndSet(false, true)) {
                read.entered.countDown();
                waitFor(read.release);
                if (read.fail) throw new IllegalStateException("Read unavailable");
            }
            return super.getAuctionById(id);
        }

        private static void waitFor(CountDownLatch latch) {
            try {
                if (!latch.await(8, TimeUnit.SECONDS))
                    throw new IllegalStateException("Test gate not released");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        }
    }

    private static AuctionRoomController controlledRoom(ControlledAuctionService service)
            throws Exception {
        assertTrue(service.login("bidder_hieu", "123456", null));
        ServiceRegistry.setService(service);
        AuctionRoomController room =
                fx(
                        () -> {
                            AuctionRoomController loaded =
                                    ViewNavigator.loadView(
                                            "/com/auction/client/views/auction-room-view.fxml",
                                            "Controlled test");
                            loaded.loadAuction("AUC-103");
                            assertAllPriceControlsLocked(loaded);
                            return loaded;
                        });
        awaitFx(room::isReady);
        return room;
    }

    private static void assertAllPriceControlsLocked(AuctionRoomController room) {
        for (String name :
                new String[] {
                    "btnPlaceBid", "btnQuickAdd1", "btnQuickAdd2", "btnQuickAdd3", "btnAutoBid"
                }) assertTrue(((Button) field(room, name)).isDisabled(), name);
        assertTrue(((TextField) field(room, "txtBidAmount")).isDisabled());
        assertTrue(((TextField) field(room, "txtAutoMax")).isDisabled());
        assertTrue(((TextField) field(room, "txtAutoIncrement")).isDisabled());
        assertTrue(((javafx.scene.layout.HBox) field(room, "devModeBox")).isDisabled());
        // Gọi trực tiếp handler để kiểm tra guard, kể cả event được gửi bằng code hoặc Enter.
        ((TextField) field(room, "txtBidAmount")).fireEvent(new javafx.event.ActionEvent());
        invoke(room, "handlePlaceBid");
        invoke(room, "handleSimulateIncomingBid");
        invoke(room, "handleAutoBid");
    }

    @Test
    @Order(4)
    void timeoutAndPendingOrFailedReadNeverUnlockOrResend() throws Exception {
        var original = ServiceRegistry.getService();
        var fake = new ControlledAuctionService(true);
        ReadGate read = new ReadGate(true);
        try {
            AuctionRoomController room = controlledRoom(fake);
            fake.gate = read;
            fx(
                    () -> {
                        ((Button) field(room, "btnPlaceBid")).fire();
                        return null;
                    });
            assertTrue(fake.sent.await(3, TimeUnit.SECONDS));
            fx(
                    () -> {
                        assertAllPriceControlsLocked(room);
                        return null;
                    });
            fake.finish.countDown();
            assertTrue(read.entered.await(3, TimeUnit.SECONDS));
            fx(
                    () -> {
                        assertAllPriceControlsLocked(room);
                        return null;
                    });
            read.release.countDown();
            awaitFx(
                    () ->
                            !(boolean) field(room, "refreshing")
                                    && !(boolean) field(room, "auctionReady"));
            fx(
                    () -> {
                        assertAllPriceControlsLocked(room);
                        assertTrue(
                                ((Label) field(room, "lblAlertMessage"))
                                        .getText()
                                        .contains("Tải lại"));
                        return null;
                    });
            ReadGate retry = new ReadGate(false);
            fake.gate = retry;
            fx(
                    () -> {
                        invoke(room, "handleReload");
                        return null;
                    });
            assertTrue(retry.entered.await(3, TimeUnit.SECONDS));
            fx(
                    () -> {
                        assertAllPriceControlsLocked(room);
                        return null;
                    });
            retry.release.countDown();
            awaitFx(room::isReady);
            fx(
                    () -> {
                        assertAllPriceControlsLocked(room);
                        assertTrue(
                                ((Label) field(room, "lblBidState"))
                                        .getText()
                                        .contains("chưa xác định"));
                        return null;
                    });
            assertEquals(1, fake.bids.get());
            assertNull(ServiceRegistry.getAutoBidService().getMyAutoBid("AUC-103"));
            fx(
                    () -> {
                        ViewNavigator.navigate(ViewNavigator.Route.CONTACT);
                        return null;
                    });
            AuctionRoomController reopened =
                    fx(
                            () -> {
                                AuctionRoomController c =
                                        ViewNavigator.loadView(
                                                "/com/auction/client/views/auction-room-view.fxml",
                                                "Reopened");
                                c.loadAuction("AUC-103");
                                return c;
                            });
            awaitFx(reopened::isReady);
            fx(
                    () -> {
                        assertAllPriceControlsLocked(reopened);
                        return null;
                    });
            assertEquals(1, fake.bids.get());
        } finally {
            read.release.countDown();
            fake.finish.countDown();
            if (fake.gate != null) fake.gate.release.countDown();
            fx(
                    () -> {
                        ViewNavigator.disposeCurrentView();
                        return null;
                    });
            ServiceRegistry.setService(original);
        }
    }

    @Test
    @Order(5)
    void responseRequiresReadStartedAfterResponseAndReadFailureKeepsLock() throws Exception {
        var original = ServiceRegistry.getService();
        var fake = new ControlledAuctionService(false);
        ReadGate beforeResponse = new ReadGate(false), afterResponse = new ReadGate(true);
        try {
            AuctionRoomController room = controlledRoom(fake);
            fake.gate = beforeResponse;
            fx(
                    () -> {
                        ((Button) field(room, "btnPlaceBid")).fire();
                        return null;
                    });
            assertTrue(fake.sent.await(3, TimeUnit.SECONDS));
            fx(
                    () -> {
                        room.onBidReceived(
                                new Bid(
                                        "test-event",
                                        "AUC-103",
                                        "someone",
                                        1,
                                        java.time.LocalDateTime.now()));
                        return null;
                    });
            assertTrue(beforeResponse.entered.await(3, TimeUnit.SECONDS));
            fake.gate = afterResponse;
            fake.finish.countDown();
            awaitFx(() -> !(boolean) field(room, "bidBusy"));
            beforeResponse.release.countDown();
            assertTrue(
                    afterResponse.entered.await(3, TimeUnit.SECONDS),
                    "Old read must trigger a post-response read");
            fx(
                    () -> {
                        assertAllPriceControlsLocked(room);
                        return null;
                    });
            afterResponse.release.countDown();
            awaitFx(
                    () ->
                            !(boolean) field(room, "refreshing")
                                    && !(boolean) field(room, "auctionReady"));
            fx(
                    () -> {
                        assertAllPriceControlsLocked(room);
                        invoke(room, "handleReload");
                        return null;
                    });
            awaitFx(room::isReady);
            fx(
                    () -> {
                        assertFalse(((Button) field(room, "btnPlaceBid")).isDisabled());
                        return null;
                    });
            assertEquals(1, fake.bids.get());
        } finally {
            beforeResponse.release.countDown();
            afterResponse.release.countDown();
            fake.finish.countDown();
            fx(
                    () -> {
                        ViewNavigator.disposeCurrentView();
                        return null;
                    });
            ServiceRegistry.setService(original);
        }
    }

    @Test
    @Order(6)
    void leavingOrChangingRoomCannotForgetInFlightCommandOrApplyOldCallback() throws Exception {
        var original = ServiceRegistry.getService();
        var fake = new ControlledAuctionService(true);
        try {
            AuctionRoomController old = controlledRoom(fake);
            fx(
                    () -> {
                        ((Button) field(old, "btnPlaceBid")).fire();
                        return null;
                    });
            assertTrue(fake.sent.await(3, TimeUnit.SECONDS));
            fx(
                    () -> {
                        old.loadAuction("AUC-101");
                        return null;
                    });
            awaitFx(old::isReady);
            fake.finish.countDown();
            var key =
                    new com.auction.client.service.BidSubmissionTracker.Key(
                            "bidder_hieu", "AUC-103");
            awaitFx(
                    () ->
                            ServiceRegistry.getBidSubmissions().phase(key)
                                    == com.auction.client.service.BidSubmissionTracker.Phase
                                            .UNKNOWN);
            fx(
                    () -> {
                        old.onBidReceived(
                                new Bid(
                                        "late",
                                        "AUC-103",
                                        "late",
                                        1,
                                        java.time.LocalDateTime.now()));
                        assertEquals("AUC-101", ((Auction) field(old, "currentAuction")).getId());
                        old.loadAuction("AUC-103");
                        return null;
                    });
            awaitFx(old::isReady);
            fx(
                    () -> {
                        assertAllPriceControlsLocked(old);
                        return null;
                    });
            assertEquals(1, fake.bids.get());
        } finally {
            fake.finish.countDown();
            fx(
                    () -> {
                        ViewNavigator.disposeCurrentView();
                        return null;
                    });
            ServiceRegistry.setService(original);
        }
    }

    private static java.util.List<String> cardIds(HomeController home, String gridName) {
        FlowPane grid = field(home, gridName);
        return grid.getChildren().stream().map(n -> (String) n.getUserData()).toList();
    }

    private static Auction homeAuction(int number, AuctionStatus status, String category) {
        var item =
                new Item(
                        "home-item-" + number,
                        "Phiên "
                                + number
                                + " — Sản phẩm kiểm thử tên dài để kiểm tra xuống dòng đầy đủ",
                        "Mô tả",
                        category,
                        100_000,
                        "seller-test");
        var now = java.time.LocalDateTime.now();
        return new Auction(
                "home-" + number,
                item,
                "seller-test",
                200_000,
                null,
                status,
                status == AuctionStatus.OPEN ? now.plusDays(3) : now.minusHours(1),
                now.plusDays(4),
                10_000);
    }

    @Test
    @Order(9)
    void homeShowsEveryRunningAndOnlyOpenUpcomingWithoutDuplicates() throws Exception {
        var original = ServiceRegistry.getService();
        var rows = new java.util.ArrayList<Auction>();
        for (int i = 0; i < 8; i++)
            rows.add(homeAuction(i, AuctionStatus.RUNNING, i % 2 == 0 ? "JEWELRY" : "ELECTRONICS"));
        rows.add(homeAuction(8, AuctionStatus.OPEN, "JEWELRY"));
        rows.add(homeAuction(9, AuctionStatus.OPEN, "ELECTRONICS"));
        rows.add(homeAuction(10, AuctionStatus.FINISHED, "JEWELRY"));
        rows.add(homeAuction(11, AuctionStatus.PAID, "JEWELRY"));
        rows.add(homeAuction(12, AuctionStatus.CANCELED, "JEWELRY"));
        rows.add(rows.getFirst());
        rows.add(rows.get(8));
        var fake =
                new com.auction.client.service.mock.MockAuctionClientService() {
                    @Override
                    public java.util.List<Auction> getAuctions(AuctionStatus status) {
                        return rows;
                    }
                };
        try {
            ServiceRegistry.setService(fake);
            HomeController home = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD));
            awaitFx(() -> !home.isLoading());
            fx(
                    () -> {
                        assertEquals(
                                java.util.stream.IntStream.range(0, 8)
                                        .mapToObj(i -> "home-" + i)
                                        .toList(),
                                cardIds(home, "auctionGrid"));
                        assertEquals(
                                java.util.List.of("home-8", "home-9"),
                                cardIds(home, "upcomingGrid"));
                        for (int repeat = 0; repeat < 3; repeat++) {
                            ((Button) field(home, "chipWatch")).fire();
                            assertEquals(
                                    java.util.List.of("home-0", "home-2", "home-4", "home-6"),
                                    cardIds(home, "auctionGrid"));
                            assertEquals(
                                    java.util.List.of("home-8"), cardIds(home, "upcomingGrid"));
                            ((TextField) field(home, "txtSearch")).setText("Phiên 2");
                            assertEquals(java.util.List.of("home-2"), cardIds(home, "auctionGrid"));
                            assertTrue(cardIds(home, "upcomingGrid").isEmpty());
                            assertTrue(
                                    ((Label) field(home, "lblUpcomingEmpty"))
                                            .getText()
                                            .contains("Không có kết quả"));
                            ((TextField) field(home, "txtSearch")).clear();
                            ((Button) field(home, "chipAll")).fire();
                        }
                        assertEquals(8, cardIds(home, "auctionGrid").size());
                        assertEquals(
                                10, ((java.util.List<?>) field(home, "countdownEntries")).size());
                        assertNull(stage.getScene().getRoot().lookup("#featuredPane"));
                        Parent root = stage.getScene().getRoot();
                        root.applyCss();
                        root.layout();
                        assertEquals(
                                28, ((Label) field(home, "lblRunningHeader")).getFont().getSize());
                        FlowPane grid = field(home, "auctionGrid");
                        assertEquals(
                                grid.getChildren().get(0).getLayoutY(),
                                grid.getChildren().get(2).getLayoutY(),
                                1,
                                "A wide grid must fit three full cards in its first row");
                        return null;
                    });
            fx(
                    () -> {
                        stage.setWidth(640);
                        return null;
                    });
            awaitFx(() -> stage.getScene().getWidth() < 700);
            fx(
                    () -> {
                        Parent root = stage.getScene().getRoot();
                        root.applyCss();
                        root.layout();
                        FlowPane grid = field(home, "auctionGrid");
                        Button login = root.lookupAll(".button").stream()
                                .filter(Button.class::isInstance).map(Button.class::cast)
                                .filter(button -> button.getText().equals("Đăng nhập"))
                                .findFirst().orElseThrow();
                        assertTrue(login.getWidth() >= login.prefWidth(-1) - 1,
                                "The sign-in label must remain readable at 640px");
                        for (var node : grid.getChildren()) {
                            var card = (javafx.scene.layout.VBox) node;
                            assertTrue(card.getWidth() <= grid.getWidth() + 1);
                            var button = (Button) card.lookup(".btn-primary");
                            assertTrue(
                                    button.getBoundsInParent().getMaxY() <= card.getHeight() + 1);
                            assertTrue(button.getWidth() >= button.prefWidth(-1) - 1);
                            for (String selector :
                                    java.util.List.of(".auction-card-title", ".countdown-label")) {
                                var label = (Label) card.lookup(selector);
                                assertTrue(
                                        label.getHeight() >= label.prefHeight(label.getWidth()) - 1,
                                        selector);
                            }
                        }
                        return null;
                    });
            snapshot("home-eight-running-narrow");
        } finally {
            fx(
                    () -> {
                        stage.setWidth(1200);
                        ViewNavigator.disposeCurrentView();
                        return null;
                    });
            ServiceRegistry.setService(original);
        }
    }

    @Test
    @Order(10)
    void homeDistinguishesLoadingFailureEmptyAndFilteredResults() throws Exception {
        var original = ServiceRegistry.getService();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var failed = new java.util.concurrent.atomic.AtomicBoolean();
        var rows =
                new java.util.concurrent.atomic.AtomicReference<java.util.List<Auction>>(
                        java.util.List.of());
        var fake =
                new com.auction.client.service.mock.MockAuctionClientService() {
                    @Override
                    public java.util.List<Auction> getAuctions(AuctionStatus status) {
                        entered.countDown();
                        ControlledAuctionService.waitFor(release);
                        if (failed.get()) throw new IllegalStateException("Read unavailable");
                        return rows.get();
                    }
                };
        try {
            ServiceRegistry.setService(fake);
            HomeController home = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD));
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            fx(
                    () -> {
                        assertHomeMessages(home, "Đang tải");
                        return null;
                    });
            release.countDown();
            awaitFx(() -> !home.isLoading());
            fx(
                    () -> {
                        assertHomeMessages(home, "Chưa có phiên");
                        return null;
                    });
            failed.set(true);
            fx(
                    () -> {
                        home.loadAuctions();
                        return null;
                    });
            awaitFx(() -> !home.isLoading());
            fx(
                    () -> {
                        assertHomeMessages(home, "Không thể tải");
                        ((TextField) field(home, "txtSearch")).setText("no_match");
                        assertHomeMessages(home, "Không thể tải");
                        return null;
                    });
            failed.set(false);
            rows.set(
                    java.util.List.of(
                            homeAuction(1, AuctionStatus.RUNNING, "JEWELRY"),
                            homeAuction(2, AuctionStatus.OPEN, "JEWELRY")));
            fx(
                    () -> {
                        home.loadAuctions();
                        return null;
                    });
            awaitFx(() -> !home.isLoading());
            fx(
                    () -> {
                        assertHomeMessages(home, "Không có kết quả");
                        ((TextField) field(home, "txtSearch")).clear();
                        assertFalse(((Label) field(home, "lblNoResults")).isVisible());
                        assertFalse(((Label) field(home, "lblUpcomingEmpty")).isVisible());
                        return null;
                    });
        } finally {
            release.countDown();
            fx(
                    () -> {
                        ViewNavigator.disposeCurrentView();
                        return null;
                    });
            ServiceRegistry.setService(original);
        }
    }

    private static void assertHomeMessages(HomeController home, String expected) {
        for (String name : java.util.List.of("lblNoResults", "lblUpcomingEmpty")) {
            Label label = field(home, name);
            assertTrue(label.isVisible() && label.isManaged());
            assertTrue(label.getText().startsWith(expected), label.getText());
        }
        assertTrue(cardIds(home, "auctionGrid").isEmpty());
        assertTrue(cardIds(home, "upcomingGrid").isEmpty());
    }

    @Test
    @Order(11)
    void walletShowsFullLongAmountAndExactTooltipAtBothWidths() throws Exception {
        assertTrue(
                ServiceRegistry.getService()
                        .register(
                                "wallet_ui_long",
                                "Testpass123",
                                "Ví kiểm thử",
                                "wallet_ui_long@example.com",
                                UserRole.BIDDER));
        long expected = 9_123_456_789_012_345L;
        ServiceRegistry.getWalletService().deposit("wallet_ui_long", expected - 50_000_000L);
        WalletController wallet = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.WALLET));
        awaitFx(() -> !wallet.isLoading());
        for (int width : new int[] {1200, 640}) {
            fx(
                    () -> {
                        stage.setWidth(width);
                        return null;
                    });
            awaitFx(() -> Math.abs(stage.getWidth() - width) < 2);
            fx(
                    () -> {
                        Parent root = stage.getScene().getRoot();
                        root.applyCss();
                        root.layout();
                        FlowPane stats = field(wallet, "statistics");
                        String[] headings = {
                            "Tổng số dư", "Có thể sử dụng", "Tiền tạm giữ cho đấu giá"
                        };
                        for (int i = 0; i < headings.length; i++) {
                            var card = (javafx.scene.layout.VBox) stats.getChildren().get(i);
                            Label heading = (Label) card.getChildren().getFirst();
                            assertEquals(headings[i], heading.getText());
                            assertTrue(heading.isWrapText());
                            assertTrue(card.getBoundsInParent().getMaxX() <= stats.getWidth() + 1);
                        }
                        if (width == 1200)
                            assertEquals(
                                    stats.getChildren().get(0).getLayoutY(),
                                    stats.getChildren().get(2).getLayoutY(),
                                    1);
                        Label[] values = field(wallet, "values");
                        assertEquals("9.123.456.789.012.345 ₫", values[0].getText());
                        assertEquals(values[0].getText(), values[0].getTooltip().getText());
                        assertEquals(values[0].getText(), values[0].getAccessibleText());
                        assertTrue(
                                values[0].getHeight()
                                        >= values[0].prefHeight(values[0].getWidth()) - 1);
                        assertEquals("Tạm giữ cho đấu giá", TransactionType.BID_HOLD.getLabel());
                        assertEquals(
                                "Giải phóng tiền tạm giữ", TransactionType.BID_RELEASE.getLabel());
                        return null;
                    });
            snapshot("wallet-long-" + width);
        }
        fx(
                () -> {
                    stage.setWidth(1200);
                    return null;
                });
    }

    @Test
    @Order(12)
    void authStylesAreAppliedAndRegisterScrollsOnSmallWindow() throws Exception {
        ServiceRegistry.getService().logout();
        AuthController auth = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.AUTH));
        fx(
                () -> {
                    stage.setWidth(640);
                    stage.setHeight(640);
                    return null;
                });
        awaitFx(() -> stage.getScene().getWidth() < 700);
        fx(
                () -> {
                    Parent root = stage.getScene().getRoot();
                    root.applyCss();
                    root.layout();
                    Label brand = field(auth, "lblBrandName");
                    assertEquals(28, brand.getFont().getSize());
                    assertEquals(javafx.scene.paint.Color.web("#18332F"), brand.getTextFill());
                    assertNull(root.lookup("#backgroundImage"));
                    assertNull(root.lookup("#imageOverlay"));
                    ((Button) field(auth, "btnSwitchMode")).fire();
                    root.applyCss();
                    root.layout();
                    ScrollPane scroll = field(auth, "authScroll");
                    assertTrue(
                            scroll.getContent().getLayoutBounds().getHeight()
                                    > scroll.getViewportBounds().getHeight());
                    scroll.setVvalue(1);
                    return null;
                });
        awaitFx(() -> ((ScrollPane) field(auth, "authScroll")).getVvalue() == 1);
        snapshot("auth-register-scrolled-640");
        fx(
                () -> {
                    stage.setWidth(1200);
                    stage.setHeight(800);
                    return null;
                });
    }

    @Test
    @Order(13)
    void accountPopupClosesWhenItsHeaderIsRemoved() throws Exception {
        var menu = prepareAccountMenu();
        try (var diagnostics = new PopupDiagnostics(menu)) {
            diagnostics.open();
            fx(() -> {
                stage.getScene().setRoot(new javafx.scene.layout.VBox());
                return null;
            });
            diagnostics.awaitState("header removal closes popup", () -> !diagnostics.popup.isShowing());
        }
    }

    @Test
    @Order(14)
    void accountPopupClosesOnFocusLossEscapeAndOwnerHide() throws Exception {
        var menu = prepareAccountMenu();
        javafx.stage.Popup popup = field(menu, "popup");
        Stage otherWindow = fx(Stage::new);
        try (var diagnostics = new PopupDiagnostics(menu)) {
            diagnostics.open();
            fx(() -> {
                otherWindow.setTitle("Kiểm thử chuyển cửa sổ");
                otherWindow.setScene(new javafx.scene.Scene(new javafx.scene.layout.VBox(), 240, 100));
                otherWindow.show();
                otherWindow.toFront();
                otherWindow.requestFocus();
                return null;
            });
            diagnostics.awaitState("other window receives native focus", otherWindow::isFocused);
            diagnostics.awaitState("focus loss closes popup", () -> !popup.isShowing());
            fx(() -> {
                otherWindow.close();
                return null;
            });
            diagnostics.open();
            fx(() -> {
                popup.getScene().getRoot().fireEvent(new javafx.scene.input.KeyEvent(
                        javafx.scene.input.KeyEvent.KEY_PRESSED, "", "",
                        javafx.scene.input.KeyCode.ESCAPE, false, false, false, false));
                return null;
            });
            diagnostics.awaitState("Escape closes popup", () -> !popup.isShowing());
            diagnostics.awaitState("Escape returns focus to trigger",
                    () -> ((Button) field(menu, "trigger")).isFocused());
            diagnostics.open();
            fx(() -> { stage.hide(); return null; });
            diagnostics.awaitState("owner hide closes popup", () -> !popup.isShowing());
            fx(() -> {
                assertNull(field(menu, "menuOwner"));
                assertNull(field(menu, "menuScene"));
                return null;
            });
        } finally {
            fx(() -> { otherWindow.close(); stage.show(); return null; });
        }
    }

    @Test
    @Order(15)
    void conciseGuideAndContactCopyWorkAtNarrowWidth() throws Exception {
        try {
            GuideController guide = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.GUIDE));
            fx(() -> {
                stage.setWidth(640);
                var sections = ((javafx.scene.layout.VBox) field(guide, "pageContent")).getChildren();
                assertEquals(6, sections.size());
                String[] ids = {"guide-start", "guide-seller", "guide-bidder", "guide-auto-bid",
                        "guide-wallet", "guide-results"};
                for (int i = 0; i < ids.length; i++) {
                    var section = (javafx.scene.layout.VBox) sections.get(i);
                    assertEquals(ids[i], section.getId());
                    assertEquals(2, section.getChildren().size());
                    Label copy = (Label) section.getChildren().get(1);
                    assertTrue(copy.isWrapText());
                    assertEquals(2, copy.getText().split("\\.\\s*").length);
                    assertFalse(copy.getText().contains("?"));
                }
                return null;
            });
            await(() -> stage.getScene().getWidth() < 700);
            snapshot("guide-640");
            ContactController contact = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.CONTACT));
            fx(() -> {
                var cards = ((javafx.scene.layout.VBox) field(contact, "pageContent")).getChildren();
                assertEquals(4, cards.size());
                var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                var previous = new javafx.scene.input.ClipboardContent();
                for (var format : clipboard.getContentTypes()) {
                    previous.put(format, clipboard.getContent(format));
                }
                try {
                    long windows = javafx.stage.Window.getWindows().stream()
                            .filter(javafx.stage.Window::isShowing).count();
                    for (var node : cards) {
                        var card = (javafx.scene.layout.VBox) node;
                        String email = ((Label) card.getChildren().get(1)).getText();
                        ((Button) card.getChildren().get(2)).fire();
                        assertEquals(email, clipboard.getString());
                        Label feedback = (Label) card.getChildren().get(3);
                        assertEquals("Đã sao chép email", feedback.getText());
                        assertTrue(feedback.isManaged());
                    }
                    assertEquals(windows, javafx.stage.Window.getWindows().stream()
                            .filter(javafx.stage.Window::isShowing).count());
                } finally {
                    clipboard.setContent(previous);
                }
                return null;
            });
            snapshot("contact-640");
        } finally {
            fx(() -> { stage.setWidth(1200); return null; });
        }
    }

    @Test
    @Order(16)
    void accountPopupSupportsOutsideClickKeyboardAndNavigation() throws Exception {
        var menu = prepareAccountMenu();
        javafx.stage.Popup popup = field(menu, "popup");
        var robot = fx(javafx.scene.robot.Robot::new);
        var previousMouse = fx(robot::getMousePosition);
        Parent ownerRoot = fx(() -> stage.getScene().getRoot());
        try (var diagnostics = new PopupDiagnostics(menu)) {
            diagnostics.open();
            var outside = fx(() -> {
                var bounds = ownerRoot.localToScreen(ownerRoot.getBoundsInLocal());
                assertNotNull(bounds);
                var point = new javafx.geometry.Point2D(bounds.getMinX() + 32, bounds.getMinY() + 32);
                assertTrue(bounds.contains(point), "click must be inside the owner content");
                assertFalse(new javafx.geometry.Rectangle2D(popup.getX(), popup.getY(),
                        popup.getWidth(), popup.getHeight()).contains(point),
                        "click must be outside the account popup");
                assertTrue(javafx.stage.Screen.getScreens().stream()
                        .anyMatch(screen -> screen.getVisualBounds().contains(point)),
                        "click must be on a visible screen");
                robot.mouseMove(point);
                diagnostics.log("Robot move requested at " + point);
                return point;
            });
            // Let the native event loop process motion before sending the button events.
            diagnostics.awaitState("Robot reaches outside-click target",
                    () -> robot.getMousePosition().distance(outside) < 2);
            fx(() -> {
                assertTrue(popup.isShowing(), "popup must remain open until the outside click");
                assertEquals(0, diagnostics.autoHideCount, "auto-hide must follow the outside click");
                assertTrue(robot.getMousePosition().distance(outside) < 2,
                        "pointer must remain at the outside-click target");
                diagnostics.log("Robot click at " + robot.getMousePosition());
                robot.mouseClick(javafx.scene.input.MouseButton.PRIMARY);
                return null;
            });
            diagnostics.awaitState("native outside click auto-hides popup",
                    () -> !popup.isShowing() && diagnostics.autoHideCount == 1);
            // Keep the pointer at the click target until JavaFX has handled auto-hide.
            fx(() -> {
                robot.mouseMove(previousMouse);
                ownerRoot.getStyleClass().add("keyboard-nav");
                return null;
            });
            diagnostics.open();
            diagnostics.awaitState("keyboard opening focuses first item",
                    () -> popup.isShowing() && ((Button) field(menu, "firstMenuItem")).isFocused());
            Button secondItem = fx(() -> {
                var content = (javafx.scene.layout.VBox) field(menu, "menuContent");
                return content.getChildren().stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .skip(1).findFirst().orElseThrow();
            });
            fx(() -> {
                robot.keyPress(javafx.scene.input.KeyCode.TAB);
                robot.keyRelease(javafx.scene.input.KeyCode.TAB);
                return null;
            });
            diagnostics.awaitState("Tab moves focus to the next menu item",
                    () -> popup.isShowing() && secondItem.isFocused());
            fx(() -> {
                Parent root = popup.getScene().getRoot();
                assertTrue(root.getStyleClass().contains("keyboard-nav"));
                root.applyCss();
                root.layout();
                Path image = Path.of("target", "qa", "account-menu-keyboard.png");
                Files.createDirectories(image.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null),
                        "png", image.toFile());
                return null;
            });
            fx(() -> {
                var content = (javafx.scene.layout.VBox) field(menu, "menuContent");
                Button wallet = content.getChildren().stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(button -> button.getText().equals("Ví của tôi"))
                        .findFirst().orElseThrow();
                wallet.fire();
                assertFalse(popup.isShowing());
                assertNull(field(menu, "menuOwner"));
                assertNull(field(menu, "menuScene"));
                return null;
            });
            diagnostics.awaitState("wallet navigation completes",
                    () -> ViewNavigator.Route.WALLET.getPath().equals(ViewNavigator.getCurrentPath()));
        } finally {
            fx(() -> {
                robot.keyRelease(javafx.scene.input.KeyCode.TAB);
                robot.mouseRelease(javafx.scene.input.MouseButton.PRIMARY);
                robot.mouseMove(previousMouse);
                ownerRoot.getStyleClass().remove("keyboard-nav");
                com.auction.client.ui.UserMenu.closeActive();
                return null;
            });
        }
    }
}
