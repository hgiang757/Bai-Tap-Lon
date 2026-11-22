package com.auction.shared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

/** Shared JSON values and envelope fields used by both client and server. */
public final class JsonProtocol {
  public static final int VERSION = 1;
  public static final int MAX_FRAME = 8 * 1024 * 1024;
  private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

  private JsonProtocol() {}

  /** TCP là luồng byte: đọc độ dài trước, rồi đọc đủ một thông điệp JSON. */
  public static JsonObject readFrame(DataInputStream in) throws IOException {
    int length = in.readInt();
    if (length <= 0 || length > MAX_FRAME) {
      throw new ProtocolException("Invalid frame size");
    }
    byte[] bytes = in.readNBytes(length);
    if (bytes.length != length) {
      throw new EOFException();
    }
    try {
      return parseObject(
          StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString());
    } catch (CharacterCodingException | JsonParseException e) {
      ProtocolException error = new ProtocolException("Malformed JSON object");
      error.initCause(e);
      throw error;
    }
  }

  /** Parses strict JSON and requires a top-level object. */
  public static JsonObject parseObject(String json) {
    JsonObject object = JSON.fromJson(json, JsonObject.class);
    if (object == null) {
      throw new JsonParseException("Expected a JSON object");
    }
    return object;
  }

  /** Writes and flushes a length-prefixed UTF-8 JSON frame. */
  public static void writeFrame(DataOutputStream out, JsonObject value) throws IOException {
    byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_FRAME) {
      throw new IOException("Message exceeds maximum frame size");
    }
    out.writeInt(bytes.length);
    out.write(bytes);
    out.flush();
  }

  /** Builds a JSON object from alternating keys and supported values. */
  public static JsonObject obj(Object... pairs) {
    JsonObject result = new JsonObject();
    for (int i = 0; i < pairs.length; i += 2) {
      Object v = pairs[i + 1];
      String k = pairs[i].toString();
      if (v == null) {
        result.add(k, JsonNull.INSTANCE);
      } else if (v instanceof JsonElement j) {
        result.add(k, j);
      } else if (v instanceof Number n) {
        result.addProperty(k, n);
      } else if (v instanceof Boolean b) {
        result.addProperty(k, b);
      } else if (v instanceof Enum<?> e) {
        result.addProperty(k, e.name());
      } else {
        result.addProperty(k, v.toString());
      }
    }
    return result;
  }

  /** Reads a text field, returning an empty string for absent or null values. */
  public static String text(JsonObject o, String key) {
    return !o.has(key) || o.get(key).isJsonNull() ? "" : o.get(key).getAsString();
  }

  /** Checks that a JSON field is a plain string, without coercing other values. */
  public static boolean isString(JsonElement value) {
    return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
  }

  /** Reads an exact integer and rejects fractional or out-of-range values. */
  public static long number(JsonObject o, String key) {
    JsonElement value = o.get(key);
    if (value == null || !value.isJsonPrimitive()) {
      throw new IllegalArgumentException("Expected integer: " + key);
    }
    try {
      return value.getAsBigDecimal().longValueExact();
    } catch (NumberFormatException | ArithmeticException e) {
      throw new IllegalArgumentException("Expected integer: " + key, e);
    }
  }
}
