# Đối chiếu cuối theo đề và thang điểm giảng viên

Ngày kiểm tra: **14/11/2026, giờ Việt Nam**. Báo cáo áp dụng cho working tree hiện
tại, có giữ các thay đổi chưa commit của người dùng. HEAD tham chiếu:
`f832295` (`fix: stabilize auction client lifecycle and persistence flows`). Không
commit, push hoặc merge trong đợt này. Các báo cáo UI/UX, Seller/Mock, stability và
archive là lịch sử kiểm tra, không thay thế hai PDF chính thức.

**Kết quả chốt: 237 test, 236 PASS, 1 failure Robot popup, 0 error/skip;
clean verify vẫn BUILD FAILURE.** Đã sửa Singleton production, ba custom exception
đúng hướng dẫn, Google Style và gate coverage. LINE coverage **82,66%**, Google
Checkstyle **0 vi phạm**; 1.436 file dữ liệu gốc còn nguyên hash. CI/Linux/Xvfb và
LAN vật lý **NOT TESTED**. Không tự chấm hoặc cam kết điểm số.

Kết quả chi tiết và số liệu: xem mục 6–7. PASS dưới đây chỉ có nghĩa là
đã đọc đường gọi production và có test tương ứng trong phạm vi nêu rõ; không phải
cam kết mọi tình huống, mọi máy hoặc điểm số tuyệt đối. OPTIONAL chỉ mức tùy chọn
của đề, không có nghĩa tính năng chưa được kiểm tra.

## 1. Tài liệu gốc, trang và cách hiểu thang điểm

Hai nguồn chính thức: `docs/2026-BTL.pdf` và
`docs/Hướng dẫn BTL LTNC-2026.pdf`.

Đã trích toàn bộ **10 trang đề** và **7 trang hướng dẫn**; bản trích theo từng
trang nằm trong `data/audit-extract/`. Số trang bên dưới là **trang vật lý PDF**.
Đề có bìa/mục lục trước nội dung: trang vật lý 3–10 tương ứng số in 1–8. Hướng
dẫn dùng số trang vật lý trùng số in. Không nhầm trang nội dung 8 với trang PDF 8.

| Tài liệu, trang PDF | Yêu cầu/điều kiện được đối chiếu |
|---|---|
| Đề tr. 1–2 | Mục lục, không phát sinh điểm chức năng riêng |
| Đề tr. 3 / số in 1 | Nhóm 3–4; được dùng công cụ hỗ trợ; commit thường xuyên; bất kỳ thành viên nào không giải thích được bất kỳ phần source nào thì cả nhóm 0 điểm |
| Đề tr. 4 / số in 2 | Đăng ký/login; Bidder/Seller/Admin; sản phẩm thêm/sửa/xóa, tên/mô tả/giá khởi điểm |
| Đề tr. 5 / số in 3 | Giá hiện tại, start/end; bid cao hơn giá hiện tại, kiểm hợp lệ, leader; tự đóng/winner; OPEN → RUNNING → FINISHED → PAID/CANCELED; lỗi bid thấp/phiên đóng/dữ liệu/kết nối |
| Đề tr. 6 / số in 4 | GUI JavaFX hoặc Swing; danh sách, chi tiết, realtime, Seller; auto-bid maxBid/increment, cạnh tranh nhiều luật, không quá trần, ưu tiên đăng ký sớm |
| Đề tr. 7 / số in 5 | Không lost update/rollback giá/hai winner; anti-sniping X/Y; realtime mọi Client xem phiên, không polling liên tục; line chart timestamp/giá, tự cập nhật mỗi bid |
| Đề tr. 8 / số in 6 | Entity/User/Item abstract, các lớp con, Auction/BidTransaction; bốn nguyên lý OOP; Client–Server, REST hoặc Socket JSON |
| Đề tr. 9 / số in 7 | MVC Client JavaFX/FXML, Server Controller → Model → DAO; chỉ Server truy cập storage; Maven/Gradle, Google Java Style, JUnit, Git; Singleton, Factory Method, Observer; Strategy/Command tùy chọn |
| Đề tr. 10 / số in 8 | Bảng điểm: concurrency, realtime và CI/CD đều **bắt buộc**; nâng cao tối đa 1,5 điểm |
| Hướng dẫn tr. 1 | Danh sách 11 nhóm bắt buộc, Java/JavaFX/MVC, toàn nhóm hiểu source, Git thường xuyên |
| Hướng dẫn tr. 2 | Tự học Socket, JavaFX/MVC, “Lưu trữ dữ liệu (Serialization, File I/O)”; cây User/Item, Singleton và Factory Method |
| Hướng dẫn tr. 3 | Observer, state transition, concurrency với synchronized/ReentrantLock; custom exceptions **InvalidBidException, AuctionClosedException, AuthenticationException**; JUnit và tách logic khỏi Controller |
| Hướng dẫn tr. 4 | Maven, Checkstyle enforce convention, GitHub Actions; “Triển khai Serialization để lưu/tải dữ liệu”; Server nhiều Client, Observer qua Socket; **coverage ≥60%** |
| Hướng dẫn tr. 5 | End-to-end Server + nhiều Client, lưu trữ, CI xanh; nâng cao; README; demo/slide/đóng góp và mỗi thành viên giải thích toàn source |
| Hướng dẫn tr. 6 | Các repo chỉ để tham khảo; bảng điểm tóm tắt cùng các nhóm bắt buộc/tùy chọn |
| Hướng dẫn tr. 7 | Phân công/commit/review thường xuyên; JavaFX, mạng và lưu trữ Serialization đều bắt buộc |

Áp dụng bảng điểm khi phần mô tả không thống nhất: concurrency/realtime được mô
tả trong mục nâng cao và CI được ghi “khuyến khích” ở đề tr. 9, nhưng bảng tr. 10
và hướng dẫn xếp chúng bắt buộc. Không hạ ba nhóm này thành tùy chọn.

Các dòng bắt buộc cộng số học là **9,5**; ba mục nâng cao là **1,5**, trong khi hai
PDF ghi tổng “10+1”. Báo cáo giữ nguyên số điểm từng dòng, không tự phân bổ thêm
0,5, không quy đổi hoặc tự chấm điểm cuối. “Tính năng khác tự sáng tạo” không
được cộng vượt trần nâng cao 1,5.

## 2. Ma trận theo bảng điểm

Đường dẫn production tương đối với `src/main/java/com/auction/`, trừ khi ghi khác.
Test nằm tại `src/test/java/com/auction/`. Tên method là điểm tra cứu ổn định sau
chuẩn hóa Google Style; không dùng số dòng của báo cáo cũ.

| Nhóm chấm chính thức | Điểm của đề | Mức đối chiếu | Production thực tế | Test/bằng chứng cụ thể |
|---|---:|---|---|---|
| Thiết kế lớp/cây kế thừa | 0,5 bắt buộc | PASS | server/model/Entity, User → Bidder/Seller/Admin; Item → Electronics/Art/Vehicle; Auction, BidTransaction | server/model/ItemFactoryTest.userRoleDispatchUsesTheRuntimeSubclass; createsCorrectCategoryAndPreservesProductAndOwner; AuctionTest.acceptedBidChangesOneLeaderWithoutMutatingThePreviousSnapshot |
| Bốn nguyên lý OOP | 1,0 bắt buộc | PASS | Entity private/getter; Auction record/requireMinimumBid/withBid; User/Item abstract; AuthLogic.createUser gọi getRoleName qua User, ProductLogic.createAuction gọi getCategory qua Item | Hai ItemFactoryTest trên; AuctionTest.minimumStepAppliesToFirstBidAndAllowsLargerJumps; server/dao/AuctionJsonMapperTest.applyingDomainBidUpdatesOnlyTheLeaderPriceAndCount |
| Design Patterns phù hợp | 1,0 bắt buộc | PASS sau sửa Singleton | Server private constructor/getInstance/main; ItemFactory abstract create + overridden creator, ProductLogic caller; AuctionService.commit → Server.publish → ServerServices.onEvent/listener | ServerStartupTest.productionConstructionIsPrivate và defaultServerNeverSeedsEvenWhenOldDemoPropertyIsPresentAndRestartPreservesData (16 caller); ItemFactoryTest; FullStackTest.failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges |
| Quản lý người dùng/sản phẩm | 1,0 bắt buộc | PASS nghiệp vụ; GUI theo mục 6 | AuthLogic.register/login/manageUser, ProductLogic.createAuction/updateAuction/owner/editable; AuctionService DELETE_PRODUCT_AUCTION | AuctionServiceTest.registerValidatesRoleUniquenessAndPassword, adminLockAppliesToExistingSessionAndCannotBeSpoofed, productCrudChecksOwnerAndPreservesAuctionHistory; FullStackTest.sellerUploadsImageBidderReadsItAndAdminManagesProduct |
| Đấu giá và kết thúc | 1,0 bắt buộc | PASS | BiddingLogic.placeBid/acceptBid; Auction.requireBidder/requireMinimumBid; SettlementLogic.refreshAuctions/settle; Server.start scheduler 250 ms | AuctionServiceTest.firstBidRequiresStepAndIntegerAndSellerCannotBidOwnProduct, scheduledAuctionsCannotReceiveEarlyBidsAndOpenAutomatically, settlementPaysSellerExactlyOnceAndSurvivesRestart, noBidAuctionFinishesWithoutPaymentAndInvalidImagesAreRejected |
| Xử lý lỗi/ngoại lệ | 1,0 bắt buộc | PASS | ApiException cùng ba subclass đúng tên hướng dẫn; Auction ném InvalidBidException/AuctionClosedException; AuthLogic.login ném AuthenticationException; readLoop đổi thành response mã lỗi cũ | CustomExceptionTest (3 tình huống thật); AuctionTest.onlyRunningStatusAcceptsBids; AuctionServiceTest.failedBidDoesNotPublishOrPersistAndSameCommandCanBeRetried; AuctionRepositoryTest.corruptFileIsNeverResetOrOverwritten; FullStackTest.unavailableServerDoesNotReturnMockData |
| Concurrency an toàn | 1,0 bắt buộc | PASS trong tải test | AuctionService.execute/tick cùng monitor, bao giá/ví/rules/receipt/save; một service dùng chung mọi Client | AuctionServiceTest.equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice, concurrentAuctionsCannotOverspendOneWallet; ServerIntegrationTest.simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer |
| Realtime Observer/Socket | 0,5 bắt buộc | PASS loopback | Server.publish/Client.writeLoop, subscription/feed; JsonTransport.read; ServerServices.onEvent/registerBidListener; Controller/ViewTasks.dispatch | ServerIntegrationTest.twoRealSocketsReceivePushAndReconnectReplaysOnlyOneBid; FullStackTest.twoAdaptersReceiveLiveBidWalletAndAutoBidWithoutMock và failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges |
| Client–Server | 0,5 bắt buộc | PASS loopback | Client ServerServices/JsonTransport; Server.Client.readLoop → AuctionService → AuctionRepository; Client không mở state.json server | FullStackTest.reconnectAfterServerRestartRequiresLoginAndRetainsMoney; unavailableServerDoesNotReturnMockData; ServerIntegrationTest; LAN thật NOT TESTED |
| MVC | 0,5 bắt buộc | PASS cấu trúc; GUI theo mục 6 | FXML + client/controller/model; SellerProductFormView chỉ giữ Draft; Server readLoop nhận request, service/domain xử lý, repository lưu | ClientUiTest.allRoutesLoadWithAuthorizedRoles; ServerUiIntegrationTest; xem ARCHITECTURE.md và kiểm nguồn không có Controller ghi state server |
| Maven/convention/chất lượng | 0,5 bắt buộc | PASS kiểm tĩnh mới; toàn build theo mục 6 | pom.xml; Google configuration Checkstyle 14.4.0 nguyên bản, kiểm cả main/test, warning làm build thất bại | target/checkstyle-result.xml; negative fixture BadStyle phải exit 1 với 5 vi phạm; không thêm suppression riêng |
| JUnit logic quan trọng | 0,5 bắt buộc | PASS các suite nghiệp vụ; tổng theo mục 6 | Surefire/JUnit; @TempDir cho Server/repository; fixture chỉ ở test | AuctionServiceTest, AuctionTest, AuctionRepositoryTest, AuctionStateValidationTest, FullStackTest, CustomExceptionTest; JaCoCo bundle LINE ≥60%, gate trong verify |
| CI/CD GitHub Actions | 0,5 bắt buộc | PARTIAL cấu hình / NOT TESTED thực thi | .github/workflows/ci.yml: push/PR, Ubuntu/JDK 25/Xvfb, verify bật GUI, upload reports if always | Đã đọc workflow; không commit/push working tree, không có artifact đúng revision này; Windows không chứng minh Linux/Xvfb |
| Auto-Bidding/PriorityQueue | 0,5 tùy chọn | OPTIONAL — PASS nghiệp vụ | BiddingLogic.setAuto/processAutoBids; PriorityQueue.remove theo order, trần/bước/ngân sách/leader, giới hạn vòng có rollback | AuctionServiceTest.autoBidRespectsLimitBudgetAndRegistrationOrder; earlierAutoBidCannotBlockAnAffordableRivalAfterItsBudgetChanges; FullStackTest.twoAdaptersReceiveLiveBidWalletAndAutoBidWithoutMock |
| Anti-sniping | 0,5 tùy chọn | OPTIONAL — PASS | AntiSnipingPolicy.computeNewEndTime, BiddingLogic.acceptBid; cửa sổ 30 giây cuối, cộng 60 giây vào deadline cũ | AntiSnipingPolicyTest.bidExactlyAtThreshold_extends, bidAfterEnd_doesNotExtend; AuctionServiceTest.antiSnipingExtendsAndDoesNotSettleAtOldDeadline |
| Bid History Visualization | 0,5 tùy chọn | OPTIONAL — PASS chart/history; giới hạn native GUI ở mục 6 | AuctionBidHistoryView.render/addPricePoint/updateChartAxes; timestamp/giá, 30 điểm cuối trên chart; Controller nhận push rồi đọc snapshot | ClientUiTest.bidHistoryChartAndRepeatedRoomLifecycle, historySnapshotOrdersCapsAndReplacesPointsAndExtensionClearsUrgency; ServerUiIntegrationTest; không tính fixture test làm bằng chứng Socket |

### Các tiêu chí cần tách nhỏ, không cộng điểm hai lần

| Tiêu chí | Mức/bằng chứng |
|---|---|
| Đăng ký/login/phân quyền Server | PASS: không tự đăng ký Admin, token kiểm Server, role UI không quyết định quyền; AuthLogic và các test service/full-stack trong bảng |
| Seller CRUD | PASS: owner + OPEN chưa bid mới sửa/xóa; xóa mềm giữ history; test productCrudChecksOwnerAndPreservesAuctionHistory và GUI form |
| Bid hợp lệ/không hợp lệ | PASS: số nguyên/bước giá/thời gian/ví/quyền kiểm lại tại Server; AuctionTest, AuctionServiceTest và CustomExceptionTest |
| Winner và state | PASS: một highestBidderId; hết giờ có bid đặt FINISHED rồi PAID trong cùng transaction, Client có thể chỉ thấy PAID; không bid giữ FINISHED; cancel có lý do và release |
| File I/O/Serialization JSON | PASS theo cơ chế phân tích ở mục 4; nằm trong kiến trúc/lưu trữ, không tự cộng một dòng điểm mới |
| Idempotency/rollback | PASS: receipt và snapshot cùng save; lost ACK dùng commandId cũ; FullStackTest.lostBidAcknowledgementRetriesTheSameCommandNotAnotherBid, pendingCommandSurvivesClientRestartAndIsAppliedOnlyOnce; các test failedBid/failedSettlement |
| GUI JavaFX | FXML, scene thật, Robot và Server UI được chạy; phân biệt từng kết quả ở mục 6, không suy từ unit test |
| Git thường xuyên/đóng góp/hiểu toàn source | NOT TESTED ở cấp từng thành viên; có lịch sử nhiều commit nhưng không suy ra mọi người đã hiểu hoặc đóng góp đầy đủ; cần diễn tập/phân công thực tế |
| Slide trình bày | NOT TESTED: đợt audit không tạo/xác minh slide hay đánh giá khả năng trình bày của từng người |

## 3. Sửa đổi trong đợt này

1. **Singleton production:** chuyển constructor Server public thành private; giữ
   accessor synchronized và một service/scheduler/listener cho Server hiện hoạt.
   TestServers nằm duy nhất trong src/test, dùng reflection để giữ khả năng tạo
   nhiều Server/cổng/thư mục cô lập. Không thêm public factory bỏ qua Singleton,
   không biến Repository/AuctionService thành global state. Test kiểm constructor,
   16 luồng cùng khởi tạo lần đầu, không seed, Socket, đóng/mở lại và dữ liệu.
2. **Đúng tên custom exception của hướng dẫn tr. 3:** thêm InvalidBidException,
   AuctionClosedException, AuthenticationException làm subclass của ApiException,
   dùng thật tại domain Auction và AuthLogic.login. Giữ nguyên BID_TOO_LOW,
   AUCTION_CLOSED, AUTHENTICATION_FAILED và cơ chế catch/rollback/response. Thêm
   ba test kiểm chính đường ném exception và mã lỗi, không chỉ test constructor.
3. **Google Style thay kiểm whitespace đơn thuần:** chuẩn hóa bằng Google Java
   Format 1.28.0 có sẵn, thêm braces bằng Java AST, mở wildcard import theo symbol
   compiler, tách khai báo biến, thêm tài liệu API ngắn; đổi tên nội bộ
   timeAxis/priceAxis (đồng bộ FXML/test), updateModeUi, ServerMapper.text/longValue
   và một tên test. Không đổi key JSON/action/schema/luật giá. Những biến cần giữ
   từ sớm được đánh final thay vì di chuyển thao tác có thể ảnh hưởng thời điểm.
   Snapshot validator giữ thứ tự validation; chỉ tách startingPrice/previous.
4. **Enforce thật:** pin Checkstyle 14.4.0 trong plugin, chép nguyên
   google_checks.xml từ JAR vào config/checkstyle.xml; warning cũng bị chặn.
   Không thêm suppression/exclude để bỏ lỗi. Bản 9.3 báo sai thụt dòng ở một số
   lambda/switch đã qua formatter; bản mới kiểm được các cấu trúc đó. Nguồn phiên
   bản/config: [Checkstyle release notes](https://checkstyle.org/release-notes.html),
   Maven artifact com.puppycrawl.tools:checkstyle:14.4.0.
5. **Coverage theo hướng dẫn tr. 4:** thêm gate JaCoCo BUNDLE/LINE/COVEREDRATIO
   minimum 0.60 ở verify, giữ nguyên test và các loại coverage được báo cáo.
6. **Vấn đáp:** cập nhật Singleton, map source theo tên method, README/setup;
   thêm [hướng dẫn theo source](viva/TEACHER_REQUIREMENTS_GUIDE.md) và
   [checklist Windows LAN](viva/WINDOWS_LAN_CHECKLIST.md). Sơ đồ lớp/sequence đã
   có ở ARCHITECTURE.md, không dựng lại kiến trúc đang đúng.

Đây có phần diff định dạng trên nhiều file vì convention là yêu cầu đã xác minh,
không phải refactor tính năng. Không dùng tổng LOC giảm làm mục tiêu. Các thay
đổi UI/Seller/Mock có trước checkpoint được giữ nguyên. Không xóa test hay hạ
timeout/assertion Robot. Một clean compile trung gian phát hiện trùng tên helper
integer với method chuyển tiền double trong adapter; đã sửa tên thành longValue.
Không lấy incremental compile lúc đó làm bằng chứng clean build.

## 4. Serialization, lưu tải và transaction

Hai PDF bắt buộc lưu trữ/Serialization nhưng **không có câu buộc sử dụng
ObjectOutputStream, MySQL hay schema của repo tham khảo**. Đề tr. 8 chấp nhận
Socket JSON. Hướng dẫn tr. 4 nêu “Serialization để lưu/tải dữ liệu”, tr. 5
“Serialization / lưu trữ dữ liệu”; tr. 6 chỉ liệt kê repo tham khảo. Chọn JSON
serialization thực tế và File I/O như sau:

| Bước | Class/method và ý nghĩa |
|---|---|
| Truyền mạng | JsonProtocol.writeFrame: JSON → UTF-8, writeInt độ dài, write/flush; readFrame kiểm 0 < length ≤8 MiB, readNBytes đủ frame, parse object. Không dùng Java object-stream qua TCP |
| Chuyển domain/state | AuctionJsonMapper.auction dựng Auction record; bid chuyển BidTransaction sang trường JSON cũ; applyBid chỉ merge trường thay đổi, bảo toàn image/history/extension fields |
| Ghi file | AuctionRepository.save dùng Gson.toJson, UTF-8, FileChannel ghi đủ state.json.tmp, force(true), Files.move ATOMIC_MOVE/REPLACE_EXISTING; không có fallback ghi đè thường |
| Khôi phục | AuctionRepository.load đọc JSON strict, kiểm duplicate keys/trailing data; AuctionService constructor gọi AuctionStateValidator.validate trước scheduler/requests |
| Chống hai writer | FileLock server.lock ngăn hai repository/Server cùng thư mục; không nhầm với Singleton hay khóa nghiệp vụ |
| Lưu thành công/rollback | AuctionService.execute chụp before, kiểm nghiệp vụ và receipt; commit save trước publish; IOException phục hồi state, xóa events, STORAGE_ERROR. Nghiệp vụ bị từ chối phục hồi sau refresh và lưu receipt REJECTED |
| Restart | Snapshot giữ users/auctions/rules/commands/transactions; session token chỉ ở RAM, phải login lại. Không lấy file .tmp chưa commit làm state chính, không tự reset file hỏng |

Entity implements Serializable và serialVersionUID có tồn tại, nhưng **không
trình bày chúng như bằng chứng production đang dùng ObjectOutputStream**. Luồng
production serialize JSON; User/Item chủ yếu cung cấp vai trò/danh mục; tiền
authoritative là long tại service/domain, không phải API double cũ của DTO/User.

Test trực tiếp: AuctionJsonMapperTest (4 case), AuctionRepositoryTest (12 case),
AuctionStateValidationTest (27 case), các test storageFailure/failedBid/
failedSettlement/extensionFieldsSurviveAutomaticBidsSettlementAndRestart trong
AuctionServiceTest, các test pending/lost ACK/restart trong FullStackTest.
Bản sao snapshot thật cũng được load/validate riêng, không start scheduler hay
execute action trên bản sao đó; byte của bản thật và bản sao không đổi.

Giới hạn: rollback bao state JSON, không phải mọi hiệu ứng ngoài state như session
RAM. force + atomic move trên filesystem hiện tại không phải chứng minh an toàn
khi mất điện thật, ổ mạng hoặc nhiều writer phân tán.

## 5. Những phần giữ nguyên và lý do

- Giữ JSON schemaVersion=1, giao thức/action, dữ liệu thật, bootstrap không seed,
  mock/fixture chỉ ở test. Không thêm MySQL, ObjectOutputStream hoặc framework.
- Giữ cây User/Item và Factory Method đang được production gọi; không tạo lớp
  pattern chỉ để đủ tên. forCategory là Simple Factory chọn creator; create là
  abstract/override Factory Method tham số hóa. Strategy/Command vẫn OPTIONAL,
  không gọi anti-sniping policy hay commandId là GoF Strategy/Command hoàn chỉnh.
- Giữ monitor chung của AuctionService vì một ví liên quan nhiều phiên; chưa có
  bằng chứng cần đổi ReentrantLock/per-auction lock. Giữ PriorityQueue lấy bằng
  remove, trần/ngân sách/thứ tự đăng ký và giới hạn vòng có rollback.
- Giữ phân tầng Client/FXML/ViewTasks, helper form và chart, Server service/DAO;
  không chia file chỉ vì dài hoặc sửa bố cục đang dùng được.
- Giữ anti-sniping cộng 60 giây vào deadline cũ trong cửa sổ 30 giây; giữ chart
  30 điểm gần nhất, đầy đủ bid vẫn trong history. Đây là giới hạn hiển thị đã có,
  không xóa lịch sử.
- Giữ cơ chế popup JavaFX và assertion native hiện có khi chưa đủ bằng chứng
  nguyên nhân để đổi production. Các lần thất bại/trạng thái mới nhất ở mục 6.

## 6. Baseline và kiểm thử thực tế

| Lượt/môi trường | Test | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Baseline từ checkpoint bất biến, desktop, kết thúc 13:48:36 +07 | 233 | 1 | 0 | 0 | BUILD FAILURE: native outside click auto-hides popup |
| Bản cuối, clean verify, kết thúc 14:13:42 +07 | 237 | 1 | 0 | 0 | **BUILD FAILURE: cùng test Robot popup** |
| ClientUiTest bản cuối | 25 | 1 | 0 | 0 | 24 PASS; accountPopupSupportsOutsideClickKeyboardAndNavigation FAIL |
| ServerUiIntegrationTest bản cuối | 2 | 0 | 0 | 0 | PASS GUI với Server thật, ảnh/ví/settlement/restart |
| Các suite còn lại bản cuối | 210 | 0 | 0 | 0 | PASS, gồm unit/integration/Socket/concurrency/custom exceptions |

**Không kết luận clean verify PASS.** Test lỗi ở bước `native outside click
auto-hides popup`, cùng failure của baseline; không bỏ assertion/skip test.
Bốn case tăng thêm là một test constructor private và ba case custom exception.
Test Singleton hiện có được mở rộng cho 16 caller; không tính thành 16 testcase.

Một probe độc lập bằng Button JavaFX chuẩn (không có code Auction/UserMenu) ghi
`focused=true`, output scale 1.25, chuột ở trong bounds nút nhưng **0 action sau
Robot.mouseClick**. Đây là bằng chứng bổ sung cho giới hạn đường phát/nhận input
native trong phiên desktop hiện tại; không đủ để quy lỗi production popup hoặc
cam kết người dùng click thật luôn đúng. Probe không tính vào JUnit hay thành PASS
cho popup. Cần kiểm lại native GUI trên desktop tương tác được; giữ FAIL hiện tại.

Bằng chứng cục bộ: `data/audit-extract/checkpoint-baseline-verify.log`,
`final-verified.log`, `final-metrics.json`, `robot-input-probe.log` và
`target/surefire-reports/TEST-*.xml`. Các lượt trung gian chỉ dùng chẩn đoán.

Baseline chuẩn được chạy từ **ZIP checkpoint bất biến** trong
`.maven-repository/teacher-audit/baseline-project/`, trước sửa: constructor Server
còn public và ServerStartupTest chỉ một test. Điều này tránh nhầm một lần Maven
đang chạy khi source tiếp tục được chỉnh thành baseline nguyên trạng.

Lệnh chốt trên Windows (Maven 3.10.0, JAVA_HOME JDK 25.0.4.1, JavaFX 26.0.2):

```powershell
mvn -o -B clean verify '-Dmaven.repo.local=.maven-repository' `
  '-Dauction.ui.tests=true' `
  '-DargLine=-Djava.io.tmpdir=D:/Bai-Tap-Lon-gon/.maven-repository/teacher-audit/test-tmp -Djavafx.cachedir=D:/Bai-Tap-Lon-gon/.maven-repository/teacher-audit/javafx-cache'
```

Chạy với quyền desktop cho Robot; temp/cache trong workspace. Không đổi user.home,
không skip GUI hoặc ignore test failure. Máy có Maven/cache bình thường dùng
`mvn -B clean verify '-Dauction.ui.tests=true'`.

Các lượt trung gian ghi nhận riêng: popup không tới target trong sandbox; desktop
tới target nhưng click ngoài không auto-hide; thử riêng scale 100% vẫn thất bại.
Một lượt sau chuẩn hóa đã qua 25 ClientUiTest nhưng timeout login Admin trong
ServerUiIntegrationTest.javafxScreensOperateOnTheRealServer. Không xóa các log lỗi
hoặc gọi chúng PASS. Computer-use đã liệt kê được cửa sổ QA nhưng capture báo
“foreground window did not report a process id”; không tính là kiểm tra native
thủ công thành công. Kết quả mới nhất mới là kết quả chốt của working tree.

## 7. JaCoCo và Checkstyle

| Counter toàn production | Covered / Tổng | Tỷ lệ |
|---|---:|---:|
| LINE | 4,797 / 5,803 | 82.66% |
| BRANCH | 1,732 / 2,401 | 72.14% |
| INSTRUCTION | 23,165 / 26,803 | 86.43% |
| METHOD | 818 / 1,055 | 77.54% |
| CLASS | 112 / 115 | 97.39% |

Gate **LINE ≥60%: PASS**; Google Checkstyle **0 vi phạm trên 127 file Java
production/test**. Giá trị đếm file được kiểm lại ở manifest cuối. Không loại
UI/model khỏi coverage, không lấy trung bình phần trăm các package.

Vì Surefire failure chặn lifecycle trước verify, report/gate/style được chạy
**riêng** sau lần test cuối bằng:

```powershell
mvn -o -B '-Dmaven.repo.local=.maven-repository' jacoco:report `
  jacoco:check@minimum-line-coverage checkstyle:check
```

Goal chất lượng riêng kết thúc 14:16:36 +07, exit 0; không đổi kết quả BUILD FAILURE
của clean verify. Sau đó chạy trực tiếp jar:jar và shade:shade@default để kiểm nội
dung artifact (không có cờ skipTests): JAR không có package mock/fixture hoặc
TestServers.class, có đủ ba custom exception, Main-Class là Server. Đây chỉ là
kiểm tra đóng gói; không phải full lifecycle/release đã xanh. Log:
data/audit-extract/packaging-inspection.log và jar-inspection.json. Raw: target/site/jacoco,
target/checkstyle-result.xml; log: data/audit-extract/final-quality.log.

`config/checkstyle.xml` là Google configuration nguyên bản của Checkstyle 14.4.0;
POM includeTestSourceDirectory=true và violationSeverity=warning. Negative fixture
BadStyle trong thư mục QA riêng cho kết quả exit 1 với 5 vi phạm, chứng minh
warning thực sự làm goal thất bại. Không có suppression/exclude riêng được thêm.
Đạt Checkstyle không phải chứng minh mọi khía cạnh readability hoặc nghiệp vụ.

## 8. CI/Linux/Xvfb và LAN

| Môi trường | Trạng thái / căn cứ |
|---|---|
| Windows Maven/GUI/Socket cô lập | PARTIAL: 236/237 PASS; full verify FAIL ở native popup; nghiệp vụ và Server UI PASS |
| GitHub-hosted CI của working tree này | **NOT TESTED**: không tự commit/push; chưa có run/artifact đúng revision |
| Linux/Xvfb local | **NOT TESTED**: WSL báo chưa cài, không tìm thấy Docker/Podman/Xvfb khả dụng |
| Hai máy Windows qua LAN thật | **NOT TESTED**: test Socket là loopback trên một máy |
| Mất điện/đứt Wi-Fi vật lý/DPI nhiều màn hình | **NOT TESTED**; lỗi lưu/timeout được mô phỏng cô lập, không thay thế phần cứng thật |

Workflow đã cấu hình Ubuntu, Temurin 25, Xvfb/GTK/ALSA, `verify` có bật GUI; artifact
Surefire/JaCoCo/Checkstyle được upload cả khi lỗi. Sau khi nhóm tự review/commit/
push, phải xem run đúng SHA, tải XML và kiểm test/coverage/style; không dùng run
cũ của HEAD khi working tree còn thay đổi. Nếu lỗi popup, đọc trace focus/bounds/
AUTO_HIDE/Robot trong XML và sửa theo nguyên nhân, không bỏ test/nới timeout.

Thử LAN theo [checklist từng tình huống](viva/WINDOWS_LAN_CHECKLIST.md), ghi kết quả
và bằng chứng riêng. Nhóm cần demo Seller + hai Bidder, bid đồng thời, push/chart,
auto-bid, anti-sniping, ví, settlement, restart và mất kết nối trên hai máy thật.

## 9. Checkpoint và bảo toàn dữ liệu thật

Trước sửa đã lưu Git status, HEAD, toàn bộ diff dạng binary và ZIP working tree
gồm tracked/untracked; không stash/reset/commit. Checkpoint riêng, được Git ignore:

`data/audit-checkpoints/20261114-133042/`

Gồm working-tree.zip, working-tree.diff, git-status.txt, HEAD.txt, real-data.zip,
data-sha256.json. Đã đọc diff có sẵn, kiểm sáu Mock chuyển sang fixture chỉ đổi
package trước đợt này, giữ các sửa UI/Seller/registry/launcher và báo cáo cũ.
Trước đổi style có thêm `data/audit-extract/pre-style-source.zip` để tách đúng
thay đổi định dạng khỏi bản Singleton vừa sửa.

Đối chiếu cuối: **1.436/1.436 file có trước còn nguyên SHA-256; 0 thay đổi,
0 mất**. data/state.json có cùng SHA-256 trước/sau (giá trị dưới). Manifest:
`data/audit-extract/data-integrity.json`; bản chi tiết trước ở checkpoint.

SHA-256 state.json trước và sau sửa:
`577b2de3d73209689bf0e148416ef525b3a5fe45ec53c630c638c4d08aa250a9`.

Test dùng @TempDir/fixture hoặc thư mục QA riêng. Không test nào được sửa trực
tiếp data/state.json; không khởi động scheduler QA trên dữ liệu thật. Snapshot
compatibility chỉ load/validate một bản sao. Đợt audit có thêm backup/log/bản trích
trong data/audit-checkpoints và data/audit-extract; vì vậy không tuyên bố toàn cây
data không thêm file, chỉ đối chiếu **1.436 file có trước** và dữ liệu nghiệp vụ.
Không đưa nội dung snapshot/tài khoản/mật khẩu/ảnh cá nhân vào báo cáo công khai.

## 10. Phần còn thiếu, rủi ro và checklist trước bảo vệ

- Chưa có bằng chứng GitHub/Linux/Xvfb và LAN vật lý cho revision cuối; CI là một
  dòng điểm bắt buộc còn phải xác minh thực thi. Không thể thay bằng Windows.
- Độ ổn định native GUI cần xem kết quả cuối và các failure trung gian ở mục 6;
  một lần PASS không bảo đảm Robot ổn định khi desktop bị giành focus.
- Chưa xác minh từng thành viên hiểu toàn source/đóng góp hoặc slide. Đây là rủi
  ro lớn vì đề quy định cả nhóm 0 điểm nếu một người không giải thích được code.
- Global monitor và snapshot toàn bộ phù hợp quy mô bài tập; chưa thử tải lớn.
  TCP chưa TLS, ví là tiền giả lập; không suy ra tính sẵn sàng cho triển khai tiền thật.
- User/Item còn API legacy double/getter/setter; backend dùng long và validation
  service. Không dùng test factory/model để chứng minh mọi thao tác ví legacy an toàn.
- Factory Method là biến thể tham số hóa nhỏ; thêm category còn cần cập nhật
  selector/validation/client label. Singleton là một Server đang hoạt động mỗi JVM,
  cho phép tạo lại sau close; reflection trong nguồn test có chủ đích phá giới hạn
  truy cập, không coi Singleton là cơ chế bảo mật hay cấm nhiều tiến trình Server.

Trước bảo vệ:

1. Review diff lớn do formatting và diff chức năng Singleton/exception/POM riêng;
   nhóm tự quyết định commit. Không đưa data/cache/backup vào Git.
2. Chạy clean verify có GUI trên máy nộp, kiểm XML không skip, LINE ≥60%, Google
   Checkstyle 0; giữ log nếu thất bại, không coi package không test là đủ.
3. Xem GitHub Actions đúng SHA với artifact đầy đủ; chạy checklist hai máy Windows.
4. Tạo dữ liệu demo qua UI thật trong thư mục riêng, lịch đúng buổi bảo vệ; không
   sửa JSON để kéo dài phiên hoặc reset tài khoản. Sao lưu khi Server đã dừng.
5. Diễn tập mỗi thành viên một vòng request, một test lỗi, OOP/pattern/concurrency/
   serialization và vì sao save phải trước publish. Bất kỳ người nào cũng phải
   giải thích được cả source ngoài module mình làm.

## 11. Câu hỏi vấn đáp từ source thật

1. Vì sao constructor Server private mà integration test vẫn có Server riêng?
   Chỉ ra Server.getInstance/main, TestServers.create và test 16 caller.
2. Singleton có làm ví thread-safe không? Chỉ ra monitor execute/tick và tình huống
   cùng ví ở hai phiên; so sánh với file lock của Repository.
3. Trong AuthLogic.createUser, cùng lời gọi getRoleName chạy method nào? Trong
   ProductLogic.createAuction, Factory Method thật ở đâu, phần nào là Simple Factory?
4. Tại sao có User.balance double nhưng backend vẫn dùng long? Chỉ nơi kiểm số
   nguyên/giới hạn và DTO mapping; không nhận API cũ là nguồn tính tiền chính.
5. Trình bày một bid qua Controller → ViewTasks → adapter/transport → readLoop →
   service/domain → repository → publish → Observer → chart trên FX thread.
6. Ba custom exception được ném ở đâu? Tại sao mã JSON không đổi và catch
   ApiException vẫn xử lý được lớp con? Mở CustomExceptionTest.
7. Nếu save thất bại sau khi đã thay leader/hold thì điều gì được phục hồi, có
   event thành công không? Mở failedBid và failedSettlement tests.
8. ACK mất khác lệnh bị từ chối thế nào? RequestId khác commandId thế nào? Vì sao
   retry cùng command key không trừ tiền/bid lần hai sau restart?
9. PriorityQueue.iterator có theo thứ tự không? Vì sao remove và order được dùng?
   Cùng trần, tiền khả dụng đổi, leader và giới hạn vòng xử lý ra sao?
10. Bid đúng 30 giây cuối hoặc đúng deadline được nhận/gia hạn không? Chỉ test
    biên và nơi acceptBid gọi AntiSnipingPolicy.
11. Vì sao chart chỉ 30 điểm nhưng history không mất? Tại sao Countdown/PING
    không phải polling giá? Listener/timer được gỡ khi nào?
12. Serialization của dự án cụ thể là object gì → định dạng gì → bytes/file nào?
    Serializable có đồng nghĩa dùng ObjectOutputStream không? Mở JsonProtocol,
    AuctionJsonMapper, AuctionRepository.load/save và test restart.
13. Google Checkstyle cũ 0 lỗi có ý nghĩa gì? Warning của Google config có chặn
    build không? Coverage 60% tính counter nào, có bao cả UI/model không?
14. Vì sao Windows test PASS vẫn chưa chứng minh CI/Linux/LAN PASS? Cần xem
    artifact/checklist nào? Nói đúng các NOT TESTED của đợt này.

Tài liệu học kèm: [Architecture](viva/ARCHITECTURE.md),
[OOP](viva/OOP_EXPLANATION.md), [Patterns](viva/DESIGN_PATTERNS.md),
[Concurrency](viva/CONCURRENCY_EXPLANATION.md), [Code map](viva/CODE_MAP.md),
[hướng dẫn theo đề](viva/TEACHER_REQUIREMENTS_GUIDE.md).
