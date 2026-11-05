package com.auction.client.model;

/** Loại giao dịch trong ví mô phỏng. */
public enum TransactionType {
    DEPOSIT("Nạp tiền"), WITHDRAW("Rút tiền"), BID_HOLD("Tạm giữ cho đấu giá"),
    BID_RELEASE("Giải phóng tiền tạm giữ"), PAYMENT("Thanh toán phiên thắng"), REFUND("Hoàn tiền"), SALE("Thu tiền bán sản phẩm");
    private final String label;
    TransactionType(String label) { this.label = label; }
    /** Tên tiếng Việt. */
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
