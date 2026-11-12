# Đối chiếu kiến trúc và kết quả rà soát

Ngày thực hiện: 12/11/2026, UTC+7. Đây là báo cáo cho working tree hiện tại; các báo cáo
11/11 trong `docs/` là lịch sử, không phải kết quả của lần này.

Ghi chú bổ sung sau lần kiểm kê cùng ngày. Các kết quả dưới đây thuộc lượt 15:58.
Maven clean của lần kiểm kê đã làm mới output trong target, gồm xóa raw log
`target/viva-final-verify.log`. Lượt verify mới có log riêng và coverage riêng trong
[báo cáo dọn dẹp](../CLEANUP_REPORT.md). Các số liệu và lịch sử lỗi dưới đây được
giữ để đối chiếu theo mốc thực hiện.

## Căn cứ và checkpoint

Đã trích xuất đủ 10 trang `docs/2026-BTL.pdf` và 7 trang
`docs/Hướng dẫn BTL LTNC-2026.pdf`. Đối chiếu các mục 3.1–3.6 và bảng điểm của đề;
hướng dẫn tuần 9–10 nêu Checkstyle và coverage >=60%. File I/O/Serialization được
đề cập; không có yêu cầu bắt buộc MySQL. Những tên lớp trong phần gợi ý phải gắn
với trách nhiệm thực tế, không có nghĩa tạo thêm lớp rỗng là đủ đáp ứng.

HEAD lúc bắt đầu: `db199b48fdfbceb192c4faa21c16a20376f81dac`. Có sẵn thay đổi chưa
commit ở README, launcher, repository, service, server, test và các tài liệu mới.
Giữ các thay đổi đó; không reset, commit hoặc push.

Checkpoint trước sửa:
`C:\Users\MAIDUC~1\AppData\Local\Temp\auction-viva-checkpoint-20261112-153909`.
Gồm `workspace-before.zip` (src/docs/config hiện có, launcher và cả dữ liệu),
`git-status.txt`, `head.txt`, `data-sha256.json`. ZIP chứa dữ liệu người dùng,
không đưa lên Git. Giải nén vào thư mục riêng để đối chiếu/phục hồi mã; không ghi
đè dữ liệu thật khi server đang chạy. Checkpoint ở thư mục tạm nên cần tự giữ bản
sao lâu dài trước khi hệ điều hành dọn thư mục này.

## Bảng đối chiếu và quyết định

| Yêu cầu | Bằng chứng trước sửa | Đánh giá / ưu tiên | Quyết định cuối |
|---|---|---|---|
| Entity | `server/model/Entity`: abstract, id/name private final | Đáp ứng | Giữ |
| User → Bidder/Seller/Admin | `User.getRoleName()`, `AuctionService.createUser()` dùng tham chiếu User | Có đa hình thực sự | Giữ; thêm test dispatch |
| Item → Electronics/Art/Vehicle | `Item.getCategory()`, `createAuction()` gọi Factory | Đáp ứng | Giữ; kiểm thử đủ 6 danh mục |
| Auction backend | Chưa có; `client/model/Auction` chỉ là DTO UI | Nên cải thiện: luật đọc khóa chuỗi JSON khó giải thích | Thêm record `server/model/Auction` chứa luật quyền/thời gian/bước giá |
| BidTransaction backend | Chưa có; `acceptBid()` tạo JSON trực tiếp | Nên cải thiện: lịch sử thiếu kiểu nghiệp vụ rõ | Thêm record bất biến và mapper ra đúng 6 trường cũ |
| JSON compatibility | Snapshot gồm users/auctions/rules/commands/transactions | Bắt buộc bảo toàn | Mapper chỉ đọc phiên và xuất bid mới; không tái serialize toàn bộ domain |
| MVC client | 8 Controller gọi interface service; FXML/CSS và ViewTasks riêng | Đáp ứng; controller phòng đấu giá và form sản phẩm dài | Giữ bố cục; validation UI hỗ trợ nhập liệu, server vẫn kiểm tra lại |
| MVC server | `Server.Client.readLoop → AuctionService → AuctionRepository` | Đáp ứng; service lớn, điều phối nhiều nghiệp vụ | Tách luật thuần sang domain; giữ chung transaction ví/bid/command |
| Singleton | Server.getInstance đồng bộ nhưng constructor public; MockDataStore constructor private | Cần giải thích chính xác | Server là default instance theo vòng đời, không Singleton nghiêm ngặt; không biến service/repository thành Singleton |
| Factory | abstract create, anonymous override và switch category | Có Factory Method tham số hóa; static selector là Simple Factory | Giữ API, làm rõ JavaDoc; không dựng thêm 6 creator class |
| Observer | server push, listener map CopyOnWriteArrayList, dispose gỡ callback | Có lỗi thật: một bid listener ném lỗi chặn listener sau và changed() | Bắt RuntimeException riêng từng callback; thêm test socket qua adapter |
| Concurrency | execute/tick synchronized, ví dùng chung, save trước push | Đã có test và đúng ranh giới nhất quán | Giữ khóa và thứ tự commit |
| Persistence | file lock, strict JSON, force + atomic move, rollback | Cần bảo toàn | Không thay; chạy test lỗi ghi/restart/chống lặp |
| Strategy/Command | AntiSnipingPolicy là policy cụ thể; action strings + commandId | Không phải hai GoF pattern hoàn chỉnh | Giải thích, không thêm lớp hình thức |
| Kiểm thử | JUnit 6.1.3, integration socket và GUI opt-in; không Mockito | Có test có ý nghĩa, thiếu test domain | Bổ sung domain/mapper/Factory/observer/concurrent tie |
| Chất lượng | CI Java 25 + Xvfb, chưa có JaCoCo/Checkstyle | Nên cải thiện | Thêm báo cáo coverage toàn bộ và quy tắc whitespace cơ bản |

Đường dẫn trong bảng dưới `src/main/java/com/auction/` nếu không ghi khác.
Các interface nghiệp vụ thật: `AuctionClientService`, `WalletService`,
`AutoBidService`, `ProductService`, `UserAdminService`, `AuctionAdminService`;
`DisposableView` là hợp đồng vòng đời màn hình. `AuctionRepository` là lớp cụ thể
implements `AutoCloseable`, không có interface repository riêng trong bản JSON-only.

## Phạm vi refactor

`Auction` backend là giá trị bất biến của trạng thái cần để kiểm tra bid, không phải
toàn bộ aggregate đã được chuyển khỏi JSON. `BidTransaction` là một bid đã chấp nhận,
không phải giao dịch nạp/rút/thanh toán trong `transactions`. `AuctionService` vẫn
điều phối trạng thái, ví, auto-bid, settlement và transaction; đây là refactor có
giới hạn, không tuyên bố backend đã loại bỏ mọi JsonObject.

Không gộp DTO JavaFX với domain server: UI cần dữ liệu hiển thị và setter; server
cần luật authoritative, tiền `long`, thời gian và trạng thái snapshot. Không đổi
API double cũ của client vì adapter đã kiểm tra số nguyên hữu hạn và giới hạn
9.000.000.000.000, nằm trong miền số nguyên biểu diễn chính xác của double.

Không đổi thuật toán auto-bid, không thêm PriorityQueue cho server chỉ để giống
gợi ý đề: bản thật sắp xếp rules theo order, mock mới dùng PriorityQueue. Không
thêm ReentrantLock song song với monitor hiện có hoặc chia khóa theo phiên vì ví
của cùng người dùng có thể tham gia nhiều phiên.

## File của lần thay đổi này

- Thêm `src/main/java/com/auction/server/model/Auction.java`, `BidTransaction.java`.
- Thêm `src/main/java/com/auction/server/dao/AuctionJsonMapper.java`.
- Sửa `AuctionService.java`: sử dụng domain cho quyền/thời gian/bước giá và bid mới.
- Sửa `ItemFactory.java`: mô tả đúng hai vai trò factory.
- Sửa `client/service/server/ServerServices.java`: cô lập lỗi observer.
- Thêm test `server/model/AuctionTest.java`, `ItemFactoryTest.java`,
  `server/dao/AuctionJsonMapperTest.java`.
- Bổ sung `AuctionServiceTest.java`, `integration/FullStackTest.java`.
- Sửa `pom.xml`, `.github/workflows/ci.yml`, `README.md` cho công cụ/báo cáo.
- Thêm `config/checkstyle.xml` và toàn bộ `docs/viva/`.

## Kiểm thử, coverage và dữ liệu

Lượt cuối từ build sạch: **BUILD SUCCESS**, kết thúc **15:58:06 ngày 12/11/2026
(UTC+7)**, verify mất 3 phút 54 giây. Maven 3.10.0 chạy bằng **JDK 25.0.4.1**;
compiler release 25. `java` trên PATH hệ thống là 26.0.2.1 nhưng Maven dùng JDK 25
từ cấu hình môi trường; không đổi target của dự án lên 26.

| Kiểm chứng | Kết quả thực tế |
|---|---|
| Baseline trước sửa | 144 test: 131 pass, 13 GUI opt-in skipped; build thành công |
| Nhóm domain/Factory/mapper/service/adapter | 43 test pass; verify ban đầu dừng ở thiếu dependency Checkstyle offline, không tính toàn build đó là pass |
| Checkstyle sau tải dependency | 0 vi phạm, BUILD SUCCESS |
| Toàn bộ từ build sạch | **164 test pass, 0 failure, 0 error, 0 skipped**, 27 test class |
| GUI mock / vòng đời | 12 test ClientUiTest pass |
| GUI + server TCP thật | 1 test ServerUiIntegrationTest pass, data tạm và port tự chọn |
| JaCoCo line toàn dự án | **4.625 / 6.061 = 76,31%**, không loại GUI/mock |
| JaCoCo instruction toàn dự án | 22.875 / 28.781 = 79,48% |
| JaCoCo branch toàn dự án | 1.549 / 2.505 = 61,84% |
| server.model | Line 60/60 = 100%; branch 18/18 = 100% |
| server.service | Line 611/679 = 89,99%; branch 249/355 = 70,14% |
| server.dao | Line 57/64 = 89,06%; branch 15/16 = 93,75% |
| server.network | Line 140/177 = 79,10%; branch 50/68 = 73,53% |
| Checkstyle ở verify cuối | 0 vi phạm |
| Dữ liệu thật | state.json **195.143 byte**, khớp nguyên byte với ZIP trước sửa |

SHA-256 state.json trước/sau:
`6c00dfceb67a9f9b114412d3e4eb04147ad26e022186f382e234d53cc1e56c15`.
Không chạy server/scheduler trên dữ liệu thật, không reset account/balance,
không xóa bid/commands/auctions, không bật Demo của ứng dụng. ZIP đã được kiểm
tra CRC thành công. Các số lượng user/bid/số dư thật không được đưa vào báo cáo.

Lệnh thực tế (cache Maven trong workspace và tmp riêng vì quyền sandbox Windows):

```powershell
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' clean
New-Item -ItemType Directory -Force -Path target/test-tmp | Out-Null
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' '-DargLine=-Djava.io.tmpdir=D:\Bai-Tap-Lon-gon\target\test-tmp' '-Dauction.ui.tests=true' verify
```

Log cuối: `target/viva-final-verify.log`. Báo cáo máy đọc: `target/surefire-reports/`,
`target/site/jacoco/jacoco.xml`, `target/checkstyle-result.xml`. Báo cáo HTML:
`target/site/jacoco/index.html`. Lệnh thông thường ngoài sandbox:
`mvn -B clean verify -Dauction.ui.tests=true`; Linux headless dùng Xvfb như CI.

Lượt đầy đủ trước clean cũng pass 164 test, line 76,36%; sau đó phát hiện XML
MysqlStateStoreTest cũ bị bỏ lại từ phiên làm việc trước trong surefire-reports.
Đã clean build directory và chạy lại toàn suite: báo cáo cuối có đúng 27 class,
không có test MySQL và không cộng dữ liệu JaCoCo cũ. Chênh lệch nhỏ coverage giữa
hai lượt đến từ đường thực thi bất đồng bộ; số cuối trong bảng là lượt clean.

Không có bài test tự động nào bị bỏ qua ở lượt cuối. Những phần **chưa chạy** là
LAN hai máy vật lý, workflow trên GitHub, thử mất điện/hỏng đĩa thật, filesystem
không hỗ trợ atomic move và kiểm thử tải lớn. Không suy ra các phần đó đã pass.

Cảnh báo hiện hữu: native access/unnamed module của JavaFX, Windows Preferences
trong sandbox, API deprecated và tài nguyên trùng khi shade. Chúng không làm test
thất bại. Không thay phiên bản JavaFX hoặc nới test để giấu cảnh báo.

Quy mô thay đổi Java so với checkpoint: main 76 → 79 file, 11.315 → 11.394 dòng;
test 24 → 27 file, 3.557 → 3.744 dòng (dòng vật lý gồm trắng/chú thích).
Phần lớn tài liệu thêm nằm ngoài Java; không tăng class theo danh sách hình thức.

Checkstyle áp dụng src/main và src/test: UTF-8, không tab, không khoảng trắng cuối
dòng, có newline cuối file. Đây là baseline nhỏ, **chưa kiểm tra đầy đủ Google Java
Style** (code cũ dùng thụt 4 dấu cách và wildcard import). Không che lỗi bằng một
danh sách loại trừ hàng nghìn dòng. Không dùng Mockito vì test hiện dùng service
thật, Clock điều khiển được và filesystem riêng, chưa cần mock framework.

JaCoCo 0.8.14 hỗ trợ Java 25 theo
[tài liệu JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/changes.html).
Surefire ghép agent bằng late evaluation, vẫn giữ `-DargLine` người dùng truyền
cho thư mục tạm. Báo cáo không loại GUI/mock để làm tăng tỷ lệ; mục tiêu 60% được
so với line coverage toàn dự án, báo riêng server chỉ để phân tích.

Các giới hạn cần nói rõ: chưa chạy LAN trên hai máy vật lý, chưa chạy workflow
trên GitHub trong phiên này, chưa thử mất điện thật/hỏng đĩa/atomic move không được
hỗ trợ; chưa thử tải lớn. TCP chưa TLS, session ở RAM, snapshot toàn bộ và khóa
toàn cục phù hợp phạm vi bài tập. Xem kịch bản demo và câu hỏi khó trong các file còn lại.

## Kiểm tra bàn giao và tài liệu đã tạo

Đã đối chiếu hash **cả 6 file trong data có trong checkpoint**, không file nào khác
hash hoặc bị mất. JAR cuối chứa Auction/BidTransaction/AuctionJsonMapper mới,
AuctionService có class major 69 (Java 25); không có MysqlStateStore hoặc package
driver com.mysql. `git diff --check` không báo lỗi whitespace. Các liên kết local
trong bộ tài liệu đã được kiểm tra tồn tại. Đã xem ảnh `target/qa/server-room.png`
và `server-wallet.png` của GUI integration: phòng đấu giá có giá/lịch sử/biểu đồ,
ví hiển thị số dư/giữ/khả dụng. Đây là ảnh test dữ liệu riêng, không phải tài khoản thật.

Sáu tài liệu bắt buộc:

1. [OOP_EXPLANATION.md](OOP_EXPLANATION.md): bốn tính chất, code thật, ví dụ và câu trả lời 30–60 giây.
2. [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md): pattern đã dùng, vai trò, giới hạn và câu hỏi thường gặp.
3. [ARCHITECTURE.md](ARCHITECTURE.md): tầng trách nhiệm và 4 sơ đồ Mermaid.
4. [CONCURRENCY_EXPLANATION.md](CONCURRENCY_EXPLANATION.md): thread, monitor, atomicity, push, tình huống cùng bid 1.200.000.
5. [VIVA_QUESTIONS.md](VIVA_QUESTIONS.md): 40 câu, mỗi câu có đáp nhanh, kỹ thuật, code và lỗi dễ mắc.
6. [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md): kịch bản thuyết trình, môi trường thử riêng và demo nhiều client/restart.

Tài liệu bổ sung: báo cáo này và [CODE_MAP.md](CODE_MAP.md), tra vị trí method,
kiểm kê 79 file Java chính và 27 file test. Các điểm dễ bị hỏi khó đã được giải
thích: Singleton nghiêm ngặt so với default instance; Factory Method so với Simple
Factory; JSON snapshot còn tồn tại; commandId không phải GoF Command; leader khác
winner; auto-bid thật không dùng PriorityQueue; rollback state khác rollback session;
event có thể đến trước ACK; local TCP test không chứng minh LAN hai máy.
