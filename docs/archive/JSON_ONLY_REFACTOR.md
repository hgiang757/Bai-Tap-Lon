# Refactor JSON-only - 11/11/2026

## Căn cứ an toàn và checkpoint

Chủ dự án xác nhận Workbench: auction_system có 8 bảng cũ, chỉ usertable và
payment_accounts mỗi bảng có 1 dòng; không có auction_state trên MySQL cục bộ.
Lịch sử Git db199b4/d746adb dùng JSON. Phần MySQL mới chưa commit, không gọi schema
cũ; không thấy AUCTION_DB_* trong Process/User/Machine hoặc cấu hình khác đã kiểm
tra. Không có dấu hiệu snapshot MySQL mới hơn. Không kiểm chứng mọi máy chủ từ xa
không được khai báo; không suy đoán đã rà toàn bộ hạ tầng ngoài dự án.

Trước sửa: không thấy listener 8888; mở độc quyền server.lock thành công, cho thấy
không server hiện tại giữ khóa lúc sao lưu. Có Java từ extension VS Code; không
dừng tiến trình đó. Không chạy server trên dữ liệu thật, không chạy migration.

Checkpoint: data/backups/json-only-20261111-233921/ (Git bỏ qua), gồm:

- state.json: bản sao nguyên byte, hash khớp bản gốc.
- checkpoint.json: HEAD, hash dữ liệu/file và số dòng trước sửa.
- working-tree.zip: mã/tài liệu và cả thay đổi chưa commit/file mới không bị ignore;
  đã kiểm tra ZIP không lỗi. Không chứa thư mục dữ liệu thật.
- working-tree.patch: diff binary so với HEAD.

Khôi phục mã bằng cách giải nén ZIP vào thư mục riêng để đối chiếu, giữ các thay đổi
mới trước khi phục hồi. Không git reset làm mất thay đổi chưa commit. Khôi phục JSON
chỉ khi có quyết định riêng, server đã dừng và đã sao lưu hiện trạng; không tự ghi
đè dữ liệu. Không commit/push và không đưa backup có dữ liệu người dùng lên Git.

## File thay đổi

File xóa (đã có bản khôi phục, không chứa dữ liệu thật):

1. src/main/java/com/auction/server/dao/MysqlStateStore.java
2. src/main/java/com/auction/server/dao/StateStore.java
3. src/test/java/com/auction/server/dao/MysqlStateStoreTest.java
4. docs/mysql/V001__auction_state.sql

| File sửa | Nội dung |
|---|---|
| pom.xml | Bỏ mysql-connector-j:9.2.0 và transformer service loader thêm cho JDBC. |
| run.ps1 | Bỏ Storage/ImportJson/Demo; server JSON thật, auction.demo=false. |
| src/main/java/com/auction/server/service/AuctionService.java | Dùng trực tiếp AuctionRepository, bỏ nhánh và constructor chọn storage. |
| src/main/java/com/auction/server/dao/AuctionRepository.java | Implements AutoCloseable, giữ toàn bộ bảo vệ JSON. |
| src/test/java/com/auction/server/service/AuctionStateValidationTest.java | Gọi constructor JSON trực tiếp. |
| README.md, MULTI_CLIENT_SETUP.md | Hướng dẫn JSON-only, dữ liệu, LAN và lỗi. |
| docs/JSON_STORAGE_SAFETY_REVIEW.md, docs/MULTI_CLIENT_TEST_RESULTS.md | Chỉ thêm nhãn lịch sử; giữ nội dung cũ để tra cứu. |

File thêm: docs/JSON_ONLY_REFACTOR.md. Hai Start-*.cmd giữ nguyên vì đã gọi đúng
run.ps1 và truyền tham số. Không xóa database, bảng hoặc dữ liệu MySQL trên máy.
Cache Maven cũ không bị xóa; driver và dependency bắc cầu của nó không còn trong JAR cuối.

| Dòng Java vật lý (gồm dòng trắng/chú thích) | Trước | Sau | Giảm |
|---|---:|---:|---:|
| src/main | 11.452 | 11.315 | 137 |
| src/test | 3.585 | 3.557 | 28 |
| Tổng | 15.037 | 14.872 | 165 |
| Số file Java | 103 | 100 | 3 |

Mốc trước là working tree thực tế lúc bắt đầu lần chuyển JSON-only, không phải HEAD.

## Dữ liệu và chức năng

JSON gốc 6.606 byte, schemaVersion=1, 5 tài khoản và 6 phiên. SHA-256:
343fb9082f7798a1e4e7d41e6648efc86e89a1abe71483c7c191e12d64377063.
Bản gốc, bản sao và hash checkpoint được đối chiếu sau kiểm thử; không đổi số dư,
tài khoản, lịch sử hoặc phiên thật. Không chạy scheduler trên dữ liệu thật.

Giữ Java release 25, JavaFX 26.0.2, Maven, TCP JSON 8888, bind 0.0.0.0 cấu hình được,
MVC, OOP và Singleton/Factory/Observer. Không đổi logic đăng ký/login, phân quyền,
quản trị, CRUD sản phẩm/phiên, bid, ví, lịch sử, auto-bid, anti-sniping, biểu đồ,
tự đóng phiên/xác định người thắng hoặc reconnect.

Khóa service tuần tự hóa giao dịch; file lock giới hạn một server. Ghi tạm,
force(true), atomic move; nếu không hỗ trợ thì báo lỗi/từ chối, không fallback ghi
đè thường. JSON lỗi không reset; load thất bại không được save. Lỗi ghi hoàn tác
bid/ví/commandId, không ACK/push thành công; biên nhận chống lặp giữ sau restart.
Client chỉ gọi TCP; sổ pending/ảnh cache không phải snapshot server.

Script luôn tắt Demo. Storage hoàn toàn mới cần cấu hình rõ mật khẩu admin để cấp
admin ban đầu; thiếu cấu hình thì báo lỗi, không dùng mật khẩu mẫu. Fixture demo
được giữ cho test; dữ liệu thật không bị tạo lại hoặc đổi mật khẩu.

## Kiểm thử thực tế

**BUILD SUCCESS** lúc 23:46:26 ngày 11/11/2026 (UTC+7): 144 tests, **131 PASS**,
0 failures, 0 errors, **13 SKIPPED** (GUI desktop opt-in). Log:
`target/json-only-final-verify.log`. Test MySQL được bỏ cùng implementation.
JAR cuối không chứa MysqlStateStore, StateStore, driver MySQL hoặc protobuf đi kèm;
class server major 69 (Java 25). Quét src/pom/script không còn JDBC, MySQL hoặc
cấu hình chọn storage. Test launcher và git diff --check thành công.

Lượt đầu BUILD FAILURE do Surefire không nạp được một số lớp test (ClassNotFoundException);
không tính là PASS. Lượt cuối chạy toàn bộ suite và package thành công; không sửa
cấu hình để bỏ qua lớp lỗi. Chưa xác định chắc nguyên nhân lỗi nạp lớp ban đầu.
Không chạy GUI, LAN nhiều máy hoặc dữ liệu thật. Cảnh báo shade trùng manifest/
module-info và API deprecated hiện có không làm build thất bại.

Lệnh môi trường hiện tại:

```powershell
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' '-DargLine=-Djava.io.tmpdir=D:\Bai-Tap-Lon-gon\target\test-tmp' verify
```

Đã dọn riêng target/classes và target/test-classes trong workspace trước build để
không đóng gói class MySQL cũ; giữ dữ liệu và backup. Ngoài sandbox dùng mvn -B verify.
Test launcher giả lập java/mvn/Read-Host kiểm tra Demo tắt, bind/port, hỏi IP và lỗi
build không khởi chạy server; không phải test GUI hoặc kết nối hai máy.

Các test JSON kiểm tra restart, byte persistence, file hỏng/thiếu collection, khóa
độc quyền, lỗi ghi và bid thất bại không push; retry cùng commandId không nhân đôi.
FullStackTest kiểm tra đăng ký/login, ví, ảnh/quản trị, push, reconnect và mất ACK.
ServerIntegrationTest kiểm tra hai socket, bid cùng mức đồng thời, frame sai và ngắt
một client không ảnh hưởng client còn lại. Tất cả dùng thư mục tạm.

## Đối chiếu PDF và kiểm thử còn lại

Đã đọc lại đủ 10 trang 2026-BTL.pdf và 7 trang Hướng dẫn BTL LTNC-2026.pdf trong docs.
Đề bài không bắt buộc MySQL; hướng dẫn có File I/O/Serialization. Chỉ bỏ abstraction
chuyển storage, không bỏ cây kế thừa User/Item, Factory, Singleton, Observer hoặc
chức năng phục vụ thang điểm. MVC/FXML/controller/model/service/DAO được giữ.

Không tuyên bố đạt 100% hoặc dự đoán điểm: chưa chạy 13 test GUI opt-in, CI trên
GitHub, chưa đo coverage >=60% và chưa có Checkstyle plugin. Backend dùng JsonObject
cho snapshot phiên/bid; cần giải thích thiết kế domain khi bảo vệ. Chưa mô phỏng mất
điện/hỏng đĩa hoặc filesystem không hỗ trợ atomic move. Khóa chung/snapshot toàn bộ
phù hợp bài tập nhỏ; chưa kiểm chứng tải lớn. TCP chưa TLS; dùng LAN tin cậy.

Cần thử ít nhất hai máy Windows: hai bidder, realtime giá/lịch sử/biểu đồ, bid đồng
thời, tắt client, mất mạng/reconnect, restart giữ ví/bid/commandId, auto-bid,
anti-sniping và quyền Seller/Admin. Dùng dữ liệu thử riêng. Checklist đầy đủ trong
MULTI_CLIENT_SETUP.md. Chạy Start-Server.cmd rồi Start-Client.cmd; cùng máy dùng
127.0.0.1, máy khác dùng IPv4 LAN thực của server. Restart cần đăng nhập lại.
