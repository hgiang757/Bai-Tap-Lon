> Báo cáo lịch sử trước chuyển JSON-only. Xem [báo cáo hiện tại](JSON_ONLY_REFACTOR.md). Các lệnh MySQL bên dưới không còn áp dụng cho phiên bản hiện tại.

# Báo cáo thay đổi và kiểm thử — 11/11/2026

> **Báo cáo lịch sử của phiên bản trước lần rà soát an toàn JSON.** Các kết quả và
> lệnh bên dưới được giữ nguyên để tra cứu, không đại diện cho kết quả hiện tại.
> Xem [báo cáo mới](JSON_STORAGE_SAFETY_REVIEW.md). Hiện vẫn giữ MySQL vì chủ dự án
> chưa xác định được nguồn dữ liệu chính thức; chưa chuyển sang JSON-only.

## Tệp thay đổi

| Tệp | Nội dung |
|---|---|
| src/main/java/com/auction/server/network/Server.java | Bind LAN mặc định; log kết nối/ngắt; xử lý requestId lỗi trong nhánh kiểm tra request. |
| src/main/java/com/auction/server/dao/StateStore.java | Biên lưu trữ chung cho JSON/MySQL. |
| src/main/java/com/auction/server/dao/AuctionRepository.java | Triển khai StateStore, giữ nguyên cách lưu tệp hiện có. |
| src/main/java/com/auction/server/dao/MysqlStateStore.java | JDBC, khóa độc quyền server, transaction/khóa dòng/revision, nhập JSON chủ động. |
| src/main/java/com/auction/server/service/AuctionService.java | Chọn storage; giữ logic kiểm tra, commit trước push và commandId. |
| pom.xml | MySQL JDBC driver và gộp service loader vào shaded JAR. |
| run.ps1 | Hỏi IP client; server mặc định bind 0.0.0.0, MySQL; tùy chọn JSON/import/demo. |
| Start-Server.cmd, Start-Client.cmd | Truyền tham số tới PowerShell. |
| src/test/java/com/auction/server/network/ServerIntegrationTest.java | Hai bid đồng thời; client gửi frame sai và ngắt không ảnh hưởng client khác. |
| src/test/java/com/auction/server/dao/MysqlStateStoreTest.java | Test JDBC chủ động với database riêng: ghi/đọc sau restart, chặn server thứ hai. |
| docs/mysql/V001__auction_state.sql | Migration tạo bảng độc lập, không xóa/reset dữ liệu. |
| README.md, MULTI_CLIENT_SETUP.md | Cập nhật lệnh; hướng dẫn Windows, MySQL, LAN, firewall và 10 bài thử. |
| docs/MULTI_CLIENT_TEST_RESULTS.md | Báo cáo này. |

## Kiểm thử

Lần verify hoàn tất: **BUILD SUCCESS**, 127 tests, **113 PASS**, 0 failures,
0 errors, **14 SKIPPED** (13 JavaFX desktop và 1 MySQL cần bật chủ động).

- Kiểm tra cú pháp PowerShell: PASS.
- `git diff --check`: PASS.
- Đóng gói lại mã nguồn cuối cùng: PASS (`mvn ... -DskipTests package`).
  JAR có MysqlStateStore, bootstrap client App và META-INF/services/java.sql.Driver.
- Lần Maven mặc định: BLOCKED vì sandbox không cho tạo cache C:\.m2\repository.
- Lần Maven dùng cache workspace nhưng chưa có thư viện: BLOCKED do không truy cập mạng.
- Đã sao chép cache Maven sẵn có vào `.maven-repository`, không thay dependency để né lỗi.
- Lần verify sau đó: FAIL do JUnit không có quyền dọn thư mục tạm Windows bên ngoài workspace;
  127 tests, 0 assertion failures, 28 errors, 14 skipped. Không coi lần này là PASS.
- Chạy lại với java.io.tmpdir trong target/test-tmp: PASS; log: target/multi-client-verify.log.
  Các test FullStackTest, ServerIntegrationTest và AuctionServiceTest đều không có lỗi.
- MySQL thật: BLOCKED, chưa có AUCTION_DB_* cho database test riêng. Có dịch vụ MySQL80
  đang chạy nhưng không dùng tài khoản hoặc database thật để thử. Test JDBC mặc định SKIPPED.
- JavaFX desktop: CHƯA KIỂM THỬ trong lần verify mặc định (13 tests cần bật auction.ui.tests).
- Hai máy vật lý/LAN/Wi-Fi: CHƯA KIỂM THỬ; xem mục D của MULTI_CLIENT_SETUP.md.

Các test tự động TCP dùng nhiều socket trên cùng máy và storage JSON trong thư mục tạm.
Chúng kiểm tra nhiều client đăng nhập, push, chống lặp khi mất ACK, số dư/phiên sau restart,
đặt giá đồng thời, frame quá lớn, và server ngừng hoạt động. Không suy ra MySQL hoặc LAN thật
đã PASS từ kết quả loopback.

Lệnh verify thực tế trong môi trường sandbox (cache Maven đã sao chép sẵn):

```powershell
New-Item -ItemType Directory -Force -Path target/test-tmp | Out-Null
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' '-DargLine=-Djava.io.tmpdir=D:\Bai-Tap-Lon-gon\target\test-tmp' verify
```

Ngoài sandbox có quyền thư mục tạm và Maven cache bình thường, dùng `mvn -B verify`.

## Hạn chế còn lại

MySQL lưu snapshot trong một dòng, chưa chuẩn hóa thành bảng quan hệ. Khóa nghiệp vụ
toàn cục ưu tiên tính nhất quán cho quy mô bài tập; mỗi lần ghi lưu toàn bộ lịch sử.
Khi kết nối MySQL hỏng phải restart server để tải trạng thái đã commit.
Sau restart server phải đăng nhập lại do session nằm trong bộ nhớ.
Chưa có TLS, chỉ dùng trong LAN tin cậy. Dữ liệu người dùng hiện có không bị chỉnh sửa
trong quá trình phát triển/kiểm thử này; chưa chạy migration trên database thật.

## Lệnh chạy

```powershell
# Sau khi cấu hình MySQL theo MULTI_CLIENT_SETUP.md
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 Server
# Thay IP ví dụ bằng IPv4 thực của máy server từ ipconfig
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 Client -ServerHost 192.168.1.100
# Demo JSON cũ, không cần cấu hình MySQL
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 Server -Storage json -Demo
```

Mở cùng phiên đang diễn ra bằng hai tài khoản bidder khác nhau. A đặt giá hợp lệ;
B phải tự cập nhật giá/lịch sử. Ngắt A rồi thử B, sau đó restart server cùng storage
và đăng nhập lại để đối chiếu. Các bước chi tiết và xử lý lỗi nằm trong hướng dẫn LAN.
