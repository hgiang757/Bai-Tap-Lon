package com.auction.client.service.mock;

import com.auction.client.model.*;
import com.auction.client.service.*;
import com.auction.client.util.FormatUtil;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

/** Dịch vụ đặt giá tự động mô phỏng. */
public final class MockAutoBidService implements AutoBidService {
    private final MockDataStore store;
    private final Supplier<AuctionClientService> client;
    private final AutoBidEngine engine;
    /** Tạo dịch vụ trên kho và phiên đăng nhập được chỉ định. */
    public MockAutoBidService(MockDataStore store, Supplier<AuctionClientService> client) {
        this.store=store; this.client=client; this.engine=new AutoBidEngine(store);
    }
    private String username() {
        User user=client.get().getCurrentUser();
        if (user == null) throw new AutoBidException("Vui lòng đăng nhập để đặt giá.");
        return user.getUsername();
    }
    @Override public AutoBid registerAutoBid(String id, long maxBid, long increment) { synchronized (store) {
        String user=username(); store.refreshStatuses();
        Auction auction=store.auctions.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        if (auction == null || auction.getStatus()!=AuctionStatus.RUNNING) throw new AutoBidException("Phiên đã kết thúc.");
        if (user.equalsIgnoreCase(auction.getSellerUsername())) throw new AutoBidException("Bạn không thể đặt giá cho sản phẩm của mình.");
        if (maxBid <= 0 || maxBid < auction.getCurrentPrice()+auction.getMinBidStep()) throw new AutoBidException("Giá tối đa phải lớn hơn giá hiện tại ít nhất một bước giá.");
        if (increment <= 0 || increment < auction.getMinBidStep()) throw new AutoBidException("Bước giá phải từ " + FormatUtil.formatCurrency(auction.getMinBidStep()).replace(" ₫", "") + " ₫ trở lên.");
        MockDataStore.Hold hold=store.holds.get(id);
        long own=hold != null && hold.username().equals(user) ? hold.amount() : 0;
        if (store.balance(user)-store.held(user)+own < maxBid) throw new AutoBidException("Số dư khả dụng không đủ cho giá tối đa.");
        var queue=store.autoQueue(id);
        queue.removeIf(a -> a.getUsername().equalsIgnoreCase(user));
        AutoBid rule=new AutoBid(UUID.randomUUID().toString(), id, user, maxBid, increment, store.nextAutoRegistration());
        queue.add(rule); engine.process(auction); return rule.copy();
    } }
    @Override public void cancelAutoBid(String id) { synchronized (store) {
        String user=username(); for (AutoBid a : store.autoQueue(id)) if (a.getUsername().equalsIgnoreCase(user)) a.deactivate();
    } }
    @Override public AutoBid getMyAutoBid(String id) { synchronized (store) {
        String user=username(); return store.autoQueue(id).stream().filter(a -> a.getUsername().equalsIgnoreCase(user)).findFirst().map(AutoBid::copy).orElse(null);
    } }
}
