# Tài khoản chạy thử

## SERVER (mặc định)

Khởi động backend với `-Dauction.demo=true`; tài khoản chỉ seed khi dữ liệu còn trống.

| Vai trò | Username | Password |
|---|---|---|
| Bidder | `bidder_hieu` | `Bidder12345` |
| Bidder 2 | `bidder_vip99` | `Bidder12345` |
| Seller | `seller_hieu` | `Seller12345` |
| Admin | `admin_hieu` | `Admin12345` |

Tài khoản đăng ký mới có 0 đồng; dùng Ví để nạp tiền giả lập. Dữ liệu được lưu trên máy chủ. Phiên mẫu hết giờ thì Seller tạo phiên mới. Không bật demo trên dữ liệu quan trọng chỉ để reset tài khoản; demo không ghi đè dữ liệu đã tồn tại.

## MOCK (chạy độc lập)


These accounts are available only when the client runs with `MockAuctionClientService`.
They are not production credentials and are not valid for a real auction service.

| Role | Username | Password |
|---|---|---|
| Bidder | `bidder_hieu` | `123456` |
| Seller | `seller_hieu` | `123456` |
| Admin | `admin_hieu` | `123456` |

New registrations, products and transactions exist only in memory until the application exits.
