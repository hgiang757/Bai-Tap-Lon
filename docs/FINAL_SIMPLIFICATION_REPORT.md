# Báo cáo hoàn thiện và tối giản giao diện JavaFX

**Chốt kiểm thử 2026-11-12T23:07:37+07:00. BUILD SUCCESS, 218/218 test PASS, 0 failure/error/skipped.**
Java production **12,475 → 11,650 dòng**, giảm
**825 dòng**. Nhóm tám file UI trực tiếp sửa
giảm **15.98%**.

## A. Tổng số dòng Java trước/sau

| Phạm vi | Trước | Sau | Chênh lệch |
|---|---:|---:|---:|
| Java production | 12475 | 11650 | -825 |
| Java test | 4292 | 4439 | +147 |
| Tổng Java | 16767 | 16089 | -678 |
| Số file Java production | 91 | 88 | -3 |
| Số file Java test | 27 | 27 | 0 |

## B. Tổng số dòng Controller trước/sau

| Phạm vi | Trước | Sau | Giảm |
|---|---:|---:|---:|
| Cả tám Controller | 4166 | 3657 | 509 (12.22%) |
| Sáu Controller được sửa | 3383 | 2874 | 509 |
| Sáu Controller + UserMenu + AppFooter | 3880 | 3260 | 620 (15.98%) |

## C. Tổng số dòng Mock Service trước/sau

Thư mục `src/main/java/com/auction/client/service/mock/` có sáu file,
**1370 → 1370 dòng**. Các file tiếp tục phục vụ chế độ mock và test.
Không có file chuyển sang src/test. Bảng phụ thuộc từng class ở mục G.

## D. File sửa, xóa hoặc tạo trong lượt này

Các dòng dưới so với checkpoint đầu lượt, bao gồm cả file trước đó chưa commit.
LOC của Markdown/FXML/CSS là dòng vật lý cùng quy ước với Java.

| File | Thao tác | LOC trước | LOC sau | Chênh lệch |
|---|---|---:|---:|---:|
| `README.md` | Sửa | 90 | 93 | +3 |
| `docs/CODE_STATISTICS.md` | Sửa | 671 | 671 | +0 |
| `docs/FINAL_SIMPLIFICATION_REPORT.md` | Tạo | 0 | 365 | +365 |
| `docs/PROJECT_FILE_INVENTORY.md` | Sửa | 1061 | 1061 | +0 |
| `docs/viva/AUDIT_AND_RESULTS.md` | Sửa | 195 | 195 | +0 |
| `docs/viva/CODE_MAP.md` | Sửa | 215 | 212 | -3 |
| `docs/viva/OOP_EXPLANATION.md` | Sửa | 131 | 131 | +0 |
| `src/main/java/com/auction/client/controller/AdminController.java` | Sửa | 514 | 436 | -78 |
| `src/main/java/com/auction/client/controller/AuctionRoomController.java` | Sửa | 1070 | 1031 | -39 |
| `src/main/java/com/auction/client/controller/ContactController.java` | Sửa | 226 | 107 | -119 |
| `src/main/java/com/auction/client/controller/GuideController.java` | Sửa | 213 | 81 | -132 |
| `src/main/java/com/auction/client/controller/HomeController.java` | Sửa | 627 | 506 | -121 |
| `src/main/java/com/auction/client/controller/SellerProductsController.java` | Sửa | 733 | 713 | -20 |
| `src/main/java/com/auction/client/ui/AppFooter.java` | Sửa | 97 | 78 | -19 |
| `src/main/java/com/auction/client/ui/UserMenu.java` | Sửa | 400 | 308 | -92 |
| `src/main/java/com/auction/server/model/AuctionDraft.java` | Xóa | 121 | 0 | -121 |
| `src/main/java/com/auction/server/model/Bid.java` | Xóa | 44 | 0 | -44 |
| `src/main/java/com/auction/server/model/Transaction.java` | Xóa | 40 | 0 | -40 |
| `src/main/resources/com/auction/client/css/app.css` | Sửa | 245 | 186 | -59 |
| `src/main/resources/com/auction/client/views/admin-view.fxml` | Sửa | 126 | 79 | -47 |
| `src/main/resources/com/auction/client/views/auction-room-view.fxml` | Sửa | 226 | 216 | -10 |
| `src/main/resources/com/auction/client/views/contact-view.fxml` | Sửa | 39 | 35 | -4 |
| `src/main/resources/com/auction/client/views/guide-view.fxml` | Sửa | 39 | 35 | -4 |
| `src/main/resources/com/auction/client/views/home-view.fxml` | Sửa | 91 | 72 | -19 |
| `src/test/java/com/auction/client/controller/ClientUiTest.java` | Sửa | 1293 | 1440 | +147 |
| `src/test/java/com/auction/integration/ServerUiIntegrationTest.java` | Sửa | 236 | 236 | +0 |

## Phạm vi, checkpoint và cách đo

Ngày thực hiện 12/11/2026, UTC+7, trong working tree `D:/Bai-Tap-Lon-gon`.
Mốc trước lấy từ trạng thái đang có các sửa đổi OOP/service chưa commit. Đọc lại
năm báo cáo được yêu cầu và trích xuất đủ 17 trang của hai PDF gốc bằng pypdf.
HEAD đầu và cuối là `c84c4a65a4c1091b61126712fbfac13b33842b95`.
Checkpoint `data/final-simplification-checkpoint-20261112/` nằm trong cây ignored.
`source-before.zip` chứa toàn bộ file tracked/untracked được Git liệt kê trước sửa,
bao gồm các helper chưa commit. `baseline.json` giữ HEAD, Git status, số dòng và
SHA-256 nguồn. `state-before.json` là bản sao riêng của dữ liệu thật.

Phép đếm dùng `read_text(encoding='utf-8-sig').splitlines()`, bao gồm dòng trống và
comment, đếm working tree thay cho chỉ HEAD. Không tính `target/`, cache Maven,
checkpoint hoặc tài liệu `.txt` vào LOC Java. Thay đổi trong báo cáo này được so
với checkpoint đầu lượt, nên phần refactor server có sẵn không bị tính lại.
Maven dùng JDK 25.0.4.1, compile release 25 và JavaFX 26.0.2. `java` trên PATH là
26.0.2.1. Các phiên bản cấu hình POM vẫn đúng yêu cầu.

## E. Thành phần giao diện đã lược bỏ

- Trang chủ bỏ lời quảng bá, khối hướng dẫn ba bước, lời mời đăng bán, icon trên
  từng chip danh mục, thông tin seller/số bid lặp trên card và popup lịch riêng.
  Các phiên sắp mở vẫn có card, đếm ngược và phòng chi tiết. Tìm theo seller vẫn
  hoạt động. Nút card thống nhất thành `Xem chi tiết`, dùng được cho mọi trạng thái.
- Admin mở trực tiếp danh sách phiên. Bỏ năm ô thống kê và bảng mười phiên mới
  lặp với bảng quản lý. Bảng phiên có bốn cột tên, giá, trạng thái và thao tác.
  Hộp Chi tiết vẫn có seller, leader, lịch, số bid, mô tả và các mức giá. Bảng người
  dùng cho cuộn ngang để giữ chiều rộng từng cột.
- Seller giữ form, ảnh, bảng, validation và khóa sửa/xóa theo trạng thái. Thay cell
  badge trạng thái tự vẽ bằng chuỗi mô tả tiếng Việt của enum.
- Phòng đấu giá bỏ dòng tiêu đề phụ và chấm `Trực tiếp` trang trí. Giữ line chart,
  gộp hai nhánh cài tooltip thành một callback dùng cho cả symbol có sẵn và symbol
  JavaFX tạo sau đó. Bỏ kiểu tô riêng điểm cuối và hiệu ứng phóng to điểm chart.
- Hướng dẫn có đúng sáu mục theo thứ tự yêu cầu, mỗi mục hai câu. Bỏ mục lục cột
  bên, animation cuộn, FAQ kỹ thuật dài và handler đăng nhập không có caller.
- Liên hệ dùng danh sách bốn thành viên một cột, giữ dữ liệu từ properties và nút
  sao chép. Thông báo sao chép nằm ngay trên card, loại bỏ popup/timer/fade và avatar.
- Menu tài khoản dùng Button có text/graphic sẵn của JavaFX. Bỏ HBox/Label/checkmark
  lồng cho từng dòng, fade khi mở/đóng, cờ đóng và handler click ngoài tự viết.
  `Popup.autoHide` xử lý click ngoài. Giữ listener focus, owner hide, scene detach,
  Escape, focus bàn phím, closeActive và gỡ listener khi popup ẩn.
- Footer giữ hai nút hướng dẫn/liên hệ, bỏ câu quảng bá và icon lặp. Bỏ các binding
  minHeight bị `AppFooter.install()` unbind ngay sau đó và các footer FXML tạo rồi
  bị thay thế. CSS bỏ các selector tùy biến đã hết caller, giữ selector JavaFX skin.

## F. Chức năng và tiêu chí đề bài

Số trang dưới đây là trang vật lý PDF, bắt đầu từ 1. Tính năng nâng cao được giữ
vì đây là phạm vi hiện có của dự án và là yêu cầu trực tiếp của người dùng.

| Tiêu chí | Đối chiếu PDF | Bằng chứng trong source hiện tại |
|---|---|---|
| Đăng ký, login, ba vai trò | Đề chính trang 4 | AuthController, AuthLogic, UserRole, kiểm tra quyền tại service |
| CRUD sản phẩm, lịch, giá, ảnh | Đề chính trang 4–5 | SellerProductsController, ProductLogic, ProductService |
| Bid, kiểm tra hợp lệ, leader | Đề chính trang 5 | Auction.requireBidder/requireMinimumBid, BiddingLogic.acceptBid |
| Đóng phiên, người thắng, trạng thái | Đề chính trang 5 | SettlementLogic.refreshAuctions/settle, AuctionStatus |
| Xử lý ngoại lệ | Đề chính trang 5 | ApiException, BidResult, ServerError, UI feedback |
| GUI JavaFX, MVC, FXML | Đề chính trang 6 và 9 | Tám route/FXML, controller, DTO, các interface service, ViewTasks |
| Encapsulation/abstraction | Đề chính trang 8 | Entity/User/Item abstract, field private, interface service |
| Inheritance/polymorphism | Đề chính trang 8 | User → Bidder/Seller/Admin, Item → Electronics/Art/Vehicle, getRoleName/getCategory override |
| Auction và BidTransaction | Đề chính trang 8 | Domain Auction bất biến chạy thật, BidTransaction và AuctionJsonMapper |
| Singleton | Đề chính trang 9 | Server.getInstance là instance mặc định, MockDataStore constructor private/getInstance |
| Factory Method | Đề chính trang 9 | ItemFactory.create abstract được override, forCategory chọn creator, ProductLogic gọi factory |
| Observer/realtime | Đề chính trang 7 và 9 | Publish sau commit, ServerServices listeners, registerRealtimeObserver, cập nhật JavaFX qua ViewTasks |
| Socket/client-server | Đề chính trang 8–9 | JsonProtocol TCP length-prefixed JSON, Server/JsonTransport, mặc định 8888, nhiều client |
| Multithreading/concurrency | Đề chính trang 7 và 10, hướng dẫn trang 3 | Virtual reader/writer, scheduler, synchronized execute/tick, transaction bao phủ mọi ví |
| Auto-Bidding | Đề chính trang 6 và 10 | BiddingLogic xử lý trần/bước/ngân sách/thứ tự, AutoBidEngine mock dùng PriorityQueue |
| Anti-sniping | Đề chính trang 7 và 10 | AntiSnipingPolicy, cộng 60 giây khi bid hợp lệ còn trên 0 tới 30 giây |
| Line chart realtime | Đề chính trang 7 và 10 | AuctionRoomController, timestamp/giá, điểm mới từ push, cửa sổ 30 điểm |
| File I/O và Serialization | Hướng dẫn trang 2, 4, 5, 7 | Gson JSON serialization, Repository load/save, FileChannel.force, atomic move, validator schema v1 |
| Maven, convention, JUnit | Đề chính trang 9–10, hướng dẫn trang 4 | POM release 25/JavaFX 26.0.2, JUnit 6.1.3, cấu hình Checkstyle cơ bản |
| JaCoCo ≥60% | Hướng dẫn trang 4 | Báo cáo XML/HTML sau clean verify, số đo ở mục I |
| GitHub Actions | Đề chính trang 10, hướng dẫn trang 4 | .github/workflows/ci.yml, JDK 25, Xvfb, verify bật GUI, upload báo cáo |

Hai PDF yêu cầu Serialization/lưu trữ nhưng không chỉ định API ObjectInputStream/
ObjectOutputStream. Snapshot tiếp tục dùng JSON. Marker Serializable trên Entity
không chứng minh đang dùng Java Object Serialization. Singleton mặc định của Server
vẫn có constructor public cho test. Checkstyle hiện chỉ kiểm whitespace/newline,
chưa chứng nhận toàn bộ Google Java Style. Những đặc điểm này đã có trong baseline.

Ví, giữ tiền, thanh toán một lần, duplicate commandId, rollback khi save lỗi,
trường mở rộng JSON và atomic persistence tiếp tục được kiểm tra qua các suite
service/DAO/network/adapter. Toàn bộ file server production còn giữ có SHA-256
trùng checkpoint. Các sửa đổi server và test server có sẵn trước lượt này được giữ.

## G. Kiểm tra legacy và Mock Service

### Ba lớp legacy đã xóa

| Lớp server | LOC bỏ | Bằng chứng và thành phần đang đảm nhiệm |
|---|---:|---|
| AuctionDraft | 121 | Tìm symbol trước sửa chỉ thấy khai báo/constructor trong chính file. Không route, FXML, config hoặc test nào nạp lớp. Domain Auction hiện tại đảm nhiệm quyền, thời gian, giá và hold |
| Bid | 44 | Ngoài file khai báo chỉ AuctionDraft dùng kiểu này. Luồng bid thật dùng BidTransaction/mapper, client có Bid riêng. Clean compile/test xác nhận không có caller Java còn lại |
| Transaction | 40 | Chỉ có định nghĩa/constructor. Ví server ghi lịch sử JSON qua WalletLogic.log, DTO Transaction client phục vụ hiển thị |

Đã xét các import wildcard, FXML injection, helper reflection GUI, POM/launcher,
resource và dữ liệu serialization. Source không có Class.forName/ServiceLoader
để nạp ba lớp này. Repository deserialize vào JsonObject, schema lưu trường dữ liệu,
không lưu Java class descriptor. Kiểm tra JAR sau clean bổ sung bằng chứng loại bỏ
class. Hướng dẫn tuần 6 có tên Bid trong sơ đồ gợi ý, đề chính có BidTransaction.
Cả bid client và BidTransaction server vẫn hiện diện và được kiểm thử trong luồng thật.

### Sáu lớp mock được giữ

| Class | LOC trước/sau | Caller production trong mode mock | Test/fixture | Quyết định |
|---|---:|---|---|---|
| MockDataStore | 505 / 505 | ServiceRegistry, mọi mock service | MockSharedStoreTest và các fixture createForTesting | Giữ trong main |
| MockAuctionClientService | 380 / 380 | ServiceRegistry, kiểm tra dev mode trong phòng đấu giá | MockAuthIntegrationTest, MockAuctionBidAccessTest, ClientUiTest | Giữ trong main |
| MockWalletService | 85 / 85 | ServiceRegistry và UI ví qua interface WalletService | MockWalletServiceTest, MockSharedStoreTest, ClientUiTest | Giữ trong main |
| MockAutoBidService | 92 / 92 | ServiceRegistry và phòng đấu giá qua AutoBidService | MockAutoBidServiceTest, ClientUiTest | Giữ trong main |
| MockManagementService | 199 / 199 | ServiceRegistry, ba interface product/user-admin/auction-admin | MockManagementServiceTest, GUI seller/admin | Giữ trong main |
| AutoBidEngine | 109 / 109 | MockAuctionClientService.placeBid và MockAutoBidService | MockAutoBidServiceTest qua đường gọi thật | Giữ trong main |

`ServiceRegistry.mockServices()` tạo nguyên bộ mock dùng cùng store.
`run.ps1 Client -Mock` là entry point đang được hỗ trợ. ClientUiTest dùng mock cho
phần lớn thao tác, ServerUiIntegrationTest dùng adapter/server thật.
Không có lớp nào trong thư mục mock chỉ thuộc phạm vi test. Việc chuyển nguyên
bộ sang src/test sẽ làm hỏng mode mock và tham chiếu ServiceRegistry ở main.
Phần sample data của MockDataStore cung cấp dữ liệu cho mode và fixture hiện tại.
Không có bằng chứng đủ để xóa nó trong lần tối giản giao diện này.

## H. Kiểm thử thực sự đã chạy

| Lượt / raw log ở checkpoint | Cases | Failure / error / skipped | Kết quả |
|---|---:|---|---|
| Baseline desktop, `baseline-desktop-verify.log` | 216 | 0 / 0 / 0 | BUILD SUCCESS |
| Compile sau tối giản, `compile.log` | — | — / — / — | BUILD SUCCESS |
| Verify đầu sau tối giản, `first-simplified-verify.log` | 217 | 0 / 1 / 0 | BUILD FAILURE |
| Hồi quy GUI, `gui-regression.log` | 16 | 0 / 0 / 0 | BUILD SUCCESS |
| Verify sau sửa binding, `intermediate-verify.log` | 218 | 0 / 0 / 0 | BUILD SUCCESS |
| Assertion Chi tiết Admin trước layout root, `admin-detail-test-before-layout.log` | 218 | 0 / 1 / 0 | BUILD FAILURE |
| GUI sau layout root, `gui-final.log` | 16 | 0 / 0 / 0 | BUILD SUCCESS |
| Verify trước sửa focus style, `verified-before-focus-style.log` | 218 | 0 / 0 / 0 | BUILD SUCCESS |
| Verify chốt, `final-verify.log` | 218 | 0 / 0 / 0 | BUILD SUCCESS |

Hai lần gọi Maven đầu gặp local repository mặc định `C:/.m2/repository` không ghi
được, trước khi chạy test. Lượt sandbox không bật GUI được dừng để đổi sang cấu hình
desktop đầy đủ, nên không dùng làm kết quả nền. Lệnh dùng đường dẫn repository có
sẵn trong workspace và thư mục tạm riêng khắc phục phụ thuộc môi trường.

Lượt đầu sau sửa có một lỗi NoSuchFieldException vì đã bỏ injection lblRunningHeader
mà test font hiện hữu vẫn dùng. Đã khôi phục binding. Lượt bổ sung assertion hộp
Chi tiết Admin ban đầu chưa layout toàn Scene nên chưa có TableRow, đã sửa bước
chuẩn bị test để layout root. Giữ nguyên các assertion kiểm tra nghiệp vụ và thêm
kiểm tra seller, leader, lịch bắt đầu/kết thúc trong hộp Chi tiết.

Hai test mới kiểm tra sáu mục hướng dẫn, nội dung hai câu, bốn email, clipboard,
feedback tại card và số cửa sổ không tăng. Test menu dùng JavaFX Robot click ngoài,
focus bàn phím và nút Ví, kiểm popup đã ẩn và listener owner/scene được gỡ.
Clipboard và vị trí chuột được hoàn trả trong thao tác kiểm thử.

| Suite | Cases | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| com.auction.client.controller.ClientUiTest | 16 | 0 | 0 | 0 |
| com.auction.client.navigation.ImageTasksTest | 5 | 0 | 0 | 0 |
| com.auction.client.navigation.ViewTasksTest | 4 | 0 | 0 | 0 |
| com.auction.client.service.BidSubmissionTrackerTest | 4 | 0 | 0 | 0 |
| com.auction.client.service.MockAuctionBidAccessTest | 1 | 0 | 0 | 0 |
| com.auction.client.service.MockAuthIntegrationTest | 2 | 0 | 0 | 0 |
| com.auction.client.service.MockAutoBidServiceTest | 6 | 0 | 0 | 0 |
| com.auction.client.service.MockManagementServiceTest | 3 | 0 | 0 | 0 |
| com.auction.client.service.MockSharedStoreTest | 2 | 0 | 0 | 0 |
| com.auction.client.service.MockWalletServiceTest | 4 | 0 | 0 | 0 |
| com.auction.client.service.ServiceRegistryTest | 2 | 0 | 0 | 0 |
| com.auction.client.ui.ProductImagesIoTest | 2 | 0 | 0 | 0 |
| com.auction.client.ui.ProductImagesTest | 1 | 0 | 0 | 0 |
| com.auction.client.ui.UiResourcesTest | 1 | 0 | 0 | 0 |
| com.auction.client.util.AuctionTimeTest | 3 | 0 | 0 | 0 |
| com.auction.client.util.FormatUtilTest | 2 | 0 | 0 | 0 |
| com.auction.client.util.ValidatorsTest | 44 | 0 | 0 | 0 |
| com.auction.integration.FullStackTest | 11 | 0 | 0 | 0 |
| com.auction.integration.ServerUiIntegrationTest | 1 | 0 | 0 | 0 |
| com.auction.server.dao.AuctionJsonMapperTest | 4 | 0 | 0 | 0 |
| com.auction.server.dao.AuctionRepositoryTest | 12 | 0 | 0 | 0 |
| com.auction.server.model.AuctionTest | 16 | 0 | 0 | 0 |
| com.auction.server.model.ItemFactoryTest | 16 | 0 | 0 | 0 |
| com.auction.server.network.ServerIntegrationTest | 3 | 0 | 0 | 0 |
| com.auction.server.service.AntiSnipingPolicyTest | 5 | 0 | 0 | 0 |
| com.auction.server.service.AuctionServiceTest | 21 | 0 | 0 | 0 |
| com.auction.server.service.AuctionStateValidationTest | 27 | 0 | 0 | 0 |
| **Tổng** | 218 | 0 | 0 | 0 |

| Chức năng | Bằng chứng kiểm thử |
|---|---|
| JavaFX thật | ClientUiTest, ServerUiIntegrationTest, không skipped khi bật UI |
| Socket, nhiều client, reconnect/restart | ServerIntegrationTest, FullStackTest, JVM con cold startup |
| Bid đồng thời, một ví ở nhiều phiên | equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice, concurrentAuctionsCannotOverspendOneWallet, simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer |
| Auto-bid trần/budget/thứ tự | autoBidRespectsLimitBudgetAndRegistrationOrder, earlierAutoBidCannotBlockAnAffordableRivalAfterItsBudgetChanges, MockAutoBidServiceTest |
| Anti-sniping | AntiSnipingPolicyTest, antiSnipingExtendsAndDoesNotSettleAtOldDeadline |
| Ví/settlement | walletHoldsOutbidReleaseAndWithdrawAreConsistent, settlementPaysSellerExactlyOnceAndSurvivesRestart |
| JSON, restart, validation | AuctionRepositoryTest, AuctionStateValidationTest, extensionFieldsSurviveAutomaticBidsSettlementAndRestart |
| Idempotency | idempotencyRejectsChangedPayloadAndPersistsFinalOutcome, lostBidAcknowledgementRetriesTheSameCommandNotAnotherBid |
| Rollback save lỗi | storageFailureRollsBackMoneyAndCommandResult, failedBidDoesNotPublishOrPersistAndSameCommandCanBeRetried, failedSettlementSaveRestoresBothWalletsAndRetryPaysExactlyOnce |
| UI giản lược | allRoutesLoadWithAuthorizedRoles kiểm bảng Admin và hộp Chi tiết, hai test hướng dẫn/contact/menu mới, các assertion tìm kiếm/grid/CRUD/ảnh/form cũ |

Hai assertion cũ chuyển từ overviewTable sang auctionTable vì bảng tổng quan đã bỏ.
Vẫn kiểm tra bảng có dữ liệu, gồm cả qua server thật. Không xóa test, giảm timeout,
nới assertion hay loại mã khỏi coverage. Chỉ hai method kiểm thử mới làm tăng số case.

Lệnh tái lập đã dùng ngoài sandbox cho desktop/Socket và quyền dọn TempDir.

```powershell
mvn -B -o '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' `
  '-Dauction.ui.tests=true' `
  '-DargLine=-Djava.io.tmpdir=D:/Bai-Tap-Lon-gon/data/final-simplification-checkpoint-20261112/test-tmp' `
  clean verify
```

Trên máy đã cấu hình Maven/temp bình thường dùng `mvn -B clean verify '-Dauction.ui.tests=true'`.
Raw log và bản sao Surefire/JaCoCo/ảnh nằm ở checkpoint. Báo cáo còn có thể mở trực
tiếp tại `target/surefire-reports/`, `target/site/jacoco/index.html`,
`target/checkstyle-result.xml` và `target/qa/`.

Đã mở ảnh QA trang chủ ở 1200/640 px, phòng đấu giá, bảng Admin, hướng dẫn/liên hệ
640 px và popup tài khoản. Lượt cuối kiểm cả popup riêng, viền focus bàn phím và
kiểu chữ chế độ đang chọn. Bảng Admin sau chỉnh có đủ chỗ cho tên/giá của fixture.
Tên quá dài vẫn có thể bị rút gọn theo chiều rộng cột, nút Chi tiết hiển thị toàn bộ.

## I. JaCoCo và Checkstyle trước/sau

| Chỉ số | Baseline | Sau tối giản |
|---|---:|---:|
| JaCoCo LINE | 5,010 / 6,576 = **76.19%** | 4,823 / 6,130 = **78.68%** |
| JaCoCo BRANCH | 1,719 / 2,717 = **63.27%** | 1,689 / 2,603 = **64.89%** |
| JaCoCo INSTRUCTION | 24,946 / 31,256 = **79.81%** | 23,946 / 29,329 = **81.65%** |
| JaCoCo METHOD | 838 / 1,190 = **70.42%** | 823 / 1,115 = **73.81%** |
| JaCoCo CLASS | 110 / 118 = **93.22%** | 109 / 113 = **96.46%** |
| Checkstyle violation | 0 | 0 |

Coverage có mẫu số mới vì bỏ mã UI và class legacy chưa được gọi, đồng thời có test
GUI mới. So phần trăm cần đọc cùng số covered/total. Không thêm exclude hay đổi rule
để cải thiện số đo. Cảnh báo JavaFX native access/unnamed module, API deprecated ở
WalletController và resource trùng khi shade vẫn xuất hiện trong build như baseline.

## J. Độ phức tạp và các quyết định giữ phần đang ổn

Tổng complexity counter do JaCoCo ghi nhận **2580 → 2448**, giảm **132**. Đây là số tổng của các method/nhánh trong bytecode được công cụ phân tích, bao gồm cả method chưa được chạy, dùng bổ sung cho bằng chứng giảm LOC.

Loại được trạng thái animation/timer, nhánh cập nhật grid liên hệ, model phiên cũ,
factory cell badge, một bảng tổng quan và helper cấu hình bảng chỉ còn một caller.
Đây là các nhánh/hành vi đã bỏ thực sự. Không tạo framework hoặc tách thêm class
để chuyển LOC. AuctionRoomController còn dài do giữ đối soát bid UNKNOWN, lifecycle,
auto-bid, chart và realtime. SellerProductsController còn form/ảnh/validation cùng
permission và xử lý task bất đồng bộ. Những phần có kiểm thử nền đã qua được giữ.
AuthController, WalletController và kiến trúc transaction server tiếp tục dùng
logic đã được kiểm chứng. Không đặt yêu cầu đạt 15–25% cho toàn bộ dự án.

## K. Dữ liệu và giới hạn xác minh

`data/state.json` có SHA-256 trước/sau cùng **`6c00dfceb67a9f9b114412d3e4eb04147ad26e022186f382e234d53cc1e56c15`**. Đối chiếu **29 file server production còn giữ** trùng hash đầu lượt. Test ghi dữ liệu trong @TempDir/thư mục checkpoint, bản sao state được kiểm tra riêng bằng Repository.load, validator và mapper, giữ nguyên bytes của bản sao. Kết quả probe `PASS: copied schemaVersion=1, mapped auctions=6, no save/service/scheduler invoked`. JAR sạch chứa Auction/BidTransaction và các lớp OOP còn dùng, loại đủ ba class legacy. Git HEAD giữ đúng mốc đầu lượt, các thay đổi được để ở working tree cho người dùng review.

- Chưa thử LAN giữa hai máy Windows vật lý, firewall, DHCP và rút mạng thật.
- Chưa chạy GitHub-hosted CI cho working tree này. Workflow được đọc và giữ.
- Kiểm tra popup mất focus dùng Stage thứ hai và Robot trong JVM. Alt+Tab sang
  ứng dụng độc lập, minimize, DPI 125%/150% và nhiều màn hình còn cần thử thủ công.
- Chưa thao tác native từng nút Admin đổi vai trò/khóa/xóa trên hai máy. Test
  service/mock/adapter kiểm quyền và hành vi, GUI kiểm bảng và hộp Chi tiết.
- Chưa kiểm thử mọi crash/power-loss/filesystem/mạng diện rộng hoặc load dài hạn.
  Có test lỗi ghi/rollback/atomic persistence và restart trong môi trường cục bộ.
- Không có phân tích call graph động hoàn chỉnh cho mọi nhánh ứng dụng. Quyết định
  xóa giới hạn ở ba class có bằng chứng source/config/persistence và clean build.
- JSON serialization đáp ứng cách lưu hiện tại. Nếu giảng viên yêu cầu riêng Java
  Object Serialization trong vấn đáp, cần đối chiếu yêu cầu bổ sung đó với hai PDF.

## L. Checklist cần làm trên hai máy Windows

Các mục dưới đây chờ nhóm thực hiện và ghi bằng chứng. Dùng dữ liệu thử riêng.

- [ ] Trên hai máy kiểm `mvn -version` dùng JDK 25, chạy build từ cùng source.
  Trên máy A tạo thư mục dữ liệu QA mới, dùng mật khẩu bootstrap thử riêng qua
  `AUCTION_ADMIN_PASSWORD`, rồi chạy JAR với `-Dauction.data=D:/Auction-LAN-QA/data`,
  `-Dauction.bind=0.0.0.0`, `-Dauction.port=8888`, `-Dauction.demo=false`.
- [ ] Máy A ghi IPv4 LAN từ ipconfig. Máy B chạy Test-NetConnection đến IPv4 đó,
  port 8888. Nếu bị chặn, cấu hình firewall Private/LocalSubnet theo hướng dẫn LAN.
- [ ] Mở một client ở A và một ở B, chọn mode SERVER và cùng IP máy A. Đăng ký seller
  và hai bidder riêng, nạp ví thử. Không dùng tài khoản hoặc snapshot thật làm fixture.
- [ ] Seller tạo phiên có ảnh, giá khởi điểm, bước giá và lịch tương lai. Sửa rồi xóa
  một phiên chưa có bid. Thử nhập thiếu, giá sai và ngày sai để kiểm feedback.
- [ ] Trên cả hai client kiểm tìm kiếm, lọc danh mục, ba nhóm trạng thái và Xem chi tiết.
  Mở cùng một phiên RUNNING, đặt giá trên A và quan sát B đổi giá/leader/history/chart.
- [ ] Hai bidder bid cùng mức gần đồng thời. Chỉ một lệnh thắng ở mức đó, lịch sử và
  số tiền giữ khớp. Bidder thua có thể đặt mức mới, seller bị chặn tự bid sản phẩm.
- [ ] Bật auto-bid ở cả hai bidder, thử cùng trần và trần khác. Kiểm ưu tiên đăng ký,
  bước giá, ngân sách, đạt trần và tắt auto-bid. Thử bid ở 30 giây cuối và kiểm gia hạn.
- [ ] Để phiên hết giờ. Kiểm người thắng, PAID, ví buyer/seller và mỗi bên chỉ có một
  giao dịch thanh toán. Thử phiên không có bid và phiên bị admin hủy để kiểm hoàn hold.
- [ ] Ngắt một client, client còn lại vẫn bid được. Ngắt mạng rồi kết nối lại, kiểm
  UI phản hồi, lỗi kết nối và Tải lại/đăng nhập. Lệnh chưa rõ kết quả phải được đối soát
  bằng commandId cũ trước thao tác giá mới, kiểm không nhân đôi bid/tiền.
- [ ] Dừng và khởi động lại server trên cùng thư mục QA. Đăng nhập lại, kiểm giá,
  winner, ví, lịch sử và settlement sau restart. Không reset JSON để vượt lỗi.
- [ ] Admin kiểm danh sách/lọc, Chi tiết, khóa/mở khóa, đổi vai trò, xóa tài khoản,
  hủy/xóa phiên theo quyền. Thử thao tác trái quyền và kiểm tài khoản hiện có bị khóa.
- [ ] Mở menu rồi click ngoài, Escape, đổi route, ẩn cửa sổ và Alt+Tab sang trình duyệt.
  Popup phải đóng. Thử DPI 100%/125%/150%, cửa sổ nhỏ, bàn phím Tab/Enter và scrollbar.
  Kiểm sáu mục hướng dẫn và sao chép đủ bốn email.
- [ ] Khi nhóm chọn phiên bản nộp và tự push, kiểm GitHub Actions trên đúng commit,
  lưu log/artifact JUnit/JaCoCo/Checkstyle cùng ảnh và biên bản LAN của nhóm.

Tham khảo lệnh và xử lý kết nối trong [MULTI_CLIENT_SETUP.md](../MULTI_CLIENT_SETUP.md).
