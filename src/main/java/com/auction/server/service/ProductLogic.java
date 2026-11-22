package com.auction.server.service;

import static com.auction.server.service.AuctionContext.error;
import static com.auction.server.service.AuctionContext.money;
import static com.auction.server.service.AuctionContext.number;
import static com.auction.server.service.AuctionContext.requireRole;
import static com.auction.server.service.AuctionContext.required;
import static com.auction.shared.JsonProtocol.isString;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionJsonMapper;
import com.auction.server.exception.ApiException;
import com.auction.server.model.ItemFactory;
import com.auction.server.model.Seller;
import com.auction.shared.ProductImage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/** Seller product lifecycle, validation and images; preserves the existing auction schema. */
final class ProductLogic {
  private static final Set<String> CATEGORIES =
      Set.of("ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER");
  private final AuctionContext context;

  ProductLogic(AuctionContext context) {
    this.context = context;
  }

  JsonArray listAuctions(JsonObject p, String sellerId) {
    JsonArray list = new JsonArray();
    String status = text(p, "status");
    String query = text(p, "search").toLowerCase(Locale.ROOT);
    String category = text(p, "category");
    for (JsonObject a : context.values("auctions")) {
      if (!a.get("deleted").getAsBoolean()
          && (sellerId == null || sellerId.equals(text(a, "sellerId")))
          && (status.isEmpty() || status.equals(text(a, "status")))
          && (category.isEmpty() || category.equals(text(a, "category")))
          && (text(a, "name") + " " + text(a, "sellerUsername"))
              .toLowerCase(Locale.ROOT)
              .contains(query)) {
        list.add(context.snapshot(a));
      }
    }
    return list;
  }

  void owner(JsonObject user, JsonObject a) {
    requireRole(user, "SELLER");
    if (!text(user, "id").equals(text(a, "sellerId"))) {
      throw error("FORBIDDEN", "Not your product");
    }
  }

  void editable(JsonObject a) {
    AuctionJsonMapper.auction(a).requireEditable();
  }

  // Sản phẩm và phiên: kiểm tra danh mục, giá, thời gian, ảnh và quyền sở hữu.
  private JsonObject validateProduct(JsonObject p) {
    String category = required(p, "category", 30);
    if (!CATEGORIES.contains(category)) {
      throw error("VALIDATION_ERROR", "Unknown category");
    }
    JsonObject fields =
        obj(
            "name",
            required(p, "name", 200),
            "description",
            required(p, "description", 10_000),
            "category",
            category,
            "startingPrice",
            money(p, "startingPrice"),
            "minBidStep",
            money(p, "minBidStep"));
    boolean immediate = startNow(p);
    LocalDateTime end = dateTime(p, "endTime");
    LocalDateTime scheduled = immediate ? null : dateTime(p, "startTime");
    validateImage(fields, p);

    LocalDateTime now = context.now();
    LocalDateTime start = immediate ? now : scheduled;
    if (!end.isAfter(start)) {
      throw error("VALIDATION_ERROR", "End must be after start");
    }
    if (!immediate && start.isBefore(now)) {
      throw error("VALIDATION_ERROR", "Start time has passed");
    }
    if (Duration.between(start, end).compareTo(Duration.ofDays(365)) > 0) {
      throw error("VALIDATION_ERROR", "Auction duration exceeds 365 days");
    }
    fields.addProperty("startTime", start.toString());
    fields.addProperty("endTime", end.toString());
    fields.addProperty("status", start.isAfter(now) ? "OPEN" : "RUNNING");
    return fields;
  }

  private static LocalDateTime dateTime(JsonObject p, String field) {
    try {
      LocalDateTime value = LocalDateTime.parse(required(p, field, 40));
      if (value.getYear() < 1 || value.getYear() > 9999) {
        throw error("VALIDATION_ERROR", "Invalid " + field);
      }
      return value;
    } catch (DateTimeParseException e) {
      throw new ApiException("VALIDATION_ERROR", "Invalid " + field, e);
    }
  }

  private static boolean startNow(JsonObject p) {
    var value = p.get("startNow");
    if (value == null) {
      return false;
    }
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
      throw error("VALIDATION_ERROR", "Invalid startNow");
    }
    return value.getAsBoolean();
  }

  private static void validateImage(JsonObject fields, JsonObject p) {
    var value = p.get("imageBase64");
    if (value == null) {
      return;
    }
    if (!isString(value)) {
      throw error("VALIDATION_ERROR", "Invalid image encoding");
    }
    String encoded = value.getAsString();
    String format = "";
    if (encoded.length() > (ProductImage.MAX_BYTES + 2) / 3 * 4) {
      throw error("VALIDATION_ERROR", "Image must be smaller than 5 MB");
    }
    if (!encoded.isEmpty()) {
      try {
        format = ProductImage.format(Base64.getDecoder().decode(encoded));
      } catch (IllegalArgumentException e) {
        throw new ApiException("VALIDATION_ERROR", "Invalid image encoding", e);
      } catch (IOException e) {
        throw new ApiException("VALIDATION_ERROR", "Invalid image", e);
      }
    }
    fields.addProperty("imageBase64", encoded);
    fields.addProperty("imageType", format);
  }

  JsonObject createAuction(JsonObject user, JsonObject p) {
    requireRole(user, "SELLER");
    JsonObject a = validateProduct(p);
    String key = context.id();
    var item =
        ItemFactory.forCategory(text(a, "category"))
            .create(
                context.id(),
                text(a, "name"),
                text(a, "description"),
                new Seller(text(user, "id"), text(user, "username"), ""));
    a.addProperty("category", item.getCategory());
    a.addProperty("id", key);
    a.addProperty("itemId", item.getId());
    a.addProperty("sellerId", text(user, "id"));
    a.addProperty("sellerUsername", text(user, "username"));
    a.addProperty("currentPrice", number(a, "startingPrice"));
    a.addProperty("highestBidderId", "");
    a.addProperty("highestBidderUsername", "");
    a.addProperty("totalBids", 0);
    a.addProperty("deleted", false);
    a.addProperty("settled", false);
    if (!a.has("imageBase64")) {
      a.addProperty("imageBase64", "");
      a.addProperty("imageType", "");
    }
    a.add("bids", new JsonArray());
    context.collection("auctions").add(key, a);
    context.event("AUCTION_CREATED", a, "");
    return context.snapshot(a);
  }

  JsonObject updateAuction(JsonObject user, JsonObject p) {
    JsonObject a = context.auction(p);
    owner(user, a);
    editable(a);
    JsonObject fields = validateProduct(p);
    fields.entrySet().forEach(e -> a.add(e.getKey(), e.getValue()));
    a.addProperty("currentPrice", number(a, "startingPrice"));
    context.event("AUCTION_UPDATED", a, "");
    return context.snapshot(a);
  }
}
