package com.auction.server.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.client.service.server.ServerServices;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerStartupTest {
  @TempDir Path directory;

  @Test
  void productionConstructionIsPrivate() {
    for (var constructor : Server.class.getDeclaredConstructors()) {
      assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
    }
    assertEquals(0, Server.class.getConstructors().length);
  }

  @Test
  void defaultServerNeverSeedsEvenWhenOldDemoPropertyIsPresentAndRestartPreservesData()
      throws Exception {
    Path storage = directory.resolve("server");
    Files.createDirectories(storage);
    Files.writeString(
        storage.resolve("state.json"),
        """
        {"schemaVersion":1,"version":0,"users":{},"auctions":{},
         "rules":{},"commands":{},"transactions":[]}
        """);
    String[] keys = {"auction.bind", "auction.port", "auction.data", "auction.demo"};
    String[] before = java.util.Arrays.stream(keys).map(System::getProperty).toArray(String[]::new);
    try {
      System.setProperty(keys[0], "127.0.0.1");
      System.setProperty(keys[1], "0");
      System.setProperty(keys[2], storage.toString());
      System.setProperty(keys[3], "true");
      try (Server server = concurrentlyGetInstance()) {
        assertSame(server, Server.getInstance(), "Default lifecycle reuses the running instance");
        server.start();
        try (var client =
            new ServerServices("127.0.0.1", server.port(), directory.resolve("client"))) {
          assertTrue(client.getAuctions(null).isEmpty());
          assertTrue(
              client.register(
                  "qa_persisted",
                  "Persist123",
                  "QA",
                  "qa@example.com",
                  com.auction.client.model.UserRole.SELLER));
          assertTrue(client.login("qa_persisted", "Persist123", null));
        }
      }
      byte[] saved = Files.readAllBytes(storage.resolve("state.json"));
      try (Server server = Server.getInstance()) {
        assertArrayEquals(saved, Files.readAllBytes(storage.resolve("state.json")));
        server.start();
        try (var client =
            new ServerServices("127.0.0.1", server.port(), directory.resolve("restored"))) {
          assertTrue(client.login("qa_persisted", "Persist123", null));
          assertTrue(client.getAuctions(null).isEmpty());
        }
      }
    } finally {
      for (int i = 0; i < keys.length; i++) {
        if (before[i] == null) {
          System.clearProperty(keys[i]);
        } else {
          System.setProperty(keys[i], before[i]);
        }
      }
    }
  }

  private Server concurrentlyGetInstance() throws Exception {
    try (var callers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var start = new java.util.concurrent.CountDownLatch(1);
      var instances = new java.util.ArrayList<java.util.concurrent.Future<Server>>();
      for (int i = 0; i < 16; i++) {
        instances.add(
            callers.submit(
                () -> {
                  start.await();
                  return Server.getInstance();
                }));
      }
      start.countDown();
      Server first = instances.getFirst().get(5, java.util.concurrent.TimeUnit.SECONDS);
      for (var result : instances) {
        assertSame(first, result.get(5, java.util.concurrent.TimeUnit.SECONDS));
      }
      return first;
    }
  }
}
