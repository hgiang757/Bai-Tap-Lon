package com.auction.client.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Validates account input before the client submits it to the server. */
public final class Validators {
  private static final Set<String> GENERIC_TLDS =
      Set.of(
          "com", "net", "org", "edu", "gov", "mil", "int", "info", "biz", "name", "pro", "io",
          "dev", "app", "xyz", "online", "site", "store", "tech", "cloud", "asia", "blog", "shop");

  private Validators() {}

  /** Normalizes Unicode, trims ends and collapses repeated spaces. */
  public static String normalizeUsername(String value) {
    return value == null
        ? ""
        : Normalizer.normalize(value, Normalizer.Form.NFC).strip().replaceAll(" +", " ");
  }

  /** Checks the normalized username length and allowed characters. */
  public static boolean username(String value) {
    String name = normalizeUsername(value);
    int length = name.codePointCount(0, name.length());
    return length >= 3
        && length <= 30
        && name.matches("[\\p{L}\\p{N}_][\\p{L}\\p{N}_ .-]*[\\p{L}\\p{N}_]");
  }

  /** Checks the supported email address format. */
  public static boolean email(String value) {
    if (value == null) {
      return false;
    }
    String email = value.trim();
    if (email.isEmpty() || email.length() > 254) {
      return false;
    }
    int at = email.indexOf('@');
    if (at <= 0 || at != email.lastIndexOf('@')) {
      return false;
    }
    String local = email.substring(0, at);
    if (local.length() > 64
        || local.startsWith(".")
        || local.endsWith(".")
        || local.contains("..")) {
      return false;
    }
    for (int i = 0; i < local.length(); i++) {
      char c = local.charAt(i);
      if (!isAsciiLetter(c) && !isAsciiDigit(c) && "._%+-".indexOf(c) < 0) {
        return false;
      }
    }
    String[] labels = email.substring(at + 1).split("\\.", -1);
    if (labels.length < 2) {
      return false;
    }
    for (String label : labels) {
      if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) {
        return false;
      }
      for (int i = 0; i < label.length(); i++) {
        char c = label.charAt(i);
        if (!isAsciiLetter(c) && !isAsciiDigit(c) && c != '-') {
          return false;
        }
      }
    }
    String tld = labels[labels.length - 1];
    for (int i = 0; i < tld.length(); i++) {
      if (!isAsciiLetter(tld.charAt(i))) {
        return false;
      }
    }
    return tld.length() == 2 || GENERIC_TLDS.contains(tld.toLowerCase(Locale.ROOT));
  }

  private static boolean isAsciiLetter(char c) {
    return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
  }

  private static boolean isAsciiDigit(char c) {
    return c >= '0' && c <= '9';
  }

  /** Checks password length and the required letter and digit. */
  public static boolean password(String value) {
    return value != null
        && value.length() >= 8
        && value.length() <= 128
        && value.matches("(?s).*[A-Za-z].*")
        && value.matches("(?s).*[0-9].*");
  }

  /** Checks that password and confirmation have equal values. */
  public static boolean matches(String password, String confirmation) {
    return password != null && password.equals(confirmation);
  }
}
