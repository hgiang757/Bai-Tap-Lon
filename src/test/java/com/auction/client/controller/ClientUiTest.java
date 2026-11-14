package com.auction.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.AutoBid;
import com.auction.client.model.Bid;
import com.auction.client.model.Item;
import com.auction.client.model.ItemCategory;
import com.auction.client.model.TransactionType;
import com.auction.client.model.UserRole;
import com.auction.client.navigation.ViewNavigator;
import com.auction.client.service.ServiceRegistry;
import com.auction.client.ui.SellerProductFormView;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

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
      if (System.nanoTime() > deadline) {
        fail("UI did not reach expected state");
      }
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
              SwingFXUtils.fromFXImage(stage.getScene().getRoot().snapshot(null, null), null),
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
    await(() -> "Sửa".equals(text) ? productForm() != null : dialogPane() != null);
  }

  private static javafx.scene.Parent productForm() {
    var node = stage.getScene().lookup("#productForm");
    if (!(node instanceof javafx.scene.Parent form) || !form.isVisible()) {
      return null;
    }
    form.applyCss();
    form.layout();
    return form;
  }

  @BeforeAll
  static void startToolkit() throws Exception {
    System.setProperty("auction.mode", "server");
    com.auction.client.fixture.FixtureServices.install();
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
                        "/com/auction/client/views/auction-room-view.fxml", "Kiểm thử phiên");
                c.setAuction(ServiceRegistry.getService().getAuctionById("AUC-103"));
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
                  && ((ListView<?>) field(room, "lvRecentBids")).getItems().size() == before + 1);
      assertEquals(before + 1, ServiceRegistry.getService().getBidHistory("AUC-103").size());
      fx(
          () -> {
            LineChart<?, ?> chart = field(room, "priceChart");
            assertEquals(Math.min(30, before + 1), chart.getData().getFirst().getData().size());
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
                new Bid("late", "AUC-103", "late", 1, java.time.LocalDateTime.now()));
            return null;
          });
      fx(
          () -> {
            assertTrue(ViewNavigator.back());
            return null;
          });
      assertEquals(
          "/com/auction/client/views/auction-room-view.fxml", ViewNavigator.getCurrentPath());
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
    await(() -> productForm() != null);
    fx(
        () -> {
          javafx.scene.Parent form = productForm();
          form.applyCss();
          form.layout();
          ((TextField) form.lookup("#productName")).setText("UI product created");
          ((TextArea) form.lookup("#productDescription")).setText("Sản phẩm kiểm thử giao diện");
          ((ComboBox<?>) form.lookup("#productCategory")).getSelectionModel().selectFirst();
          ((TextField) form.lookup("#productPrice")).setText("500000");
          ((TextField) form.lookup("#productStep")).setText("10000");
          ((DatePicker) form.lookup("#productStart"))
              .setValue(java.time.LocalDate.now().plusDays(1));
          ((DatePicker) form.lookup("#productEnd")).setValue(java.time.LocalDate.now().plusDays(2));
          ((TextField) form.lookup("#productStartTime")).setText("10:00");
          ((TextField) form.lookup("#productEndTime")).setText("10:00");
          Button save = (Button) form.lookup("#productSave");
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
          ((DatePicker) form.lookup("#productEnd")).setValue(java.time.LocalDate.now().plusDays(2));
          assertFalse(save.isDisabled());
          save.fire();
          return null;
        });
    await(
        () ->
            !((ProgressIndicator) field(seller, "progress")).isVisible()
                && ((TableView<Auction>) field(seller, "table"))
                    .getItems().stream()
                        .anyMatch(a -> a.getItemName().equals("UI product" + " created")));
    fx(
        () -> {
          ((TextField) field(seller, "search")).setText("UI product created");
          return null;
        });
    scheduleRowAction(seller, "Sửa");
    fx(
        () -> {
          ((TextField) productForm().lookup("#productName")).setText("UI product updated");
          ((Button) productForm().lookup("#productSave")).fire();
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
                && ServiceRegistry.getProductService().listProducts("seller_hieu").stream()
                    .noneMatch(a -> a.getItemName().equals("UI product updated")));
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
    fx(
        () -> {
          TableView<Auction> table = field(admin, "auctionTable");
          stage.getScene().getRoot().applyCss();
          stage.getScene().getRoot().layout();
          assertEquals(4, table.getColumns().size());
          var row =
              table.lookupAll(".table-row-cell").stream()
                  .filter(TableRow.class::isInstance)
                  .map(TableRow.class::cast)
                  .filter(candidate -> candidate.getIndex() == 0)
                  .findFirst()
                  .orElseThrow();
          Button detail =
              row.lookupAll(".button").stream()
                  .filter(Button.class::isInstance)
                  .map(Button.class::cast)
                  .filter(button -> button.getText().equals("Chi tiết"))
                  .findFirst()
                  .orElseThrow();
          detail.fire();
          Auction auction = table.getItems().getFirst();
          DialogPane pane = dialogPane();
          assertNotNull(pane);
          String copy = pane.getContentText();
          assertTrue(copy.contains(auction.getItemName()));
          assertTrue(copy.contains("Người bán: " + auction.getSellerUsername()));
          assertTrue(copy.contains("Người dẫn đầu: " + auction.getHighestBidderUsername()));
          assertTrue(
              copy.contains(
                  auction
                      .getStartTime()
                      .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
          assertTrue(
              copy.contains(
                  auction
                      .getEndTime()
                      .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
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
                      "/com/auction/client/views/auction-room-view.fxml", "Auto-bid test");
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
                com.auction.client.navigation.ViewTasks.class.getDeclaredConstructor(
                    java.util.concurrent.Executor.class, java.util.function.Consumer.class);
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
    return fx(
        () -> {
          var menu =
              (com.auction.client.ui.UserMenu) stage.getScene().getRoot().lookup(".user-menu-host");
          assertNotNull(menu);
          return menu;
        });
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
    final CountDownLatch entered = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);
    final java.util.concurrent.atomic.AtomicBoolean claimed =
        new java.util.concurrent.atomic.AtomicBoolean();
    final boolean fail;

    ReadGate(boolean fail) {
      this.fail = fail;
    }
  }

  private static final class ControlledAuctionService
      extends com.auction.client.fixture.MockAuctionClientService {
    volatile ReadGate gate;
    volatile ReadGate recoveryGate;
    final CountDownLatch sent = new CountDownLatch(1);
    final CountDownLatch finish = new CountDownLatch(1);
    final java.util.concurrent.atomic.AtomicInteger bids =
        new java.util.concurrent.atomic.AtomicInteger();
    final boolean timeout;

    ControlledAuctionService(boolean timeout) {
      this.timeout = timeout;
    }

    @Override
    public boolean recoverPendingCommands() {
      ReadGate recovery = recoveryGate;
      if (recovery == null || !recovery.claimed.compareAndSet(false, true)) {
        return false;
      }
      recovery.entered.countDown();
      waitFor(recovery.release);
      return true;
    }

    @Override
    public com.auction.client.service.BidResult placeBid(String id, double amount) {
      bids.incrementAndGet();
      sent.countDown();
      waitFor(finish);
      if (timeout) {
        throw new IllegalStateException("timeout: server outcome unknown");
      }
      return com.auction.client.service.BidResult.fail("Explicit rejection for this test");
    }

    @Override
    public Auction getAuctionById(String id) {
      ReadGate read = gate;
      if (read != null && read.claimed.compareAndSet(false, true)) {
        read.entered.countDown();
        waitFor(read.release);
        if (read.fail) {
          throw new IllegalStateException("Read unavailable");
        }
      }
      return super.getAuctionById(id);
    }

    private static void waitFor(CountDownLatch latch) {
      try {
        if (!latch.await(8, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Test gate not released");
        }
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
                      "/com/auction/client/views/auction-room-view.fxml", "Controlled test");
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
        }) {
      assertTrue(((Button) field(room, name)).isDisabled(), name);
    }
    assertTrue(((TextField) field(room, "txtBidAmount")).isDisabled());
    assertTrue(((TextField) field(room, "txtAutoMax")).isDisabled());
    assertTrue(((TextField) field(room, "txtAutoIncrement")).isDisabled());
    // Gọi trực tiếp handler để kiểm tra guard, kể cả event được gửi bằng code hoặc Enter.
    ((TextField) field(room, "txtBidAmount")).fireEvent(new javafx.event.ActionEvent());
    invoke(room, "handlePlaceBid");
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
      awaitFx(() -> !(boolean) field(room, "refreshing") && !(boolean) field(room, "auctionReady"));
      fx(
          () -> {
            assertAllPriceControlsLocked(room);
            assertTrue(((Label) field(room, "lblAlertMessage")).getText().contains("Tải lại"));
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
            assertTrue(((Label) field(room, "lblBidState")).getText().contains("chưa xác định"));
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
                        "/com/auction/client/views/auction-room-view.fxml", "Reopened");
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
      if (fake.gate != null) {
        fake.gate.release.countDown();
      }
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
    ReadGate beforeResponse = new ReadGate(false);
    ReadGate afterResponse = new ReadGate(true);
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
                new Bid("test-event", "AUC-103", "someone", 1, java.time.LocalDateTime.now()));
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
      awaitFx(() -> !(boolean) field(room, "refreshing") && !(boolean) field(room, "auctionReady"));
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
      var key = new com.auction.client.service.BidSubmissionTracker.Key("bidder_hieu", "AUC-103");
      awaitFx(
          () ->
              ServiceRegistry.getBidSubmissions().phase(key)
                  == com.auction.client.service.BidSubmissionTracker.Phase.UNKNOWN);
      fx(
          () -> {
            old.onBidReceived(new Bid("late", "AUC-103", "late", 1, java.time.LocalDateTime.now()));
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
            "Phiên " + number + " — Sản phẩm kiểm thử tên dài để kiểm tra xuống dòng đầy đủ",
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
    final var original = ServiceRegistry.getService();
    var rows = new java.util.ArrayList<Auction>();
    for (int i = 0; i < 8; i++) {
      rows.add(homeAuction(i, AuctionStatus.RUNNING, i % 2 == 0 ? "JEWELRY" : "ELECTRONICS"));
    }
    rows.add(homeAuction(8, AuctionStatus.OPEN, "JEWELRY"));
    rows.add(homeAuction(9, AuctionStatus.OPEN, "ELECTRONICS"));
    rows.add(homeAuction(10, AuctionStatus.FINISHED, "JEWELRY"));
    rows.add(homeAuction(11, AuctionStatus.PAID, "JEWELRY"));
    rows.add(homeAuction(12, AuctionStatus.CANCELED, "JEWELRY"));
    rows.add(rows.getFirst());
    rows.add(rows.get(8));
    var fake =
        new com.auction.client.fixture.MockAuctionClientService() {
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
                java.util.stream.IntStream.range(0, 8).mapToObj(i -> "home-" + i).toList(),
                cardIds(home, "auctionGrid"));
            assertEquals(java.util.List.of("home-8", "home-9"), cardIds(home, "upcomingGrid"));
            for (int repeat = 0; repeat < 3; repeat++) {
              ((Button) field(home, "chipWatch")).fire();
              assertEquals(
                  java.util.List.of("home-0", "home-2", "home-4", "home-6"),
                  cardIds(home, "auctionGrid"));
              assertEquals(java.util.List.of("home-8"), cardIds(home, "upcomingGrid"));
              ((TextField) field(home, "txtSearch")).setText("Phiên 2");
              assertEquals(java.util.List.of("home-2"), cardIds(home, "auctionGrid"));
              assertTrue(cardIds(home, "upcomingGrid").isEmpty());
              assertTrue(
                  ((Label) field(home, "lblUpcomingEmpty")).getText().contains("Không có kết quả"));
              ((TextField) field(home, "txtSearch")).clear();
              ((Button) field(home, "chipAll")).fire();
            }
            assertEquals(8, cardIds(home, "auctionGrid").size());
            assertEquals(10, ((java.util.List<?>) field(home, "countdownEntries")).size());
            assertNull(stage.getScene().getRoot().lookup("#featuredPane"));
            Parent root = stage.getScene().getRoot();
            root.applyCss();
            root.layout();
            assertEquals(28, ((Label) field(home, "lblRunningHeader")).getFont().getSize());
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
            Button login =
                root.lookupAll(".button").stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(button -> button.getText().equals("Đăng nhập"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(
                login.getWidth() >= login.prefWidth(-1) - 1,
                "The sign-in label must remain readable at 640px");
            for (var node : grid.getChildren()) {
              var card = (javafx.scene.layout.VBox) node;
              assertTrue(card.getWidth() <= grid.getWidth() + 1);
              var button = (Button) card.lookup(".btn-primary");
              assertTrue(button.getBoundsInParent().getMaxY() <= card.getHeight() + 1);
              assertTrue(button.getWidth() >= button.prefWidth(-1) - 1);
              for (String selector : java.util.List.of(".auction-card-title", ".countdown-label")) {
                var label = (Label) card.lookup(selector);
                assertTrue(label.getHeight() >= label.prefHeight(label.getWidth()) - 1, selector);
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
        new com.auction.client.fixture.MockAuctionClientService() {
          @Override
          public java.util.List<Auction> getAuctions(AuctionStatus status) {
            entered.countDown();
            ControlledAuctionService.waitFor(release);
            if (failed.get()) {
              throw new IllegalStateException("Read unavailable");
            }
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
            assertEquals(40, ((ComboBox<?>) field(wallet, "typeFilter")).getHeight(), 1);
            assertEquals(40, ((TextField) field(wallet, "search")).getHeight(), 1);
            String[] headings = {"Tổng số dư", "Có thể sử dụng", "Tiền tạm giữ cho đấu giá"};
            for (int i = 0; i < headings.length; i++) {
              var card = (javafx.scene.layout.VBox) stats.getChildren().get(i);
              Label heading = (Label) card.getChildren().getFirst();
              assertEquals(headings[i], heading.getText());
              assertTrue(heading.isWrapText());
              assertTrue(card.getBoundsInParent().getMaxX() <= stats.getWidth() + 1);
            }
            if (width == 1200) {
              assertEquals(
                  stats.getChildren().get(0).getLayoutY(),
                  stats.getChildren().get(2).getLayoutY(),
                  1);
            }
            Label[] values = field(wallet, "values");
            assertEquals("9.123.456.789.012.345 ₫", values[0].getText());
            assertEquals(values[0].getText(), values[0].getTooltip().getText());
            assertEquals(values[0].getText(), values[0].getAccessibleText());
            assertTrue(values[0].getHeight() >= values[0].prefHeight(values[0].getWidth()) - 1);
            assertEquals("Tạm giữ cho đấu giá", TransactionType.BID_HOLD.getLabel());
            assertEquals("Giải phóng tiền tạm giữ", TransactionType.BID_RELEASE.getLabel());
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
  void accountMenuToggleStaysInSceneAndLeavesOtherControlsUsable() throws Exception {
    var menu = prepareAccountMenu();
    ScrollPane pane = field(menu, "menuPane");
    Button trigger = field(menu, "trigger");
    try {
      fx(
          () -> {
            stage.setWidth(640);
            stage.setHeight(640);
            return null;
          });
      awaitFx(() -> stage.getScene().getWidth() < 650);
      fx(
          () -> {
            Parent root = stage.getScene().getRoot();
            root.applyCss();
            root.layout();
            var content = ((javafx.scene.layout.StackPane) root).getChildren().getFirst();
            final double contentWidth = content.getLayoutBounds().getWidth();
            final long windows =
                javafx.stage.Window.getWindows().stream()
                    .filter(javafx.stage.Window::isShowing)
                    .count();
            assertNull(pane.getParent());
            assertTrue(trigger.isFocusTraversable());
            trigger.fire();
            root.applyCss();
            root.layout();
            assertSame(stage.getScene(), pane.getScene());
            assertSame(root, pane.getParent());
            assertEquals(
                windows,
                javafx.stage.Window.getWindows().stream()
                    .filter(javafx.stage.Window::isShowing)
                    .count(),
                "Opening an account menu must not create a window");
            assertEquals(
                contentWidth,
                content.getLayoutBounds().getWidth(),
                .01,
                "Opening the menu must not resize the view");
            var bounds = pane.localToScene(pane.getBoundsInLocal());
            assertTrue(
                bounds.getMinX() >= 0 && bounds.getMaxX() <= stage.getScene().getWidth() + 1);
            assertTrue(
                bounds.getMinY() >= 0 && bounds.getMaxY() <= stage.getScene().getHeight() + 1);
            TextField search = (TextField) root.lookup("#txtSearch");
            search.requestFocus();
            assertSame(search, stage.getScene().getFocusOwner(), "Other controls stay usable");
            search.setText("menu does not block search");
            assertSame(root, pane.getParent(), "Changing focus does not auto-hide this menu");
            search.clear();
            trigger.fire();
            assertNull(pane.getParent(), "Second activation closes the same menu");
            // JavaFX Button handles Space itself; no custom keyboard menu handler is installed.
            for (var type :
                java.util.List.of(
                    javafx.scene.input.KeyEvent.KEY_PRESSED,
                    javafx.scene.input.KeyEvent.KEY_RELEASED)) {
              trigger.fireEvent(
                  new javafx.scene.input.KeyEvent(
                      type,
                      " ",
                      " ",
                      javafx.scene.input.KeyCode.SPACE,
                      false,
                      false,
                      false,
                      false));
            }
            assertSame(root, pane.getParent(), "Standard Space activation opens the menu");
            trigger.fire();
            assertNull(pane.getParent());
            for (int i = 0; i < 20; i++) {
              trigger.fire();
              assertEquals(1, root.lookupAll(".user-menu-pane").size());
              trigger.fire();
              assertNull(pane.getParent());
              assertFalse(pane.prefHeightProperty().isBound());
            }
            trigger.fire();
            return null;
          });
      snapshot("account-menu-640");
    } finally {
      fx(
          () -> {
            com.auction.client.ui.UserMenu.closeActive();
            stage.setWidth(1200);
            stage.setHeight(800);
            return null;
          });
    }
  }

  @Test
  @Order(14)
  void accountMenuCleansUpOnOwnerHideHeaderRemovalAndLogout() throws Exception {
    var menu = prepareAccountMenu();
    ScrollPane pane = field(menu, "menuPane");
    fx(
        () -> {
          ((Button) field(menu, "trigger")).fire();
          assertNotNull(pane.getParent());
          stage.hide();
          assertNull(pane.getParent());
          assertFalse(pane.prefHeightProperty().isBound());
          stage.show();
          ((Button) field(menu, "trigger")).fire();
          assertNotNull(pane.getParent());
          stage.getScene().setRoot(new javafx.scene.layout.VBox());
          assertNull(pane.getParent(), "Removing the header releases its menu");
          assertFalse(pane.prefHeightProperty().isBound());
          return null;
        });
    var signedIn = prepareAccountMenu();
    ScrollPane logoutPane = field(signedIn, "menuPane");
    fx(
        () -> {
          ((Button) field(signedIn, "trigger")).fire();
          accountAction(signedIn, "Đăng xuất").fire();
          assertNull(logoutPane.getParent(), "Logout closes the menu before background I/O");
          return null;
        });
    awaitFx(
        () ->
            ServiceRegistry.getService().getCurrentUser() == null
                && ViewNavigator.Route.AUTH.getPath().equals(ViewNavigator.getCurrentPath()));
    fx(
        () -> {
          assertTrue(stage.getScene().getRoot().lookupAll(".user-menu-pane").isEmpty());
          assertFalse(logoutPane.prefHeightProperty().isBound());
          return null;
        });
  }

  private static Button accountAction(com.auction.client.ui.UserMenu menu, String label) {
    javafx.scene.layout.VBox content = field(menu, "menuContent");
    return content.getChildren().stream()
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> button.getText().equals(label))
        .findFirst()
        .orElseThrow();
  }

  @Test
  @Order(15)
  void conciseGuideAndContactCopyWorkAtNarrowWidth() throws Exception {
    try {
      GuideController guide = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.GUIDE));
      fx(
          () -> {
            stage.setWidth(640);
            var sections = ((javafx.scene.layout.VBox) field(guide, "pageContent")).getChildren();
            assertEquals(6, sections.size());
            String[] ids = {
              "guide-start",
              "guide-seller",
              "guide-bidder",
              "guide-auto-bid",
              "guide-wallet",
              "guide-results"
            };
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
      fx(
          () -> {
            var cards = ((javafx.scene.layout.VBox) field(contact, "pageContent")).getChildren();
            assertEquals(4, cards.size());
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var previous = new javafx.scene.input.ClipboardContent();
            for (var format : clipboard.getContentTypes()) {
              previous.put(format, clipboard.getContent(format));
            }
            try {
              long windows =
                  javafx.stage.Window.getWindows().stream()
                      .filter(javafx.stage.Window::isShowing)
                      .count();
              for (var node : cards) {
                var card = (javafx.scene.layout.VBox) node;
                String email = ((Label) card.getChildren().get(1)).getText();
                ((Button) card.getChildren().get(2)).fire();
                assertEquals(email, clipboard.getString());
                Label feedback = (Label) card.getChildren().get(3);
                assertEquals("Đã sao chép email", feedback.getText());
                assertTrue(feedback.isManaged());
              }
              assertEquals(
                  windows,
                  javafx.stage.Window.getWindows().stream()
                      .filter(javafx.stage.Window::isShowing)
                      .count());
            } finally {
              clipboard.setContent(previous);
            }
            return null;
          });
      snapshot("contact-640");
    } finally {
      fx(
          () -> {
            stage.setWidth(1200);
            return null;
          });
    }
  }

  @Test
  @Order(16)
  void accountMenuNavigationAndRepeatedUseDoNotDuplicateContent() throws Exception {
    var oldPanes = new java.util.ArrayList<ScrollPane>();
    var routes =
        java.util.List.of(
            ViewNavigator.Route.WALLET,
            ViewNavigator.Route.PRODUCTS,
            ViewNavigator.Route.GUIDE,
            ViewNavigator.Route.CONTACT,
            ViewNavigator.Route.DASHBOARD);
    var labels =
        java.util.List.of(
            "Ví của tôi", "Chế độ bán hàng", "Hướng dẫn sử dụng", "Liên hệ", "Chế độ đấu giá");
    for (int i = 0; i < 10; i++) {
      var menu = prepareAccountMenu();
      ScrollPane pane = field(menu, "menuPane");
      oldPanes.add(pane);
      int index = i % routes.size();
      fx(
          () -> {
            ViewNavigator.setPrimaryStage(stage);
            ViewNavigator.setPrimaryStage(stage);
            ((Button) field(menu, "trigger")).fire();
            Parent root = stage.getScene().getRoot();
            root.applyCss();
            root.layout();
            assertEquals(1, root.lookupAll(".user-menu-pane").size());
            accountAction(menu, labels.get(index)).fire();
            assertNull(pane.getParent());
            assertFalse(pane.prefHeightProperty().isBound());
            assertTrue(stage.getScene().getRoot().lookupAll(".user-menu-pane").isEmpty());
            return null;
          });
      awaitFx(() -> routes.get(index).getPath().equals(ViewNavigator.getCurrentPath()));
    }
    fx(
        () -> {
          for (ScrollPane old : oldPanes) {
            assertNull(old.getParent());
            assertFalse(old.prefHeightProperty().isBound());
          }
          return null;
        });
  }

  @Test
  @Order(17)
  void oldRecoveryCannotReconcileAnotherRoomUnknownCommand() throws Exception {
    var original = ServiceRegistry.getService();
    var fake = new ControlledAuctionService(false);
    ReadGate recovery = new ReadGate(false);
    try {
      final AuctionRoomController room = controlledRoom(fake);
      var tracker = ServiceRegistry.getBidSubmissions();
      var oldKey =
          new com.auction.client.service.BidSubmissionTracker.Key("bidder_hieu", "AUC-103");
      var newKey =
          new com.auction.client.service.BidSubmissionTracker.Key("bidder_hieu", "AUC-101");
      tracker.uncertain(tracker.begin(oldKey));
      tracker.uncertain(tracker.begin(newKey));
      fake.recoveryGate = recovery;
      fx(
          () -> {
            invoke(room, "handleReload");
            return null;
          });
      assertTrue(recovery.entered.await(3, TimeUnit.SECONDS));
      fx(
          () -> {
            room.loadAuction("AUC-101");
            return null;
          });
      awaitFx(room::isReady);
      recovery.release.countDown();
      awaitFx(
          () ->
              tracker.phase(oldKey)
                  == com.auction.client.service.BidSubmissionTracker.Phase.AWAITING_REFRESH);
      fx(
          () -> {
            assertEquals(
                com.auction.client.service.BidSubmissionTracker.Phase.UNKNOWN,
                tracker.phase(newKey));
            assertAllPriceControlsLocked(room);
            assertEquals("AUC-101", ((Auction) field(room, "currentAuction")).getId());
            return null;
          });
      assertEquals(0, fake.bids.get());
    } finally {
      recovery.release.countDown();
      fx(
          () -> {
            ViewNavigator.disposeCurrentView();
            return null;
          });
      ServiceRegistry.setService(original);
    }
  }

  @Test
  @Order(18)
  void historySnapshotOrdersCapsAndReplacesPointsAndExtensionClearsUrgency() throws Exception {
    assertTrue(ServiceRegistry.getService().login("bidder_hieu", "123456", null));
    AuctionRoomController room =
        fx(
            () -> {
              AuctionRoomController loaded =
                  ViewNavigator.loadView(
                      "/com/auction/client/views/auction-room-view.fxml", "History snapshot");
              loaded.loadAuction("AUC-103");
              return loaded;
            });
    awaitFx(room::isReady);
    fx(
        () -> {
          Auction auction = field(room, "currentAuction");
          var deadline = auction.getEndTime();
          try {
            auction.setEndTime(ServiceRegistry.getService().serverNow().plusSeconds(240));
            invoke(room, "updateCountdown");
            Label countdown = field(room, "lblCountdown");
            assertTrue(countdown.getStyleClass().contains("countdown-label-urgent"));
            auction.setEndTime(ServiceRegistry.getService().serverNow().plusSeconds(350));
            invoke(room, "updateCountdown");
            assertFalse(countdown.getStyleClass().contains("countdown-label-urgent"));
            assertTrue(countdown.getStyleClass().contains("countdown-label"));
          } finally {
            auction.setEndTime(deadline);
          }
          com.auction.client.ui.AuctionBidHistoryView view = field(room, "historyView");
          var now = ServiceRegistry.getService().serverNow();
          var bids = new java.util.ArrayList<Bid>();
          for (int i = 34; i >= 0; i--) {
            bids.add(
                new Bid(
                    "history-" + i, auction.getId(), "user_" + i, 1000 + i, now.plusSeconds(i)));
          }
          view.render(auction, bids, now);
          LineChart<Number, Number> chart = field(room, "priceChart");
          var points = chart.getData().getFirst().getData();
          assertEquals(30, points.size());
          assertEquals(1005, points.getFirst().getYValue().intValue());
          assertEquals(1034, points.getLast().getYValue().intValue());
          ListView<String> history = field(room, "lvRecentBids");
          assertEquals(35, history.getItems().size());
          assertTrue(history.getItems().getFirst().contains("1.034"));
          view.render(auction, java.util.List.of(bids.getFirst()), now);
          assertEquals(1, points.size(), "A fresh snapshot replaces rather than duplicates points");
          assertEquals(1, history.getItems().size());
          view.render(auction, java.util.List.of(), now);
          assertEquals(1, points.size(), "An empty history still displays the starting price");
          assertTrue(history.getItems().isEmpty());
          assertTrue(((Label) field(room, "lblNoBids")).isVisible());
          return null;
        });
  }

  private static void fillProduct(javafx.scene.Parent form) {
    ((TextField) form.lookup("#productName")).setText("QA pane product");
    ((TextArea) form.lookup("#productDescription")).setText("Isolated form validation");
    ((ComboBox<?>) form.lookup("#productCategory")).getSelectionModel().selectFirst();
    ((TextField) form.lookup("#productPrice")).setText("500000");
    ((TextField) form.lookup("#productStep")).setText("10000");
    ((DatePicker) form.lookup("#productStart")).setValue(java.time.LocalDate.now().plusDays(1));
    ((DatePicker) form.lookup("#productEnd")).setValue(java.time.LocalDate.now().plusDays(2));
    ((TextField) form.lookup("#productStartTime")).setText("10:00");
    ((TextField) form.lookup("#productEndTime")).setText("10:00");
  }

  // runLater and AnimationTimer callbacks do not guarantee that CSS/layout has run.
  // Wait for two clean post-layout pulses, never for the geometry assertion to pass.
  private static void awaitSceneLayout(BooleanSupplier ready, Supplier<String> diagnostics)
      throws Exception {
    CompletableFuture<Void> done = new CompletableFuture<>();
    Scene scene = fx(stage::getScene);
    Runnable listener =
        new Runnable() {
          private int cleanPulses;

          @Override
          public void run() {
            try {
              cleanPulses =
                  ready.getAsBoolean() && !layoutPending(scene.getRoot()) ? cleanPulses + 1 : 0;
              if (cleanPulses >= 2) {
                done.complete(null);
              } else {
                Platform.requestNextPulse();
              }
            } catch (Throwable error) {
              done.completeExceptionally(error);
            }
          }
        };
    fx(
        () -> {
          scene.addPostLayoutPulseListener(listener);
          Platform.requestNextPulse();
          return null;
        });
    try {
      done.get(8, TimeUnit.SECONDS);
    } catch (TimeoutException error) {
      fail("JavaFX CSS/layout did not settle\n" + fx(diagnostics::get), error);
    } finally {
      fx(
          () -> {
            scene.removePostLayoutPulseListener(listener);
            return null;
          });
    }
  }

  private static boolean layoutPending(Parent parent) {
    if (parent.isNeedsLayout()) {
      return true;
    }
    return parent.getChildrenUnmodifiable().stream()
        .filter(node -> node.isManaged() && node.isVisible())
        .filter(Parent.class::isInstance)
        .map(Parent.class::cast)
        .anyMatch(ClientUiTest::layoutPending);
  }

  private static String sellerLayoutDiagnostics(int requestedWidth, SellerProductFormView form) {
    ScrollPane scroll = field(form, "scroll");
    Button save = field(form, "save");
    Label validation = field(form, "validation");
    return sellerWindowMetrics(requestedWidth)
        + "\nroot="
        + layoutMetrics(stage.getScene().getRoot())
        + "\nformHost="
        + layoutMetrics(form.getParent())
        + "\nform="
        + layoutMetrics(form)
        + "\nfooter="
        + layoutMetrics(field(form, "footer"))
        + "\nscroll="
        + layoutMetrics(scroll)
        + "\nviewport="
        + scroll.getViewportBounds()
        + "\ncontent="
        + layoutMetrics(scroll.getContent())
        + "\ncolumns="
        + layoutMetrics(field(form, "columns"))
        + "\ninformation="
        + layoutMetrics(field(form, "information"))
        + "\nscheduling="
        + layoutMetrics(field(form, "scheduling"))
        + "\nvalidation="
        + layoutMetrics(validation)
        + ", text="
        + validation.getText()
        + "\nsave="
        + layoutMetrics(save)
        + "\ncancel="
        + layoutMetrics(field(form, "cancel"));
  }

  private static String sellerWindowMetrics(int requestedWidth) {
    return "Seller layout requested="
        + requestedWidth
        + "x"
        + (requestedWidth == 640 ? 640 : requestedWidth == 960 ? 720 : 800)
        + ", stage="
        + stage.getWidth()
        + "x"
        + stage.getHeight()
        + ", scene="
        + stage.getScene().getWidth()
        + "x"
        + stage.getScene().getHeight()
        + ", outputScale="
        + stage.getOutputScaleX()
        + "x"
        + stage.getOutputScaleY()
        + ", renderScale="
        + stage.getRenderScaleX()
        + "x"
        + stage.getRenderScaleY();
  }

  private static String layoutMetrics(javafx.scene.Node node) {
    String size =
        node instanceof javafx.scene.layout.Region region
            ? "size="
                + region.getWidth()
                + "x"
                + region.getHeight()
                + ", needsLayout="
                + region.isNeedsLayout()
                + ", "
            : "";
    return size
        + "layout="
        + node.getLayoutBounds()
        + ", local="
        + node.getBoundsInLocal()
        + ", scene="
        + node.localToScene(node.getBoundsInLocal());
  }

  private static void assertSellerLayout(int width, SellerProductFormView activeForm, String state)
      throws Exception {
    Supplier<String> details = () -> state + "\n" + sellerLayoutDiagnostics(width, activeForm);
    awaitSceneLayout(() -> true, details);
    fx(
        () -> {
          String diagnostics = details.get();
          Button save = field(activeForm, "save");
          ScrollPane scroll = field(activeForm, "scroll");
          var footer = (javafx.scene.layout.HBox) field(activeForm, "footer");
          var bounds = save.localToScene(save.getBoundsInLocal());
          System.out.println(
              sellerWindowMetrics(width)
                  + ", state="
                  + state
                  + ", save="
                  + bounds
                  + ", formWidth="
                  + activeForm.getWidth()
                  + ", footerWidth="
                  + footer.getWidth()
                  + ", scrollWidth="
                  + scroll.getWidth()
                  + ", viewportWidth="
                  + scroll.getViewportBounds().getWidth()
                  + ", layoutPending="
                  + layoutPending(stage.getScene().getRoot()));
          assertTrue(bounds.getMaxY() <= stage.getScene().getHeight() + 1, diagnostics);
          assertTrue(bounds.getMaxX() <= stage.getScene().getWidth() + 1, diagnostics);
          for (javafx.scene.Node node :
              java.util.List.<javafx.scene.Node>of(
                  field(activeForm, "footer"), field(activeForm, "cancel"), save)) {
            var visible = node.localToScene(node.getBoundsInLocal());
            assertTrue(visible.getMinX() >= -1 && visible.getMinY() >= -1, diagnostics);
            assertTrue(visible.getMaxX() <= stage.getScene().getWidth() + 1, diagnostics);
            assertTrue(visible.getMaxY() <= stage.getScene().getHeight() + 1, diagnostics);
          }
          Label validation = field(activeForm, "validation");
          assertTrue(validation.isWrapText(), diagnostics);
          assertTrue(validation.getWidth() < footer.getWidth(), diagnostics);
          if (state.equals("long-save-error")) {
            assertTrue(
                validation.getHeight() > validation.getFont().getSize() * 2,
                "Long error must wrap onto multiple lines\n" + diagnostics);
          }
          assertTrue(
              scroll.getContent().getBoundsInLocal().getWidth()
                  <= scroll.getViewportBounds().getWidth() + 1,
              "Form must fit the viewport\n" + diagnostics);
          assertFalse(
              scroll.lookupAll(".scroll-bar").stream()
                  .filter(ScrollBar.class::isInstance)
                  .map(ScrollBar.class::cast)
                  .anyMatch(
                      bar ->
                          bar.isVisible()
                              && bar.getOrientation() == javafx.geometry.Orientation.HORIZONTAL),
              "Form must not need a horizontal scrollbar\n" + diagnostics);
          return null;
        });
  }

  @Test
  @Order(19)
  void sellerPaneStaysInMainWindowAndCleansUpOnCancelEscapeAndNavigation() throws Exception {
    assertTrue(ServiceRegistry.getService().login("seller_hieu", "123456", null));
    SellerProductsController seller =
        fx(() -> ViewNavigator.navigate(ViewNavigator.Route.PRODUCTS));
    await(() -> !seller.isLoading());
    int before = ServiceRegistry.getProductService().listProducts("seller_hieu").size();
    try {
      Supplier<String> resizeDetails =
          () ->
              "stage="
                  + stage.getWidth()
                  + "x"
                  + stage.getHeight()
                  + ", scene="
                  + stage.getScene().getWidth()
                  + "x"
                  + stage.getScene().getHeight()
                  + ", outputScale="
                  + stage.getOutputScaleX()
                  + "x"
                  + stage.getOutputScaleY()
                  + ", root="
                  + layoutMetrics(stage.getScene().getRoot());
      awaitSceneLayout(() -> true, resizeDetails);
      // Measure window decorations on this platform; Stage dimensions include them.
      double frameWidth = fx(() -> stage.getWidth() - stage.getScene().getWidth());
      double frameHeight = fx(() -> stage.getHeight() - stage.getScene().getHeight());
      for (int width : new int[] {1200, 960, 640}) {
        int height = width == 640 ? 640 : width == 960 ? 720 : 800;
        fx(
            () -> {
              stage.setWidth(width);
              stage.setHeight(height);
              return null;
            });
        awaitSceneLayout(
            () ->
                Math.abs(stage.getWidth() - width) < 1
                    && Math.abs(stage.getHeight() - height) < 1
                    && Math.abs(stage.getScene().getWidth() - (width - frameWidth)) < 1
                    && Math.abs(stage.getScene().getHeight() - (height - frameHeight)) < 1,
            () -> "Resize requested=" + width + "x" + height + ", " + resizeDetails.get());
        fx(
            () -> {
              ((Button) field(seller, "addButton")).fire();
              return null;
            });
        await(() -> productForm() != null);
        SellerProductFormView activeForm = fx(() -> field(seller, "activeForm"));
        awaitSceneLayout(() -> true, () -> sellerLayoutDiagnostics(width, activeForm));
        fx(
            () -> {
              Parent form = activeForm;
              assertSame(stage, form.getScene().getWindow());
              String diagnostics = sellerLayoutDiagnostics(width, activeForm);
              var left = (javafx.scene.layout.VBox) field(activeForm, "information");
              var right = (javafx.scene.layout.VBox) field(activeForm, "scheduling");
              var priceGroup = (javafx.scene.layout.VBox) field(activeForm, "priceGroup");
              var stepGroup = (javafx.scene.layout.VBox) field(activeForm, "stepGroup");
              if (width >= 960) {
                assertEquals(
                    left.getLayoutY(),
                    right.getLayoutY(),
                    1,
                    "Wide form must use two columns\n" + diagnostics);
                assertEquals(
                    priceGroup.getLayoutY(),
                    stepGroup.getLayoutY(),
                    1,
                    "Price fields share a row\n" + diagnostics);
              } else {
                assertTrue(
                    right.getLayoutY() > left.getLayoutY(),
                    "Narrow form stacks columns\n" + diagnostics);
                assertTrue(
                    stepGroup.getLayoutY() > priceGroup.getLayoutY(),
                    "Narrow form stacks prices\n" + diagnostics);
              }
              assertEquals(
                  1,
                  javafx.stage.Window.getWindows().stream()
                      .filter(javafx.stage.Window::isShowing)
                      .count(),
                  "No product Stage or Dialog");
              fillProduct(form);
              Button save = (Button) form.lookup("#productSave");
              assertFalse(save.isDisabled());
              DatePicker start = (DatePicker) form.lookup("#productStart");
              start.getEditor().setText("31/02/2027");
              assertTrue(save.isDisabled(), "Invalid typed date must not use the previous value");
              start.getEditor().setText(start.getConverter().toString(start.getValue()));
              assertFalse(save.isDisabled());
              ((TextField) form.lookup("#productStep")).setText("0");
              assertTrue(save.isDisabled());
              assertTrue(
                  ((Label) form.lookup("#productValidation")).getText().contains("bước giá"));
              return null;
            });
        assertSellerLayout(width, activeForm, "invalid-step");
        fx(
            () -> {
              activeForm.showSaveError(
                  "Không thể lưu sản phẩm. Vui lòng kiểm tra và thử lại. ".repeat(4));
              return null;
            });
        assertSellerLayout(width, activeForm, "long-save-error");
        fx(
            () -> {
              activeForm.setBusy(true);
              return null;
            });
        assertSellerLayout(width, activeForm, "busy");
        fx(
            () -> {
              activeForm.setBusy(false);
              ((TextField) activeForm.lookup("#productStep")).setText("10000");
              assertFalse(((Button) activeForm.lookup("#productSave")).isDisabled());
              return null;
            });
        assertSellerLayout(width, activeForm, "valid");
        snapshot(
            "seller-form-"
                + width
                + "-scale-"
                + System.getProperty("glass.win.uiScale", "default"));
        var imageTasks =
            fx(() -> (com.auction.client.ui.SellerProductFormView) field(seller, "activeForm"));
        fx(
            () -> {
              ((Button) productForm().lookup("#productCancel")).fire();
              return null;
            });
        assertTrue(imageTasks.isClosed());
        assertNull(fx(ClientUiTest::productForm));
      }
      fx(
          () -> {
            ((Button) field(seller, "addButton")).fire();
            return null;
          });
      await(() -> productForm() != null);
      fx(
          () -> {
            productForm()
                .lookup("#productName")
                .fireEvent(
                    new javafx.scene.input.KeyEvent(
                        javafx.scene.input.KeyEvent.KEY_PRESSED,
                        "",
                        "",
                        javafx.scene.input.KeyCode.ESCAPE,
                        false,
                        false,
                        false,
                        false));
            assertNull(productForm());
            ((Button) field(seller, "addButton")).fire();
            return null;
          });
      var imageTasks =
          fx(() -> (com.auction.client.ui.SellerProductFormView) field(seller, "activeForm"));
      fx(
          () -> {
            ViewNavigator.navigate(ViewNavigator.Route.WALLET);
            return null;
          });
      assertTrue(imageTasks.isClosed());
      assertNull(fx(() -> stage.getScene().lookup("#productForm")));
      assertEquals(before, ServiceRegistry.getProductService().listProducts("seller_hieu").size());
    } finally {
      fx(
          () -> {
            stage.setWidth(1200);
            stage.setHeight(800);
            return null;
          });
    }
  }

  @Test
  @Order(20)
  void sellerSaveFailureKeepsDraftForCorrectionWithoutExtraWindow() throws Exception {
    assertTrue(ServiceRegistry.getService().login("seller_hieu", "123456", null));
    var original = ServiceRegistry.getProductService();
    var failing =
        (com.auction.client.service.ProductService)
            java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {com.auction.client.service.ProductService.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("saveProduct")) {
                    throw new IllegalStateException("QA save rejected");
                  }
                  return method.invoke(original, args);
                });
    ServiceRegistry.setProductService(failing);
    try {
      SellerProductsController seller =
          fx(() -> ViewNavigator.navigate(ViewNavigator.Route.PRODUCTS));
      await(() -> !seller.isLoading());
      fx(
          () -> {
            ((Button) field(seller, "addButton")).fire();
            return null;
          });
      await(() -> productForm() != null);
      fx(
          () -> {
            fillProduct(productForm());
            ((Button) productForm().lookup("#productSave")).fire();
            return null;
          });
      await(
          () ->
              !seller.isLoading()
                  && ((Label) field(seller, "message")).getText().contains("QA save rejected"));
      fx(
          () -> {
            assertEquals(
                "QA pane product", ((TextField) productForm().lookup("#productName")).getText());
            assertFalse(((Button) productForm().lookup("#productSave")).isDisabled());
            ((Button) productForm().lookup("#productBack")).fire();
            assertNull(productForm());
            return null;
          });
    } finally {
      fx(
          () -> {
            ViewNavigator.disposeCurrentView();
            return null;
          });
      ServiceRegistry.setProductService(original);
    }
  }

  @Test
  @Order(21)
  void homeSeparatesHistoryAndPreservesSearchCategoryAndStatusFilters() throws Exception {
    HomeController home = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD));
    await(() -> !home.isLoading());
    fx(
        () -> {
          FlowPane finished = field(home, "finishedGrid");
          assertTrue(finished.getChildren().isEmpty());
          assertFalse(((javafx.scene.layout.VBox) field(home, "finishedPanel")).isManaged());
          ((ToggleButton) field(home, "historyTab")).fire();
          assertFalse(finished.getChildren().isEmpty());
          for (var card : finished.getChildren()) {
            card.applyCss();
            assertEquals(1, card.lookupAll(".badge-status").size());
            assertTrue(
                card.lookupAll(".countdown-label").isEmpty(),
                "Terminal card has no duplicate ended badge");
          }
          ((ComboBox<String>) field(home, "homeStatus"))
              .setValue(AuctionStatus.FINISHED.getDescription());
          assertEquals(
              AuctionStatus.FINISHED.getDescription(),
              ((ComboBox<?>) field(home, "homeStatus")).getValue());
          for (var card : finished.getChildren()) {
            assertEquals(
                AuctionStatus.FINISHED.getDescription(),
                ((Label) card.lookup(".badge-status")).getText());
          }
          ((TextField) field(home, "txtSearch")).setText("QA nonexistent product");
          assertTrue(finished.getChildren().isEmpty());
          ((TextField) field(home, "txtSearch")).clear();
          ((ComboBox<String>) field(home, "homeStatus"))
              .setValue(AuctionStatus.OPEN.getDescription());
          assertTrue(((ToggleButton) field(home, "activeTab")).isSelected());
          assertFalse(((FlowPane) field(home, "upcomingGrid")).getChildren().isEmpty());
          ((Button) field(home, "chipArt")).fire();
          for (var card : ((FlowPane) field(home, "upcomingGrid")).getChildren()) {
            String id = (String) card.getUserData();
            assertEquals(
                ItemCategory.ART,
                ItemCategory.from(
                    ServiceRegistry.getService().getAuctionById(id).getItemCategory()));
          }
          return null;
        });
    snapshot("home-active-filtered");
    fx(
        () -> {
          ((Button) field(home, "chipAll")).fire();
          ((ToggleButton) field(home, "historyTab")).fire();
          return null;
        });
    snapshot("home-history");
  }

  @Test
  @Order(22)
  void sellerScheduleUsesServerTimeRestoresManualInputAndRechecksExpiry() throws Exception {
    var clock =
        new java.util.concurrent.atomic.AtomicReference<>(
            java.time.LocalDateTime.of(2032, 3, 2, 10, 15));
    var drafts = new java.util.ArrayList<com.auction.client.ui.SellerProductFormView.Draft>();
    var holder =
        new java.util.concurrent.atomic.AtomicReference<
            com.auction.client.ui.SellerProductFormView>();
    var locale = java.util.Locale.getDefault();
    try {
      fx(
          () -> {
            ViewNavigator.disposeCurrentView();
            var form =
                new com.auction.client.ui.SellerProductFormView(
                    null,
                    null,
                    clock::get,
                    draft -> {
                      drafts.add(draft);
                      holder.get().setBusy(true);
                    },
                    () -> {});
            holder.set(form);
            stage.getScene().setRoot(form);
            ViewNavigator.applyAppStylesheet(stage.getScene());
            form.applyCss();
            form.layout();
            assertFalse(
                ((Label) form.lookup("#productValidation"))
                    .getPseudoClassStates()
                    .contains(javafx.css.PseudoClass.getPseudoClass("error")),
                "Fresh form has no red error");
            fillProduct(form);
            var start = (DatePicker) form.lookup("#productStart");
            var end = (DatePicker) form.lookup("#productEnd");
            final var startTime = (TextField) form.lookup("#productStartTime");
            start.setValue(clock.get().toLocalDate().plusDays(1));
            end.setValue(clock.get().toLocalDate().plusDays(2));
            final var manual = start.getValue();
            var immediate = (CheckBox) form.lookup("#productStartNow");
            immediate.fire();
            assertTrue(start.isDisabled());
            assertTrue(startTime.isDisabled());
            assertEquals(
                clock.get().toLocalDate(), start.getValue(), "Display uses injected Server clock");
            immediate.fire();
            assertEquals(manual, start.getValue(), "Turning off immediate restores the draft date");
            assertEquals("10:00", startTime.getText());
            assertFalse(start.isDisabled());
            Button save = (Button) form.lookup("#productSave");
            assertFalse(save.isDisabled());
            clock.set(manual.atTime(10, 0, 1));
            save.fire();
            assertTrue(drafts.isEmpty(), "Expired schedule must not reach the Controller");
            assertTrue(((Label) form.lookup("#productValidation")).getText().contains("đã qua"));
            immediate.fire();
            assertFalse(save.isDisabled());
            save.fire();
            save.fire();
            assertEquals(1, drafts.size(), "Busy form cannot double-submit");
            assertTrue(drafts.getFirst().schedule().startNow());
            assertEquals(
                clock.get(),
                drafts.getFirst().schedule().start(),
                "Immediate does not round to next minute");
            form.setBusy(false);
            form.showSaveError("QA backend rejected draft");
            assertTrue(
                ((Label) form.lookup("#productValidation"))
                    .getText()
                    .contains("QA backend rejected"));
            assertEquals("QA pane product", ((TextField) form.lookup("#productName")).getText());
            assertTrue(save.isDefaultButton());
            assertTrue(((Button) form.lookup("#productCancel")).isCancelButton());
            return null;
          });
      assertEquals(locale, java.util.Locale.getDefault(), "Form must not change JVM locale");
    } finally {
      fx(
          () -> {
            if (holder.get() != null) {
              holder.get().close();
            }
            ViewNavigator.navigate(ViewNavigator.Route.DASHBOARD);
            return null;
          });
    }
  }

  @Test
  @Order(23)
  void sellerDatePickerSkinAndImageSelectionRemainUsable() throws Exception {
    assertTrue(ServiceRegistry.getService().login("seller_hieu", "123456", null));
    SellerProductsController seller =
        fx(() -> ViewNavigator.navigate(ViewNavigator.Route.PRODUCTS));
    await(() -> !seller.isLoading());
    fx(
        () -> {
          ((Button) field(seller, "addButton")).fire();
          return null;
        });
    await(() -> productForm() != null);
    fx(
        () -> {
          Parent form = productForm();
          fillProduct(form);
          var start = (DatePicker) form.lookup("#productStart");
          var time = (TextField) form.lookup("#productStartTime");
          assertEquals(40, start.getHeight(), 1);
          assertEquals(40, time.getHeight(), 1);
          assertEquals("HH:mm", time.getPromptText());
          assertEquals(
              "10:00",
              time.queryAccessibleAttribute(javafx.scene.AccessibleAttribute.TEXT),
              "Screen reader must receive the time value, not the format hint");
          assertTrue(time.getAccessibleHelp().contains("Giờ bắt đầu"));
          assertTrue(time.getWidth() >= 96);
          assertTrue(start.getSkin() instanceof javafx.scene.control.skin.DatePickerSkin);
          assertEquals(
              0,
              start.getEditor().getBorder().getStrokes().getFirst().getWidths().getTop(),
              .01,
              "Editor inside DatePicker has no second border");
          assertTrue(start.lookup(".arrow-button").getBoundsInLocal().getWidth() > 0);
          start.show();
          return null;
        });
    await(() -> ((DatePicker) productForm().lookup("#productStart")).isShowing());
    snapshot("seller-calendar-open");
    fx(
        () -> {
          var start = (DatePicker) productForm().lookup("#productStart");
          start.setValue(java.time.LocalDate.now().plusDays(1));
          start.hide();
          start.getEditor().setText("31/02/2027");
          assertTrue(((Button) productForm().lookup("#productSave")).isDisabled());
          start.getEditor().setText(start.getConverter().toString(start.getValue()));
          return null;
        });
    var form = fx(() -> (com.auction.client.ui.SellerProductFormView) field(seller, "activeForm"));
    var picker = fx(() -> (com.auction.client.ui.ProductImagePicker) field(form, "imagePicker"));
    Path directory = Files.createTempDirectory("seller-image-picker-");
    Path invalid = directory.resolve("invalid.jpg");
    Path valid = directory.resolve("valid.png");
    Files.writeString(invalid, "not an image");
    ImageIO.write(
        new java.awt.image.BufferedImage(12, 12, java.awt.image.BufferedImage.TYPE_INT_RGB),
        "png",
        valid.toFile());
    fx(
        () -> {
          picker.select(invalid.toFile());
          return null;
        });
    await(() -> !picker.isLoading() && !picker.error().isEmpty());
    fx(
        () -> {
          assertNull(picker.path());
          assertTrue(((Button) productForm().lookup("#productSave")).isDisabled());
          picker.select(valid.toFile());
          return null;
        });
    await(
        () ->
            !picker.isLoading()
                && picker.error().isEmpty()
                && ((javafx.scene.image.ImageView) productForm().lookup("#productImage")).getImage()
                    != null);
    snapshot("seller-image-selected");
    fx(
        () -> {
          assertEquals(valid.toAbsolutePath().toString(), picker.path());
          ((Button) productForm().lookup("#productRemoveImage")).fire();
          assertNull(picker.path());
          assertNull(
              ((javafx.scene.image.ImageView) productForm().lookup("#productImage")).getImage());
          ((Button) productForm().lookup("#productCancel")).fire();
          assertTrue(picker.isClosed());
          return null;
        });
  }

  @Test
  @Order(24)
  void dialogsHaveOwnerAndCloseWhenTheirPageOrOwnerChanges() throws Exception {
    fx(
        () -> {
          var dialog = new Dialog<ButtonType>();
          dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
          ViewNavigator.prepareDialog(dialog);
          assertSame(stage, dialog.getOwner());
          assertEquals(javafx.stage.Modality.WINDOW_MODAL, dialog.getModality());
          dialog.show();
          assertTrue(dialog.isShowing());
          ViewNavigator.navigate(ViewNavigator.Route.WALLET);
          assertFalse(dialog.isShowing(), "Navigation closes a dialog from the old page");
          dialog.show();
          stage.hide();
          assertFalse(dialog.isShowing(), "Owner hide closes the dialog");
          stage.show();
          return null;
        });
  }

  @Test
  @Order(25)
  void adminNavigationAndFiltersFitSmallWindowsWithoutLosingTables() throws Exception {
    assertTrue(ServiceRegistry.getService().login("admin_hieu", "123456", null));
    AdminController admin = fx(() -> ViewNavigator.navigate(ViewNavigator.Route.ADMIN));
    await(() -> !((ProgressIndicator) field(admin, "progress")).isVisible());
    try {
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
            var navigation = root.lookup(".admin-navigation");
            assertNotNull(navigation);
            assertTrue(
                navigation.localToScene(navigation.getBoundsInLocal()).getMaxX()
                    <= stage.getScene().getWidth() + 1);
            Label title = field(admin, "title");
            assertTrue(title.isWrapText());
            assertTrue(((TableView<?>) field(admin, "auctionTable")).getWidth() > 500);
            var table = (TableView<?>) field(admin, "auctionTable");
            table.applyCss();
            table.layout();
            var horizontal =
                table.lookupAll(".scroll-bar").stream()
                    .filter(ScrollBar.class::isInstance)
                    .map(ScrollBar.class::cast)
                    .filter(bar -> bar.getOrientation() == javafx.geometry.Orientation.HORIZONTAL)
                    .findFirst()
                    .orElseThrow();
            assertTrue(
                horizontal.isVisible(), "Narrow table must allow reaching the action column");
            horizontal.setValue(horizontal.getMax());
            return null;
          });
      awaitFx(
          () -> {
            var table = (TableView<?>) field(admin, "auctionTable");
            var bounds = table.localToScene(table.getBoundsInLocal());
            return table.lookupAll(".button").stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(
                        button ->
                            java.util.Set.of("Chi tiết", "Hủy", "Xóa").contains(button.getText()))
                    .filter(
                        button -> bounds.contains(button.localToScene(button.getBoundsInLocal())))
                    .count()
                >= 3;
          });
      snapshot("admin-640");
      fx(
          () -> {
            Parent root = stage.getScene().getRoot();
            root.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(b -> b.getText().equals("Quản lý người dùng"))
                .findFirst()
                .orElseThrow()
                .fire();
            return null;
          });
      await(() -> !((ProgressIndicator) field(admin, "progress")).isVisible());
      fx(
          () -> {
            var search = (TextField) field(admin, "userSearch");
            search.setText("qa_nonexistent_admin_user");
            assertTrue(((TableView<?>) field(admin, "userTable")).getItems().isEmpty());
            search.clear();
            assertFalse(((TableView<?>) field(admin, "userTable")).getItems().isEmpty());
            return null;
          });
    } finally {
      fx(
          () -> {
            stage.setWidth(1200);
            stage.setHeight(800);
            return null;
          });
    }
  }
}
