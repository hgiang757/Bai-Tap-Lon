package com.auction.server.service;

import static com.auction.server.service.AuctionContext.*;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionJsonMapper;
import com.auction.server.model.*;
import com.google.gson.*;

import java.io.IOException;
import java.time.*;
import java.util.*;

/** Seller product lifecycle, validation and images; preserves the existing auction schema. */
final class ProductLogic {
    private final AuctionContext context;

    ProductLogic(AuctionContext context) {
        this.context = context;
    }

    JsonArray listAuctions(JsonObject p, String sellerId) {
        JsonArray list = new JsonArray();
        String status = text(p, "status"),
                query = text(p, "search").toLowerCase(Locale.ROOT),
                category = text(p, "category");
        for (JsonObject a : context.values("auctions"))
            if (!a.get("deleted").getAsBoolean()
                    && (sellerId == null || sellerId.equals(text(a, "sellerId")))
                    && (status.isEmpty() || status.equals(text(a, "status")))
                    && (category.isEmpty() || category.equals(text(a, "category")))
                    && (text(a, "name") + " " + text(a, "sellerUsername"))
                            .toLowerCase(Locale.ROOT)
                            .contains(query)) list.add(context.snapshot(a));
        return list;
    }

    void owner(JsonObject user, JsonObject a) {
        requireRole(user, "SELLER");
        if (!text(user, "id").equals(text(a, "sellerId")))
            throw error("FORBIDDEN", "Not your product");
    }

    void editable(JsonObject a) {
        AuctionJsonMapper.auction(a).requireEditable();
    }

    // Sản phẩm và phiên: kiểm tra danh mục, giá, thời gian, ảnh và quyền sở hữu.
    private JsonObject validateProduct(JsonObject p) {
        String category = required(p, "category", 30);
        if (!Set.of("ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER")
                .contains(category)) throw error("VALIDATION_ERROR", "Unknown category");
        LocalDateTime start, end;
        try {
            start = LocalDateTime.parse(required(p, "startTime", 40));
            end = LocalDateTime.parse(required(p, "endTime", 40));
        } catch (java.time.DateTimeException e) {
            throw error("VALIDATION_ERROR", "Use ISO local time in Asia/Ho_Chi_Minh");
        }
        if (start.isBefore(context.now().minusSeconds(2))
                || !end.isAfter(start)
                || Duration.between(start, end).compareTo(Duration.ofDays(365)) > 0)
            throw error("VALIDATION_ERROR", "Invalid start/end time (maximum duration 365 days)");
        return obj(
                "name",
                required(p, "name", 200),
                "description",
                required(p, "description", 10_000),
                "category",
                category,
                "startingPrice",
                money(p, "startingPrice"),
                "minBidStep",
                money(p, "minBidStep"),
                "startTime",
                start,
                "endTime",
                end);
    }

    private void image(JsonObject a, JsonObject p) {
        if (!p.has("imageBase64")) return;
        String encoded = text(p, "imageBase64");
        if (!encoded.isEmpty()) {
            byte[] data;
            try {
                data = Base64.getDecoder().decode(encoded);
            } catch (IllegalArgumentException e) {
                throw error("VALIDATION_ERROR", "Invalid image encoding");
            }
            if (data.length >= 5 * 1024 * 1024)
                throw error("VALIDATION_ERROR", "Image must be smaller than 5 MB");
            try (var in =
                    javax.imageio.ImageIO.createImageInputStream(
                            new java.io.ByteArrayInputStream(data))) {
                var readers = javax.imageio.ImageIO.getImageReaders(in);
                if (!readers.hasNext()) throw error("VALIDATION_ERROR", "Unsupported image");
                var reader = readers.next();
                try {
                    reader.setInput(in);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000)
                        throw error("VALIDATION_ERROR", "Image dimensions too large");
                    a.addProperty("imageType", reader.getFormatName());
                } finally {
                    reader.dispose();
                }
            } catch (IOException e) {
                throw error("VALIDATION_ERROR", "Invalid image");
            }
        }
        a.addProperty("imageBase64", encoded);
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
        a.addProperty(
                "status",
                LocalDateTime.parse(text(a, "startTime")).isAfter(context.now()) ? "OPEN" : "RUNNING");
        a.addProperty("highestBidderId", "");
        a.addProperty("highestBidderUsername", "");
        a.addProperty("totalBids", 0);
        a.addProperty("deleted", false);
        a.addProperty("settled", false);
        a.addProperty("imageBase64", "");
        a.addProperty("imageType", "");
        a.add("bids", new JsonArray());
        image(a, p);
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
        image(a, p);
        context.event("AUCTION_UPDATED", a, "");
        return context.snapshot(a);
    }
}
