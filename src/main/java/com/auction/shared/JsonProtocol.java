package com.auction.shared;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Shared JSON values and envelope fields used by both client and server. */
public final class JsonProtocol {
    public static final int VERSION = 1;
    public static final int MAX_FRAME = 8 * 1024 * 1024;

    private JsonProtocol() {}

    /** TCP là luồng byte: đọc độ dài trước, rồi đọc đủ một thông điệp JSON. */
    public static JsonObject readFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_FRAME) throw new IOException("Invalid frame size");
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException();
        try {
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Malformed JSON object", e);
        }
    }

    public static void writeFrame(DataOutputStream out, JsonObject value) throws IOException {
        byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FRAME) throw new IOException("Message exceeds maximum frame size");
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    public static JsonObject obj(Object... pairs) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < pairs.length; i += 2) {
            Object v = pairs[i + 1];
            String k = pairs[i].toString();
            if (v == null) result.add(k, JsonNull.INSTANCE);
            else if (v instanceof JsonElement j) result.add(k, j);
            else if (v instanceof Number n) result.addProperty(k, n);
            else if (v instanceof Boolean b) result.addProperty(k, b);
            else if (v instanceof Enum<?> e) result.addProperty(k, e.name());
            else result.addProperty(k, v.toString());
        }
        return result;
    }

    public static String text(JsonObject o, String key) {
        return !o.has(key) || o.get(key).isJsonNull() ? "" : o.get(key).getAsString();
    }

    public static long number(JsonObject o, String key) {
        try {
            return o.get(key).getAsBigDecimal().longValueExact();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Expected integer: " + key, e);
        }
    }
}
