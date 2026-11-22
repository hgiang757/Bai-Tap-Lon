package com.auction.client.ui;

import com.auction.client.model.Auction;
import com.auction.client.util.AuctionTime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Nhập lịch bằng control JavaFX chuẩn; chỉ kiểm định dạng phía Client, không gọi Server. */
public final class AuctionScheduleInput extends VBox {
  private static final String HINT = "Ngày dd/MM/yyyy · Giờ HH:mm (24 giờ), theo giờ máy chủ.";
  private static final DateTimeFormatter DAY =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private final Supplier<LocalDateTime> clock;
  private final LocalDateTime originalStart;
  private final LocalDateTime originalEnd;
  private final CheckBox now = new CheckBox("Bắt đầu ngay");
  private final DatePicker start = day("productStart", "Ngày bắt đầu");
  private final DatePicker end = day("productEnd", "Ngày kết thúc");
  private final TextField startTime = time("productStartTime", "Giờ bắt đầu");
  private final TextField endTime = time("productEndTime", "Giờ kết thúc");
  private Runnable changed = () -> {};
  private LocalDate manualDay;
  private String manualDateText;
  private String manualTime;
  private boolean updating;

  /** Stores the immediate-start flag and the selected auction start and end times. */
  public record Selection(boolean startNow, LocalDateTime start, LocalDateTime end) {}

  /** Builds schedule controls from the server clock and optional original auction. */
  public AuctionScheduleInput(Supplier<LocalDateTime> clock, Auction original) {
    this.clock = clock;
    originalStart = original == null ? null : original.getStartTime();
    originalEnd = original == null ? null : original.getEndTime();
    setSpacing(16);
    now.setId("productStartNow");
    Label heading = new Label("Thời gian đấu giá");
    heading.getStyleClass().add("seller-section-title");
    Label hint = new Label(HINT);
    hint.setWrapText(true);
    hint.getStyleClass().add("field-hint");
    getChildren()
        .addAll(
            heading,
            now,
            row("Bắt đầu *", start, startTime),
            row("Kết thúc *", end, endTime),
            hint);
    if (original != null) {
      start.setValue(originalStart.toLocalDate());
      end.setValue(originalEnd.toLocalDate());
      startTime.setText(originalStart.format(TIME));
      endTime.setText(originalEnd.format(TIME));
    }
    for (DatePicker day : List.of(start, end)) {
      day.valueProperty().addListener((o, before, value) -> notifyChanged());
      day.getEditor().textProperty().addListener((o, before, value) -> notifyChanged());
    }
    for (TextField time : List.of(startTime, endTime)) {
      time.textProperty().addListener((o, before, value) -> notifyChanged());
    }
    now.selectedProperty()
        .addListener(
            (o, before, selected) -> {
              updating = true;
              if (selected) {
                manualDay = start.getValue();
                manualDateText = start.getEditor().getText();
                manualTime = startTime.getText();
                LocalDateTime current = clock.get();
                start.setValue(current.toLocalDate());
                startTime.setText(current.format(TIME));
              } else {
                start.setValue(manualDay);
                start.getEditor().setText(manualDateText);
                startTime.setText(manualTime);
              }
              start.setDisable(selected);
              startTime.setDisable(selected);
              hint.setText(selected ? "Máy chủ sẽ lấy giờ bắt đầu khi lưu." : HINT);
              updating = false;
              notifyChanged();
            });
  }

  public void setOnChange(Runnable changed) {
    this.changed = changed;
  }

  private void notifyChanged() {
    if (!updating) {
      changed.run();
    }
  }

  /** Reads the selected times, using the server clock for immediate starts. */
  public Selection selection() {
    return new Selection(
        now.isSelected(),
        now.isSelected()
            ? clock.get()
            : dateTime(
                parseDate(start.getEditor().getText()),
                AuctionTime.parse(startTime.getText()),
                originalStart),
        dateTime(
            parseDate(end.getEditor().getText()),
            AuctionTime.parse(endTime.getText()),
            originalEnd));
  }

  /** Returns the current schedule error, or an empty string when valid. */
  public String validationMessage() {
    boolean immediate = now.isSelected();
    LocalDate startDate = parseDate(start.getEditor().getText());
    LocalDate endDate = parseDate(end.getEditor().getText());
    LocalTime startHour = AuctionTime.parse(startTime.getText());
    if (!immediate && startDate == null) {
      return "Ngày bắt đầu cần đúng dd/MM/yyyy và phải là ngày tồn tại.";
    }
    if (!immediate && startHour == null) {
      return "Giờ bắt đầu cần đúng HH:mm (00:00–23:59).";
    }
    if (endDate == null) {
      return "Ngày kết thúc cần đúng dd/MM/yyyy và phải là ngày tồn tại.";
    }
    LocalTime endHour = AuctionTime.parse(endTime.getText());
    if (endHour == null) {
      return "Giờ kết thúc cần đúng HH:mm (00:00–23:59).";
    }
    LocalDateTime current = clock.get();
    LocalDateTime startsAt = immediate ? current : dateTime(startDate, startHour, originalStart);
    LocalDateTime endsAt = dateTime(endDate, endHour, originalEnd);
    if (!endsAt.isAfter(startsAt)) {
      return "Kết thúc phải sau thời gian bắt đầu.";
    }
    if (!immediate && startsAt.isBefore(current)) {
      return "Thời gian bắt đầu đã qua. Chọn lại hoặc dùng Bắt đầu ngay.";
    }
    if (Duration.between(startsAt, endsAt).compareTo(Duration.ofDays(365)) > 0) {
      return "Thời lượng đấu giá tối đa 365 ngày.";
    }
    return "";
  }

  static LocalDate parseDate(String text) {
    if (text == null || text.length() != 10) {
      return null;
    }
    try {
      LocalDate date = LocalDate.parse(text, DAY);
      return date.getYear() > 0 ? date : null;
    } catch (DateTimeParseException error) {
      return null;
    }
  }

  static LocalDateTime dateTime(LocalDate date, LocalTime time, LocalDateTime original) {
    if (date == null || time == null) {
      return null;
    }
    LocalDateTime entered = LocalDateTime.of(date, time);
    // Phần giây của lịch đã lưu được giữ khi người bán không đổi ngày hoặc phút.
    return original != null && entered.equals(original.truncatedTo(ChronoUnit.MINUTES))
        ? original
        : entered;
  }

  private static DatePicker day(String id, String accessibleName) {
    DatePicker picker = new DatePicker();
    picker.setId(id);
    picker.setPromptText("dd/MM/yyyy");
    picker.setAccessibleText(accessibleName + " (dd/MM/yyyy)");
    picker.setChronology(IsoChronology.INSTANCE);
    picker.setMinWidth(0);
    picker.setMaxWidth(Double.MAX_VALUE);
    picker.setConverter(
        new StringConverter<>() {
          public String toString(LocalDate value) {
            return value == null ? "" : DAY.format(value);
          }

          public LocalDate fromString(String text) {
            LocalDate date = parseDate(text);
            // Keep the invalid draft visible on focus loss so the form can explain the error.
            return date == null && text != null && !text.isEmpty() ? picker.getValue() : date;
          }
        });
    ScheduleTextFormatter.install(picker.getEditor(), '/', 8);
    picker
        .getEditor()
        .addEventFilter(
            KeyEvent.KEY_PRESSED,
            event -> {
              if (event.getCode() == KeyCode.ENTER
                  && parseDate(picker.getEditor().getText()) == null) {
                event.consume();
              }
            });
    picker
        .getEditor()
        .focusedProperty()
        .addListener(
            (o, before, focused) ->
                picker.pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("entry-focused"), focused));
    return picker;
  }

  private static TextField time(String id, String accessibleName) {
    TextField input = new TextField();
    input.setId(id);
    input.setAccessibleHelp(accessibleName + " (HH:mm)");
    input.setPromptText("HH:mm");
    input.getStyleClass().add("seller-time-input");
    ScheduleTextFormatter.install(input, ':', 4);
    return input;
  }

  private static VBox row(String text, DatePicker day, TextField time) {
    Label label = new Label(text);
    label.getStyleClass().add("seller-field-label");
    label.setLabelFor(day);
    HBox inputs = new HBox(8, day, time);
    inputs.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    HBox.setHgrow(day, Priority.ALWAYS);
    return new VBox(6, label, inputs);
  }
}
