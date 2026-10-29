package com.auction.client.service;

import com.auction.client.model.Auction;
import com.auction.client.model.Bid;
import java.util.List;

/** Hợp đồng quản lý sản phẩm của người bán. */
public interface ProductService {
    /** Lấy phiên của người bán. */
    List<Auction> listProducts(String seller);
    /** Lưu sản phẩm và phiên tương ứng. */
    void saveProduct(Auction auction, String imagePath);
    /** Xóa sản phẩm đủ điều kiện. */
    void deleteProduct(String seller, String auctionId);
    /** Lấy đường dẫn ảnh của phiên. */
    String getImagePath(String auctionId);
    /** Lấy lịch sử đặt giá của phiên. */
    List<Bid> getBidHistory(String auctionId);
}
