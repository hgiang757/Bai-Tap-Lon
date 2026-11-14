package com.auction.server.service;

import static com.auction.server.service.AuctionContext.error;
import static com.auction.server.service.AuctionContext.requireRole;
import static com.auction.server.service.AuctionContext.required;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.exception.AuthenticationException;
import com.auction.server.model.Admin;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Credentials, sessions, registration and account administration; never persists independently. */
final class AuthLogic {
  private final AuctionContext context;
  private final WalletLogic wallets;
  private final Map<String, Session> sessions = new HashMap<>();

  private record Session(String userId, Instant expires) {}

  AuthLogic(AuctionContext context, WalletLogic wallets) {
    this.context = context;
    this.wallets = wallets;
  }

  void logout(String token) {
    sessions.remove(token);
  }

  static void validatePassword(String password) {
    if (password.length() < 8
        || password.length() > 128
        || !password.matches("(?s).*[A-Za-z].*")
        || !password.matches("(?s).*[0-9].*")) {
      throw error("VALIDATION_ERROR", "Password requires 8-128 characters, letters and digits");
    }
  }

  JsonObject authenticatedUser(String token) {
    Session s = sessions.get(token);
    if (s == null || !context.clock.instant().isBefore(s.expires())) {
      sessions.remove(token);
      throw error("UNAUTHENTICATED", "Session expired");
    }
    JsonObject u = context.collection("users").getAsJsonObject(s.userId());
    if (u == null || u.get("locked").getAsBoolean() || u.get("deleted").getAsBoolean()) {
      throw error("UNAUTHENTICATED", "Account unavailable");
    }
    return u;
  }

  JsonObject publicUser(JsonObject u) {
    JsonObject result = u.deepCopy();
    result.remove("passwordHash");
    return result;
  }

  JsonObject createUser(
      String username, String password, String fullName, String email, String role, long balance) {
    String hash = Passwords.hash(password);
    User account =
        switch (role) {
          case "ADMIN" -> new Admin(context.id(), username, hash);
          case "SELLER" -> new Seller(context.id(), username, hash);
          case "BIDDER" -> new Bidder(context.id(), username, hash);
          default -> throw error("VALIDATION_ERROR", "Unknown role");
        };
    JsonObject u =
        obj(
            "id",
            account.getId(),
            "username",
            account.getName(),
            "fullName",
            fullName,
            "email",
            email,
            "role",
            account.getRoleName(),
            "passwordHash",
            account.getPasswordHash(),
            "balance",
            balance,
            "locked",
            false,
            "deleted",
            false,
            "createdAt",
            context.now());
    context.collection("users").add(text(u, "id"), u);
    return u;
  }

  JsonObject login(JsonObject p) {
    String name = required(p, "username", 254);
    JsonObject u =
        context.values("users").stream()
            .filter(
                x ->
                    text(x, "username").equalsIgnoreCase(name)
                        || text(x, "email").equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    if (u == null
        || u.get("locked").getAsBoolean()
        || u.get("deleted").getAsBoolean()
        || !Passwords.verify(text(p, "password"), text(u, "passwordHash"))) {
      throw new AuthenticationException("Invalid credentials or account locked");
    }
    if (sessions.size() >= 10_000) {
      sessions.entrySet().removeIf(e -> !context.clock.instant().isBefore(e.getValue().expires()));
    }
    if (sessions.size() >= 10_000) {
      throw error("SERVER_BUSY", "Too many sessions");
    }
    String token = context.id() + context.id();
    Instant expiry = context.clock.instant().plus(Duration.ofHours(12));
    sessions.put(token, new Session(text(u, "id"), expiry));
    return obj("token", token, "expiresAt", expiry, "user", publicUser(u));
  }

  JsonObject register(JsonObject p) {
    String username = required(p, "username", 30);
    String email = required(p, "email", 254);
    String password = text(p, "password");
    String r = required(p, "role", 10);
    if (!Set.of("BIDDER", "SELLER").contains(r)) {
      throw error("FORBIDDEN", "Public registration cannot create admin");
    }
    if (!username.matches("[\\p{L}\\p{N}_][\\p{L}\\p{N}_ .-]{1,28}[\\p{L}\\p{N}_]")
        || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw error("VALIDATION_ERROR", "Invalid username or email");
    }
    validatePassword(password);
    if (context.values("users").stream()
        .anyMatch(
            u ->
                text(u, "username").equalsIgnoreCase(username)
                    || text(u, "email").equalsIgnoreCase(email))) {
      throw error("CONFLICT", "Username or email already exists");
    }
    return publicUser(createUser(username, password, required(p, "fullName", 120), email, r, 0));
  }

  JsonObject manageUser(JsonObject actor, JsonObject p, String action) {
    requireRole(actor, "ADMIN");
    String target = required(p, "userId", 100);
    JsonObject u = context.collection("users").getAsJsonObject(target);
    if (u == null || u.get("deleted").getAsBoolean()) {
      throw error("NOT_FOUND", "User not found");
    }
    if (target.equals(text(actor, "id"))) {
      throw error("FORBIDDEN", "Cannot modify your own admin account");
    }
    if ((action.equals("DELETE_USER") || action.equals("CHANGE_USER_ROLE"))
        && (wallets.heldBalance(target) > 0
            || context.values("auctions").stream()
                .anyMatch(
                    a ->
                        text(a, "sellerId").equals(target)
                            && Set.of("OPEN", "RUNNING").contains(text(a, "status"))))) {
      throw error("CONFLICT", "Account has active auction obligations");
    }
    switch (action) {
      case "LOCK_USER" -> u.addProperty("locked", true);
      case "UNLOCK_USER" -> u.addProperty("locked", false);
      case "DELETE_USER" -> u.addProperty("deleted", true);
      case "CHANGE_USER_ROLE" -> {
        String r = required(p, "role", 10);
        if (!Set.of("ADMIN", "BIDDER", "SELLER").contains(r)) {
          throw error("VALIDATION_ERROR", "Unknown role");
        }
        u.addProperty("role", r);
      }
      default -> throw error("UNKNOWN_ACTION", action);
    }
    if (!action.equals("UNLOCK_USER")) {
      context.values("rules").stream()
          .filter(r -> text(r, "userId").equals(target))
          .forEach(r -> r.addProperty("active", false));
    }
    context.event("USER_CHANGED", null, target);
    return publicUser(u);
  }
}
