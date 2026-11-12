> Báo cáo lịch sử trước chuyển JSON-only. Xem [báo cáo hiện tại](JSON_ONLY_REFACTOR.md). Các lệnh MySQL bên dưới không còn áp dụng cho phiên bản hiện tại.

# Rà soát lưu trữ và kiểm thử an toàn - 11/11/2026

## 1. Phạm vi đã được xác nhận

Mục tiêu ban đầu là chuyển sang JSON-only. Khi kiểm tra, có `data/state.json` và dịch
vụ MySQL80 đang chạy nhưng không có cấu hình `AUCTION_DB_*` trong tiến trình làm việc
để đối chiếu dữ liệu. Chủ dự án xác nhận: **"Chưa rõ; giữ MySQL và chỉ sửa phần an toàn"**.

Vì vậy, **chưa thực hiện chuyển đổi JSON-only**. Giữ nguyên DAO MySQL, `StateStore`,
dependency, migration, lựa chọn storage và mặc định MySQL trong script. Không tự chạy
database, import/export, migration hoặc khởi động server trên dữ liệu thật.
Java entry point vẫn có mặc định JSON từ phiên bản trước; phải kiểm tra và chọn
storage đúng trước khi dùng lệnh Java trực tiếp. Không coi JSON hiện có là bản mới nhất.

Repository có thay đổi chưa commit từ trước lần làm việc này. Các thay đổi đó được
giữ lại; danh sách dưới đây chỉ mô tả phần sửa trong lần rà soát này, không phải toàn
bộ `git diff` so với HEAD. Không commit hoặc push.

## 2. Dữ liệu thật

Đã đọc JSON để kiểm kê, không ghi lại hoặc chuyển đổi dữ liệu:

| Thuộc tính | Giá trị trước và sau |
|---|---|
| File | `data/state.json` |
| Kích thước | 6.606 byte |
| Schema | `schemaVersion=1` |
| Số tài khoản / phiên | 5 / 6 |
| Auto-bid / biên nhận lệnh / giao dịch ví | 0 / 1 / 1 |
| SHA-256 | `343fb9082f7798a1e4e7d41e6648efc86e89a1abe71483c7c191e12d64377063` |

Hash trùng khớp sau build/test; `data/server.lock` cũng không đổi. Không xóa dữ liệu
thật, không sửa tài khoản, không reset phiên. Dịch vụ MySQL đang chạy không chứng
minh database nào thuộc ứng dụng hoặc database đó đã nhận giao dịch mới hơn JSON.
Chưa truy vấn được MySQL nên không kết luận về độ mới của nó.

## 3. File đã sửa, thêm và xóa

| File đã sửa | Nội dung |
|---|---|
| `src/main/java/com/auction/server/dao/AuctionRepository.java` | Đọc JSON strict; chỉ cho lưu sau khi load thành công; khóa các phương thức; báo lỗi rõ khi trùng thư mục, đã đóng hoặc không hỗ trợ atomic move; tái sử dụng Gson; đóng channel giải phóng khóa. |
| `src/main/java/com/auction/server/service/AuctionService.java` | Kiểm tra schema, version và collection khi mở snapshot; ghi log nguyên nhân lỗi commit; giữ hoàn tác và commit trước push. |
| `src/main/java/com/auction/server/network/Server.java` | Thay IP cố định `10.11.21.7` bằng bind mặc định `0.0.0.0`. |
| `run.ps1` | Đồng bộ bind mặc định `0.0.0.0`; giữ nguyên mặc định storage và Demo tắt. |
| `src/test/java/com/auction/server/service/AuctionServiceTest.java` | Thêm test lỗi lưu bid: không thành công/push, không giữ tiền hoặc biên nhận; gửi lại cùng commandId sau sửa lỗi/restart chỉ ghi một bid. |
| `README.md` | Nêu quyết định giữ MySQL, tránh đổi sang JSON cũ; sửa hướng dẫn Demo và tài khoản admin ban đầu; giải thích an toàn JSON. |
| `MULTI_CLIENT_SETUP.md` | Nêu trạng thái chưa xác minh dữ liệu; sửa mô tả storage; demo ở thư mục/cổng riêng; hướng dẫn lỗi JSON. |
| `docs/MULTI_CLIENT_TEST_RESULTS.md` | Chỉ thêm nhãn báo cáo lịch sử và liên kết báo cáo hiện tại; giữ nội dung lịch sử. |

File thêm:

- `src/test/java/com/auction/server/dao/AuctionRepositoryTest.java` - 10 trường hợp về restart, file tạm, khóa độc quyền, JSON hỏng và lỗi ghi.
- `src/test/java/com/auction/server/service/AuctionStateValidationTest.java` - 7 trường hợp thiếu dữ liệu bắt buộc, từ chối khởi động mà không reset, giải phóng khóa.
- `docs/JSON_STORAGE_SAFETY_REVIEW.md` - báo cáo này.

**File xóa: không có. Dependency loại bỏ: không có.** Hash của `pom.xml`, hai file
`Start-*.cmd`, `MysqlStateStore.java`, `StateStore.java` và `MysqlStateStoreTest.java`
khớp lúc bắt đầu. Giữ `mysql-connector-j:9.2.0` và các dependency hiện có theo xác nhận.

## 4. Số dòng

Đếm dòng vật lý `.java` trong `src`, bao gồm dòng trắng/chú thích, so với bản làm việc
thực tế lúc bắt đầu, không lấy HEAD làm mốc:

| Phạm vi | Trước | Sau | Chênh lệch |
|---|---:|---:|---:|
| Java chính (`src/main`) | 11.416 | 11.452 | +36 |
| Java kiểm thử (`src/test`) | 3.422 | 3.585 | +163 |
| Tổng Java | 14.838 | 15.037 | +199 |
| Số file Java | 101 | 103 | +2 |

Chưa giảm code MySQL vì chưa đủ căn cứ bảo toàn dữ liệu. Phần tăng chủ yếu là test
bảo vệ dữ liệu; không thêm abstraction hoặc thay kiến trúc nghiệp vụ.

## 5. Kiến trúc và chức năng được giữ

- Maven release **25**, JavaFX **26.0.2**, TCP Socket JSON, MVC và Client-Server.
- Cổng mặc định **8888**, bind có thể cấu hình, nhiều socket với luồng đọc/ghi độc lập.
- Đăng ký/đăng nhập, Bidder/Seller/Admin, quản lý sản phẩm/phiên và quản trị người dùng.
- Đặt giá, ví/giữ tiền/thanh toán giả lập, lịch sử và commandId chống xử lý lặp.
- Khóa nghiệp vụ `synchronized`, scheduler đóng phiên/xác định người thắng.
- Push realtime, auto-bid, anti-sniping và biểu đồ JavaFX hiện có.
- Xử lý lỗi, timeout, reconnect và đăng nhập lại sau restart.

Client vẫn chỉ gọi server qua TCP; file phía client là sổ lệnh chờ/ảnh cache, không
phải snapshot nghiệp vụ của server. Logic nghiệp vụ không bị xóa hoặc gộp.

JSON ghi file tạm cùng thư mục, `force(true)`, rồi `ATOMIC_MOVE`. Khi không hỗ trợ,
phương án là **từ chối giao dịch**, giữ file chính và báo lỗi để chuyển dữ liệu đã
xác minh sang filesystem local phù hợp. Không dùng fallback ghi đè không nguyên tử
hoặc tự phục hồi từ `.tmp`. Chưa thử trên filesystem không hỗ trợ atomic move hay
mô phỏng mất điện; không bảo đảm chống mọi loại hỏng đĩa/phần cứng.

## 6. Build và kiểm thử thực tế

Lượt cuối hoàn tất lúc **22:37:41 ngày 11/11/2026, UTC+7**:

**BUILD SUCCESS - 145 tests, 131 PASS, 0 failures, 0 errors, 14 SKIPPED.**
Maven chạy bằng JDK 25.0.4.1; JAR manifest và class major version 69 xác nhận Java 25.
JAR vẫn chứa MySQL driver và hai lớp lưu trữ theo phạm vi đã xác nhận.

| Kiểm tra | Kết quả / bằng chứng |
|---|---|
| Maven compile, JUnit, đóng gói JAR | PASS, `target/json-safety-verify.log` |
| Đăng ký/đăng nhập và phân quyền | PASS tự động, `FullStackTest`, `AuctionServiceTest` |
| Hai client socket cùng server và push bid | PASS trên cùng máy, `ServerIntegrationTest`, `FullStackTest` |
| Bid đồng thời, không vượt số dư | PASS tự động, `ServerIntegrationTest`, `AuctionServiceTest` |
| Ngắt/malformed client A, B vẫn hoạt động | PASS tự động, `ServerIntegrationTest` |
| Restart: trạng thái/ví/lịch sử/commandId | PASS tự động trên dữ liệu tạm |
| JSON hỏng, thiếu collection, khóa hai repository | PASS, 17 trường hợp ở hai test mới |
| Lỗi ghi bid không ACK thành công hoặc push, retry không nhân đôi | PASS, test mới trong `AuctionServiceTest` |
| Cú pháp và tham số launch PowerShell | PASS với `java`/`mvn`/`Read-Host` được giả lập; kiểm tra bind, IP nhập, port, Demo tắt và lỗi build |
| `git diff --check` | PASS |
| Dữ liệu thật không đổi | PASS đối chiếu SHA-256 |
| Loại bỏ dependency MySQL | CHƯA THỰC HIỆN theo xác nhận chủ dự án |
| GUI JavaFX desktop | 13 SKIPPED; chưa bật `auction.ui.tests` trong lượt này |
| MySQL thật | 1 SKIPPED; không có database test riêng/cấu hình opt-in |
| Nhiều máy Windows qua LAN/Wi-Fi | CHƯA KIỂM THỬ |
| GitHub Actions trên GitHub | CHƯA CHẠY trong lần này; workflow được giữ nguyên |

Các test dùng thư mục tạm trong `target/test-tmp`; không chạy server trên `data/`.
Kiểm tra launcher dùng hàm giả, không phải kiểm chứng kết nối JavaFX hoặc database thật.
Hai file `.cmd` được giữ nguyên; chúng gọi PowerShell với `-ExecutionPolicy Bypass`.
Kiểm tra script trực tiếp ban đầu bị ExecutionPolicy chặn; harness chạy trong tiến
trình PowerShell riêng với cùng cờ như `.cmd` đã PASS, không thay policy hệ thống.

Lượt verify ban đầu **BUILD FAILURE** do Surefire không nạp được
`com.auction.client.ui.UiResourcesTest` (`ClassNotFoundException`); không tính là PASS.
Lượt cuối nạp và chạy được lớp đó cùng toàn bộ suite. Không sửa cấu hình test để bỏ
qua lớp lỗi. Build vẫn có cảnh báo shade trùng manifest/module-info và API deprecated
hiện có; không phải lỗi test.

Lệnh thực tế (dùng cache sẵn có trong workspace và thư mục tạm được cấp quyền):

```powershell
New-Item -ItemType Directory -Force -Path target/test-tmp | Out-Null
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' '-DargLine=-Djava.io.tmpdir=D:\Bai-Tap-Lon-gon\target\test-tmp' verify
```

Ngoài môi trường hạn chế quyền/cache, dùng `mvn -B verify`.

## 7. Đối chiếu hai PDF

Đã trích đọc đủ 10 trang [đề bài](../2026-BTL.pdf) và 7 trang
[hướng dẫn](../Hướng%20dẫn%20BTL%20LTNC-2026.pdf), đồng thời xem ảnh trang thang điểm
và các trang tổng quan/lưu trữ. Đề bài không bắt buộc MySQL; hướng dẫn có File I/O,
Serialization. Việc chưa bỏ MySQL là quyết định bảo toàn dữ liệu, không phải yêu cầu chấm điểm.

| Yêu cầu | Vị trí / mức kiểm chứng |
|---|---|
| Cây kế thừa, đóng gói, trừu tượng, đa hình | `server/model`: Entity, User và Bidder/Seller/Admin; Item và Electronics/Art/Vehicle, phương thức override. Giữ nguyên. |
| Singleton, Factory, Observer | `Server.getInstance`, `ItemFactory`, publisher/listener qua Socket. Giữ nguyên. |
| Người dùng, sản phẩm, đặt giá, kết thúc, lỗi | `AuctionService`, test nghiệp vụ và FullStack đã chạy. |
| Concurrency, realtime | Khóa chung, push sau commit; test nhiều socket/bid đồng thời đã chạy. Chưa xác minh LAN vật lý. |
| Client-Server, MVC, FXML, DAO | Giữ tầng giao diện/controller/service, `Server`, `AuctionService`, repository. |
| Maven/JUnit/GitHub Actions | Build/test local thành công; `.github/workflows/ci.yml` có sẵn. Chưa chạy CI từ xa. |
| Auto-bid, anti-sniping, biểu đồ giá | Logic auto-bid/anti-sniping có test đã chạy; màn hình/biểu đồ chưa kiểm thử desktop trong lần này. |

Chưa tuyên bố đáp ứng 100% hoặc dự đoán điểm. Hướng dẫn còn nêu Checkstyle và coverage
>=60%; `pom.xml` hiện không có Checkstyle/công cụ đo coverage nên chưa có bằng chứng
cho hai mục này. Backend dùng JsonObject cho snapshot phiên/bid thay vì các lớp domain
riêng Auction/BidTransaction; cần giải thích rõ thiết kế khi bảo vệ và đối chiếu với
giảng viên. Không thêm lớp hình thức chỉ để đủ tên hoặc tăng số dòng.

## 8. Cách chạy hiện tại

Sau khi xác định đúng database đang dùng và cấu hình MySQL theo
[hướng dẫn LAN](../../MULTI_CLIENT_SETUP.md), chạy:

```powershell
.\Start-Server.cmd
.\Start-Client.cmd
```

Server dùng MySQL, Demo tắt, bind `0.0.0.0:8888`. Client hỏi IP; cùng máy nhập
`127.0.0.1`, máy khác nhập IPv4 LAN thực của server từ `ipconfig`. Chưa xác minh dữ
liệu thì không đổi sang `-Storage json` để bỏ qua lỗi MySQL. Không khởi động mới
trên dữ liệu thật chỉ để thử; scheduler có thể đóng/thanh toán phiên đã hết giờ.

## 9. Bước tiếp theo để bỏ MySQL an toàn

Chưa thực hiện các bước sau trên dữ liệu thật:

1. Xác định database/host đang triển khai từ người quản lý dữ liệu; dừng tiến trình
   auction server đang ghi, giữ các file hiện tại và sao lưu database cùng JSON.
2. Đọc duy nhất dòng `auction_state WHERE id=1`, xuất nguyên `payload` UTF-8 thành
   **file mới ngoài `data/state.json`**, ghi riêng revision và hash. Không dùng dữ
   liệu định dạng bảng/CSV có escape thay cho JSON payload, không sửa/xóa dòng gốc.
3. Kiểm tra schemaVersion, users, auctions/bids, rules, transactions, commands và số
   dư/tiền giữ; đối chiếu với JSON hiện tại cùng các giao dịch đã biết. Revision
   MySQL và `version` nghiệp vụ là hai bộ đếm khác nhau, không so trực tiếp. Thời
   gian sửa file đơn lẻ không đủ chứng minh dữ liệu mới nhất.
4. Thử bản xuất ở thư mục/cổng riêng, Demo tắt; kiểm tra đăng nhập, ví, lịch sử,
   commandId và restart. Giữ bản gốc bất biến để so sánh các thay đổi scheduler hợp lệ.
5. Sau khi xác nhận nguồn chính thức, mới chuyển dữ liệu bằng kế hoạch có backup,
   bỏ DAO/dependency MySQL và cập nhật mặc định JSON-only. Không ghi đè bản JSON
   hiện tại hoặc tự động chuyển đổi khi chưa xác nhận dữ liệu.

Giới hạn còn lại: một server, khóa nghiệp vụ toàn cục, ghi toàn bộ snapshot mỗi giao
dịch; phù hợp bài tập nhỏ. TCP chưa TLS, chỉ dùng LAN tin cậy. Session ở RAM nên sau
restart phải đăng nhập lại. Nguồn dữ liệu chính thức và khả năng chạy trên nhiều máy
Windows vẫn cần xác minh; không suy ra từ các test loopback.
