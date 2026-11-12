# 40 câu hỏi vấn đáp có đáp án

Các đường dẫn class dưới `src/main/java/com/auction/`; test dưới
`src/test/java/com/auction/`. Tra method và số dòng trong [CODE_MAP.md](CODE_MAP.md).
Mỗi câu có câu trả lời nhanh, giải thích và lỗi dễ mắc; không học thuộc một câu
rồi bỏ qua phần code minh họa.

## OOP

### 1. Bốn tính chất OOP nằm ở đâu?

**Đáp nhanh:** đóng gói trong Entity/AuctionService; kế thừa User/Item; đa hình ở
getRoleName/getCategory; trừu tượng ở User/Item và service interfaces.
**Kỹ thuật:** chỉ rõ call site `createUser()`/`createAuction()`, không chỉ chỉ ra
chữ override. **Code:** server/model và AuctionService. **Dễ sai:** đưa ví dụ lớp
không có trong repo hoặc gọi mọi getter là toàn bộ OOP.

### 2. Vì sao User là abstract, không tạo trực tiếp?

**Đáp nhanh:** tài khoản phải có vai trò cụ thể do lớp con cung cấp.
**Kỹ thuật:** constructor protected giữ passwordHash chung, `getRoleName()` là
abstract. **Code:** User, Bidder, Seller, Admin; `createUser()`. **Dễ sai:** nói
abstract class không có constructor hoặc không chứa method cụ thể.

### 3. Ví dụ đa hình thực sự chạy trong dự án?

**Đáp nhanh:** User account tham chiếu Admin/Seller/Bidder, gọi getRoleName.
**Kỹ thuật:** dynamic dispatch theo lớp runtime; kết quả đưa vào JSON role.
**Code:** AuthLogic.createUser; ItemFactoryTest.userRoleDispatchUsesTheRuntimeSubclass.
**Dễ sai:** nhầm overloading với overriding, hoặc chỉ đưa danh sách lớp kế thừa.

### 4. Record Auction có phải đóng gói không?

**Đáp nhanh:** có, các component private final, chỉ đọc bằng accessor, luật qua method.
**Kỹ thuật:** record chứa toàn giá trị bất biến String/long/LocalDateTime; không
giữ reference JsonObject bị sửa sau rollback. **Code:** server/model/Auction.
**Dễ sai:** nói mọi record đều deep immutable, kể cả khi component là List mutable.

### 5. Vì sao client và server đều có Auction?

**Đáp nhanh:** client dùng DTO hiển thị, server dùng domain kiểm tra bid.
**Kỹ thuật:** package khác nhau; backend dùng long và luật quyền/thời gian, UI có
setter và định dạng. **Code:** client/model/Auction, server/model/Auction,
ServerMapper, AuctionJsonMapper. **Dễ sai:** nói sửa giá trong DTO client đổi được
giá chính thức trên server.

### 6. BidTransaction có chuyển tiền không?

**Đáp nhanh:** không, nó biểu diễn một bid đã chấp nhận trong lịch sử.
**Kỹ thuật:** record gồm id, auctionId, bidderUsername, amount, timestamp, automatic;
wallet log và settlement là trách nhiệm khác. **Code:** acceptBid, settle, log,
AuctionJsonMapper.bid. **Dễ sai:** nhầm bid history với collection transactions của ví.

## Design Patterns

### 7. Singleton nghiêm ngặt ở đâu?

**Đáp nhanh:** MockDataStore có private constructor và static final INSTANCE.
**Kỹ thuật:** JVM class initialization công bố instance an toàn; vẫn phải khóa
khi sửa store. **Code:** MockDataStore, ServiceRegistry.mockServices.
**Dễ sai:** nói Singleton tự làm các ArrayList thread-safe hoặc gọi mock là JSON server.

### 8. Server.getInstance có cấm mọi instance thứ hai không?

**Đáp nhanh:** không, nó cấp instance mặc định; constructor public phục vụ test.
**Kỹ thuật:** accessor static synchronized, có thể tạo lại khi closed. File lock
ngăn ghi cùng thư mục. **Code:** Server.getInstance, constructor, AuctionRepository.
**Dễ sai:** chỉ ra private Server constructor không tồn tại hoặc coi file lock là Singleton.

### 9. Factory Method khác Simple Factory thế nào trong code?

**Đáp nhanh:** create là abstract method được override; forCategory là static selector.
**Kỹ thuật:** anonymous creator dùng category chọn Item con; đây là biến thể tham
số hóa có switch tập trung. **Code:** ItemFactory, createAuction.
**Dễ sai:** gọi bất kỳ hàm static tạo object là Factory Method GoF.

### 10. Thêm một loại sản phẩm sửa đâu?

**Đáp nhanh:** Factory, validation category, enum nhãn/icon client và test.
**Kỹ thuật:** thêm subclass khi có hành vi riêng; hiện ba danh mục bổ sung dùng
CategoryItem. **Code:** ItemFactory, validateProduct, ItemCategory, ItemFactoryTest.
**Dễ sai:** hứa không sửa bất kỳ code cũ nào hoặc chỉ thêm enum mà quên server validation.

### 11. Observer đăng ký và hủy ở đâu?

**Đáp nhanh:** controller đăng ký listener khi mở phòng, gỡ khi đổi phòng/dispose.
**Kỹ thuật:** map theo auctionId, CopyOnWriteArrayList; ViewTasks dispatch callback
sang FX. **Code:** registerRealtimeObserver, detachAuction, dispose, ServerServices.
**Dễ sai:** chỉ đăng ký mà không gỡ, dẫn tới cập nhật view cũ và callback trùng.

### 12. Có Strategy hay Command GoF không?

**Đáp nhanh:** chưa có đủ cấu trúc để tuyên bố hai pattern đó.
**Kỹ thuật:** AntiSnipingPolicy là policy cụ thể; handleAction switch và commandId
là routing/idempotency, không phải object Command có execute/undo. **Code:**
AntiSnipingPolicy, handleAction. **Dễ sai:** thấy chữ “command” hoặc “policy” rồi gán pattern.

## MVC

### 13. View, Model, Controller nằm ở đâu?

**Đáp nhanh:** FXML/CSS là View, DTO client là Model hiển thị, JavaFX controller điều phối UI.
**Kỹ thuật:** service interfaces và adapter nối UI với server model/service/repository.
**Code:** auction-room-view.fxml, AuctionRoomController, AuctionClientService.
**Dễ sai:** cho rằng MVC chỉ là tên ba thư mục hoặc server phải có màn hình JavaFX.

### 14. Controller kiểm tra giá có vi phạm MVC không?

**Đáp nhanh:** kiểm tra để phản hồi nhanh được, nhưng server phải kiểm tra lại.
**Kỹ thuật:** giá UI có thể cũ, nên validateBidder/requireMinimumBid/ví chạy trong
transaction server. **Code:** handlePlaceBid, BiddingLogic.placeBid, Auction.
**Dễ sai:** tin nút disable là đủ bảo vệ quyền hoặc coi UI validation là dữ liệu chính thức.

### 15. Vì sao chưa tách AuctionService thành nhiều manager?

**Đáp nhanh:** ví, bid, rules và command receipt phải commit/rollback cùng nhau.
**Kỹ thuật:** tách luật thuần sang Auction là cải thiện nhỏ; chia transaction hoặc
lock không có căn cứ dễ phá tính nhất quán. **Code:** execute, commit, Auction.
**Dễ sai:** nói class dài luôn sai hoặc khẳng định service hiện không còn nhiều trách nhiệm.

## Socket

### 16. TCP có giữ ranh giới mỗi JSON message không?

**Đáp nhanh:** không, phải framing.
**Kỹ thuật:** writeInt độ dài 4 byte, JSON UTF-8, readNBytes đủ length; giới hạn
8 MiB trước cấp phát. **Code:** shared/JsonProtocol.readFrame/writeFrame.
**Dễ sai:** cho rằng một read luôn tương ứng một send hoặc dùng số ký tự làm số byte UTF-8.

### 17. requestId khác commandId?

**Đáp nhanh:** requestId ghép response; commandId nhận diện thao tác ghi qua retry.
**Kỹ thuật:** requestId → future trong pending map; userId:commandId → receipt trong
state.commands. **Code:** JsonTransport.call, AuctionService.execute.
**Dễ sai:** đổi commandId mỗi lần retry rồi hứa không bị trừ tiền hai lần.

### 18. Client mất mạng thì chuyện gì xảy ra?

**Đáp nhanh:** pending futures lỗi, adapter báo mất kết nối; reconnect đăng ký feed lại.
**Kỹ thuật:** command chưa rõ kết quả giữ sổ pending và replay mã cũ; restart server
làm session RAM mất nên cần login. **Code:** Connection.fail, response, replay,
recoverPendingCommands. **Dễ sai:** coi timeout là chắc chắn server chưa commit.

### 19. Một client gửi frame quá lớn có làm dừng server?

**Đáp nhanh:** kết nối đó bị đóng, client khác vẫn dùng được.
**Kỹ thuật:** readFrame ném IOException trước allocation payload; readLoop finally
close riêng client. **Code:** JsonProtocol và ServerIntegrationTest.
**Dễ sai:** bắt lỗi rồi bỏ qua length để tiếp tục đọc stream đã mất đồng bộ.

## Multithreading

### 20. Server xử lý nhiều client bằng gì?

**Đáp nhanh:** một acceptor và hai task virtual thread đọc/ghi cho mỗi socket.
**Kỹ thuật:** executor newVirtualThreadPerTaskExecutor; scheduler riêng gọi tick.
Nghiệp vụ vẫn tuần tự theo service lock. **Code:** Server.accept/start/Client.
**Dễ sai:** nói một virtual thread cho cả hệ thống hoặc nhiều thread đồng nghĩa nhiều transaction song song.

### 21. Vì sao không sửa UI ngay từ listener network?

**Đáp nhanh:** scene graph phải được cập nhật trên FX Application Thread.
**Kỹ thuật:** ViewTasks.dispatch dùng Platform.runLater và kiểm tra closed; submit
chạy network nền. **Code:** onBidReceived, ViewTasks.
**Dễ sai:** dùng synchronized(Label) để thay cho runLater hoặc gọi socket blocking trên FX thread.

### 22. Tại sao có reader riêng với event dispatch?

**Đáp nhanh:** listener đọc snapshot có thể chờ response, reader phải tiếp tục nhận được response đó.
**Kỹ thuật:** JsonTransport.read chuyển event sang single executor, reader hoàn tất
future độc lập. **Code:** JsonTransport.read, ServerServices.onEvent.
**Dễ sai:** gọi callback có blocking request ngay trong vòng đọc duy nhất.

## Concurrency

### 23. Hai người cùng bid 1.200.000 khi giá 1.000.000?

**Đáp nhanh:** nếu bước 100.000 và đủ điều kiện, người lấy monitor trước được nhận;
người sau thấy mức tối thiểu mới 1.300.000 và BID_TOO_LOW.
**Kỹ thuật:** khóa bao trùm cả read/check/update/save, không chỉ cập nhật giá.
**Code:** execute, requireMinimumBid, test equalConcurrentBids.
**Dễ sai:** nói luôn A thắng hoặc người nhấn sớm hơn chắc chắn thắng; quên auto-bid/lỗi lưu.

### 24. Khóa theo từng phiên có đủ bảo vệ ví không?

**Đáp nhanh:** chưa đủ nếu một ví tham gia nhiều phiên.
**Kỹ thuật:** hai khóa phiên khác nhau vẫn có thể cùng thấy số dư cũ; khóa service
hiện bảo vệ cả heldBalance của mọi phiên. **Code:** availableForAuction,
concurrentAuctionsCannotOverspendOneWallet. **Dễ sai:** giảm scope lock mà không xét tài nguyên dùng chung.

### 25. Vì sao không có hai winner hoặc thanh toán hai lần?

**Đáp nhanh:** chỉ một highestBidderId; settle chạy dưới khóa, có settled/status chặn lại.
**Kỹ thuật:** tiền buyer/seller và trạng thái PAID lưu chung snapshot; tick lại/restart
không settle phiên PAID. **Code:** refreshAuctions, settle, settlement test.
**Dễ sai:** gọi người đang dẫn đầu là winner cuối hoặc nói chỉ cần UUID là tránh thanh toán lặp.

### 26. Deadlock khác race condition?

**Đáp nhanh:** deadlock là chờ vòng tròn; race là kết quả phụ thuộc truy cập trạng thái không phối hợp.
**Kỹ thuật:** thứ tự hiện service → repository, repository không callback lên
service; publisher production enqueue. **Code:** execute, commit, publish.
**Dễ sai:** tuyên bố dùng synchronized nên không bao giờ deadlock.

### 27. Queue đầy và observer lỗi thì sao?

**Đáp nhanh:** queue server đầy đóng client chậm; một listener lỗi không chặn các listener khác.
**Kỹ thuật:** outbound 256, offer không đợi; callback catch RuntimeException riêng.
Listener chậm vẫn làm chậm event dispatch. **Code:** Client.offer,
ServerServices.onEvent, failingBidObserver test.
**Dễ sai:** hứa queue vô hạn, không mất sự kiện, hoặc mọi loại Throwable đều được nuốt.

## JSON và persistence

### 28. Vì sao JSON thay MySQL?

**Đáp nhanh:** phù hợp File I/O và quy mô bài tập, dễ đọc dữ liệu tập trung.
**Kỹ thuật:** transaction vẫn cần service lock, file lock, atomic replacement và
receipt; snapshot lớn dần, không hỗ trợ multi-writer phân tán. **Code:** AuctionRepository.
**Dễ sai:** nói JSON tự có transaction hoặc đề bắt buộc MySQL.

### 29. Lưu JSON thất bại thì bid và tiền ra sao?

**Đáp nhanh:** rollback state, xóa events, trả STORAGE_ERROR.
**Kỹ thuật:** state.deepCopy trước transaction; commands/bids/wallet/rules cùng
snapshot; không push thành công trước save. **Code:** commit,
failedBidDoesNotPublishOrPersistAndSameCommandCanBeRetried.
**Dễ sai:** chỉ rollback giá mà quên tiền/command hoặc nói rollback mọi session RAM.

### 30. Vì sao dùng file tạm và atomic move?

**Đáp nhanh:** tránh làm hỏng snapshot đang tốt khi ghi dở.
**Kỹ thuật:** write đủ buffer, force(true), ATOMIC_MOVE + REPLACE_EXISTING; không hỗ
trợ thì báo lỗi, không ghi đè thường. **Code:** AuctionRepository.save.
**Dễ sai:** hứa đã thử mọi tình huống mất điện/đĩa hoặc coi flush Java stream là đủ cho mọi tầng lưu trữ.

### 31. Có đọc state.json từ client không?

**Đáp nhanh:** không, client chỉ qua socket.
**Kỹ thuật:** client có pending command và image cache riêng; server giữ snapshot
users/auctions/rules/commands/transactions. **Code:** ServerServices.pendingFile/getImagePath,
AuctionRepository. **Dễ sai:** nhầm mọi file JSON trên client là database server.

### 32. Mapper mới có làm rơi trường JSON cũ không?

**Đáp nhanh:** không tái tạo toàn bộ snapshot; chỉ đọc các trường cần và ghi bid mới.
**Kỹ thuật:** state JSON gốc vẫn được giữ/save; `auto` được map rõ từ automatic,
timestamp giữ LocalDateTime.toString như trước. **Code:** AuctionJsonMapper,
AuctionJsonMapperTest. **Dễ sai:** serialize record Auction thành toàn bộ phiên rồi làm mất các trường ngoài domain.

## Exception

### 33. ApiException khác IOException?

**Đáp nhanh:** ApiException mô tả lỗi nghiệp vụ bằng code; IOException mô tả lỗi I/O.
**Kỹ thuật:** service đổi lỗi lưu thành STORAGE_ERROR và rollback; handler đổi lỗi
request thành response, adapter đổi code thành thông báo UI. **Code:** ApiException,
execute/commit, ServerError. **Dễ sai:** catch Exception rồi luôn trả success.

### 34. Server từ chối khác kết quả chưa biết thế nào?

**Đáp nhanh:** response lỗi rõ ràng là rejection; timeout chưa chứng minh kết quả.
**Kỹ thuật:** BidSubmissionTracker UNKNOWN giữ khóa qua đổi view; adapter đối soát
pending command rồi mới đọc snapshot cho phép tiếp tục. **Code:** BidSubmissionTracker,
ServerServices.write/replay; ClientUiTest.
**Dễ sai:** tải lịch sử một lần rồi xóa pending và cho gửi một command mới.

## Unit Test

### 35. Test thời gian không đợi thật hai giờ bằng gì?

**Đáp nhanh:** inject Clock và advance đồng hồ test.
**Kỹ thuật:** MutableClock cho now theo giờ server, tick chủ động; kiểm tra end
boundary và settlement chính xác. **Code:** AuctionServiceTest.MutableClock,
AuctionTest.startIsInclusiveAndEndIsExclusive.
**Dễ sai:** dùng sleep dài làm test chậm, dễ sai theo tốc độ máy.

### 36. Unit test khác test socket và GUI?

**Đáp nhanh:** domain test kiểm luật thuần; integration kiểm đường truyền/lưu trữ;
GUI test còn nạp FXML và chạy FX toolkit.
**Kỹ thuật:** @TempDir cho server/client data, port 0, assertion về lịch sử/ví/push;
GUI opt-in bằng auction.ui.tests. **Code:** AuctionTest, FullStackTest,
ServerIntegrationTest, ServerUiIntegrationTest.
**Dễ sai:** suy từ loopback PASS sang LAN hai máy hoặc chạy test trên data thật.

## Maven và GitHub Actions

### 37. mvn test, package, verify khác nhau?

**Đáp nhanh:** test chạy kiểm thử; package đóng JAR; verify còn chạy quality checks
được gắn ở phase verify.
**Kỹ thuật:** compiler release 25, Surefire JUnit, Shade đóng JAR, JaCoCo report và
Checkstyle check tại verify. **Code:** pom.xml, run.ps1.
**Dễ sai:** dùng -DskipTests rồi tuyên bố test PASS.

### 38. Coverage và CI chứng minh điều gì?

**Đáp nhanh:** coverage đo phần code được chạy; CI lặp build/test trên GitHub.
**Kỹ thuật:** JaCoCo báo line/branch riêng, không chứng minh assertions đúng mọi
trường hợp; CI dùng Java 25 + Xvfb bật UI tests và upload reports. **Code:** pom.xml,
.github/workflows/ci.yml, AUDIT_AND_RESULTS.
**Dễ sai:** gọi line coverage là branch coverage, bỏ UI khỏi mẫu số mà không nói,
hoặc nói CI GitHub xanh dù mới chỉ chạy Maven local.

## Chức năng đấu giá

### 39. Auto-bid chọn người và giá thế nào?

**Đáp nhanh:** sắp rules theo order đăng ký, bỏ leader, chọn bid đủ tiền không vượt
maxBid, lặp đến khi không còn lượt hợp lệ.
**Kỹ thuật:** increment >= minBidStep, bằng trần ưu tiên luật sớm; giới hạn 10.000
vòng, vượt thì rollback giao dịch thay vì lưu dở. Server dùng list sắp xếp, mock
dùng PriorityQueue. **Code:** setAuto, processAutoBids, AutoBidEngine.
**Dễ sai:** nói server thật có PriorityQueue hoặc tự bid tiếp vào chính leader.

### 40. Anti-sniping và số dư giữ tiền hoạt động thế nào?

**Đáp nhanh:** bid khi còn 0 < thời gian <=30 giây cộng 60 giây vào deadline; bid
giữ tiền, hết phiên mới chuyển tiền.
**Kỹ thuật:** total balance không trừ ngay, held tính từ các phiên chưa settled mà
người dùng dẫn đầu; bị vượt/hủy thì giải phóng. Bid sát giờ có thể gia hạn nhiều
lần; đúng deadline không nhận bid nữa. **Code:** AntiSnipingPolicy, acceptBid,
heldBalance, settle; AntiSnipingPolicyTest.
**Dễ sai:** cộng 60 vào lúc bid thay vì deadline hiện tại, hoặc trừ balance hai
lần ở cả hold và payment.
