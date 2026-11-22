package com.auction.client.service.server;

import static com.auction.shared.JsonProtocol.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.shared.JsonProtocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class JsonTransportTest {
  @Test
  void disconnectDuringRequestReportsConnectionFailureAndNextCallReconnects() throws Exception {
    try (var listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        var worker = Executors.newVirtualThreadPerTaskExecutor();
        var client = new JsonTransport("127.0.0.1", listener.getLocalPort(), 2_000, event -> {})) {
      listener.setSoTimeout(3_000);
      final var responses =
          worker.submit(
              () -> {
                try (var first = listener.accept()) {
                  JsonProtocol.readFrame(new DataInputStream(first.getInputStream()));
                }
                try (var second = listener.accept()) {
                  var request =
                      JsonProtocol.readFrame(new DataInputStream(second.getInputStream()));
                  JsonProtocol.writeFrame(
                      new DataOutputStream(second.getOutputStream()),
                      obj(
                          "type",
                          "response",
                          "requestId",
                          request.get("requestId"),
                          "success",
                          true,
                          "result",
                          obj("status", "OK")));
                }
                return null;
              });
      IllegalStateException error =
          assertThrows(IllegalStateException.class, () -> client.call("PING", "", "", obj()));
      assertTrue(error.getMessage().contains("Không thể liên lạc máy chủ"));
      assertInstanceOf(EOFException.class, error.getCause().getCause());
      assertEquals(
          "OK",
          client.call("PING", "", "", obj()).getAsJsonObject("result").get("status").getAsString());
      responses.get(3, TimeUnit.SECONDS);
    }
  }

  @Test
  void incompleteResponseIsReportedAsProtocolFailure() throws Exception {
    try (var listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        var worker = Executors.newVirtualThreadPerTaskExecutor();
        var client = new JsonTransport("127.0.0.1", listener.getLocalPort(), 2_000, event -> {})) {
      listener.setSoTimeout(3_000);
      var response =
          worker.submit(
              () -> {
                try (var peer = listener.accept()) {
                  var request = JsonProtocol.readFrame(new DataInputStream(peer.getInputStream()));
                  JsonProtocol.writeFrame(
                      new DataOutputStream(peer.getOutputStream()),
                      obj(
                          "type",
                          "response",
                          "requestId",
                          request.get("requestId"),
                          "success",
                          true));
                }
                return null;
              });
      IllegalStateException error =
          assertThrows(IllegalStateException.class, () -> client.call("PING", "", "", obj()));
      assertEquals("Phản hồi của máy chủ không đúng định dạng.", error.getMessage());
      assertInstanceOf(IOException.class, error.getCause().getCause());
      response.get(3, TimeUnit.SECONDS);
    }
  }

  @Test
  void connectedServerWithoutReplyTimesOutAndClosesTheSocket() throws Exception {
    try (var listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        var worker = Executors.newVirtualThreadPerTaskExecutor();
        var client = new JsonTransport("127.0.0.1", listener.getLocalPort(), 500, event -> {})) {
      listener.setSoTimeout(3_000);
      var eof =
          worker.submit(
              () -> {
                try (var peer = listener.accept()) {
                  peer.setSoTimeout(3_000);
                  var input = new DataInputStream(peer.getInputStream());
                  JsonProtocol.readFrame(input);
                  return input.read();
                }
              });
      IllegalStateException error =
          assertThrows(IllegalStateException.class, () -> client.call("PING", "", "", obj()));
      assertEquals("Máy chủ phản hồi quá thời gian.", error.getMessage());
      assertEquals(-1, eof.get(3, TimeUnit.SECONDS));
    }
  }
}
