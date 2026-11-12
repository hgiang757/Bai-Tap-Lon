package com.auction.server.network;

import static com.auction.shared.JsonProtocol.obj;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

class ServerIntegrationTest {
    @TempDir Path directory;

    static class Peer implements AutoCloseable {
        Socket socket;
        DataInputStream in;
        DataOutputStream out;
        String token = "";
        List<JsonObject> events = new ArrayList<>();

        Peer(int port) throws Exception {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(5000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }

        JsonObject call(String action, JsonObject payload, String command) throws Exception {
            String id = UUID.randomUUID().toString();
            Server.writeFrame(
                    out,
                    obj(
                            "protocolVersion",
                            1,
                            "requestId",
                            id,
                            "action",
                            action,
                            "token",
                            token,
                            "commandId",
                            command,
                            "payload",
                            payload));
            while (true) {
                JsonObject r = Server.readFrame(in);
                if (r.has("event")) events.add(r);
                else if (r.get("requestId").getAsString().equals(id)) {
                    assertTrue(r.get("success").getAsBoolean(), r.toString());
                    return r;
                }
            }
        }

        JsonElement call(String action, JsonObject payload) throws Exception {
            return call(action, payload, UUID.randomUUID().toString()).get("result");
        }

        void login(String username, String password) throws Exception {
            token =
                    call("LOGIN", obj("username", username, "password", password))
                            .getAsJsonObject()
                            .get("token")
                            .getAsString();
        }

        public void close() throws Exception {
            socket.close();
        }
    }

    @Test
    void twoRealSocketsReceivePushAndReconnectReplaysOnlyOneBid() throws Exception {
        try (Server server = new Server("127.0.0.1", 0, directory, true)) {
            server.start();
            try (Peer one = new Peer(server.port());
                    Peer two = new Peer(server.port())) {
                one.login("bidder_hieu", "Bidder12345");
                two.login("bidder_vip99", "Bidder12345");
                String id =
                        one.call("LIST_AUCTIONS", obj())
                                .getAsJsonArray()
                                .get(0)
                                .getAsJsonObject()
                                .get("id")
                                .getAsString();
                two.call("SUBSCRIBE_AUCTION", obj("auctionId", id));
                String command = UUID.randomUUID().toString();
                JsonObject payload = obj("auctionId", id, "amount", 2_000_000);
                one.call("PLACE_BID", payload, command);
                JsonObject event = Server.readFrame(two.in);
                assertEquals("BID_PLACED", event.get("event").getAsString());
                assertFalse(event.toString().contains("passwordHash"));
                try (Peer reconnect = new Peer(server.port())) {
                    reconnect.token = one.token;
                    reconnect.call("PLACE_BID", payload, command);
                    assertEquals(
                            1,
                            reconnect
                                    .call("GET_BID_HISTORY", obj("auctionId", id))
                                    .getAsJsonArray()
                                    .size());
                }
                two.call("UNSUBSCRIBE_AUCTION", obj("auctionId", id));
            }
        }
    }

    @Test
    void simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer() throws Exception {
        try (Server server = new Server("0.0.0.0", 0, directory, true)) {
            server.start();
            try (Peer one = new Peer(server.port()); Peer two = new Peer(server.port())) {
                one.login("bidder_hieu", "Bidder12345");
                two.login("bidder_vip99", "Bidder12345");
                String auction = one.call("LIST_AUCTIONS", obj()).getAsJsonArray()
                        .get(0).getAsJsonObject().get("id").getAsString();
                try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                    CountDownLatch start = new CountDownLatch(1);
                    List<Future<Boolean>> results = new ArrayList<>();
                    for (Peer peer : List.of(one, two)) results.add(pool.submit(() -> {
                        start.await();
                        try {
                            peer.call("PLACE_BID", obj("auctionId", auction, "amount", 2_000_000));
                            return true;
                        } catch (AssertionError rejected) { return false; }
                    }));
                    start.countDown();
                    int accepted = 0;
                    for (Future<Boolean> result : results) if (result.get(10, TimeUnit.SECONDS)) accepted++;
                    assertEquals(1, accepted);
                }
                assertEquals(1, two.call("GET_BID_HISTORY", obj("auctionId", auction)).getAsJsonArray().size());
                try (Peer malformed = new Peer(server.port())) {
                    malformed.out.writeInt(Server.MAX_FRAME + 1);
                    malformed.out.flush();
                    assertEquals(-1, malformed.in.read());
                }
                one.close();
                assertEquals(1, two.call("GET_BID_HISTORY", obj("auctionId", auction)).getAsJsonArray().size());
            }
        }
    }

    @Test
    void oversizedFrameIsRejectedBeforeAllocation() throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        new DataOutputStream(data).writeInt(Server.MAX_FRAME + 1);
        assertThrows(
                IOException.class,
                () ->
                        Server.readFrame(
                                new DataInputStream(new ByteArrayInputStream(data.toByteArray()))));
    }
}
