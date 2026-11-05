package com.auction.server.network;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Integration boundary owned by member 2. No domain implementation is supplied by
 * the networking module. One provider is loaded using the Java ServiceLoader API.
 * Implementations must validate permissions, commit/rollback business transactions,
 * deduplicate commandId per user and publish events only after durable commit.
 */
public interface BackendGateway extends AutoCloseable {
    JsonObject execute(String action, String token, String commandId, JsonObject payload);
    String authenticatedUserId(String token);
    default void setEventSink(Consumer<JsonObject> sink) { }
    default void tick() { }
    @Override default void close() throws IOException { }
}
