# Học vấn đáp theo đề và source hiện tại

Đọc [bảng đối chiếu cuối](../TEACHER_REQUIREMENTS_FINAL_AUDIT.md) trước khi nói dự án
đã đạt. Kết quả test, CI và LAN là các bằng chứng riêng. Mỗi thành viên cần tự lần
theo source dưới đây; không chỉ học thuộc câu trả lời.

## 1. Cấu trúc và sơ đồ lớp

[ARCHITECTURE.md](ARCHITECTURE.md) có sơ đồ Client–Server, sơ đồ lớp và sequence.
Hai cây ở `src/main/java/com/auction/server/model` là `Entity → User →
Bidder/Seller/Admin` và `Entity → Item → Electronics/Art/Vehicle`.
`ItemFactory.CategoryItem` phục vụ ba danh mục còn lại. `Auction` và
`BidTransaction` là record phía Server, khác DTO cùng tên phía Client.

`AuthLogic.createUser()` tạo biến kiểu User rồi gọi `getRoleName()`; lớp thực tế
quyết định kết quả. `ProductLogic.createAuction()` dùng ItemFactory và gọi
`getCategory()` qua Item. Đây là caller production thật cho kế thừa/đa hình.
`ItemFactoryTest.userRoleDispatchUsesTheRuntimeSubclass` và
`createsCorrectCategoryAndPreservesProductAndOwner` kiểm chứng hai nhánh.

Đóng gói: Entity giữ id/name private; Auction record giữ trạng thái bất biến;
AuctionService giữ context/repository private và trả deepCopy. Trừu tượng:
User/Item có abstract method; Controller gọi AuctionClientService, không mở Socket.
Xem ví dụ chi tiết trong [OOP_EXPLANATION.md](OOP_EXPLANATION.md).

## 2. Tự lần theo một lệnh đặt giá

1. `client/controller/AuctionRoomController.handlePlaceBid()` đọc số tiền,
   giữ ticket theo tài khoản/phòng, dùng `navigation/ViewTasks.submit()` chạy nền.
2. `client/service/server/ServerServices.placeBid()/write()` tạo/giữ commandId
   và pending command; `JsonTransport.call()` gắn requestId rồi gửi frame JSON.
3. `server/network/Server.Client.readLoop()` kiểm envelope và gọi
   `AuctionService.execute()`. Đây là nơi nhận request phía Server, không phải
   Controller JavaFX.
4. Trong monitor service: refresh thời gian, xác thực token, kiểm commandId;
   `BiddingLogic.placeBid()` kiểm quyền/phiên, `Auction.requireMinimumBid()`
   kiểm giá, `WalletLogic.availableForAuction()` kiểm tiền; `acceptBid()` ghi
   leader, lịch sử, hold/release và anti-sniping; `processAutoBids()` xử lý đối thủ.
5. `AuctionService.commit()` lưu snapshot và receipt bằng
   `AuctionRepository.save()`. Thành công mới gọi publisher. Lỗi lưu phục hồi
   snapshot trước đó, bỏ events và trả STORAGE_ERROR.
6. `Server.publish()` đưa event vào queue của Client; `Client.writeLoop()` gửi.
   `JsonTransport.read()` tách event/response; `ServerServices.onEvent()` gọi
   listener. `ViewTasks.dispatch()` chuyển cập nhật về FX thread; phòng đọc
   snapshot rồi gọi `AuctionBidHistoryView.render()`.

RequestId ghép một response với lần gửi; commandId nhận diện cùng một ý định ghi
để retry. Mất ACK chưa chứng minh lệnh thất bại: Client giữ UNKNOWN/pending rồi
đối soát, không tự tạo commandId mới. Test:
`FullStackTest.lostBidAcknowledgementRetriesTheSameCommandNotAnotherBid` và
`pendingCommandSurvivesClientRestartAndIsAppliedOnlyOnce`.

## 3. Ba pattern bắt buộc

- **Singleton:** `Server` final, constructor private; `getInstance()` static
  synchronized và `main()` dùng accessor. Một Server đang hoạt động dùng chung
  listener/service/scheduler; sau close được tạo lại. `ServerStartupTest` kiểm
  private constructor, 16 luồng cùng khởi tạo, TCP và restart. `TestServers.create`
  chỉ ở src/test dùng reflection cho nhiều Server cô lập, không có trong JAR.
- **Factory Method:** abstract `ItemFactory.create()` được creator override;
  `forCategory()` là Simple Factory chọn creator. Caller thật là
  `ProductLogic.createAuction()`. Không gọi mọi static factory là Factory Method.
- **Observer:** service tạo event, commit rồi publish; Server lọc subscription,
  adapter quản lý listener, Controller đăng ký/gỡ khi đổi phòng/dispose. Test
  `ServerIntegrationTest.twoRealSocketsReceivePushAndReconnectReplaysOnlyOneBid`
  và `FullStackTest.failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges`.

Chi tiết và giới hạn ở [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md). Không trình bày
commandId như Command GoF hay một policy cụ thể như Strategy hoàn chỉnh.

## 4. Đồng thời và realtime

`AuctionService.execute()/tick()` synchronized dùng chung monitor cho mọi phiên,
ví, auto-bid, receipt và lưu trữ. Vì một ví có thể tham gia nhiều phiên, chỉ khóa
từng Auction sẽ chưa đủ để ngăn tiêu cùng một khoản tiền hai lần. Trong một
transaction: đọc giá mới nhất → kiểm tra → cập nhật → lưu → thông báo.

Hai bid cùng giá: một bid được nhận, bid sau kiểm trên giá mới và bị từ chối.
Test `AuctionServiceTest.equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice`,
`concurrentAuctionsCannotOverspendOneWallet` và test Socket
`ServerIntegrationTest.simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer`.
Đây là kiểm tra hữu hạn, không phải chứng minh mọi lịch chạy hoặc tải lớn.

Socket dùng một writer queue giới hạn cho mỗi Client. Listener collection dùng
ConcurrentHashMap/CopyOnWriteArrayList; UI chỉ sửa trên FX thread. Heartbeat PING
kiểm kết nối và countdown cập nhật đồng hồ; chúng không polling giá thay push.
Xem [CONCURRENCY_EXPLANATION.md](CONCURRENCY_EXPLANATION.md).

## 5. Serialization JSON và File I/O

Hướng dẫn PDF tr. 2, 4, 5, 7 yêu cầu Serialization/lưu tải; đề PDF tr. 8–9 cho
phép Socket JSON, không bắt buộc ObjectOutputStream hoặc MySQL.

- `shared/JsonProtocol.writeFrame/readFrame`: JSON → byte UTF-8, thêm độ dài 4 byte,
  đọc đủ frame, giới hạn 8 MiB. TCP không giữ ranh giới message.
- `AuctionJsonMapper.auction/bid/applyBid`: chuyển giữa snapshot JSON và domain,
  chỉ merge trường bid để giữ các trường mở rộng. Không tự serialize lại toàn
  object graph User/Item và không dùng Java object-stream trong production.
- `AuctionRepository.save`: Gson.toJson → UTF-8 → state.json.tmp → force(true) →
  atomic move thay state.json. Không fallback sang ghi đè không atomic.
- `AuctionRepository.load`: đọc UTF-8, kiểm JSON strict/duplicate keys;
  `AuctionStateValidator.validate` kiểm cấu trúc và các tham chiếu trước khi chạy.
- `AuctionService` constructor load dữ liệu; scheduler chỉ chạy khi Server.start.
  Session đăng nhập ở RAM, nên restart phải login lại. Dữ liệu nghiệp vụ và receipt
  ở snapshot. JSON không tự có transaction: service snapshot/rollback mới cung
  cấp tính nhất quán cho một lệnh.

Test thật: `AuctionRepositoryTest.committedSnapshotSurvivesRestartAndIgnoresUncommittedTemporaryFile`,
`corruptFileIsNeverResetOrOverwritten`, `writeFailurePreservesCommittedFileAndCanBeRetried`;
`AuctionServiceTest.failedBidDoesNotPublishOrPersistAndSameCommandCanBeRetried`,
`failedSettlementSaveRestoresBothWalletsAndRetryPaysExactlyOnce`.
`Entity implements Serializable` không có nghĩa production đang dùng ObjectOutputStream.

## 6. Chức năng nâng cao

`BiddingLogic.setAuto/processAutoBids` lưu maxBid/increment/order, lấy ứng viên bằng
PriorityQueue.remove, bỏ người đang dẫn đầu và kiểm lại tiền/trần sau mỗi lượt.
`AuctionServiceTest.autoBidRespectsLimitBudgetAndRegistrationOrder` kiểm thứ tự/trần;
`earlierAutoBidCannotBlockAnAffordableRivalAfterItsBudgetChanges` kiểm ngân sách đổi.

`AntiSnipingPolicy.computeNewEndTime` nhận thời điểm Server: bid còn tối đa 30 giây
và chưa hết hạn thì tăng deadline thêm 60 giây. `BiddingLogic.acceptBid` gọi thật;
`AntiSnipingPolicyTest` kiểm biên; `AuctionServiceTest.antiSnipingExtendsAndDoesNotSettleAtOldDeadline`
kiểm chưa thanh toán ở hạn cũ. Quy tắc là cộng 60 giây vào deadline cũ.

`AuctionBidHistoryView.render/addPricePoint/updateChartAxes` dùng timestamp làm X,
giá làm Y; chart giữ 30 điểm cuối, danh sách vẫn dùng history. Test
`ClientUiTest.historySnapshotOrdersCapsAndReplacesPointsAndExtensionClearsUrgency`;
`FullStackTest.twoAdaptersReceiveLiveBidWalletAndAutoBidWithoutMock` và
`ServerUiIntegrationTest.uiCreatedAccountsProductsBidsWalletAndImageSurviveNonDemoRestart`
kiểm nguồn Socket, UI, ví và dữ liệu sau restart.

## 7. JUnit, coverage và GitHub Actions

`pom.xml` chạy JUnit bằng Surefire; JaCoCo report và gate LINE ≥60% ở verify.
Checkstyle 14.4.0 dùng nguyên Google configuration trong config/checkstyle.xml,
kiểm cả production/test và chặn cả warning. Không dùng kết quả cấu hình whitespace
cũ để thay bằng chứng của cấu hình Google mới.
Hướng dẫn tr. 4 yêu cầu coverage ≥60%; dùng counter LINE của toàn bundle, không
lấy trung bình phần trăm các package. Không sửa test/threshold để làm build xanh.

`.github/workflows/ci.yml`: push/pull_request → Ubuntu, JDK 25 → Xvfb → Maven verify
với auction.ui.tests=true → upload Surefire/JaCoCo/Checkstyle kể cả khi lỗi.
Working tree chưa được commit/push nên không thể lấy một run cũ làm bằng chứng
CI cho nó. Phải xem artifact đúng SHA. Kết quả mới nhất và các FAIL/NOT TESTED
được ghi trong báo cáo audit, không học thuộc số liệu của các báo cáo archive.

## 8. Tự kiểm tra trước khi bảo vệ

Mỗi người mở một method trong mỗi phần trên, giải thích đầu vào/đầu ra, nơi gọi,
trường hợp bị từ chối và test tương ứng. Thử sửa miệng một quy tắc, ví dụ bước
giá hoặc khoảng anti-sniping: chỉ ra phải sửa đâu và chạy test nào. Diễn tập theo
[checklist LAN](WINDOWS_LAN_CHECKLIST.md). Giảng viên có thể hỏi bất kỳ phần nào,
không chỉ module cá nhân đã viết.

## 9. Ngoại lệ đúng tên trong hướng dẫn

`server/exception/InvalidBidException`, `AuctionClosedException` và
`AuthenticationException` extends ApiException. `Auction.requireMinimumBid` ném
loại đầu; `Auction.requireBidder` ném loại thứ hai khi phiên không nhận bid;
`AuthLogic.login` ném loại thứ ba khi sai thông tin hoặc tài khoản không dùng được.
Mã TCP vẫn BID_TOO_LOW/AUCTION_CLOSED/AUTHENTICATION_FAILED; catch ApiException ở
service/network bắt được lớp con. CustomExceptionTest gọi ba đường nghiệp vụ này,
không chỉ new exception để test tên lớp.

Kết quả lượt audit trước là baseline lịch sử. Menu nay dùng pane trong Scene,
không còn yêu cầu auto-hide/focus/ESC hay test Robot cho menu. Kết quả hiện tại ở
[báo cáo tối giản](../FINAL_SIMPLIFICATION_AND_REQUIREMENTS.md); không học thuộc
số liệu hoặc failure của báo cáo cũ. Các test nghiệp vụ và ESC form/dialog vẫn giữ.
