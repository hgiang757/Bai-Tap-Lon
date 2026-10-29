package com.auction.client.service;

import com.auction.client.model.Auction;
import java.util.List;

/** Hợp đồng quản trị các phiên đấu giá. */
public interface AuctionAdminService {
    /** Lấy toàn bộ phiên. */
    List<Auction> listAuctions();
    /** Hủy phiên với lý do bắt buộc. */
    void cancelAuction(String id, String reason);
    /** Xóa phiên không hợp lệ. */
    void deleteAuction(String id);
}
