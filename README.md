# Hệ thống đấu giá trực tuyến

> Ứng dụng đấu giá nhiều người dùng: client JavaFX gửi yêu cầu qua TCP, server kiểm tra nghiệp vụ và lưu dữ liệu tập trung bằng JSON.

## Chức năng chính

- **Bidder:** tham gia phiên, đặt giá, theo dõi lịch sử và quản lý ví.
- **Seller:** đăng sản phẩm và quản lý phiên đấu giá của mình.
- **Admin:** quản lý tài khoản và phiên.
- **Hệ thống:** cập nhật giá realtime; hỗ trợ auto-bid, gia hạn chống trả giá cuối giờ và chốt người thắng.

Ví dùng tiền mô phỏng. Dữ liệu được server lưu tại `data/state.json`.

## Công nghệ và kiến trúc

- Java 25, JavaFX 26.0.2, Maven.
- Client và server là hai tiến trình riêng, giao tiếp bằng TCP với thông điệp JSON.
- Server là nơi kiểm tra luật đấu giá và ví; client không ghi trực tiếp dữ liệu server.
- Không dùng MySQL, Spring hay REST.

```text
FXML/CSS → Controller → Client Service → TCP/JSON
                                      ↓
JSON file ← Repository ← AuctionService ← Server
```

Ở phía server, `AuctionService` điều phối nghiệp vụ; các lớp `*Logic` xử lý từng nhóm như đăng nhập, sản phẩm, đặt giá, ví và thanh toán. `AuctionRepository` đọc/ghi snapshot JSON. Khi một yêu cầu làm thay đổi dữ liệu, server lưu trước rồi mới phát sự kiện cập nhật cho các client.

## Chạy trên Windows

Cài JDK 25 và Maven, sau đó mở hai terminal tại thư mục dự án:

```powershell
.\Start-Server.cmd
```

Terminal thứ hai:

```powershell
.\Start-Client.cmd
```

Client hỏi địa chỉ server; nhấn Enter nếu chạy cùng máy. Nếu chạy trên LAN, nhập địa chỉ IPv4 của máy server. Giữ terminal server mở trong lúc sử dụng. Lần chạy đầu Maven cần Internet để tải thư viện.

Server mới cần mật khẩu admin khởi tạo qua biến môi trường `AUCTION_ADMIN_PASSWORD`. Hãy xem [hướng dẫn chạy nhiều máy](MULTI_CLIENT_SETUP.md) để cấu hình LAN và tài khoản quản trị. Không xóa hoặc chỉnh `data/state.json` khi server đang chạy.

## Điểm bắt đầu khi vấn đáp

Muốn trình bày luồng đặt giá, lần lượt mở:

1. `AuctionRoomController` — client nhận thao tác đặt giá.
2. `ServerServices` và `JsonTransport` — đóng gói, gửi yêu cầu TCP/JSON.
3. `Server.Client.readLoop()` — server nhận và định tuyến yêu cầu.
4. `AuctionService.execute()` quản lý phiên và lệnh gửi lại; `dispatchAction()`/`handleAction()` gọi `BiddingLogic.placeBid()` để xử lý giá.
5. `AuctionRepository.save()` — lưu dữ liệu; sau đó server phát cập nhật realtime.

Các đường dẫn và sơ đồ chi tiết có trong [bản đồ code](docs/viva/CODE_MAP.md). Tài liệu ôn tập:

- [Kiến trúc và luồng dữ liệu](docs/viva/ARCHITECTURE.md)
- [Giải thích OOP](docs/viva/OOP_EXPLANATION.md)
- [Design pattern](docs/viva/DESIGN_PATTERNS.md)
- [Câu hỏi vấn đáp](docs/viva/VIVA_QUESTIONS.md)
- [Kịch bản demo](docs/viva/PRESENTATION_GUIDE.md)

## Build và kiểm thử

```powershell
mvn -B verify
```

Lệnh trên build dự án và chạy bộ kiểm thử tự động. Xem [báo cáo tối giản gần nhất](docs/FINAL_SIMPLIFICATION_AND_REQUIREMENTS.md) và các báo cáo khác trong `docs/` để biết phạm vi đã rà soát.
