# Client JavaFX – Digital Auction

Ứng dụng desktop đấu giá của thành viên 3: đăng nhập/đăng ký, tìm phiên, đặt giá, lịch sử/biểu đồ, ví, auto-bid và quản lý sản phẩm/người dùng.

**Mặc định dùng `auction.mode=server`: kết nối backend Socket JSON và lưu dữ liệu trên máy chủ.** Khởi động server trước theo [README gốc](../README.md). Ví vẫn dùng tiền giả lập. Chế độ mock độc lập: `mvn javafx:run "-Dauction.mode=mock"`. Không tự fallback khi mất mạng.

## Chạy ứng dụng

Cần JDK hỗ trợ release 25 và Maven; JavaFX 26.0.2 được khai báo trong [pom.xml](pom.xml). Mở terminal tại `auction-client-ui/`:

```powershell
mvn -version
mvn javafx:run
```

Tài khoản SERVER demo: `bidder_hieu / Bidder12345`, `seller_hieu / Seller12345`, `admin_hieu / Admin12345`, `bidder_vip99 / Bidder12345`. Tài khoản đăng ký mới có 0 đồng; nạp tiền trong Ví. Tài khoản MOCK cùng tên dùng mật khẩu `123456`.

## Cấu trúc và luồng xử lý

| Thư mục trong `src/main` | Vai trò |
|---|---|
| `java/com/auction/client/controller` | Xử lý sự kiện của từng màn hình |
| `java/com/auction/client/model` | Dữ liệu tài khoản, sản phẩm, phiên, giá và ví |
| `java/com/auction/client/service` | Hợp đồng nghiệp vụ và bộ dịch vụ dùng chung |
| `java/com/auction/client/service/server` | Transport JSON, ánh xạ DTO, adapter 6 dịch vụ, reconnect và commandId bền vững |
| `java/com/auction/client/service/mock` | Xử lý nghiệp vụ mô phỏng |
| `java/com/auction/client/navigation` | Chuyển trang, tác vụ nền và dọn màn hình |
| `java/com/auction/client/ui`, `util` | Thành phần giao diện và tiện ích |
| `resources/com/auction/client` | FXML, CSS và tài nguyên |

Luồng chính: thao tác trên View → Controller → Service → kết quả cập nhật View. Tác vụ nền tránh chặn giao diện; khi rời trang, controller dừng timer/listener và bỏ callback cũ. Lệnh đặt giá chưa rõ kết quả vẫn bị khóa để tránh gửi trùng; snapshot giá thông thường không tự mở khóa đó; bấm Tải lại sẽ replay mã lệnh bền vững trước khi đọc snapshot mới và mở khóa. Các thao tác ghi được gửi lại tối đa một lần với cùng commandId, không tạo thao tác thứ hai.

Trang chủ chia phiên `RUNNING` vào “Đang diễn ra”, `OPEN` vào “Sắp diễn ra”, kết hợp tìm kiếm/danh mục; phần Kết quả giữ các phiên FINISHED/PAID/CANCELED để tìm lại người thắng. Ví phân biệt tổng số dư, tiền có thể sử dụng và tiền tạm giữ cho các phiên đang dẫn đầu. Nền màu nằm trong CSS, ảnh sản phẩm vẫn được giữ.

## Kiểm tra

```powershell
mvn -B test
mvn -B test "-Dauction.ui.tests=true"
python scripts/check-encoding.py
python scripts/check-docs.py
python scripts/check-contrast.py
```

Test JavaFX cần desktop. Chụp màn hình mẫu bằng `mvn javafx:run "-Dauction.mode=mock" "-Dmain.class=com.auction.client.dev.SnapshotApp"`.

## Tài liệu

- [Bản đồ file](docs/CODE_MAP.md): chức năng từng file.
- [Kết quả kiểm tra](docs/VALIDATION.md): kết quả đã ghi nhận và giới hạn.
- [Bàn giao backend](docs/CLIENT_HANDOVER.md): lịch sử bàn giao trước tích hợp; hợp đồng hiện hành ở [PROTOCOL](../auction-server/PROTOCOL.md).
- [Thiết kế](docs/ui/DESIGN.md), [đợt sửa giao diện](docs/UI_REFRESH.md).
