package com.auction.client.util;

import java.text.Normalizer;

public final class Validators {
    private Validators() { }
    public static String normalizeUsername(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFC)
                .strip().replaceAll(" +", " ");
    }
    public static boolean username(String value) {
        String name = normalizeUsername(value);
        int length = name.codePointCount(0, name.length());
        return length >= 3 && length <= 30
                && name.matches("[\\p{L}\\p{N}_][\\p{L}\\p{N}_ .-]*[\\p{L}\\p{N}_]");
    }
    public static boolean email(String value) {
        if (value == null) return false;
        String email = value.trim();
        return email.length() <= 254 && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    public static boolean password(String value) {
        return value != null && value.length() >= 8 && value.matches("(?s).*[A-Za-z].*") && value.matches("(?s).*[0-9].*");
    }
    public static boolean matches(String password, String confirmation) {
        return password != null && password.equals(confirmation);
    }
}
