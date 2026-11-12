# Rà soát cuối hệ thống đấu giá Java

Ngày rà soát: **12/11/2026, UTC+7**. Báo cáo áp dụng cho working tree tại
`D:/Bai-Tap-Lon-gon`, không chỉ cho commit HEAD. Mốc build sạch cuối: **2026-11-12T18:27:12+07:00**.

**Kết quả:** **BUILD SUCCESS; 195 test, 0 failure, 0 error, 0 skipped; Checkstyle 0 vi phạm**. Có sửa trực tiếp các lỗi được chứng minh; giữ JSON-only,
không thêm MySQL, không reset dữ liệu, không commit hoặc push. Các mục LAN hai máy,
GitHub-hosted CI và môi trường desktop khác được phân biệt với test đã thực hiện.
Không suy ra điểm số hoặc bảo đảm không còn mọi lỗi từ một bộ test thành công.

## A. Căn cứ, kiến trúc và trạng thái thực tế

### Căn cứ và checkpoint

- Đọc trực tiếp hai PDF: `docs/2026-BTL.pdf` đủ 10 trang, đặc biệt mục 3.1-3.6 và
  bảng điểm ở trang PDF 10; `docs/Hướng dẫn BTL LTNC-2026.pdf` đủ 7 trang, đặc biệt
  tuần 9 về Checkstyle, tuần 10 về coverage >=60% và bảng điểm ở trang PDF 6.
  Trích xuất bằng pypdf từ PDF gốc; không lấy kết luận báo cáo cũ thay nội dung đề.
- Đối chiếu README, POM, MULTI_CLIENT_SETUP, AUDIT_AND_RESULTS, PROJECT_FILE_INVENTORY,
  CODE_STATISTICS, CLEANUP_REPORT với nguồn Java, FXML/CSS, test và workflow hiện tại.
  Các số liệu 15:58 và 16:20 trong tài liệu cũ là lịch sử, không phải kết quả lần này.
- HEAD trước sửa: `db199b48fdfbceb192c4faa21c16a20376f81dac`. Có sẵn nhiều file
  modified/untracked; giữ nguyên chúng và so thay đổi lần này với checkpoint.
- Checkpoint: `C:/Users/MAIDUC~1/AppData/Local/Temp/auction-review-checkpoint-buy41i8i`.
  `workspace.zip` giữ file tracked/untracked được Git liệt kê trước sửa;
  `before.diff`, `git-status.txt`, `manifest.json` và
  `manifest-with-data-hashes.json` giữ diff, trạng thái, hash nguồn và hash dữ liệu.
  ZIP ban đầu không chứa `data/` ignored. Sau đó có thêm bản sao snapshot phục vụ
  kiểm tra tương thích trong `compatibility/`; thư mục checkpoint này chứa dữ liệu
  riêng và không nên đưa lên Git hoặc chia sẻ công khai.
- Tất cả test ghi dữ liệu dùng `@TempDir`/thư mục thử riêng. Không chạy Server,
  AuctionService hay scheduler trên `data/state.json` thật. Kiểm tra tương thích
  dùng bản sao và chỉ gọi Repository.load + AuctionStateValidator.validate.

### Thành phần và luồng xử lý

```text
App/Main -> JavaFX FXML + Controller -> các interface service
    -> ServerServices -> JsonTransport -> TCP JSON
    -> Server.Client.readLoop -> AuctionService -> AuctionRepository -> state.json

Sau commit thành công:
AuctionService -> outbound queue của từng socket -> push event
    -> ServerServices observers -> ViewTasks -> JavaFX Application Thread
```

- Maven compile release **25**, JavaFX **26.0.2**, Gson **2.13.2**, JUnit **6.1.3**.
  Maven thực tế dùng JDK **25.0.4.1**, Maven **3.10.0**. `java` trên PATH là
  26.0.2.1; không nâng target dự án lên 26 vì khác biệt này.
- Server mặc định `0.0.0.0:8888`; singleton mặc định qua `Server.getInstance()`.
  Constructor public phục vụ instance riêng trong test. Acceptor riêng, reader/writer
  dùng virtual thread, tối đa 200 kết nối và queue gửi 256 thông điệp mỗi client.
  Secure UUID generator và PBKDF2 được warm-up trước khi start acceptor/báo ready;
  trên máy kiểm thử, cold warm-up mất khoảng 8,26 giây. Không giảm độ mạnh mật khẩu
  hoặc tăng timeout để che độ trễ của request đầu.
- Protocol: int 4 byte độ dài + JSON UTF-8, tối đa 8 MiB/frame;
  `protocolVersion`, `requestId`, `action`, `token`, `commandId`, `payload`.
  Reader client ghép phản hồi theo requestId; executor sự kiện riêng tránh callback
  observer chặn reader đang đợi phản hồi của callback đó.
- `AuctionService.execute()` và `tick()` cùng monitor. Giao dịch bao gồm trạng thái
  phiên, toàn bộ ví liên quan, bids, rules, transactions và receipt commandId.
  Save xong mới push. Không chia khóa từng phiên vì một ví tham gia nhiều phiên.
- SchemaVersion=1 giữ `users`, `auctions`, `rules`, `commands`, `transactions` và
  `version`. Server là nơi duy nhất truy cập snapshot nghiệp vụ. Client có sổ
  pending command và cache ảnh riêng theo host/port, không mở dữ liệu server.
- Repository giữ file lock độc quyền, kiểm tra JSON strict và khóa trùng, ghi tmp,
  `force(true)` rồi atomic replacement. Không hỗ trợ atomic move thì từ chối ghi;
  không fallback ghi đè thông thường, không tự reset hoặc lấy tmp làm snapshot.
- Validator mới kiểm tra nội dung và liên kết snapshot trước khi phục vụ request:
  kiểu tiền/boolean/thời gian, ID, tham chiếu user/auction, leader, lịch sử tăng giá,
  số bid, nghĩa vụ ví, trạng thái settlement, rules, receipt và giao dịch.
  Trường mở rộng không bị loại bỏ; không đổi schema hoặc migrate dữ liệu.
- Ví dùng `long` ở server, tiền giả lập. `available = balance - held`; held được
  suy ra từ các phiên chưa settled đang dẫn đầu. Outbid/hủy giải phóng hold;
  hết giờ trừ buyer, cộng seller và ghi giao dịch trong cùng snapshot, một lần.
- Scheduler 250 ms mở/đóng phiên. Khi có người thắng, FINISHED rồi PAID được xử lý
  trong cùng giao dịch; trạng thái bền vững thường là PAID, không có bước bấm trả
  tiền thật riêng. Phiên không có bid kết thúc FINISHED, không thanh toán.
- Auto-bid thật sắp rules theo `order`, xét max/increment/ngân sách, giới hạn 10.000
  vòng rồi rollback nếu vượt. Mock có PriorityQueue. Anti-sniping cộng 60 giây khi
  bid còn trên 0 đến 30 giây trước deadline; server quyết định thời gian.
- ViewTasks tách pool nghiệp vụ/ảnh, xử lý stale callback và dispose. Controller
  hủy timer, gỡ observers khi rời màn hình. Adapter không âm thầm fallback sang mock.

## B. Đối chiếu tiêu chí giảng viên

Quy ước: **PASS** = có hiện thực và bằng chứng trong phạm vi ghi ở cột test;
**PARTIAL** = có nhưng còn khoảng cách cụ thể; **FAIL** = lỗi/thiếu đã xác nhận chưa
sửa; **NOT TESTED** = chưa thực thi được phép kiểm chứng đó. PASS không có nghĩa
đã thử mọi tổ hợp đầu vào, mọi máy hoặc mọi tải. Đường dẫn rút gọn dưới đây tính
từ `src/main/java/com/auction/`; test tính từ `src/test/java/com/auction/`.

| Yêu cầu / căn cứ | File, method chứng minh | Trạng thái | Test / kiểm chứng | Thiếu sót, giới hạn |
| --- | --- | --- | --- | --- |
| Lớp chính Entity/User/Item/Auction/BidTransaction, đề 3.3 | `server/model/*`, `AuctionService.createUser/acceptBid`, `AuctionJsonMapper` | PASS | AuctionTest, ItemFactoryTest, AuctionJsonMapperTest | Auction backend chứa luật bid; state tổng thể còn là JSON trong service. |
| Abstract class | `Entity`, `User`, `Item`, `ItemFactory` | PASS | ItemFactoryTest và compile | Không tạo lớp rỗng chỉ để đủ tên. |
| Interface | `client/service/*Service`, `DisposableView` | PASS | ServiceRegistryTest, GUI/integration | Repository cụ thể implements AutoCloseable, không có interface DAO riêng. |
| Encapsulation | Thuộc tính private, getter; record bất biến Auction/BidTransaction | PASS | Domain tests và review source | DTO client có setter và double; nghiệp vụ authoritative dùng long. |
| Inheritance | User -> Bidder/Seller/Admin; Item -> Electronics/Art/Vehicle | PASS | ItemFactoryTest | Ba danh mục bổ sung dùng CategoryItem. |
| Polymorphism | `User.getRoleName`, `Item.getCategory`, override `ItemFactory.create` | PASS | `userRoleDispatchUsesTheRuntimeSubclass`, factory sáu danh mục | Không chỉ khai báo override mà có caller thật. |
| Abstraction | Abstract model + service interfaces tách socket/controller | PASS | Domain, registry, full-stack | Không tuyên bố đã đạt mọi nguyên lý SOLID. |
| Singleton, đề 3.6 | `Server.getInstance`, `MockDataStore.getInstance` | PARTIAL | Review synchronization và vòng đời | Server là default instance mỗi JVM; public constructor cho phép instance riêng, không phải Singleton GoF nghiêm ngặt. |
| Factory Method | `ItemFactory.forCategory/create` | PASS | ItemFactoryTest | Selector static là Simple Factory; abstract create/override là phần Factory Method tham số hóa. |
| Observer | `Server.publish`, `ServerServices.onEvent`, register/unregister listeners | PASS | Hai socket/adapter nhận push; observer lỗi không chặn observer khác | Mạng đứt cần reconcile; không bảo đảm delivery đúng một lần của mọi event. |
| Strategy/Command, tùy chọn trong đề 3.6 | AntiSnipingPolicy, handleAction, commandId receipts | PARTIAL | Review source và tests policy/idempotency | Policy cụ thể và switch action không phải hai GoF patterns hoàn chỉnh; commandId chống lặp không tự biến thành Command Pattern. Không bắt buộc bổ sung lớp hình thức. |
| MVC client, đề 3.4 | 8 FXML, controller, model, service interfaces | PASS | ClientUiTest, ServerUiIntegrationTest | Một số controller dài; không refactor hàng loạt trong lượt sửa lỗi. |
| Controller-Model-DAO server | `Server.Client.readLoop -> AuctionService -> AuctionRepository` | PASS | Service, repository, socket tests | Service vẫn điều phối JSON aggregate, domain chưa tách toàn bộ. |
| Client-Server; chỉ server truy cập dữ liệu | ServerServices/JsonTransport, Server/Repository | PASS | FullStackTest, ServerUiIntegrationTest; kiểm source/config | Client cache/pending không phải database nghiệp vụ. |
| Java Socket JSON | `JsonProtocol.readFrame/writeFrame`, `Server` | PASS | ServerIntegrationTest, FullStackTest | TCP chưa TLS; phạm vi LAN tin cậy. |
| Cổng TCP 8888 mặc định | `Server.getInstance/main`, launcher | PASS | JAR thật, hai kết nối loopback trên cổng mặc định 8888; push, disconnect, cold restart và replay pass | Test JUnit dùng port 0 để không đụng server đang chạy. |
| Đăng ký/đăng nhập/đăng xuất, đề 3.1.1 | `AuctionService.register/login`, `ServerServices.logout`, AuthController | PASS | AuctionServiceTest, FullStackTest, ClientUiTest | Session ở RAM; restart server cần đăng nhập lại. |
| Bidder/Seller/Admin và phân quyền | `requireRole`, `owner`, `authenticatedUser`, `Auction.requireBidder` | PASS | Tests quyền, own product, admin lock, GUI route | Seller được bid sản phẩm người khác; admin không dùng ví/bid. Chưa click mọi thao tác admin qua GUI. |
| CRUD sản phẩm, đề 3.1.2 | `createAuction/updateAuction/editable`, SellerProductsController | PASS | CRUD service + upload ảnh full-stack + form JavaFX thật | Chỉ sửa/xóa OPEN chưa bid, giữ lịch sử đã diễn ra. |
| Tên/mô tả/giá/lịch/ảnh | `validateProduct/image`, ServerServices, ProductImages | PASS | Form limits, invalid images, upload/download/reuse/change cache | Chưa thử mọi định dạng/driver ảnh hoặc DPI Windows. |
| Bid hợp lệ, bước giá, leader, đề 3.1.3 | `placeBid/acceptBid`, `Auction.requireMinimumBid/requireBidder` | PASS | First bid, giá phân số/thấp, phiên chưa mở/đã đóng, own product | Giá đầu tiên phải >= giá khởi điểm + bước giá. |
| Tự đóng và xác định winner, đề 3.1.4 | `tick/refreshAuctions/settle` | PASS | MutableClock, no-bid finish, paid restart, cancel | Không có thanh toán ngân hàng; FINISHED -> PAID nguyên tử với ví giả lập. |
| Xử lý lỗi/ngoại lệ, đề 3.1.5 | ApiException, ServerError, rollback, ViewTasks | PASS | Malformed frame, unavailable/reconnect, invalid bid/image, failure rollback | Windows Preferences bị sandbox chặn được log và bắt lỗi. |
| JavaFX GUI, đề 3.1.6 | `client/controller`, `resources/.../views` | PASS | 14 ClientUiTest + 1 ServerUiIntegrationTest, ảnh QA | Native Alt+Tab sang ứng dụng khác, nhiều màn hình/DPI vẫn cần manual. |
| Concurrency, đề 3.2.2 | Synchronized service; virtual threads; queue độc lập | PASS | Hai bid bằng nhau chỉ một thành công; hai phiên không overspend cùng ví; client lỗi không dừng client khác | Chưa benchmark 200 client hoặc chứng minh bằng model checking. |
| Realtime không polling giá, đề 3.2.4 | SUBSCRIBE_FEED/AUCTION, push, observers | PASS | Socket/adapter/GUI rival bid cập nhật | Heartbeat là kiểm tra kết nối; timer đồng hồ không phải polling giá liên tục. |
| Auto-bidding, đề 3.2.1 | `setAuto/processAutoBids`, Mock AutoBidEngine | PASS | Trần/bước/budget/thứ tự; hồi quy ngân sách thay đổi | Ưu tiên luật cũ ở trần chỉ khi còn đủ tiền cho trần đó. |
| PriorityQueue trong mục nâng cao bảng điểm | MockDataStore/AutoBidEngine; server dùng sorted rules | PARTIAL | MockAutoBidServiceTest và review thuật toán thật | Nếu giảng viên yêu cầu PriorityQueue trên backend thật thì hiện chưa đáp ứng chi tiết này. |
| Anti-sniping, đề 3.2.3 | AntiSnipingPolicy + acceptBid | PASS | Biên 30 giây, đúng deadline, sau deadline, thời điểm đóng cũ | Dùng Clock của server, không đồng hồ client. |
| Price curve, đề 3.2.5 | AuctionRoomController chart/history/observer | PASS | Chart cập nhật sau bid, ảnh server-room | Hiển thị tối đa 30 điểm; lịch sử đầy đủ lấy riêng. |
| Ví, hold, thanh toán giả lập | `wallet/heldBalance/transfer/settle/log` | PASS | Hold/release/withdraw, cross-auction budget, restart, rollback | Không phải ví tiền thật; không giữ toàn bộ max auto-bid trước. |
| Admin tài khoản/phiên | `manageUser/cancel/DELETE_AUCTION`, AdminController | PASS | Service quyền/lock + adapter cancel/archive; mock management | Tương tác GUI đổi role/xóa tài khoản chưa được tự động hóa đầy đủ. |
| Reconnect/timeout/commandId | JsonTransport, ServerServices pending/replay, BidSubmissionTracker | PASS | Mất ACK, restart client, restart server, unknown UI không gửi lặp | Pending phụ thuộc thư mục client, host/port; session cần login lại sau restart. |
| File I/O/serialization, hướng dẫn tuần 9 | Repository JSON snapshot, validator | PASS | RepositoryTest, StateValidationTest, schema v1 copy | Dùng JSON serialization, không dùng Java ObjectOutputStream hoặc MySQL. |
| Maven, đề 3.5 | pom.xml release/dependencies/plugins | PASS | Clean rồi verify; shaded JAR | Mạng lần đầu cần tải dependencies; lượt này dùng cache offline. |
| JUnit logic quan trọng | 27 test classes | PASS | 195 test, 0 failure, 0 error, 0 skipped ở lượt cuối | Không dùng Mockito; fixture thật, Clock và TempDir. |
| JaCoCo >=60%, hướng dẫn tuần 10 | pom prepare-agent/report, root counters | PASS | 4893/6292 = 77.77% line toàn dự án | Không loại UI/mock để nâng tỷ lệ; không có build gate coverage tối thiểu trong POM. |
| Checkstyle tích hợp Maven, hướng dẫn tuần 9 | `config/checkstyle.xml`, verify/check | PASS | 0 violation, main + test | Chỉ baseline whitespace. |
| Google Java Style Guide, đề 3.5 | Baseline hiện có, source wildcard imports/thụt 4 spaces | PARTIAL | Kiểm source và cấu hình | Chưa enforce toàn bộ Google Style. Không reformat toàn repo để làm diff lớn trong lượt sửa lỗi. |
| Git thường xuyên | Local git log có nhiều commit/merge | PARTIAL | Đọc lịch sử, HEAD và diff | Không xác minh tiến độ theo tuần/đóng góp từng người trên remote; không commit/push thay người dùng. |
| GitHub Actions + JUnit | `.github/workflows/ci.yml`, Java 25, Xvfb, verify, upload reports | PARTIAL | Review cấu hình; cùng lifecycle chạy local | Workflow trên GitHub-hosted runner: NOT TESTED cho working tree chưa commit này. |
| Demo nhiều máy Windows | MULTI_CLIENT_SETUP, kịch bản H | NOT TESTED | MANUAL TEST REQUIRED | Local TCP/JavaFX không thay thế LAN hai máy vật lý. |
| Mỗi thành viên hiểu toàn bộ code | `docs/viva/*`, source hiện hành | NOT TESTED | MANUAL VIVA REQUIRED | Công cụ không xác nhận được hiểu biết/đóng góp của thành viên. |

## C. Lỗi phát hiện, nguyên nhân và sửa chữa

| ID / mức độ | Bằng chứng và nguyên nhân | File/method | Cách sửa | Kết quả |
| --- | --- | --- | --- | --- |
| R1 - HIGH | Chỉ validate collection cấp root; user có số dư âm/tiền dạng string, boolean sai, tham chiếu thiếu, bidCount sai, rules/commands/transactions rỗng sai cấu trúc vẫn mở được. 13 ca corruption đều không ném IOException trên mã cũ. | AuctionService constructor/validateState | Tách AuctionStateValidator, kiểm kiểu/nội dung/tham chiếu/financial invariants trước phục vụ; lỗi đóng khóa và giữ nguyên file. | 20 ca corruption mới + 7 ca thiếu root pass; restart dữ liệu hợp lệ và bản sao schema v1 pass. |
| R2 - HIGH | Gson tree giữ giá trị sau cùng của khóa trùng; 2 fixture duplicate key được load thành công trên mã cũ, có thể che mất dữ liệu trước khi save. | AuctionRepository.load | Duyệt JsonReader strict phát hiện tên trùng ở từng object trước khi dựng tree. | 12 RepositoryTest pass; file sai giữ nguyên bytes và không cho save. |
| R3 - MEDIUM | Luật auto-bid đăng ký sớm với cùng max vẫn chặn đối thủ ở trần dù ví đã giữ tiền tại phiên khác và không đủ ngân sách. Ca tái hiện: max 5 triệu, giữ 98 triệu ở phiên khác; đối thủ có tiền không lên được 5 triệu. | AuctionService.processAutoBids, Mock AutoBidEngine.process | Chỉ ưu tiên luật trước ở trần nếu availableForAuction đủ max; giữ thứ tự ưu tiên khi cả hai còn đủ tiền. | Hồi quy thật và mock pass; test thứ tự cũ, no self-outbid, giới hạn/budget/concurrency tiếp tục pass. |
| R4 - MEDIUM | Form cho bấm Lưu với tên >200, mô tả >10.000, thời lượng >365 ngày, sau đó server từ chối; ServerError che mọi nguyên nhân bằng cùng thông báo chung. Ca tên quá dài fail trên GUI cũ. | SellerProductsController.form, AuctionTime.valid, ServerError | Đồng bộ giới hạn form với server, thông báo trường lỗi; dịch chi tiết lỗi tên/mô tả/thời gian/ảnh đã biết. Server vẫn validate độc lập. | GUI limits, API rejection không thêm sản phẩm, và tạo sản phẩm từ form JavaFX qua TCP thật pass. |
| R5 - MEDIUM | Popup autoHide=false, chỉ lọc click trong Scene; thiếu observer owner focus/showing/scene detach. Ca gỡ header khỏi Scene trên mã cũ timeout vì popup vẫn hiện. | UserMenu constructor/openMenu/removeSceneHandlers | Bật native auto-hide, theo dõi focus owner + popup, hide ngay khi owner ẩn/header rời Scene; lưu Scene gốc để gỡ handlers, dừng animation cũ và chỉ giữ một menu active. | 2 GUI tests mới pass: detach, cửa sổ Stage khác lấy focus, Esc, owner hide và gỡ tham chiếu. Alt+Tab ứng dụng khác vẫn cần manual trên máy đích. |
| R6 - LOW | Validators.password chấp nhận 129 ký tự trong khi server giới hạn 128; test biên thất bại trên mã cũ. | Validators.password, AuthController/FXML | Thêm upper bound 128; hint/error dùng cùng giới hạn. | Test 128 hợp lệ/129 bị chặn pass. |
| R7 - MEDIUM (rủi ro từ source, chưa tái hiện sai số) | Hai DecimalFormat static mutable được dùng chung cho UI và service nền. Tài liệu DecimalFormat trong JDK 25 src.zip yêu cầu đồng bộ khi chia sẻ giữa thread. Stress test cũ chạy pass, không coi đó là chứng minh thread-safe. | FormatUtil formatCurrency/formatCompactPriceAxis | Đồng bộ ba method dùng chung hai formatter trên cùng monitor lớp; không đổi output. | Test tám thread x 2.000 vòng, bốn định dạng/vòng và test số long giữ đủ chữ số pass. |
| R8 - MEDIUM | getImagePath tạo UUID file mới và đăng ký deleteOnExit mỗi lần refresh, kể cả bytes không đổi; số file và registry deleteOnExit tăng theo lượt tải. | ServerServices.getImagePath | Cache bằng SHA-256 nội dung, publish file bằng tmp + atomic move; reuse cùng bytes, không đăng ký deleteOnExit theo mỗi request. | Full-stack kiểm path lặp không đổi, ảnh mới có bytes/path mới và cache client độc lập pass. |
| R9 - LOW | Nội dung ví/hướng dẫn dùng từ “mock” cho cả chế độ server, mô tả sai thành phần thực hiện thanh toán. | wallet-view.fxml, GuideController | Đổi thành “hệ thống”, giữ ý nghĩa ví giả lập. | FXML/route/GUI wallet/guide tiếp tục load trong suite cuối. |
| R10 - LOW | Ảnh QA 640 px cho thấy nút Đăng nhập bị rút thành “Đăng n…”. Account button được thêm sau vòng đặt minWidth cho các nút header. | AppHeader.refreshAccount | Đặt minWidth theo preferred width cho nút Đăng nhập vừa tạo. | Thêm assertion ở bài responsive home; kết quả cuối và ảnh mới được kiểm lại. |
| R11 - MEDIUM | JAR restart trong JVM mới: login đầu mất 8,14 giây, vượt timeout mặc định 8 giây. Script 15 giây đi hết luồng nhưng script dùng đúng 8 giây tái hiện TimeoutError ở login sau restart. Probe JVM mới tách được PBKDF2 129 ms và lần đầu UUID.randomUUID 8.014 ms; test restart cùng JVM bỏ sót khởi tạo bộ sinh token ngẫu nhiên. | AuctionService.login/id/constructor, Passwords.warmUp | Warm-up UUID secure generator và PBKDF2 trước khi server báo ready/accept requests; giữ PBKDF2 120.000 vòng, salt/hash và timeout client. Giá trị kiểm tra không phải credential và không lưu vào JSON. | Kiểm lại bằng JAR cold restart và timeout đúng 8 giây; kết quả ở E. |
| R12 - MEDIUM | Timeout cũ chỉ bắt đầu ở future.get sau khi writeFrame xong. Peer nhận TCP nhưng không đọc body 6 MiB làm writer bị chặn, không tuân timeout 300 ms; test cũ vẫn chưa kết thúc sau 4 giây. | JsonTransport.call/close | Deadline chạy độc lập bao phủ gửi và chờ phản hồi; đóng socket khi hết hạn để giải phóng write, hủy timer khi hoàn tất và shutdown khi đóng transport. Không tự gửi lại lệnh với commandId mới. | Hồi quy stalled reader và các test mất ACK/reconnect/cold JVM chạy lại. |

Thông báo Seller cũ chưa có log/đầu vào cụ thể nên không thể khẳng định R4 là mọi
nguyên nhân từng xảy ra. Đã chứng minh và sửa các đường lỗi hiện tại nêu trên;
thời gian bắt đầu đã qua vẫn phải bị từ chối theo nghiệp vụ. Checkbox “Bắt đầu ngay”
hiện chọn phút kế tiếp, không phải bắt đầu tức thời ở giây bấm; nếu để form quá
lâu cần chọn lại lịch hợp lệ. Không nới server validation để chấp nhận lịch quá hạn.

## D. Toàn bộ file thay đổi trong lượt này

**28 file source/test/resource khác checkpoint và 1 báo cáo mới.** Đây là phạm vi
lần sửa hiện tại; `git diff HEAD` còn chứa thay đổi có sẵn trước đó. Không chỉnh POM,
CI, README, launcher, tài liệu cũ, dữ liệu thật hoặc dependency production.

| Đường dẫn | Nội dung / lý do | Ảnh hưởng |
| --- | --- | --- |
| `src/main/java/com/auction/server/service/AuctionStateValidator.java` (mới) | Validate sâu schema v1 và bất biến tài chính R1. | Từ chối startup với dữ liệu hỏng; không rewrite snapshot hoặc bỏ trường mở rộng. |
| `src/main/java/com/auction/server/service/AuctionService.java` | Gọi validator; điều kiện budget khi tie auto-bid; warm-up crypto trước phục vụ R1/R3/R11. | Giữ transaction/monitor/save-before-push và định dạng API. |
| `src/main/java/com/auction/server/service/Passwords.java` | Warm-up secure UUID generator và PBKDF2 trước khi ready R11. | Không giảm iteration/đổi hash; chuyển chi phí cold initialization sang startup. |
| `src/main/java/com/auction/server/dao/AuctionRepository.java` | Reader strict từ chối duplicate keys R2. | Chỉ thay đường load; lock/force/atomic save giữ nguyên. |
| `src/main/java/com/auction/client/service/mock/AutoBidEngine.java` | Xét budget của luật ưu tiên ở trần R3. | Mock thể hiện cùng quy tắc tie, không thay persistence thật. |
| `src/main/java/com/auction/client/controller/SellerProductsController.java` | Name/description limits, date duration và kiểm lại trước save R4. | Form chặn đầu vào server chắc chắn từ chối. |
| `src/main/java/com/auction/client/util/AuctionTime.java` | Bound 365 ngày R4. | Đồng bộ form với validateProduct. |
| `src/main/java/com/auction/client/service/server/ServerError.java` | Thông báo tiếng Việt theo field/ảnh R4. | Không hiển thị raw lỗi server tùy ý, không thay errorCode. |
| `src/main/java/com/auction/client/ui/UserMenu.java` | Auto-hide, focus/lifecycle/listener/animation R5. | Đóng popup khi chuyển focus, ẩn owner hoặc rời Scene. |
| `src/main/java/com/auction/client/ui/AppHeader.java` | Giữ đủ chiều rộng nút Đăng nhập R10. | Không cắt nhãn nút ở cửa sổ 640 px; giữ bố cục header. |
| `src/main/java/com/auction/client/util/Validators.java` | Upper bound mật khẩu R6. | Client không nhận mật khẩu server chắc chắn từ chối. |
| `src/main/java/com/auction/client/controller/AuthController.java` | Lời nhắc 8-128 ký tự R6. | Thay nội dung lỗi, không đổi auth flow. |
| `src/main/resources/com/auction/client/views/auth-view.fxml` | Hint 8-128 ký tự R6. | Giữ layout/FXML bindings. |
| `src/main/java/com/auction/client/util/FormatUtil.java` | Đồng bộ DecimalFormat R7. | Giữ chính xác format long/double; chi phí khóa ngắn khi format. |
| `src/main/java/com/auction/client/service/server/ServerServices.java` | Cache ảnh theo content hash, atomic publication R8. | Ảnh mới vẫn nạp; cùng bytes không tạo file/cache-registration mới. |
| `src/main/java/com/auction/client/service/server/JsonTransport.java` | Deadline độc lập bao phủ cả write bị chặn R12. | Socket timeout giải phóng worker; timer cancel/remove, executor đóng cùng transport; giữ correlation và pending command. |
| `src/main/java/com/auction/client/controller/GuideController.java` | Sửa mô tả settlement R9. | Chỉ text. |
| `src/main/resources/com/auction/client/views/wallet-view.fxml` | Sửa mô tả settlement R9. | Chỉ text, giữ ví giả lập. |
| `src/test/java/com/auction/server/service/AuctionStateValidationTest.java` | Thêm 13 nested + 7 financial corruption cases. | TempDir; assert IOException/bytes/tmp/lock, giữ 7 root cases. |
| `src/test/java/com/auction/server/dao/AuctionRepositoryTest.java` | Thêm hai duplicate-key cases. | Không giảm kiểm chứng save/restart/rollback cũ. |
| `src/test/java/com/auction/server/service/AuctionServiceTest.java` | Auto-bid cùng trần khi ví đổi budget. | Hai phiên, hold và tiền khả dụng được kiểm tra. |
| `src/test/java/com/auction/client/service/MockAutoBidServiceTest.java` | Hồi quy tie/budget cho mock. | Giữ test ưu tiên thời gian khi đủ tiền. |
| `src/test/java/com/auction/client/controller/ClientUiTest.java` | Limits form Seller + hai popup lifecycle/focus tests + kiểm độ rộng nút Đăng nhập. | 12 -> 14 test; dùng toolkit thật, không mô phỏng kết quả bằng mock popup. |
| `src/test/java/com/auction/client/util/AuctionTimeTest.java` | Biên 365 ngày và vượt một giây. | Giữ start/end/parse tests cũ. |
| `src/test/java/com/auction/client/util/ValidatorsTest.java` | Password 128/129. | Bổ sung một method, không bỏ case email/password cũ. |
| `src/test/java/com/auction/client/util/FormatUtilTest.java` | Tám thread format tiền/chart đồng thời. | Kiểm đúng chuỗi, không chỉ kiểm không ném exception. |
| `src/test/java/com/auction/integration/FullStackTest.java` | Lỗi field cụ thể + cache reuse/change/client isolation + cold server JVM login + stalled socket writer. | 8 -> 11 test; TCP thật, TempDir, giữ mất ACK/restart/pending; JVM con và socket lỗi được đóng trong finally/try-with-resources. |
| `src/test/java/com/auction/integration/ServerUiIntegrationTest.java` | Điền form Seller và lưu qua adapter/server thật. | Kiểm kết quả trên bảng và backend; không chỉ load FXML. |
| `docs/FINAL_PROJECT_REVIEW.md` (mới) | Báo cáo tổng hợp hiện tại. | Tài liệu lịch sử giữ nguyên mốc; không nhân bản báo cáo mới sang file khác. |

Không xóa test, bỏ assertion cũ, exclude code khỏi coverage hoặc giảm quy tắc chất
lượng để build xanh. Không xóa source/file nghiệp vụ; Maven clean chỉ tái tạo target.

## E. Kiểm thử và bằng chứng

### Các lượt đã thực hiện

| Lượt | Kết quả | Ý nghĩa |
| --- | --- | --- |
| Baseline trước sửa | BUILD SUCCESS, 164 test, 0 failure/error/skipped | Trạng thái ban đầu chạy được nhưng thiếu ca cho lỗi mới. Không dùng coverage baseline vì target chưa clean. |
| Hồi quy trên mã cũ | 24 test, 16 failures, 2 errors | Chứng minh JSON duplicate/nested, auto budget, Seller limits, popup detach. Hai GUI errors là assertion/timeout của ca hồi quy. |
| Sau nhóm sửa đầu | 66 test, 0 failure/error/skipped | Repository/service/schema/form/popup detach. |
| Password + format trước sửa bổ sung | 46 test, 1 failure | Password 129 ký tự được chấp nhận sai; stress DecimalFormat chưa tái hiện lỗi. |
| Lượt mở rộng | 103 test, 0 failure/error/skipped | Focus/Esc/owner hide, financial corruption, JavaFX form qua TCP, mock tie và auth boundary. |
| `clean verify` với tmp ở ổ C | BUILD FAILURE, 193 test, 0 failure, 73 errors | JUnit cleanup TempDir gặp AccessDeniedException/getRealPath do sandbox; không lấy lượt này làm pass. |
| Clean/verify trước sửa nhãn nút qua ảnh QA | BUILD SUCCESS, 193 test, 0 failure/error/skipped | Sau đó thấy nhãn bị cắt ở ảnh 640 px nên sửa R10 và chạy sạch lại. |
| Clean/verify trước cold-crypto fix | BUILD SUCCESS, 193 test, 0 failure/error/skipped, line 77,68% | Sau đó process-level smoke test phát hiện R11 nên sửa và chạy sạch lại. |
| JAR cold login trước warm-up | Script 5 giây timeout; script 15 giây đo 8,14 giây; script đúng timeout client 8 giây cũng timeout | Không đánh đồng TCP bind thành readiness; đã đổi script chờ log ready, vẫn tái hiện cold login chậm khi chưa warm-up. |
| Probe JVM mới sau warm-up đầy đủ | Startup 8.262 ms; UUID tiếp theo 0 ms; hash + verify 273 ms | Chi phí khởi tạo chuyển sang startup; thông tin kiểm tra không được lưu vào snapshot. |
| Hồi quy JUnit cho cold JVM | PASS, 1 test riêng, 0 failure/error/skipped, BUILD SUCCESS | `FullStackTest.coldServerJvmCanAuthenticateWithinTheNormalClientTimeout` khởi động JVM con bằng snapshot fixture đã có, đợi ready rồi login đầu bằng timeout mặc định; child process được đóng cả khi assertion thất bại. |
| Stalled socket writer trước sửa | FAIL, test không kết thúc trong 4 giây với request timeout 300 ms | Peer cục bộ cố ý không đọc request; không dùng dữ liệu thật. |
| Stalled writer + cold JVM sau sửa | PASS, 2 test, 0 failure/error/skipped, BUILD SUCCESS | `socket-cold-regression-after.log`; deadline giải phóng worker và cold login dùng timeout bình thường. |
| Clean rồi verify với tmp trong workspace | **BUILD SUCCESS; 195 test, 0 failure, 0 error, 0 skipped; Checkstyle 0 vi phạm** | Lượt kết luận, log và JaCoCo mới sau clean. |
| Snapshot schema v1 hiện có trên bản sao | PASS, bytes không đổi | Repository + validator, không khởi động service/scheduler trên dữ liệu thật. |
| Shaded JAR / port mặc định | **PASS**, port 8888, hai client, push, ngắt một client, restart tiến trình, login lại, giữ hai bid và replay không tạo bid trùng. Timeout script đúng 8 giây; ba login lần lượt [0.047, 0.031, 0.062] giây | Chỉ dữ liệu fixture riêng. |
| Dữ liệu thật | **6/6 file data/backup giữ nguyên SHA-256**; state.json 195,143 bytes, SHA-256 `6c00dfceb67a9f9b114412d3e4eb04147ad26e022186f382e234d53cc1e56c15` | So SHA-256 toàn bộ file data đã ghi nhận trước sửa. |

Lệnh thành công cuối trong sandbox (tách clean để tạo lại thư mục tmp sau khi
target bị xóa; tương đương chạy verify trên build sạch):

```powershell
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' clean
New-Item -ItemType Directory -Force -Path target/test-tmp
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' `
  '-DargLine=-Djava.io.tmpdir=D:\Bai-Tap-Lon-gon\target\test-tmp' `
  '-Dauction.ui.tests=true' verify
```

Ngoài sandbox, lệnh thông thường là `mvn -B clean verify -Dauction.ui.tests=true`
trên Windows có desktop; Linux dùng Xvfb như workflow. Không truyền tmp dưới target
trong cùng lệnh clean nếu thư mục đó chưa được lifecycle tạo lại trước test.

Raw logs nằm ngoài target để clean không xóa/khóa log:
`baseline-verify.log`, `regression-before.log`, `regression-after.log`,
`format-validation-before.log`, `extended-regression.log`, `final-clean-verify.log`
(lượt tmp lỗi), `pre-visual-fix-verify.log`, `pre-crypto-warmup-verify.log`,
`jar-cold-before-warmup.log`, `jar-smoke-before-warmup-15s.json`,
`cold-crypto-diagnosis.json`, `cold-warmup-after.log`, `pbkdf-only-warmup-verify.log`,
`cold-jvm-regression.log`, `jar-smoke-server.log`, `jar-smoke-result.json`,
`uuid-warmup-before-permanent-regression.log`, `before-stalled-writer-fix-verify.log`,
`socket-write-timeout-before.log`, `socket-cold-regression-after.log`,
`final-clean.log`, `final-verify.log`
trong checkpoint nêu ở A.

Báo cáo cuối có thể mở tại `target/surefire-reports/`,
`target/site/jacoco/index.html`, `target/site/jacoco/jacoco.xml`,
`target/checkstyle-result.xml`. JAR: `target/digital-auction-1.0.0.jar`.

### Kết quả JUnit cuối theo suite

| Suite | Tests | Failures | Errors | Skipped | Giây |
| --- | --- | --- | --- | --- | --- |
| `com.auction.client.controller.ClientUiTest` | 14 | 0 | 0 | 0 | 20.44 |
| `com.auction.client.navigation.ImageTasksTest` | 5 | 0 | 0 | 0 | 0.102 |
| `com.auction.client.navigation.ViewTasksTest` | 4 | 0 | 0 | 0 | 0.087 |
| `com.auction.client.service.BidSubmissionTrackerTest` | 4 | 0 | 0 | 0 | 0.085 |
| `com.auction.client.service.MockAuctionBidAccessTest` | 1 | 0 | 0 | 0 | 8.175 |
| `com.auction.client.service.MockAuthIntegrationTest` | 2 | 0 | 0 | 0 | 8.232 |
| `com.auction.client.service.MockAutoBidServiceTest` | 6 | 0 | 0 | 0 | 8.255 |
| `com.auction.client.service.MockManagementServiceTest` | 3 | 0 | 0 | 0 | 8.169 |
| `com.auction.client.service.MockSharedStoreTest` | 2 | 0 | 0 | 0 | 8.155 |
| `com.auction.client.service.MockWalletServiceTest` | 4 | 0 | 0 | 0 | 8.199 |
| `com.auction.client.service.ServiceRegistryTest` | 2 | 0 | 0 | 0 | 8.3 |
| `com.auction.client.ui.ProductImagesIoTest` | 2 | 0 | 0 | 0 | 8.412 |
| `com.auction.client.ui.ProductImagesTest` | 1 | 0 | 0 | 0 | 8.372 |
| `com.auction.client.ui.UiResourcesTest` | 1 | 0 | 0 | 0 | 0.07 |
| `com.auction.client.util.AuctionTimeTest` | 3 | 0 | 0 | 0 | 0.088 |
| `com.auction.client.util.FormatUtilTest` | 2 | 0 | 0 | 0 | 0.224 |
| `com.auction.client.util.ValidatorsTest` | 44 | 0 | 0 | 0 | 0.282 |
| `com.auction.integration.FullStackTest` | 11 | 0 | 0 | 0 | 23.421 |
| `com.auction.integration.ServerUiIntegrationTest` | 1 | 0 | 0 | 0 | 17.023 |
| `com.auction.server.dao.AuctionJsonMapperTest` | 3 | 0 | 0 | 0 | 0.208 |
| `com.auction.server.dao.AuctionRepositoryTest` | 12 | 0 | 0 | 0 | 9.147 |
| `com.auction.server.model.AuctionTest` | 7 | 0 | 0 | 0 | 0.16 |
| `com.auction.server.model.ItemFactoryTest` | 8 | 0 | 0 | 0 | 0.157 |
| `com.auction.server.network.ServerIntegrationTest` | 3 | 0 | 0 | 0 | 9.735 |
| `com.auction.server.service.AntiSnipingPolicyTest` | 5 | 0 | 0 | 0 | 0.095 |
| `com.auction.server.service.AuctionServiceTest` | 18 | 0 | 0 | 0 | 17.4 |
| `com.auction.server.service.AuctionStateValidationTest` | 27 | 0 | 0 | 0 | 14.378 |
| **Tổng** | **195** | **0** | **0** | **0** | 27 suite |

### Coverage và coding checks

| Chỉ số JaCoCo toàn dự án | Covered / total | Tỷ lệ |
| --- | --- | --- |
| LINE | 4893 / 6292 | 77.77% |
| BRANCH | 1687 / 2653 | 63.59% |
| INSTRUCTION | 24336 / 30114 | 80.81% |
| METHOD | 809 / 1079 | 74.98% |
| CLASS | 103 / 107 | 96.26% |
| COMPLEXITY | 1407 / 2436 | 57.76% |

Checkstyle: **0 vi phạm**, 107 Java files (80 main, 27 test).

Counter lấy ở cấp root của jacoco.xml lượt sạch, không cộng số từ báo cáo cũ và
không loại package UI/mock. Checkstyle kiểm UTF-8, tab/trailing whitespace/newline
trong source main/test; **0 violation theo baseline không chứng minh Google Style**.

GUI: 14 ca ClientUiTest và 1 ca ServerUiIntegrationTest chạy với toolkit JavaFX
Windows, có cả responsive width 640/1200, chart/history, loading/failure, unknown
bid không gửi lại, navigation/dispose, form Seller và menu popup. Ảnh QA ở
`target/qa/`; kết quả xem ảnh: đã xem server-room, room-narrow, server-seller, server-admin, auth-register-scrolled-640, wallet-long-640 và home-eight-running-narrow. Nút Đăng nhập ở ảnh 640 px sau R10 hiển thị đủ nhãn; tiền long giữ đầy đủ chữ số; Seller có thanh cuộn ngang để truy cập cột thao tác. Đây là ảnh fixture, không phải dữ liệu thật.

Socket: service tests dùng latch/virtual thread để tạo cạnh tranh; socket tests
dùng server thật trên loopback. FullStack có proxy cố ý làm mất ACK đầu để kiểm tra
retry cùng commandId chỉ tạo một bid. JSON tests cố ý gây lỗi tmp để chứng minh
rollback tiền/bid/receipt và không phát event thành công trước save.

Các cảnh báo đã phân loại: Windows Preferences bị sandbox chặn, API deprecated,
JavaFX native access/unnamed module, tài nguyên trùng khi shade; fixture thiếu ảnh,
observer cố ý ném lỗi và commit cố ý thất bại có log mong đợi. Không sửa test để
ẩn các cảnh báo đó. `git diff --check` pass; đã rà diff so checkpoint và kiểm đủ file của lượt này có trong phần D. ZIP checkpoint kiểm CRC pass; các thay đổi POM/CI/README/launcher có sẵn giữ nguyên bytes.

## F. Giới hạn, việc chưa thể xác minh

Rà chất lượng không phát hiện lý do đủ mạnh để xóa các nhóm source hiện có:
entry points qua launcher/POM, controller qua FXML, interfaces có adapter/mock,
tests có discovery và tài nguyên có caller. Các catch bỏ qua IOException khi đóng
socket là cleanup best-effort; lỗi nghiệp vụ/persistence có errorCode, rollback hoặc
log, không được coi là thành công âm thầm. Chưa dùng bộ phân tích call graph đầy đủ
hoặc detector duplication ngữ nghĩa, nên không tuyên bố không còn code chết/trùng.
Năm dependency trực tiếp đều có caller; javafx-swing hiện chủ yếu phục vụ screenshot
test nhưng scope compile vẫn rộng hơn nhu cầu quan sát được, giữ nguyên packaging
đang chạy thay vì đổi dependency trong lượt sửa nghiệp vụ. Credential cố định của
mock/demo không được dùng làm mật khẩu mặc định cho dữ liệu thật mới; launcher thật
tắt Demo và yêu cầu AUCTION_ADMIN_PASSWORD khi bootstrap.

1. **MANUAL TEST REQUIRED:** LAN/Wi-Fi trên hai máy Windows thật, firewall/AP isolation,
   IP DHCP và ngắt mạng vật lý. Loopback nhiều socket/adapter không thay thế việc này.
2. **NOT TESTED:** workflow trên GitHub-hosted runner với đúng working tree mới.
   Không commit/push hoặc trigger triển khai để vượt yêu cầu người dùng.
3. **MANUAL TEST REQUIRED:** Alt+Tab sang ứng dụng khác như trình duyệt, minimize,
   DPI 125%/150%, nhiều màn hình. Automated popup focus test dùng Stage khác trong
   cùng tiến trình, dù có sử dụng native JavaFX focus thực.
4. **NOT TESTED:** mất điện/hỏng đĩa thật, filesystem không hỗ trợ atomic move,
   durability metadata thư mục sau mất điện. Test lỗi ghi có chủ đích không chứng
   minh mọi kiểu lỗi phần cứng. File tmp không được tự coi là bản khôi phục hợp lệ.
5. Chưa benchmark tải lớn, slow client 200 kết nối hoặc snapshot/bid history gần
   giới hạn frame 8 MiB. Global monitor + deep-copy/full JSON snapshot là lựa chọn
   nhất quán cho bài tập nhỏ, có giới hạn hiệu năng và kích thước dữ liệu.
6. Google Java Style chưa được enforce đầy đủ; POM không có coverage gate bắt buộc.
   Server default Singleton và PriorityQueue backend cần được giải thích đúng như B.
7. TCP chưa TLS, không phải hệ thống giao dịch tiền thật; auth session ở RAM.
   Mock có fixture/password và các đơn giản hóa riêng, không dùng để chứng minh
   phân quyền hoặc persistence production. Server demo mặc định tắt ở launcher thật.
8. Controller/service còn dài; không có detector duplication hoặc memory profiler
   toàn hệ thống. Đã sửa vòng đời popup, formatter chia sẻ và cache ảnh có tác động
   cụ thể, không tuyên bố đã chứng minh không còn mọi leak/race/deadlock.
9. Cache ảnh theo nội dung giữ được các phiên bản ảnh đã tải trong thư mục cache;
   chưa có chính sách eviction theo dung lượng. Thay đổi ảnh có thể tạo phiên bản
   mới; tải lại cùng ảnh không tăng số file theo từng refresh.
10. Checkbox lịch Seller “Bắt đầu ngay” chọn phút kế tiếp như mô tả C; kiểm tra cuối
    và server sẽ từ chối lịch đã quá hạn. Đây là chi tiết UX còn cần giải thích.
11. Chưa kiểm thử từng nút admin role/delete bằng thao tác GUI native. Handler,
    server role checks và các test service/adapter là bằng chứng hiện có; kịch bản
    manual bên dưới bao gồm các thao tác đó.

## G. Mức độ sẵn sàng bảo vệ

**Sẵn sàng demo local có điều kiện**, sau khi đối chiếu kết quả cuối ở E: có luồng
đăng nhập -> Seller CRUD -> nhiều bidder cạnh tranh -> push/chart -> hold/settlement
-> restart, và ca regression cho lỗi dữ liệu/UI đã phát hiện. Không thấy yêu cầu
chức năng cốt lõi nào hoàn toàn vắng mặt trong nguồn hiện tại. Còn các tiêu chí
PARTIAL và NOT TESTED trong B/F; không kết luận đạt tối đa hoặc bảo đảm demo LAN
chưa được thử sẽ thành công.

Trước bảo vệ cần hoàn thành H, kiểm tra GitHub Actions cho phiên bản nhóm chọn nộp,
và để từng thành viên giải thích được:

- Vì sao khóa service phải bao phủ nhiều phiên dùng cùng ví; tại sao event sau save.
- Vì sao commandId khác requestId và replay receipt không tạo bid/tiền lần hai.
- Vì sao không đọc tmp để reset JSON lỗi, và validator mới từ chối kiểu hỏng nào.
- Khác nhau giữa leader khi RUNNING và winner/PAID; hold không phải trừ balance ngay.
- Auto-bid ưu tiên thời điểm trong ngân sách còn khả dụng; rule max không được reserve toàn bộ.
- Vai trò Factory Method/selector, default Singleton, Observer, DTO client/domain server.
- Những giới hạn nêu ở F và ý nghĩa chính xác của kết quả test/coverage.

Tài liệu `docs/viva/` vẫn hữu ích để ôn nhưng số test/coverage trong đó là lịch sử;
dùng báo cáo này cho kết quả của lượt hiện tại.

## H. Kiểm thử cuối trên hai máy Windows

Các dòng dưới đây là **MANUAL TEST REQUIRED**, chưa đánh dấu PASS. Dùng bản build
Windows đã verify, một server và ít nhất hai client, dữ liệu và tài khoản thử riêng.

### Chuẩn bị

1. Hai máy cài JDK 25/Maven, cùng LAN tin cậy; kiểm `java -version`, `mvn -version`.
   Build `mvn -B clean verify -Dauction.ui.tests=true` khi có desktop. Không trộn JAR
   chứa native JavaFX của OS khác. Tìm IPv4 của máy server bằng `ipconfig`.
2. Server dùng thư mục thử mới, giữ nguyên biến `$testData` khi restart:

   ```powershell
   $testData = Join-Path $env:TEMP ('auction-lan-' + [guid]::NewGuid().ToString('N'))
   $env:AUCTION_ADMIN_PASSWORD = Read-Host 'Mat khau admin thu rieng, 8-128 ky tu gom chu va so'
   java "-Dauction.data=$testData" '-Dauction.bind=0.0.0.0' '-Dauction.port=8888' `
     '-Dauction.demo=false' -jar target/digital-auction-1.0.0.jar
   ```

   Không chỉ định `data` thật, không xóa/reset snapshot để vượt lỗi. Admin mới là
   `admin_hieu`, dùng mật khẩu thử vừa nhập. Khi restart, chạy lại lệnh java với
   **cùng thư mục** đã tạo, không sinh GUID khác.
3. Nếu firewall chặn, người quản trị xem rule Private/LocalSubnet trong
   MULTI_CLIENT_SETUP. Máy client chạy `Test-NetConnection <IPv4-server> -Port 8888`.
   Không dùng `0.0.0.0`, URL hoặc `IP:port` làm hostname client.
4. Mỗi client chọn SERVER, dùng cùng IPv4/cổng. Có thể dùng `.\Start-Client.cmd`
   hoặc `run.ps1 Client -ServerHost <IPv4-server>`. Đăng ký một Seller và hai Bidder
   khác tên/email; không dùng chế độ Mock để đánh giá nhiều client/persistence.

### Kịch bản và kết quả mong đợi

| Bước | Thao tác | Kết quả cần quan sát/ghi lại |
| --- | --- | --- |
| 1. Auth | Đăng ký Seller/A/B, login bằng username/email, logout/login lại; thử sai mật khẩu, mật khẩu 129 ký tự. | Đúng role, lỗi dễ hiểu, không tạo admin bằng đăng ký công khai, không giữ session sau logout. |
| 2. Seller form | Tạo sản phẩm tên/mô tả/ngành/ảnh, giá 1.000.000, bước 100.000; chọn lịch tương lai đủ thời gian điền form. | Lưu thành công, xuất hiện trên hai client, ảnh đầy đủ. |
| 3. Validation | Tên 201 ký tự, mô tả 10.001, thời lượng >365 ngày, giá phân số/âm/NaN, ảnh sai/ảnh >=5 MiB. | Chặn form hoặc server trả lỗi đúng; không có sản phẩm/bid/tiền phát sinh sai. |
| 4. CRUD | Sửa và xóa một phiên OPEN chưa bid; thử thao tác sản phẩm người khác; thử sửa phiên RUNNING/đã bid. | Chỉ chủ sở hữu và phiên đủ điều kiện được sửa/xóa. |
| 5. Ví | A/B nạp giả lập mỗi người 10 triệu, rút một phần, xem lịch sử và số dư. | balance/held/available khớp; không rút vượt available; không dùng tiền thật. |
| 6. Mở phiên | Cả hai mở cùng sản phẩm khi tới lịch. | OPEN -> RUNNING theo server; đếm ngược/giá/lịch sử/chart đúng. |
| 7. Đồng thời | Cả hai cùng thử mức đầu 1.100.000. | Chỉ một chấp nhận; lịch sử thêm một bid, một leader, một hold; client còn lại nhận giá mới. |
| 8. Bid sai/outbid | Thử giá bằng/thấp hiện tại, thiếu bước và vượt tiền; rồi B đặt hợp lệ cao hơn. | Lỗi rõ; A được release hold khi bị vượt; chart/history hai máy tự đổi, không cần refresh. |
| 9. Auto-bid | Hai mức max khác nhau, cùng max và đăng ký khác thời điểm; bật/tắt rồi manual bid. | Không vượt max, không self-outbid, ưu tiên sớm khi đủ tiền; hết ngân sách không chặn đối thủ có tiền. |
| 10. Ví nhiều phiên | A dẫn đầu một phiên khác, làm giảm tiền khả dụng, rồi cạnh tranh cùng trần auto ở phiên đầu. | Không overspend; budget được tính lại qua cả hai phiên. |
| 11. Anti-sniping | Đặt bid hợp lệ trong 30 giây cuối. | Deadline tăng 60 giây trên cả hai client; chưa settle tại deadline cũ. |
| 12. Kết thúc | Để một phiên có bid và một phiên không bid hết giờ; restart và xem lại. | Có bid: PAID, đúng winner, buyer trừ/seller cộng một lần; không bid: FINISHED, không payment. |
| 13. Admin account | Khóa/mở A, đổi role tài khoản không còn nghĩa vụ, thử tự khóa/xóa, thử xóa/đổi role tài khoản đang có nghĩa vụ. | Các giới hạn đúng, session bị khóa không viết được; không xóa dấu vết tiền/bid. |
| 14. Admin auction | Hủy phiên có bid với lý do; archive sau hủy; thử archive phiên chưa hủy. | Release hold/stop rules, giữ lý do/lịch sử; chặn archive trái trạng thái. |
| 15. Client hỏng | Tắt đột ngột client A. | B tiếp tục dùng server bình thường; A mở lại đăng nhập và lấy snapshot hiện tại. |
| 16. Mất mạng/ACK | Ngắt Wi-Fi client đang bid; quan sát trạng thái unknown và reconnect. | UI còn phản hồi, không spam nút gửi lệnh mới; giữ pending và đối soát cùng commandId. |
| 17. Restart server | Ghi lại bid/balance/hold/history, dừng server và mở lại cùng `$testData`. | Bắt login lại; dữ liệu/receipt giữ nguyên, không bid/payment trùng. Không xóa sổ pending để thử lại. |
| 18. Popup | Mở menu rồi Alt+Tab sang trình duyệt, click ngoài, Esc, minimize, đổi màn hình. | Menu đóng, không nổi trên ứng dụng khác hoặc còn giữ focus/handlers của view cũ. |
| 19. Layout | Auth/room/wallet/seller/admin/guide/contact ở 640 và 1200 px, DPI 125%/150%. | Có thể cuộn, thao tác truy cập được, tiền không bị cắt mất chữ số; ghi rõ ảnh lỗi nếu có. |
| 20. JSON lỗi | Dừng server thử, sao chép folder thử sang bản khác, làm hỏng JSON/duplicate key/âm balance rồi mở trên bản đó. | Server từ chối startup rõ ràng và không tự reset. Giữ bản tốt nguyên vẹn để đối chiếu. |

Mỗi phép manual nên ghi máy/OS/JDK, IP, thời điểm, bước, kết quả thực tế và ảnh/log
chỉ từ dữ liệu thử. Kết thúc demo thì dừng server trước khi sao lưu snapshot; không
đưa snapshot, pending command chứa thông tin riêng hoặc checkpoint lên Git.
