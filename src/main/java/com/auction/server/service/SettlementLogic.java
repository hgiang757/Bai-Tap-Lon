package com.auction.server.service;

import static com.auction.server.service.AuctionContext.*;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionJsonMapper;
import com.auction.server.model.Auction;
import com.google.gson.JsonObject;

/** Server-time transitions, one-time settlement and administrative cancellation. */
final class SettlementLogic {
    private final AuctionContext context;
    private final WalletLogic wallets;

    SettlementLogic(AuctionContext context, WalletLogic wallets) {
        this.context = context;
        this.wallets = wallets;
    }

    // Đồng hồ server quyết định mở/kết thúc; hết giờ thanh toán một lần.
    void refreshAuctions() {
        for (JsonObject a : context.values("auctions")) {
            Auction auction = AuctionJsonMapper.auction(a);
            if (a.get("deleted").getAsBoolean() || !auction.status().isActive())
                continue;
            if (auction.hasExpired(context.now())) {
                settle(a);
                continue;
            }
            if (auction.shouldStart(context.now())) {
                a.addProperty("status", "RUNNING");
                context.event("AUCTION_STARTED", a, "");
            }
        }
    }

    private void stopRules(JsonObject a) {
        context.values("rules").stream()
                .filter(r -> text(r, "auctionId").equals(text(a, "id")))
                .forEach(r -> r.addProperty("active", false));
    }

    private void settle(JsonObject a) {
        a.addProperty("status", "FINISHED");
        stopRules(a);
        Auction auction = AuctionJsonMapper.auction(a);
        String winner = auction.highestBidderId();
        if (!winner.isEmpty() && !auction.settled()) {
            JsonObject buyer = context.collection("users").getAsJsonObject(winner),
                    seller = context.collection("users").getAsJsonObject(text(a, "sellerId"));
            long amount = auction.currentPrice();
            buyer.addProperty("balance", number(buyer, "balance") - amount);
            seller.addProperty("balance", Math.addExact(number(seller, "balance"), amount));
            a.addProperty("status", "PAID");
            a.addProperty("paidAt", context.now().toString());
            wallets.log(buyer, "PAYMENT", amount, text(a, "id"));
            wallets.log(seller, "SALE", amount, text(a, "id"));
        }
        a.addProperty("settled", true);
        context.event("AUCTION_FINISHED", a, "");
    }

    JsonObject cancel(JsonObject user, JsonObject p) {
        requireRole(user, "ADMIN");
        JsonObject a = context.auction(p);
        String reason = required(p, "reason", 1000);
        AuctionJsonMapper.auction(a).requireCancelable();
        a.addProperty("status", "CANCELED");
        a.addProperty("cancellationReason", reason);
        a.addProperty("settled", true);
        stopRules(a);
        if (!text(a, "highestBidderId").isEmpty())
            wallets.log(
                    context.collection("users").getAsJsonObject(text(a, "highestBidderId")),
                    "BID_RELEASE",
                    number(a, "currentPrice"),
                    text(a, "id"));
        context.event("AUCTION_CANCELED", a, "");
        return context.snapshot(a);
    }
}
