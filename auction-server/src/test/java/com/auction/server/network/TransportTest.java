package com.auction.server.network;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static com.auction.server.network.Protocol.obj;

class TransportTest {
    /** Test fixture only. This does not implement or ship any auction business operation. */
    static final class GatewayFixture implements BackendGateway {
        int count;
        Consumer<JsonObject> sink;
        @Override public String authenticatedUserId(String token) {
            if (!Set.of("one", "two").contains(token)) throw new NetworkException("UNAUTHENTICATED", "Test token invalid");
            return token;
        }
        @Override public void setEventSink(Consumer<JsonObject> callback) { sink=callback; }
        @Override public JsonObject execute(String action,String token,String command,JsonObject payload) {
            if (action.equals("INCREMENT")) {
                int before=count;
                Thread.yield();
                count=before+1;
                sink.accept(obj("type","event","event","TEST_UPDATE","auctionId","a","userId","","sequence",count));
            }
            return obj("success",true,"result",obj("count",count));
        }
    }
    static final class Peer implements AutoCloseable {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final String token;
        final List<JsonObject> events=new ArrayList<>();
        Peer(int port,String token) throws Exception {
            socket=new Socket("127.0.0.1",port);socket.setSoTimeout(5000);this.token=token;
            in=new DataInputStream(socket.getInputStream());out=new DataOutputStream(socket.getOutputStream());
        }
        JsonObject call(String action) throws Exception {
            String id=UUID.randomUUID().toString();
            Server.writeFrame(out,obj("protocolVersion",1,"requestId",id,"action",action,"token",token,"payload",new JsonObject()));
            while(true){
                JsonObject reply=Server.readFrame(in);
                if(reply.has("event"))events.add(reply);
                else if(Protocol.text(reply,"requestId").equals(id))return reply;
            }
        }
        @Override public void close() throws Exception { socket.close(); }
    }
    @Test void unconfiguredBackendNeverAcknowledgesFakeLoginOrBid() throws Exception {
        try(Server server=new Server("127.0.0.1",0,new UnconfiguredBackend())){
            server.start();
            try(Peer peer=new Peer(server.port(),"")){
                JsonObject health=peer.call("PING");
                assertTrue(health.get("success").getAsBoolean());
                assertFalse(health.getAsJsonObject("result").get("backendConfigured").getAsBoolean());
                for(String action:List.of("LOGIN","PLACE_BID")){
                    JsonObject reply=peer.call(action);
                    assertFalse(reply.get("success").getAsBoolean());
                    assertEquals("BACKEND_NOT_CONFIGURED",Protocol.text(reply,"errorCode"));
                }
            }
        }
    }
    @Test void twoClientsReceiveOrderedPushAndPrivateEventsStayPrivate() throws Exception {
        GatewayFixture gateway=new GatewayFixture();
        try(Server server=new Server("127.0.0.1",0,gateway)){
            server.start();
            try(Peer one=new Peer(server.port(),"one");Peer two=new Peer(server.port(),"two")){
                assertTrue(one.call("SUBSCRIBE_FEED").get("success").getAsBoolean());
                assertTrue(two.call("SUBSCRIBE_FEED").get("success").getAsBoolean());
                one.call("INCREMENT");two.call("PING");
                assertEquals(1,one.events.size());assertEquals(1,two.events.size());
                gateway.sink.accept(obj("type","event","event","PRIVATE_TEST","auctionId","","userId","one"));
                one.call("PING");two.call("PING");
                assertTrue(one.events.stream().anyMatch(e->Protocol.text(e,"event").equals("PRIVATE_TEST")));
                assertFalse(two.events.stream().anyMatch(e->Protocol.text(e,"event").equals("PRIVATE_TEST")));
            }
        }
    }
    @Test void concurrentSocketsCannotLoseUpdatesThroughTheDispatcher() throws Exception {
        GatewayFixture gateway=new GatewayFixture();
        try(Server server=new Server("127.0.0.1",0,gateway);ExecutorService pool=Executors.newFixedThreadPool(8)){
            server.start();List<Future<?>> tasks=new ArrayList<>();
            for(int i=0;i<40;i++)tasks.add(pool.submit(()->{
                try(Peer peer=new Peer(server.port(),"one")){assertTrue(peer.call("INCREMENT").get("success").getAsBoolean());}
                catch(Exception e){throw new RuntimeException(e);}
            }));
            for(Future<?> task:tasks)task.get(15,TimeUnit.SECONDS);
            assertEquals(40,gateway.count);
        }
    }
    @Test void malformedFrameIsRejectedBeforeAllocatingThePayload() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        new DataOutputStream(bytes).writeInt(Server.MAX_FRAME+1);
        assertThrows(IOException.class,()->Server.readFrame(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
}

