package com.auction.client.model;

/** Loại giao dịch trong ví mô phỏng. */
public enum TransactionType {
    DEPOSIT("Nạp tiền"), WITHDRAW("Rút tiền"), BID_HOLD("Giữ tiền đặt giá"),
    BID_RELEASE("Hoàn tiền giữ"), PAYMENT("Thanh toán phiên thắng"), REFUND("Hoàn tiền");
    private final String label;
    TransactionType(String label) { this.label = label; }
    /** Tên tiếng Việt. */
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
