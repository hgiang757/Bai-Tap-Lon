# Hệ thống đấu giá trực tuyến

Hệ thống hỗ trợ người dùng tham gia đấu giá, người bán quản lý sản phẩm và quản trị viên quản lý người dùng và phiên đấu giá. Giao diện JavaFX kết nối backend qua Socket JSON. Máy chủ xác thực, xử lý đấu giá/ví/quản trị và lưu dữ liệu; client nhận cập nhật realtime. Chế độ mock vẫn có để phát triển giao diện độc lập.

## Cấu trúc thư mục

- `auction-client-ui/`: ứng dụng JavaFX, dịch vụ mock, tài liệu và kiểm thử client.
- `auction-server/`: backend Socket JSON, tài khoản/quyền, sản phẩm/ảnh, đấu giá, ví, auto-bid, scheduler và lưu trữ.

## Chạy backend

```powershell
cd auction-server
mvn -B verify
java -Dauction.demo=true -jar target/auction-server-1.0.0.jar
```

Server mặc định tại `127.0.0.1:8888`. Tài khoản demo, cấu hình và quy tắc nằm trong [hướng dẫn backend](auction-server/README.md); hợp đồng request/response/push trong [giao thức v1](auction-server/PROTOCOL.md). Client mặc định kết nối server này; không tự chuyển sang mock khi lỗi mạng.

## Chạy client kết nối server

Cần JDK 25 và Maven. Từ thư mục gốc của repo:

```bash
cd auction-client-ui
mvn javafx:run
```

Mở thêm terminal và chạy cùng lệnh để có nhiều client. Có thể cấu hình `mvn javafx:run "-Dauction.host=IP_MAY_CHU" "-Dauction.port=8888"`. Chạy riêng mock bằng `mvn javafx:run "-Dauction.mode=mock"`.

Tài khoản thử được liệt kê trong [docs/DEV_ACCOUNTS.md](auction-client-ui/docs/DEV_ACCOUNTS.md).

## Chạy nhanh trên Windows

Cần JDK 25 và Maven trong PATH. Có thể nhấp đúp `Start-Server.cmd`, chờ thông báo server đã mở cổng, rồi nhấp đúp `Start-Client.cmd`. Mỗi lần mở thêm `Start-Client.cmd` sẽ tạo một client khác. Giữ cửa sổ server mở trong lúc sử dụng.

Từ thư mục gốc, terminal thứ nhất:

```powershell
.\run.ps1 Server
```

Terminal thứ hai (và thứ ba nếu muốn demo cạnh tranh giá):

```powershell
.\run.ps1 Client
```

Tài khoản SERVER demo: `bidder_hieu / Bidder12345`, `bidder_vip99 / Bidder12345`, `seller_hieu / Seller12345`, `admin_hieu / Admin12345`. Đăng ký mới có số dư 0; mở Ví để nạp tiền giả lập trước khi đấu. Tài khoản mock dùng mật khẩu khác (`123456`). Seed chỉ tạo lần đầu; nếu phiên mẫu đã hết giờ, đăng nhập Seller để tạo phiên mới, không cần xóa dữ liệu.

## Kiểm thử toàn dự án

```powershell
mvn -B verify
mvn -B verify "-Dauction.ui.tests=true"
```

Lệnh thứ hai cần desktop (Linux dùng Xvfb). Root Maven build server, client và `auction-integration-tests`; CI có kiểm thử Socket lẫn JavaFX kết nối backend. `auction-server/data` lưu dữ liệu máy chủ; lệnh chưa rõ kết quả được lưu ở thư mục người dùng `.digital-auction/server` và được đối soát bằng cùng commandId. Ví chỉ giả lập tiền; chưa có cổng thanh toán/giao hàng. Giao thức v1 phù hợp dữ liệu BTL, chưa phân trang tập dữ liệu lớn.

## Kiểm thử riêng client

```bash
cd auction-client-ui
mvn -B test
python scripts/check-encoding.py
```

## Kiểm thử server

```bash
cd auction-server
mvn -B test
```

Các kiểm thử server gồm: xử lý đặt giá và ngoại lệ (`BidProcessorTest`), đấu giá tự động (`AutoBidEngineTest`), chống bắn tỉa (`AntiSnipingPolicyTest`) và lưu trữ dữ liệu (`FileDaoTest`, `UserDAOTest`, `ItemDAOTest`). GitHub Actions chạy cả kiểm thử client và server mỗi khi push.

## Phân công

- Thành viên 1: Hệ thống & Mạng
- Thành viên 2: Dữ liệu & Logic nghiệp vụ
- Thành viên 3: Lập trình giao diện (client JavaFX, giao diện và adapter Socket)
- Thành viên 4: Nâng cao & Đảm bảo chất lượng
