package com.auction.client.ui;

import com.auction.client.navigation.ViewTasks;
import com.auction.client.util.FormatUtil;
import com.auction.shared.AssistantChat;
import com.auction.shared.AssistantChat.Reply;
import java.util.function.Function;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** An embedded chat overlay. All methods and callbacks that touch controls run on the FX thread. */
public final class AuctionAssistantView extends StackPane {
  private static final String TITLE = "Trợ lý đấu giá AI";
  private final Button launcher = new Button();
  private final VBox panel = new VBox(10);
  private final VBox messages = new VBox(9);
  private final ScrollPane scroll = new ScrollPane(messages);
  private final TextArea input = new TextArea();
  private final Button send = new Button("Gửi");
  private final FlowPane suggestions = new FlowPane(6, 6);
  private final HBox loading = new HBox(7);
  private ViewTasks tasks;
  private Function<String, Reply> ask;
  private boolean busy;

  /** Builds the floating launcher and chat panel for use from FXML or Java. */
  public AuctionAssistantView() {
    setAlignment(Pos.BOTTOM_RIGHT);
    setPickOnBounds(false);
    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    launcher.setId("assistantLauncher");
    launcher.getStyleClass().addAll("btn-primary", "assistant-launcher");
    launcher.setGraphic(Icons.icon("chat", 24));
    launcher.setAccessibleText(TITLE);
    launcher.setTooltip(new Tooltip(TITLE));
    launcher.setOnAction(event -> showPanel(true));

    panel.setId("assistantPanel");
    panel.getStyleClass().add("assistant-panel");
    panel.setPrefSize(380, 455);
    panel.setMinSize(0, 0);
    panel.setMaxSize(380, 455);
    panel.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == KeyCode.ESCAPE) {
            showPanel(false);
            event.consume();
          }
        });

    Label heading = new Label(TITLE);
    heading.getStyleClass().add("assistant-title");
    Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    HBox header =
        new HBox(
            5,
            heading,
            space,
            windowButton("assistantMinimize", "−", "Thu nhỏ", () -> showPanel(false)),
            windowButton("assistantClose", "×", "Đóng cuộc trò chuyện", this::closeChat));
    header.setAlignment(Pos.CENTER_LEFT);

    messages.setId("assistantMessages");
    messages.getStyleClass().add("assistant-messages");
    scroll.getStyleClass().add("assistant-scroll");
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setMinHeight(0);
    scroll.setAccessibleText("Tin nhắn trợ lý");
    VBox.setVgrow(scroll, Priority.ALWAYS);
    messages.heightProperty().addListener((obs, oldHeight, height) -> scroll.setVvalue(1));

    addSuggestion("Giới thiệu", "Hãy giới thiệu sản phẩm đang được đấu giá.");
    addSuggestion("Giá thị trường", "Sản phẩm này có giá thị trường đã xác minh không?");
    addSuggestion(
        "Đánh giá giá đấu", "Hãy so sánh giá đấu hiện tại với giá thị trường đã xác minh.");
    addSuggestion("Lưu ý khi mua", "Tôi cần kiểm tra những gì trước khi đấu giá sản phẩm này?");

    ProgressIndicator spinner = new ProgressIndicator();
    spinner.setMaxSize(16, 16);
    loading.setId("assistantLoading");
    loading.setAlignment(Pos.CENTER_LEFT);
    loading.getStyleClass().add("assistant-loading");
    loading.getChildren().addAll(spinner, new Label("Trợ lý đang trả lời…"));

    input.setId("assistantInput");
    input.getStyleClass().add("assistant-input");
    input.setPromptText("Nhập câu hỏi về sản phẩm");
    input.setAccessibleText("Câu hỏi về sản phẩm");
    input.setWrapText(true);
    input.setPrefRowCount(2);
    input.setTextFormatter(
        new TextFormatter<>(
            change ->
                change.getControlNewText().length() <= AssistantChat.MAX_QUESTION_LENGTH
                    ? change
                    : null));
    input.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
            submit(input.getText());
            event.consume();
          }
        });
    send.setId("assistantSend");
    send.getStyleClass().addAll("btn-primary", "assistant-send");
    send.setOnAction(event -> submit(input.getText()));
    input.textProperty().addListener((obs, oldText, text) -> updateSend());
    HBox composer = new HBox(8, input, send);
    composer.setAlignment(Pos.BOTTOM_RIGHT);
    HBox.setHgrow(input, Priority.ALWAYS);

    panel.getChildren().addAll(header, scroll, suggestions, loading, composer);
    getChildren().addAll(panel, launcher);
    show(panel, false);
    setBusy(false);
    show(this, false);
  }

  /** Binds a new product and viewer. A null handler detaches the widget from the room. */
  public void bind(Function<String, Reply> handler) {
    closeChat();
    ask = handler;
    show(this, handler != null);
  }

  private void showPanel(boolean open) {
    show(panel, open);
    show(launcher, !open);
    if (open) {
      if (tasks == null) {
        tasks = ViewTasks.assistant();
        addMessage("Bạn muốn biết gì về sản phẩm đang đấu giá?", "assistant-reply");
      }
      input.requestFocus();
    } else if (isVisible()) {
      launcher.requestFocus();
    }
  }

  private void closeChat() {
    if (tasks != null) {
      tasks.close();
      tasks = null;
    }
    messages.getChildren().clear();
    input.clear();
    setBusy(false);
    showPanel(false);
  }

  private void submit(String draft) {
    String question = draft.strip();
    if (question.isEmpty() || busy || ask == null || tasks == null) {
      return;
    }
    final ViewTasks scope = tasks;
    final var handler = ask;
    addMessage(question, "assistant-question");
    input.clear();
    setBusy(true);
    scope.submit(
        "question",
        () -> {
          if (scope.isClosed()) {
            throw new IllegalStateException("Phiên trò chuyện đã đóng.");
          }
          return handler.apply(question);
        },
        reply -> {
          setBusy(false);
          String price =
              reply.hasBids() ? "Giá đấu lúc hỏi: " : "Giá khởi điểm (chưa có lượt đấu): ";
          addMessage(
              reply.answer()
                  + "\n\n"
                  + price
                  + FormatUtil.formatCurrency(reply.currentPrice())
                  + "\n"
                  + reply.marketSummary(),
              "assistant-reply");
        },
        error -> {
          setBusy(false);
          addMessage(
              error instanceof IllegalStateException
                  ? error.getMessage()
                  : "Trợ lý chưa trả lời được. Vui lòng thử lại.",
              "assistant-error");
        });
  }

  private void setBusy(boolean running) {
    busy = running;
    show(loading, running);
    suggestions.setDisable(running);
    updateSend();
  }

  private void updateSend() {
    send.setDisable(busy || input.getText().isBlank());
  }

  private void addSuggestion(String label, String question) {
    Button button = new Button(label);
    button.getStyleClass().add("assistant-suggestion");
    button.setOnAction(event -> submit(question));
    suggestions.getChildren().add(button);
  }

  private void addMessage(String text, String style) {
    Label bubble = new Label(text);
    bubble.getStyleClass().addAll("assistant-bubble", style);
    bubble.setWrapText(true);
    bubble.setMaxWidth(302);
    HBox row = new HBox(bubble);
    row.setAlignment("assistant-question".equals(style) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    messages.getChildren().add(row);
    if (messages.getChildren().size() > 25) {
      messages.getChildren().removeFirst();
    }
    scroll.setVvalue(1);
  }

  private static Button windowButton(String id, String symbol, String label, Runnable action) {
    Button button = new Button(symbol);
    button.setId(id);
    button.getStyleClass().add("assistant-window-button");
    button.setTooltip(new Tooltip(label));
    button.setAccessibleText(label);
    button.setOnAction(event -> action.run());
    return button;
  }

  private static void show(Node node, boolean visible) {
    node.setVisible(visible);
    node.setManaged(visible);
  }
}
