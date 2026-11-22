package com.auction.client.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Formats date/time digits as they are edited. Calendar validation is separate. */
final class ScheduleTextFormatter {
  private ScheduleTextFormatter() {}

  static void install(TextField field, char separator, int digitLimit) {
    field.setTextFormatter(new TextFormatter<>(change -> format(change, separator, digitLimit)));
  }

  private static TextFormatter.Change format(
      TextFormatter.Change change, char separator, int digitLimit) {
    if (!change.isContentChange()) {
      return change;
    }
    String old = change.getControlText();
    String inserted = change.getText();
    String proposed = change.getControlNewText();
    int start = change.getRangeStart();
    int end = change.getRangeEnd();
    int digitsBefore = countDigits(old, start);
    for (int i = 0; i < inserted.length(); i++) {
      char c = inserted.charAt(i);
      if (isDigit(c)) {
        digitsBefore++;
      } else if (c != separator
          || !separatorAfter(digitsBefore, digitLimit)
          || (start + i > 0 && proposed.charAt(start + i - 1) == separator)) {
        return null;
      }
    }
    int totalDigits = countDigits(proposed, proposed.length());
    if (totalDigits > digitLimit) {
      return null;
    }
    // Backspace/Delete at a separator crosses it without erasing a neighboring digit.
    if (inserted.isEmpty()
        && end == start + 1
        && old.charAt(start) == separator
        && end < old.length()) {
      int caret = change.getControlCaretPosition() == end ? start : end;
      change.setRange(caret, caret);
      change.selectRange(caret, caret);
      return change;
    }
    StringBuilder formatted = new StringBuilder();
    int digits = 0;
    for (int i = 0; i < proposed.length(); i++) {
      char c = proposed.charAt(i);
      if (isDigit(c)) {
        if (separatorAfter(digits, digitLimit)) {
          formatted.append(separator);
        }
        formatted.append(c);
        digits++;
      }
    }
    if (separatorAfter(digits, digitLimit) && proposed.endsWith(String.valueOf(separator))) {
      formatted.append(separator);
    }
    String result = formatted.toString();
    if (!result.equals(proposed)) {
      int anchor = position(proposed, change.getAnchor(), result);
      int caret = position(proposed, change.getCaretPosition(), result);
      change.setRange(0, old.length());
      change.setText(result);
      change.selectRange(anchor, caret);
    }
    return change;
  }

  private static int position(String proposed, int position, String formatted) {
    int digits = countDigits(proposed, position);
    int offset = 0;
    while (digits > 0 && offset < formatted.length()) {
      if (isDigit(formatted.charAt(offset++))) {
        digits--;
      }
    }
    if (position > 0
        && !isDigit(proposed.charAt(position - 1))
        && offset < formatted.length()
        && !isDigit(formatted.charAt(offset))) {
      offset++;
    }
    return offset;
  }

  private static int countDigits(String text, int end) {
    int count = 0;
    for (int i = 0; i < end; i++) {
      if (isDigit(text.charAt(i))) {
        count++;
      }
    }
    return count;
  }

  private static boolean separatorAfter(int digits, int digitLimit) {
    return digits == 2 || (digitLimit == 8 && digits == 4);
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }
}
