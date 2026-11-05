package com.auction.server.network;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A single fair lock serializes every request, scheduler tick and session lookup
 * through the gateway. Two client sockets cannot concurrently update shared
 * business state via this entry point. Financial rollback/deduplication remains
 * the responsibility of the transactional backend provider.
 */
public final class ServerRequestDispatcher implements BackendGateway {
    private final BackendGateway delegate;
    private final ReentrantLock lock=new ReentrantLock(true);
    public ServerRequestDispatcher(BackendGateway delegate) { this.delegate=java.util.Objects.requireNonNull(delegate); }
    @Override public JsonObject execute(String action,String token,String commandId,JsonObject payload) {
        lock.lock();
        try { return delegate.execute(action,token,commandId,payload); }
        finally { lock.unlock(); }
    }
    @Override public String authenticatedUserId(String token) {
        lock.lock();
        try { return delegate.authenticatedUserId(token); }
        finally { lock.unlock(); }
    }
    @Override public void tick() { lock.lock();try { delegate.tick(); }finally { lock.unlock(); } }
    @Override public void setEventSink(Consumer<JsonObject> sink) { delegate.setEventSink(sink); }
    @Override public void close() throws IOException { lock.lock();try { delegate.close(); }finally { lock.unlock(); } }
}
