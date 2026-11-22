# Ngữ cảnh kỹ thuật Digital Auction

Đọc cùng [README](../README.md) để chạy dự án. Khi đọc code hoặc chuẩn bị vấn đáp, bắt đầu ở [bản đồ source](#bản-đồ-source), sau đó lần theo [một lệnh đặt giá](#diễn-giải-một-lệnh-đặt-giá). Bằng chứng kiểm thử và phần chưa xác minh nằm ở [cuối tài liệu](#trạng-thái-kiểm-chứng).

## Nền tảng và điểm khởi động

- Bài tập Java về đấu giá trực tuyến. Java 25, JavaFX 26.0.2 và FXML, Gson 2.13.2, TCP JSON qua socket. Server giữ dữ liệu tập trung trong JSON, không có MySQL hay JDBC ở production.
- [pom.xml](../pom.xml) dùng Maven, JUnit Jupiter 6.1.3, JaCoCo 0.8.14 với ngưỡng coverage dòng toàn dự án 60% và Checkstyle theo Google Java Style. Manifest JAR chạy `com.auction.server.network.Server`. Maven `javafx:run` chạy `com.auction.client.Main`, còn `com.auction.client.App` là launcher chuyển tiếp để chạy từ classpath.
- Layout `src/main/java/com/auction` gồm `client`, `server`, `shared`. FXML và CSS nằm trong `src/main/resources/com/auction/client`. Unit, integration, GUI fixture ở `src/test/java`. Test launcher Windows ở `src/test/powershell`.

## Bản đồ source

| Lớp hoặc package | Trách nhiệm và điểm bắt đầu |
|---|---|
| [client/controller](../src/main/java/com/auction/client/controller) | `AuthController` đăng nhập/đăng ký, `HomeController` lọc phiên và xem kết quả, `AuctionRoomController` đặt giá, auto-bid và mở chat, `SellerProductsController` lưu bản nháp sản phẩm, `WalletController` quản lý ví giả lập, `AdminController` quản trị. |
| [client/ui](../src/main/java/com/auction/client/ui) | `SellerProductFormView`, `AuctionScheduleInput`, `ScheduleTextFormatter` và `ProductImagePicker` chuẩn bị dữ liệu UI. `AuctionBidHistoryView.render()` hiển thị biểu đồ và lịch sử từ một snapshot. `AuctionAssistantView` hiển thị chat và gửi lại câu hỏi lỗi. |
| [client/navigation](../src/main/java/com/auction/client/navigation) | `ViewNavigator` tải FXML và chuyển màn hình. `ViewTasks` tách công việc nền, tác vụ ảnh, tác vụ AI và cập nhật FX. Đóng view gỡ listener/timer và kết quả UI cũ. Lệnh nghiệp vụ đã giao nền vẫn có thể hoàn thành. |
| [client/service](../src/main/java/com/auction/client/service) | Các interface cho phiên, auto-bid, ví, sản phẩm và admin. `ServiceRegistry` dùng chung `ServerServices`; production chỉ có mode `server`. `BidSubmissionTracker` theo dõi lệnh chưa rõ kết quả theo phiên/tài khoản. Fixture Mock chỉ ở `src/test`. |
| [client/service/server](../src/main/java/com/auction/client/service/server) | `ServerServices` quản lý session, command journal, heartbeat, feed và ảnh cache. `JsonTransport` đọc frame, khớp phản hồi theo `requestId` và dispatch push. `ServerMapper` chuyển JSON thành DTO; `ServerError` dịch mã lỗi cho UI. |
| [shared](../src/main/java/com/auction/shared) | `JsonProtocol` đóng khung, parse JSON nghiêm ngặt và đọc số nguyên chính xác. `ProductImage` giới hạn và giải mã ảnh. `AssistantChat` định nghĩa các loại câu hỏi chatbot. |
| [server/network/Server.java](../src/main/java/com/auction/server/network/Server.java) | `getInstance()` quản lý một Server production; constructor private. `Client.readLoop()` nhận request, kiểm envelope và điều phối. Mỗi client có reader, writer và queue output 256; scheduler gọi `tick()`. `TestServers` tạo instance cô lập chỉ ở test. |
| [server/service](../src/main/java/com/auction/server/service) | `AuctionService.execute()` là ranh giới giao dịch, dùng `AuctionContext` cho snapshot/event. `AuthLogic` quản lý session/quyền, `ProductLogic` CRUD phiên, `BiddingLogic` bid/auto-bid, `WalletLogic` hold và ví, `SettlementLogic` chuyển trạng thái/tất toán, `AntiSnipingPolicy` tính gia hạn. `AuctionStateValidator` kiểm dữ liệu khi nạp. |
| [server/model](../src/main/java/com/auction/server/model) và [server/dao](../src/main/java/com/auction/server/dao) | `Entity → User/Item` và lớp con thể hiện OOP. `ItemFactory` tạo loại sản phẩm. Record `Auction` diễn đạt luật và một trạng thái bất biến; `BidTransaction` biểu diễn bid đã nhận. `AuctionJsonMapper` ánh xạ domain và JSON; `AuctionRepository` khóa thư mục, đọc và lưu snapshot. `client/model` là DTO giao diện riêng. |

Luồng ghi đi từ FXML/controller qua `ViewTasks → ServerServices → JsonTransport → Server.Client.readLoop() → AuctionService.execute() → helper nghiệp vụ → AuctionRepository.save()`. Sau commit, server đưa event vào hàng đợi writer. Client nhận push, đọc snapshot khi cần và render bằng FX thread. Luồng AI đọc dữ kiện dưới khóa, rời khóa trước khi gọi provider.

## Nghiệp vụ và tính nhất quán

- Đăng ký tạo Bidder/Seller; admin được cấp khi storage mới. Mật khẩu dùng PBKDF2 với salt. Session trong RAM nên cần đăng nhập lại sau restart. Admin có thể quản lý tài khoản, hủy phiên với lý do và lưu trữ phiên đã hủy theo luật.
- Seller chỉ sửa/xóa phiên của mình khi `OPEN` và chưa có bid. `ProductLogic` kiểm danh mục, tên, mô tả, giá, bước giá, ảnh và thời gian. VND nguyên dương tối đa `9_000_000_000_000` mỗi giá trị. Khoảng phiên tối đa 365 ngày; ảnh < 5 MiB và phải giải mã được. Form tự chèn `/` và `:`, parse ngày/giờ nghiêm ngặt. `startNow` do server giải quyết khi lưu theo `Asia/Ho_Chi_Minh`, thời gian UI chỉ để hiển thị/kiểm trước.
- Bid tối thiểu bằng `currentPrice + minBidStep`. Admin và chủ phiên bị cấm đặt giá. Server kiểm giờ, trạng thái, tiền khả dụng và giá dưới cùng monitor. Hold là nghĩa vụ của người dẫn đầu trên các phiên chưa tất toán; người vừa mất vị trí được giải phóng hold. Ví là mô phỏng. Khi tính tiền cho người đang dẫn đầu cần tính lại hold của chính phiên đó.
- Trạng thái `OPEN → RUNNING`. Phiên hết giờ không có bid thành `FINISHED`. Phiên có winner chuyển `PAID` trong giao dịch kết thúc, trừ tiền người mua và cộng tiền người bán đúng một lần. Admin hủy phiên đang hoạt động thành `CANCELED`, giải phóng hold và tắt auto-bid. Mốc cuối là exclusive. Bid hợp lệ trong 30 giây cuối tăng deadline cũ 60 giây. Kiểm tra giờ, lịch sử bid và anti-sniping dùng cùng thời điểm nhận lệnh dưới khóa. Scheduler chỉ tạo snapshot giao dịch khi có phiên đến mốc chuyển trạng thái.
- Auto-bid chạy trong giao dịch gốc, dùng `PriorityQueue` theo thứ tự đăng ký, xét trần, bước, ngân sách và loại người đang dẫn đầu. Cùng trần ưu tiên luật đăng ký sớm nếu đủ điều kiện. Vượt trần số vòng thì trả `AUTO_BID_LIMIT` và hoàn tác giao dịch.
- `execute()` đồng bộ trên một service, bao gồm refresh theo giờ, tra receipt, đổi ví/leader, lưu và đệm event. `requestId` nhận diện một lượt gửi; `commandId` nhận diện lệnh nghiệp vụ có thể gửi lại. Đối soát cùng mã và payload; cùng mã khác payload trả `CONFLICT`. Event có thể tới trước ACK. Mất mạng phải đăng ký feed lại và đọc snapshot; heartbeat mặc định 5 giây để kiểm kết nối, không polling liên tục để thay push.
- `ApiException` mang mã nghiệp vụ; các lớp con gồm `InvalidBidException`, `AuctionClosedException`, `AuthenticationException`. `ServerError` là từ chối đã phản hồi, timeout/mất socket sau khi gửi có thể là kết quả chưa rõ. `STORAGE_ERROR` hoàn tác state/receipt và bỏ event chưa commit. Session RAM không nằm trong snapshot.
- Server đọc `data/state.json` mặc định, có `-Dauction.data` để chọn thư mục riêng. `server.lock` ngăn hai tiến trình cùng ghi. Repository đọc UTF-8 nghiêm ngặt, kiểm schema/quan hệ, ghi `state.json.tmp`, `force(true)` rồi atomic move. JSON lỗi được giữ để điều tra; không copy `.tmp` lên file chính. Giữ `schemaVersion=1` và trường tương thích nếu sửa mapper. Client có command journal theo tài khoản và ảnh cache theo server dưới `.digital-auction/server`, tách khỏi snapshot.

## OOP, collections và mẫu thiết kế

| Khái niệm | Cách đọc trong code |
|---|---|
| Đóng gói | `Entity` giữ định danh và tên ở trường private, truy cập qua phương thức. `Auction` phía server là record của một snapshot bất biến, `withBid()` tạo record mới. Record chỉ bất biến sâu nếu thành phần của nó bất biến. `AuctionService` giữ snapshot nội bộ và tạo bản sao sâu cho phản hồi. |
| Kế thừa và composition | `Entity → User → Bidder/Seller/Admin` và `Entity → Item → Electronics/Art/Vehicle`. Các lớp con dùng `super`. `Item` có thuộc tính `Seller` để biểu diễn chủ sở hữu. `SellerProductFormView` có helper cho lịch và ảnh. Controller phòng đấu giá có `AuctionBidHistoryView` để tách render khỏi điều phối. |
| Đa hình | `AuthLogic.createUser()` gọi `getRoleName()` trên biến kiểu `User` và nhận override của lớp thực tế. `ProductLogic.createAuction()` gọi `getCategory()` trên `Item` tạo bởi factory. Controller gọi interface `AuctionClientService`. Production dùng `ServerServices`, còn test có thể tiêm fixture. |
| Trừu tượng | Abstract class `User/Item` giữ phần dùng chung và khai báo hành vi cần override. Interface `AuctionClientService`, `ProductService`, `WalletService` mô tả hợp đồng theo mục đích. DTO trong `client.model` phục vụ hiển thị, có setter và định dạng riêng. Luật giá và ví nằm ở server, nơi VND được tính bằng `long`. |
| Collections và generics | `List<Bid>` giữ thứ tự lịch sử. `Map<String, CompletableFuture<JsonObject>>` ghép request đang chờ với response. `ConcurrentHashMap` và `CopyOnWriteArrayList` hỗ trợ thêm/gỡ listener trong lúc dispatch, `BlockingQueue<JsonObject>` giới hạn output và `PriorityQueue<JsonObject>` chọn ứng viên auto-bid theo ưu tiên. Generics kiểm kiểu khi biên dịch, còn thread safety dựa vào thiết kế truy cập. |

Ví dụ có caller tương ứng trong [ItemFactory](../src/main/java/com/auction/server/model/ItemFactory.java), [AuthLogic](../src/main/java/com/auction/server/service/AuthLogic.java) và [Auction](../src/main/java/com/auction/server/model/Auction.java):

```java
Seller seller = new Seller("s", "seller", "");
Item item = ItemFactory.forCategory("ART").create("i", "Tranh", "Sơn dầu", seller);
String id = item.getId();                  // Kế thừa từ Entity
String category = item.getCategory();       // Override của Art

List<User> users = List.of(new Bidder("b", "buyer", ""),
        seller, new Admin("a", "admin", ""));
List<String> roles = users.stream().map(User::getRoleName).toList();
// [BIDDER, SELLER, ADMIN]

Auction auction = new Auction("a", "s", 1_000_000, 100_000, "RUNNING", start, end);
auction.requireMinimumBid(1_200_000);
long minimum = auction.minimumNextBid();   // 1.100.000
```

`BidTransaction` giữ dữ kiện bid đã nhận như người bid, giá, thời điểm và cờ tự động. Bút toán ví thuộc `WalletLogic/SettlementLogic`. `Entity implements Serializable` là khai báo kiểu; đường lưu thật serialize JSON bằng Gson và File I/O. `AuctionJsonMapper` chỉ đổi phần dữ liệu cần thiết, ví dụ `automatic` sang trường `auto` và timestamp `LocalDateTime.toString()`. Việc serialize cả record để thay một phiên có thể bỏ rơi trường tương thích còn trong snapshot.

- **Singleton**. `Server.getInstance()` là `static synchronized`, constructor private và có thể tạo lại sau `close()`. Một JVM production dùng cùng listener, scheduler và service. `TestServers.create()` ở nguồn test mở constructor bằng reflection để dùng cổng động và storage riêng. Tính nhất quán của bid/ví do monitor service và file lock bảo vệ.
- **Factory**. `ItemFactory.forCategory()` là Simple Factory chọn creator. `create()` là Factory Method được từng creator override; `ProductLogic.createAuction()` gọi trong luồng thật. `JEWELRY/REAL_ESTATE/OTHER` dùng chung `CategoryItemFactory`. Khi bổ sung loại, đối chiếu factory, whitelist server và nhãn/icon `client.model.ItemCategory`.
- **Observer**. `AuctionContext.event()` đệm event rồi `AuctionService.commit()` gọi `Server.publish()`. Server đưa event theo feed/subscription vào queue. `ServerServices.onEvent()` gọi listener. `AuctionRoomController.registerRealtimeObserver()` và `detachAuction()` quản lý đăng ký theo phòng. `ViewTasks.dispatch()` đưa cập nhật lên FX thread. Sau reconnect đăng ký feed lại và đọc snapshot vì push có thể bị mất khi socket đóng.
- **Adapter và Repository**. `ServerServices` ánh xạ interface thành action TCP và DTO. `AuctionRepository` chuyên File I/O. `AntiSnipingPolicy` là policy cụ thể có thể test độc lập; project chưa có hệ Strategy có nhiều implementation được tiêm, cũng chưa có Command GoF với object lệnh execute/undo. `commandId` bảo đảm đối soát khi gửi lại cùng lệnh.

## Diễn giải một lệnh đặt giá

1. `AuctionRoomController.handlePlaceBid()` đọc số tiền, kiểm trước để phản hồi nhanh và chụp ngữ cảnh tài khoản/phòng. `ViewTasks.submit()` đưa công việc sang worker. `ServerServices.write()` lưu lệnh chờ; `JsonTransport.call()` gửi `PLACE_BID` qua TCP.
2. `JsonProtocol.writeFrame/readFrame` dùng độ dài 4 byte và JSON UTF-8, giới hạn 8 MiB trước khi đọc payload. TCP là luồng byte nên reader đọc đủ độ dài. Frame quá lớn hoặc hỏng làm đóng socket đó; những client khác vẫn hoạt động. `requestId` xác định một lần gửi; receipt server dùng khóa `userId:commandId` cho một thao tác.
3. `Server.Client.readLoop()` chuyển lệnh đúng envelope vào `AuctionService.execute()`. Dưới khóa chung, service refresh trạng thái, kiểm session, đối soát receipt và gọi `BiddingLogic.placeBid()`. `Auction.requireBidder()/requireMinimumBid()` kiểm quyền, thời gian và giá; `WalletLogic.availableForAuction()` kiểm tiền. Form và nút bị disable chỉ hỗ trợ giao diện.
4. `acceptBid()` đổi leader, ghi history, cập nhật hold/release, gọi anti-sniping và xét auto-bid. `commit()` lưu cùng snapshot và receipt rồi phát event. Client có thể thấy push trước ACK. `AuctionBidHistoryView.render()` dựng chart và danh sách từ snapshot; chart giữ 30 điểm mới nhất, lịch sử có thể dài hơn.

Ví dụ A và B cùng bid 1.200.000 VND khi giá cũ 1.000.000, bước 100.000, phiên còn giờ và cả hai đủ tiền. Nếu A lấy monitor trước và lưu thành công, B đọc giá mới và bị `BID_TOO_LOW` vì ngưỡng là 1.300.000. Receipt từ chối vẫn được lưu nếu giao dịch lưu thành công. Nếu A lưu lỗi, B có thể được nhận sau khi A rollback. Nếu có auto-bid, giá/leader cuối lệnh có thể đổi thêm. Người đang dẫn đầu chưa phải winner cuối, và thứ tự bấm nút không bảo đảm thứ tự vào monitor.

## Race condition, đồng bộ và phục hồi

| Nơi chạy | Vai trò |
|---|---|
| Acceptor của server, giới hạn 200 client | Nhận kết nối; executor virtual thread chạy reader và writer cho mỗi socket. Writer xử lý I/O mạng từ queue riêng. |
| Scheduler server | Gọi `tick()` mỗi 250 ms để mở phiên hoặc tất toán, đi qua cùng monitor với request. `synchronized` có tính reentrant nên `tick()` có thể gọi `execute()`. |
| `auction-reader` và `auction-events` | Reader hoàn tất future hoặc chuyển push vào executor event. Listener có thể đọc snapshot mà reader vẫn nhận được response. Callback chậm vẫn làm chậm dispatch. |
| Heartbeat và ViewTasks | Heartbeat 5 giây kiểm kết nối. ViewTasks tách pool nghiệp vụ, ảnh và AI. `Platform.runLater` đưa kết quả hiển thị về FX Application Thread. |

- **Race condition** xảy ra khi các luồng đọc/ghi trạng thái chung mà không phối hợp. **Lost update** là một ghi đè kết quả của lượt khác. Khóa phải bao cả đọc, kiểm tra, cập nhật và lưu. Khóa riêng từng phiên chưa đủ vì một ví có thể đang hold ở nhiều phiên.
- **Deadlock** là chờ vòng tròn trên tài nguyên giữ bởi nhau. Luồng lưu hiện có thứ tự monitor service → monitor repository, repository không gọi ngược service. Publisher production enqueue, không chờ response. Callback tùy ý có thể làm nghẽn nếu chặn lâu; giữ callback nhanh và tránh I/O chặn trên FX thread.
- **Rollback**. Khi từ chối nghiệp vụ, service có thể giữ thay đổi refresh trạng thái và lưu receipt từ chối. Khi lưu thất bại, service phục hồi toàn bộ snapshot trước lệnh và bỏ event chưa commit. `AuctionRepository` ghi đủ buffer rồi `force(true)` và `ATOMIC_MOVE + REPLACE_EXISTING`. `FileLock` chống ghi chồng từ tiến trình thứ hai; nếu atomic move không hỗ trợ thì báo lỗi. Phạm vi rollback là state JSON, không bao hàm session trong RAM hoặc bảo đảm qua mọi lỗi đĩa/mất điện.
- **Đối soát mất ACK**. `BidSubmissionTracker` dùng key `(username, auctionId)` và ticket. `UNKNOWN` cần đối soát journal; đọc lại một snapshot đơn lẻ không mở khóa. `AuctionRoomController.refreshAuction()` chụp key trước khi chạy nền; stamp của lượt đọc giúp loại kết quả cũ. `WalletController.reload()` dùng khóa `wallet-read` riêng với `wallet-write` để push đến trước ACK không làm mất callback của thao tác ghi. `LOGIN/REGISTER` có thể lấy token mới sau restart mà không cần một lệnh lỗi với token cũ trước đó.
- **Auto-bid**. `BiddingLogic.processAutoBids()` xét `maxBid`, `increment`, `order` và ngân sách sau từng lượt. Dùng `PriorityQueue.remove()` vì iterator của heap không duyệt theo thứ tự ưu tiên. Bỏ leader, ưu tiên đăng ký sớm khi cùng trần và đủ điều kiện, giới hạn 10.000 vòng để tránh lưu kết quả dở. `AntiSnipingPolicy` cộng 60 giây vào **deadline cũ** nếu còn hơn 0 và tối đa 30 giây tại một bid hợp lệ; có thể gia hạn nhiều lần.
- **Hold và kết thúc**. Tổng balance chưa trừ ngay lúc bid; tiền khả dụng giảm vì hold. Khi có winner, `SettlementLogic.settle()` ghi PAYMENT/SALE và đánh dấu settled trong cùng transaction. Có thể chỉ thấy `PAID` từ client vì `FINISHED` là bước giữa trong lần tất toán có bid. Không bid thì `FINISHED` không chuyển tiền; admin hủy thì release hold và dừng rule.

## Exception, dữ liệu sai và vòng đời UI

| Dấu hiệu | Chỗ đọc source |
|---|---|
| `BID_TOO_LOW`, `AUCTION_CLOSED`, `AUCTION_NOT_STARTED` | `Auction.requireMinimumBid()` và `requireBidder()`; hai exception theo yêu cầu là `InvalidBidException` và `AuctionClosedException`. |
| `AUTHENTICATION_FAILED`, `FORBIDDEN` | `AuthLogic.login()` ném `AuthenticationException`, service kiểm role và owner. Handler chuyển `ApiException` thành mã và adapter hiện thông báo. |
| Giá, lịch hay ảnh không hợp lệ | `SellerProductFormView` kiểm tiền và overflow, `AuctionScheduleInput` parse nghiêm ngặt bằng `LocalDate/LocalTime`, `ScheduleTextFormatter` chỉ định dạng nhập. `ProductLogic` kiểm lại bên server, xử lý `startNow`. `ProductImage` xác thực giới hạn byte, pixel và giải mã. |
| `STORAGE_ERROR` hoặc JSON hỏng | `AuctionRepository` dùng `IOException` cho lỗi I/O, từ chối JSON sai định dạng, trùng key hoặc trailing data; `AuctionStateValidator` kiểm schema, kiểu, quan hệ, hold/receipt trước khi chạy. `AuctionService` hoàn tác giao dịch nếu không lưu được. |
| Timeout hoặc socket đứt | `JsonTransport.Connection.fail()` kết thúc futures đang chờ, giữ cause; `ServerServices.recoverPendingCommands()` đối soát lệnh. Timeout khác response từ chối đã biết và khác cùng mã lệnh với payload xung đột. |

Form Seller là pane trong cùng Scene, có nút Lưu/Hủy/Quay lại và giữ draft khi lỗi. FileChooser ảnh dùng owner cửa sổ chính. Hộp chọn ngày có thể hiển thị tháng/thứ theo locale JVM, còn nhập tay theo định dạng cố định. Các bảng nhiều cột ở 640 px có thể cuộn ngang; phải kiểm đến được cột thao tác. Khi test responsive, chờ CSS/layout pulse của parent sau khi đổi kích thước hoặc feedback, rồi mới đo bounds. Đợt Seller cũ từng thấy `needsLayout=true` dù kích thước Stage đã đổi.

Menu tài khoản hiện gắn pane cùng Scene, mở bằng toggle/Space và đóng khi điều hướng, logout, ẩn owner hoặc tháo header. `UserMenu.hideMenu()` gỡ pane và unbind chiều cao. Ba test menu thay kỳ vọng của popup cũ về click ngoài/focus/ESC theo yêu cầu mới; các case bàn phím, điều hướng và cleanup vẫn được kiểm. Điều khiển ESC của form/dialog có phạm vi riêng. Khi rời màn hình, đóng scope ảnh/timer/listener và chặn kết quả đọc cũ. Lệnh ghi đã nhận tiếp tục được đối soát đúng ngữ cảnh.

## Gemini và chatbot

- `ServerServices.askAssistant()` qua TCP đến `Server`. Server kiểm session, lấy dữ kiện qua `AuctionService.assistantContext()`, rồi chuyển cho `AuctionAssistant`. Quota mặc định 4 job AI đồng thời toàn server, 1 đang chờ mỗi kết nối, 12 lượt/tài khoản/giờ và 200 lượt/server/ngày UTC. Câu hỏi tối đa 1.000 ký tự. Lịch sử RAM tối đa 3 lượt theo tài khoản/phòng/sản phẩm, hết hạn sau 30 phút.
- `AuctionAssistant.fromEnvironment()` chọn Gemini mặc định. `GeminiClient` gọi GenerateContent của `gemini-3.5-flash-lite` qua HTTPS và đặt `GEMINI_API_KEY` trong header `x-goog-api-key`. `AiHttp` giới hạn toàn bộ trao đổi trong 12 giây và phản hồi tối đa 128 KiB. HTTP 503 có thể được thử lại một lần trong thời hạn ban đầu. Client có timeout chat riêng. Ollama dùng `AUCTION_AI_PROVIDER=ollama`, `OLLAMA_MODEL` và loopback `127.0.0.1:11434` với timeout 30 giây. [Tài liệu model Gemini](https://aistudio.google.com/docs/models/gemini-3.5-flash-lite)
- `Thông tin phiên`, `Giá thị trường` và `So sánh giá` dùng dữ kiện server. Mặc định chưa có dữ liệu giá thị trường xác minh. `MarketPriceService` nhận giá từ nguồn có URL HTTPS, thời điểm và loại tiền cho cùng model/tình trạng. Câu hỏi tự do gửi model dữ kiện sản phẩm, giờ mở/đóng và tối đa 12 mốc giá gần nhất gồm giá và thời điểm. Quyền của trợ lý giới hạn ở đọc snapshot phiên.
- Launcher server Windows ưu tiên `GEMINI_API_KEY` trong môi trường rồi mới đọc khóa DPAPI ở `%LOCALAPPDATA%\DigitalAuction\gemini-key.dpapi`. `.\Gemini.cmd -ConfigureGemini` thay khóa DPAPI. `-GeminiFromClipboard` đọc clipboard trực tiếp. `Start-Server.cmd` tiếp tục khởi động nếu thiếu hoặc không đọc được khóa đã lưu. Java chạy trực tiếp dùng cấu hình môi trường.
- Launcher kiểm đầu vào trước khi lưu DPAPI. Launcher và Java bỏ BOM và khoảng trắng ở hai đầu, giữ nguyên nội dung hợp lệ bên trong. Trường hợp terminal gửi riêng ký tự `U+0016`, launcher đọc clipboard. Lỗi cấu hình cục bộ, xác thực Gemini, HTTP, DNS, TLS và timeout có thông báo riêng. Trạng thái `configured` xác nhận kiểm tra cục bộ.
- `.\Gemini.cmd -VerifyGemini` chạy profile Maven `live-gemini` với server thử, cổng động, dữ liệu tạm và một phòng JavaFX. Bài test kiểm tra câu trả lời Google xuất hiện trong UI. Maven mặc định loại test live. `MockGemini` và `OllamaClientTest` dùng HTTP loopback. `AssistantTcpIntegrationTest` kiểm tra chat, giới hạn dữ kiện và thao tác đấu giá trên cùng kết nối.
- UI hỗ trợ Enter gửi, Shift+Enter xuống dòng, Escape thu nhỏ và Gửi lại sau lỗi. Quota/lịch sử không nằm trong JSON. Gemini dùng tối đa 1.024 token đầu ra mỗi yêu cầu theo cấu hình client provider hiện tại.

## Quyết định kỹ thuật và giới hạn

- Một server có quyền quyết định nhất quán giữa phiên, ví, lịch sử và receipt. Snapshot JSON dễ dạy File I/O và dễ đối soát trong bài tập. Ghi lại toàn bộ snapshot dưới khóa chung tạo chi phí khi dữ liệu lớn. Một file lock chỉ bảo vệ một storage, không phải thiết kế cluster.
- Tách helper theo nghiệp vụ nhưng giữ một ranh giới giao dịch. Mapper dựng record theo snapshot hiện hành, tránh giữ domain cũ sau rollback. Không ghi socket hoặc chờ HTTP provider bên trong phần commit. Socket TCP ứng dụng chưa TLS, phù hợp LAN tin cậy.
- Những cách làm cũ đã rời production gồm MySQL/JDBC, fallback Mock, popup tài khoản native và seed demo khi khởi động. Popup từng gây lỗi focus/Robot ở CI; pane hiện tại cùng scene chỉ đóng theo hành vi code mô tả. Hồi quy UI layout nên chờ pulse thay vì tăng sai số tùy tiện. Giữ ảnh QA và bằng chứng từng đợt, không dùng kết quả lịch sử để suy ra trạng thái hiện tại.
- Mốc Git `ace115f` giữ báo cáo refactor OOP, audit đề, tài liệu LAN cũ, và ảnh QA theo đường dẫn cũ. Khi cần truy nguyên, có thể đọc `git show ace115f:docs/archive/JSON_ONLY_REFACTOR.md` hoặc `git show ace115f:docs/TEACHER_REQUIREMENTS_FINAL_AUDIT.md`. Các lệnh MySQL, mô tả Mock production và số liệu release cũ trong lịch sử chỉ mô tả phiên bản tại thời điểm đó.
- Ảnh QA đã lưu từng chứng minh form Seller từ hộp thoại riêng chuyển vào trang, layout 960/1200 px và DPI cao, màn hình Auth/Admin hẹp, kết quả phiên cũ và history/chart sau restart. Khi kiểm lại, ưu tiên test hiện tại và screenshot từ dữ liệu thử biệt lập. Sáu phiên cũ từng được xác định là seed server đã lưu trong JSON, khác Mock RAM. Giữ lịch sử nghiệp vụ và demo bằng phiên tạo riêng, không reset snapshot.

## Luyện vấn đáp và diễn tập

Bắt đầu bằng luồng JavaFX → TCP → service → repository → push; mở `handlePlaceBid`, `execute`, `commit` và `save`. Tiếp theo mở nơi gọi `getRoleName`/`getCategory`, ví dụ Factory Method và cách listener được gỡ khi rời phòng. Mỗi thành viên luyện giải thích quyền, giá, ví, trường hợp mất ACK, failure khi lưu và cách JUnit kiểm từng kết quả. Dùng [đề bài](2026-BTL.pdf) và [hướng dẫn LTNC](H%C6%B0%E1%BB%9Bng%20d%E1%BA%ABn%20BTL%20LTNC-2026.pdf) để đối chiếu thang điểm. Đối chiếu từng tiêu chí OOP, MVC, File I/O/Serialization, đồng thời, realtime, JUnit và CI; các tính năng nâng cao gồm auto-bid, anti-sniping và chart. Hồ sơ dự án chọn JSON tập trung theo yêu cầu File I/O. Khi dẫn từ PDF, phân biệt số trang in và số trang vật lý.

### Diễn tập trên dữ liệu riêng

Dùng [lệnh chạy LAN và sao lưu trong README](../README.md#chạy-nhiều-client-qua-lan). Dùng thư mục `-Dauction.data` độc lập, cổng trống và đăng ký Seller cùng hai Bidder trên UI. Khi thử lại restart, giữ cùng thư mục, cùng cổng và chụp giá, history, ví và hold trước khi dừng sạch. Không chiếu hash mật khẩu, token hoặc cả snapshot lên màn hình khi bảo vệ. Dọn biến môi trường bootstrap trong terminal thử sau khi xong.

| Tình huống | Điều cần quan sát |
|---|---|
| Hai máy, hai tài khoản, cùng phiên | Cùng danh sách và giá; bid cập nhật leader, chart và history không cần refresh tay. |
| Seller và Admin | Seller CRUD ảnh/giá/lịch phiên tương lai đúng owner; Admin khóa/đổi quyền/hủy phiên theo luật và giữ lý do hủy. |
| Bid thấp, bid bằng nhau, một ví nhiều phiên | Từ chối hợp lệ, chỉ một bid bằng giá được nhận nếu cả hai đã đến server, tổng hold không vượt số dư và không rút được phần hold. UI có thể nhận push và chặn trước khi gửi; cần test có latch để chứng minh đồng thời ở server. |
| Auto-bid và anti-sniping | Kiểm trần/bước/ngân sách, quyền ưu tiên khi cùng trần; bid sát 30 giây làm deadline và countdown tăng 60 giây. Dùng phiên thử riêng cho tình huống này. |
| Hết giờ, hủy và restart | Không bid → FINISHED; có bid → PAID với một winner và một cặp PAYMENT/SALE; CANCELED release hold. Sau restart, đăng nhập lại và kiểm receipt, ảnh, ví và rule. Server thứ hai cùng data bị file lock chặn. |
| Mất mạng hoặc đóng client | Client kia tiếp tục. Sau mất ACK phải đối soát journal/history cùng `commandId`; thử đóng/mở phòng để phát hiện listener hoặc timer trùng. |
| UI ở 640 px và DPI cao | Truy cập cột thao tác Admin, kiểm Auth, cuộn tới lịch/ảnh/Lưu ở Seller, lỗi giữ draft, menu cùng Scene và đóng đúng khi điều hướng. |

LAN hai máy vẫn chưa được xác minh. Khi diễn tập thực tế, ghi revision, số cổng, môi trường, người thử và PASS/FAIL/chưa thử cho từng tình huống. Với kiểm thử đồng thời cục bộ, lệnh để nhóm sử dụng khi cần là:

```powershell
mvn -B '-Dtest=AuctionServiceTest#equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice,ServerIntegrationTest#simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer' test
```

### Bản đồ test theo câu hỏi kỹ thuật

| Muốn chứng minh | Test nên mở |
|---|---|
| Domain, đa hình và exception | `AuctionTest.startIsInclusiveAndEndIsExclusive`, `ItemFactoryTest.userRoleDispatchUsesTheRuntimeSubclass`, `createsCorrectCategoryAndPreservesProductAndOwner`, `CustomExceptionTest`. Test thời gian dùng `MutableClock` và tick chủ động. |
| Hai bid, hold và auto-bid | `AuctionServiceTest.equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice`, `concurrentAuctionsCannotOverspendOneWallet`, `autoBidRespectsLimitBudgetAndRegistrationOrder`, `earlierAutoBidCannotBlockAnAffordableRivalAfterItsBudgetChanges`, `antiSnipingExtendsAndDoesNotSettleAtOldDeadline`. |
| JSON hỏng và rollback | `AuctionRepositoryTest.committedSnapshotSurvivesRestartAndIgnoresUncommittedTemporaryFile`, `corruptFileIsNeverResetOrOverwritten`, `writeFailurePreservesCommittedFileAndCanBeRetried`; `AuctionServiceTest.failedBidDoesNotPublishOrPersistAndSameCommandCanBeRetried`, `failedSettlementSaveRestoresBothWalletsAndRetryPaysExactlyOnce`, `settlementPaysSellerExactlyOnceAndSurvivesRestart`. |
| TCP, mất ACK, Observer và đăng nhập lại | `ServerIntegrationTest.twoRealSocketsReceivePushAndReconnectReplaysOnlyOneBid`; `FullStackTest.lostBidAcknowledgementRetriesTheSameCommandNotAnotherBid`, `pendingCommandSurvivesClientRestartAndIsAppliedOnlyOnce`, `failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges`, `loginAfterRestartDoesNotRequireAnExpiredSessionReadFirst`. |
| Singleton và seed | `ServerStartupTest.productionConstructionIsPrivate` và `defaultServerNeverSeedsEvenWhenOldDemoPropertyIsPresentAndRestartPreservesData` kiểm nhiều caller, port thực và snapshot sau restart. |
| UI, chart, persistence và AI | `ClientUiTest.oldRecoveryCannotReconcileAnotherRoomUnknownCommand`, `historySnapshotOrdersCapsAndReplacesPointsAndExtensionClearsUrgency` và các test menu; `ServerUiIntegrationTest.uiCreatedAccountsProductsBidsWalletAndImageSurviveNonDemoRestart`; `AuctionAssistantTest`, `AssistantTcpIntegrationTest` và `JsonTransportTest`. |

`mvn test` chạy test, `package` đóng JAR và `verify` chạy các quality gate được gắn vào phase. JaCoCo LINE và BRANCH là hai counter khác nhau. CI dùng JDK 25, Linux/Xvfb, bật JavaFX test và upload artifact; đối chiếu đúng SHA khi cần bằng chứng hosted. Kết quả local không đánh giá mọi lịch chạy đa luồng, tải lớn hoặc native Windows ở mọi DPI.

## Trạng thái kiểm chứng

- Ngày 21/11/2026, Maven `verify` offline trên Windows/JDK 25 bật `auction.ui.tests=true` và scale 150% đã qua 291 test, coverage dòng 83,90% và Checkstyle 0 vi phạm. `mvn test` chạy tập trung 70 test hồi quy cũng đạt. Log ở `target/audit-verified.log` và `target/audit-regression-test.log`. JaCoCo report và quality gate đọc cùng file dữ liệu của lượt chạy.
- `.\Gemini.cmd -VerifyGemini` dùng khóa Windows đã lưu, nhận HTTP 200 và xác nhận câu trả lời xuất hiện trong phòng JavaFX thử. `src/test/powershell/GeminiLaunchTest.ps1` kiểm nhập ẩn, clipboard, thứ tự ưu tiên khóa và lưu, đọc, thay khóa giả qua DPAPI. JAR server trả `PING` trên cổng thử với thư mục dữ liệu mới khi Gemini chưa cấu hình.
- Còn cần xác minh workflow Linux/Xvfb trên GitHub Actions cho phiên bản được push, LAN trên hai máy vật lý, tải lớn và lỗi phần cứng. Nguồn giá thị trường production chưa được tích hợp.

## Bàn giao cho Codex

1. Đọc tài liệu này và README, rồi kiểm `git status`, hướng dẫn repo, Java, FXML, script và test liên quan. Ưu tiên source hiện tại khi tài liệu lịch sử khác code.
2. Bảo toàn thay đổi đang làm, data, config, schema, receipt, quyền, atomicity và đường chạy production. Dùng fixture hoặc thư mục dữ liệu biệt lập để thử. Không tự chỉnh nghiệp vụ hoặc refactor lớn để giảm dòng.
3. Ưu tiên Java dễ đọc, có tên rõ, luật nghiệp vụ ở server và thông báo lỗi có nghĩa. Kiểm tra cả rollback, mất ACK và caller cũ khi sửa lệnh ghi.
4. Giữ secret ngoài source, log, dữ liệu mẫu, tham số dòng lệnh và Git. Chỉ chạy live khi có quyền dùng credential hợp lệ. Không tự commit/push.
5. Cập nhật README hoặc tài liệu này khi có thay đổi giá trị lâu dài. Không tạo một báo cáo Markdown cho mỗi lần sửa.
