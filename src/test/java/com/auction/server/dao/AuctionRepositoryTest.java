package com.auction.server.dao;

import static com.auction.shared.JsonProtocol.obj;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuctionRepositoryTest {
  @TempDir Path directory;

  @Test
  void committedSnapshotSurvivesRestartAndIgnoresUncommittedTemporaryFile() throws Exception {
    JsonObject state = obj("schemaVersion", 1, "version", 7);
    try (var repository = new AuctionRepository(directory)) {
      assertNull(repository.load());
      repository.save(state);
    }
    byte[] committed = Files.readAllBytes(directory.resolve("state.json"));
    Files.writeString(directory.resolve("state.json.tmp"), "{interrupted write");
    try (var repository = new AuctionRepository(directory)) {
      assertEquals(state, repository.load());
      assertArrayEquals(committed, Files.readAllBytes(directory.resolve("state.json")));
    }
  }

  @Test
  void secondRepositoryCannotWriteTheSameDataDirectory() throws Exception {
    try (var first = new AuctionRepository(directory)) {
      assertThrows(IOException.class, () -> new AuctionRepository(directory));
      assertNull(first.load());
      first.save(obj("version", 1));
    }
    try (var reopened = new AuctionRepository(directory)) {
      assertEquals(1, reopened.load().get("version").getAsLong());
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{",
        "null",
        "[]",
        "",
        "{unquoted:1}",
        "{} trailing",
        "{\"version\":1,\"version\":2}",
        "{\"users\":{\"u\":{},\"u\":{}}}"
      })
  void corruptFileIsNeverResetOrOverwritten(String original) throws Exception {
    Path file = directory.resolve("state.json");
    Files.writeString(file, original);
    byte[] bytes = Files.readAllBytes(file);
    try (var repository = new AuctionRepository(directory)) {
      assertThrows(IOException.class, repository::load);
      assertThrows(IOException.class, () -> repository.save(obj("version", 0)));
      assertArrayEquals(bytes, Files.readAllBytes(file));
    }
  }

  @Test
  void writeFailurePreservesCommittedFileAndCanBeRetried() throws Exception {
    try (var repository = new AuctionRepository(directory)) {
      repository.load();
      repository.save(obj("version", 1));
      byte[] committed = Files.readAllBytes(directory.resolve("state.json"));
      Path temporary = Files.createDirectory(directory.resolve("state.json.tmp"));
      assertThrows(IOException.class, () -> repository.save(obj("version", 2)));
      assertArrayEquals(committed, Files.readAllBytes(directory.resolve("state.json")));
      Files.delete(temporary);
      repository.save(obj("version", 2));
    }
    try (var reopened = new AuctionRepository(directory)) {
      assertEquals(2, reopened.load().get("version").getAsLong());
    }
  }

  @Test
  void savingRequiresSuccessfulLoadAndAnOpenRepository() throws Exception {
    var repository = new AuctionRepository(directory);
    try (repository) {
      assertThrows(IOException.class, () -> repository.save(obj()));
      assertNull(repository.load());
      assertThrows(IOException.class, () -> repository.save(null));
      assertFalse(Files.exists(directory.resolve("state.json")));
    }
    assertThrows(IOException.class, repository::load);
    assertThrows(IOException.class, () -> repository.save(obj()));
    repository.close();
  }
}
