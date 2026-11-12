# Hệ thống đấu giá trực tuyến

Ứng dụng Java 25, JavaFX 26.0.2 và Maven. Client JavaFX kết nối server qua TCP Socket
JSON. Server là nơi duy nhất quản lý nghiệp vụ và lưu `data/state.json` bền vững.
Dự án hiện JSON-only, không cần MySQL hoặc biến cấu hình database.

Bộ kiểm kê ngày 12/11/2026 gồm [trách nhiệm từng file](docs/PROJECT_FILE_INVENTORY.md),
[thống kê code trước/sau](docs/CODE_STATISTICS.md) và
[thao tác dọn dẹp cùng kết quả kiểm thử mới](docs/CLEANUP_REPORT.md).

## Chạy trên Windows

Cài JDK 25 và Maven, thêm vào PATH. Kiểm tra `java -version`, `mvn -version`.
Nhấp đúp Start-Server.cmd trong File Explorer, giữ cửa sổ server mở, rồi nhấp đúp
Start-Client.cmd. Hoặc trong PowerShell tại thư mục có pom.xml:

```powershell
.\Start-Server.cmd
# Terminal khác hoặc máy client khác
.\Start-Client.cmd
```

Server dùng JSON thật, Demo tắt, bind `0.0.0.0:8888`. Client hỏi IP/hostname:
Enter dùng `127.0.0.1` trên cùng máy; máy khác nhập IPv4 LAN thực của server từ
`ipconfig`. Không dùng `0.0.0.0` làm địa chỉ client. Maven lần đầu cần mạng để tải
thư viện. Khi lỗi, cửa sổ CMD giữ thông báo. Xem [hướng dẫn LAN](MULTI_CLIENT_SETUP.md).

Dữ liệu hiện có không bị tạo lại, đổi số dư hoặc xóa lịch sử. Với thư mục hoàn toàn
mới, cần đặt rõ `AUCTION_ADMIN_PASSWORD` (8-128 ký tự, gồm chữ và số) để cấp admin
ban đầu `admin_hieu`; không có mật khẩu mặc định trong chế độ thật. Không tạo
bidder/seller hoặc phiên mẫu khi chạy script. Không xóa JSON để vượt qua lỗi khởi động.

## Chức năng và kiến trúc

- Đăng ký/đăng nhập; phân quyền Bidder/Seller/Admin và quản trị tài khoản.
- CRUD sản phẩm/phiên, tìm kiếm/lọc, ảnh sản phẩm và lịch sử đấu giá.
- Bid đồng thời, ví/giữ tiền/thanh toán giả lập, tự đóng phiên và xác định người thắng.
- Server push realtime, biểu đồ giá JavaFX, auto-bid và anti-sniping.
- CommandId chống xử lý lặp, timeout, thông báo mất kết nối và reconnect.

Client và server chạy riêng. MVC nằm ở controller/FXML/model. Adapter client gọi
TCP; Server điều phối request, AuctionService xử lý nghiệp vụ, AuctionRepository
đọc/ghi JSON. Giữ cây kế thừa User/Item, Factory, Singleton và Observer.
Đọc [bộ tài liệu bảo vệ và kết quả rà soát](docs/viva/AUDIT_AND_RESULTS.md),
[OOP](docs/viva/OOP_EXPLANATION.md), [kiến trúc](docs/viva/ARCHITECTURE.md),
[patterns](docs/viva/DESIGN_PATTERNS.md), [concurrency](docs/viva/CONCURRENCY_EXPLANATION.md),
[câu hỏi vấn đáp](docs/viva/VIVA_QUESTIONS.md) và [kịch bản demo](docs/viva/PRESENTATION_GUIDE.md).

Chỉ server mở snapshot; client lưu riêng sổ pending và ảnh cache trong thư mục
người dùng `.digital-auction/server`. Khóa service tuần tự hóa giao dịch, khóa file
ngăn hai server ghi chung data. Repository ghi file tạm, force(true), atomic move;
không hỗ trợ thì báo lỗi/từ chối giao dịch, không fallback ghi đè thông thường.
JSON hỏng/sai cấu trúc không bị reset; lỗi lưu hoàn tác bid/ví/commandId và không
phát push thành công. Không tự phục hồi từ `.tmp` hoặc snapshot cũ.

Giữ schemaVersion=1. Dừng server trước khi sao lưu/khôi phục; không sửa JSON khi
server chạy. `data/` chứa dữ liệu và backup, được bỏ qua bởi Git; không đăng backup
lên Git vì có dữ liệu người dùng. Restart cần đăng nhập lại. Ví dùng tiền giả lập,
TCP chưa TLS; chỉ dùng trong LAN tin cậy.

## Build và kiểm thử

```powershell
mvn -B verify
# Test desktop JavaFX; Linux dùng Xvfb
mvn -B verify '-Dauction.ui.tests=true'
# Chạy server từ JAR, mặc định JSON và không Demo
mvn -B package -DskipTests
java -jar target/digital-auction-1.0.0.jar
# Client với IP chỉ định
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 Client -ServerHost 192.168.1.100
# UI mock để phát triển độc lập
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 Client -Mock
```

`verify` tạo JaCoCo tại `target/site/jacoco/index.html` và kiểm tra Checkstyle
(quy tắc whitespace cơ bản, chưa phải toàn bộ Google Java Style). Để đo một lượt
coverage mới không cộng dồn dữ liệu cũ, dùng `mvn -B clean verify` và bật UI tests
nếu máy có desktop; CI Linux đã dùng Xvfb. Coverage được báo cáo trung thực cho
toàn dự án; xem kết quả và phạm vi kiểm thử trong `docs/viva/AUDIT_AND_RESULTS.md`.

Test server dùng dữ liệu tạm, không dùng data/state.json thật. Dữ liệu demo trong
fixture test không được tạo bởi script chạy chính thức. Không dùng Mock để kiểm
chứng nhiều client thật. [Báo cáo refactor](docs/archive/JSON_ONLY_REFACTOR.md) ghi backup,
file thay đổi, số dòng, kết quả thực tế và các kiểm thử còn cần làm thủ công.
