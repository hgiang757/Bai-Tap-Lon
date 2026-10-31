package com.auction.server.model;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String description;
    private Seller owner; // Quan hệ "Has-A" (Aggregation): Item được sở hữu bởi 1 Seller

    public Item(String id, String name, String description, Seller owner) {
        super(id, name); // Gán ID và Tên sản phẩm
        this.description = description;
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Seller getOwner() {
        return owner;
    }

    // Phương thức đa hình: Mỗi loại sản phẩm có thể có phí kiểm định/bảo hiểm khác nhau
    public abstract double calculateInsuranceFee(double currentPrice);
}



class ElectronicsItem extends Item {
    private static final long serialVersionUID = 1L;
    private int warrantyMonths; // Thuộc tính riêng: Số tháng bảo hành

    public ElectronicsItem(String id, String name, String description, Seller owner, int warrantyMonths) {
        super(id, name, description, owner);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public double calculateInsuranceFee(double currentPrice) {
        return currentPrice * 0.05; // Phí bảo hiểm 5% cho hàng điện tử
    }
}

class ArtItem extends Item {
    private static final long serialVersionUID = 1L;
    private String artist; // Thuộc tính riêng: Tên tác giả/họa sĩ

    public ArtItem(String id, String name, String description, Seller owner, String artist) {
        super(id, name, description, owner);
        this.artist = artist;
    }

    @Override
    public double calculateInsuranceFee(double currentPrice) {
        return currentPrice * 0.10; // Phí bảo quản nghệ thuật 10%
    }
}