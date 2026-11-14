# Hệ thống đấu giá trực tuyến

Đợt UI mới nhất: [tối giản menu và kết quả kiểm thử](docs/FINAL_SIMPLIFICATION_AND_REQUIREMENTS.md).
Menu dùng pane trong Scene; kiểm thử Windows đã PASS, CI/Linux/LAN còn phải xác minh.


Ứng dụng Java 25, JavaFX 26.0.2 và Maven. Client JavaFX kết nối server qua TCP Socket
JSON. Server là nơi duy nhất quản lý nghiệp vụ và lưu `data/state.json` bền vững.
Dự án hiện JSON-only, không cần MySQL hoặc biến cấu hình database.

Bộ kiểm kê ngày 12/11/2026 gồm [trách nhiệm từng file](docs/archive/PROJECT_FILE_INVENTORY.md),
[thống kê code trước/sau](docs/archive/CODE_STATISTICS.md) và
[thao tác dọn dẹp cùng kết quả kiểm thử mới](docs/archive/CLEANUP_REPORT.md).

Lần refactor OOP/service tiếp theo được ghi tại
[báo cáo OOP và service](docs/archive/OOP_SERVICE_REFACTOR_REPORT.md), gồm đối chiếu PDF,
method đã chuyển, LOC, hash dữ liệu và kết quả kiểm thử hiện tại.

Kết quả rà soát Client hiện tại: [ổn định Client và dữ liệu](docs/FINAL_CLIENT_STABILITY_REPORT.md).
Client và launcher chỉ dùng ServerServices/TCP; `auction.mode=mock` bị từ chối.
Sáu lớp mô phỏng được chuyển nguyên hành vi sang `src/test/java/com/auction/client/fixture`;
test GUI tiêm fixture rõ ràng, ứng dụng chính không nạp chúng. Xem
[báo cáo Seller/Mock](docs/SELLER_UI_MOCK_CLEANUP_REPORT.md).

Đợt tối giản JavaFX trước được ghi trong [báo cáo hoàn thiện cuối](docs/FINAL_SIMPLIFICATION_REPORT.md),
gồm số liệu mới, kiểm tra mock/legacy và checklist thử nghiệm trên hai máy Windows.

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
TCP; Server điều phối request, AuctionService giữ transaction và gọi AuthLogic,
ProductLogic, BiddingLogic, WalletLogic, SettlementLogic; AuctionRepository
đọc/ghi JSON. Giữ cây kế thừa User/Item, Factory Method và Observer. `Server.getInstance()`
là Singleton production với constructor private, dùng chung một Server đang hoạt động.
`TestServers` chỉ nằm trong nguồn test và dùng reflection để tạo Server cô lập cho integration test.
Đọc [bộ tài liệu bảo vệ và kết quả rà soát](docs/archive/AUDIT_AND_RESULTS.md),
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

## Form Seller và kết quả

Thêm/Sửa nằm ngay trong trang Quản lý sản phẩm, không mở cửa sổ form riêng.
Form dùng card tối đa 1000 px, hai cột hoặc một cột theo cửa sổ; hỗ trợ 640 px.
Bắt đầu ngay dùng giờ Server lúc gửi Lưu; lịch gõ tay là dd/MM/yyyy và HH:mm.
Xem [báo cáo UI/UX hiện tại](docs/FINAL_UI_UX_REVIEW.md) cho ảnh và kết quả kiểm thử.
Lưu/Hủy/← Danh sách và ESC điều khiển cùng một pane; lỗi lưu giữ lại dữ liệu nhập.
Chỉ phiên OPEN chưa có bid và đúng chủ sở hữu mới được sửa/xóa; Server kiểm tra lại.
Trang chủ mặc định hiển thị đang/sắp mở. Chọn **Kết quả / Lịch sử** để xem phiên
kết thúc, đã thanh toán hoặc hủy; tìm kiếm, danh mục và bộ lọc trạng thái vẫn áp dụng.
Lịch sử được giữ trong JSON, không tự xóa phiên cũ.

Entry point Server luôn tắt seed, kể cả khi còn cấu hình cũ `auction.demo=true`.
Constructor private nhận `demo=true` chỉ được fixture `TestServers` gọi bằng reflection trong test;
không dùng nó để chuẩn bị dữ liệu bảo vệ. Tạo dữ liệu qua UI thật, giữ thư mục data khi restart.

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
```

`verify` tạo JaCoCo tại `target/site/jacoco/index.html` và kiểm tra Checkstyle
(Google configuration của Checkstyle 14.4.0, có kiểm cả test; warning làm build thất bại).
Gate JaCoCo yêu cầu LINE coverage toàn production ≥60% theo hướng dẫn giảng viên. Để đo một lượt
coverage mới không cộng dồn dữ liệu cũ, dùng `mvn -B clean verify` và bật UI tests
nếu máy có desktop; CI Linux đã dùng Xvfb. Coverage được báo cáo trung thực cho
toàn dự án; xem kết quả và phạm vi kiểm thử trong `docs/archive/AUDIT_AND_RESULTS.md`.

Test server dùng dữ liệu tạm, không dùng data/state.json thật. Dữ liệu demo trong
fixture test không được tạo bởi script chạy chính thức. Không dùng Mock để kiểm
chứng nhiều client thật. [Báo cáo refactor](docs/archive/JSON_ONLY_REFACTOR.md) ghi backup,
file thay đổi, số dòng, kết quả thực tế và các kiểm thử còn cần làm thủ công.

Đối chiếu cuối theo đề và thang điểm: [Teacher audit](docs/TEACHER_REQUIREMENTS_FINAL_AUDIT.md);
[học theo source](docs/viva/TEACHER_REQUIREMENTS_GUIDE.md); [checklist LAN hai máy](docs/viva/WINDOWS_LAN_CHECKLIST.md).

Đợt tối giản menu hiện tại: [kết quả và yêu cầu còn cần xác minh](docs/FINAL_SIMPLIFICATION_AND_REQUIREMENTS.md).
Nút tài khoản mở/đóng pane ngay trong Scene; chọn mục, logout hoặc rời trang đóng menu.
Menu không tự đóng theo click ngoài/focus/ESC và không có cửa sổ Popup riêng.
