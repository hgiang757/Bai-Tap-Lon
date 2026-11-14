# Design Patterns đang được sử dụng

Vị trí file và tên method: [CODE_MAP.md](CODE_MAP.md). Production nằm dưới
`src/main/java/com/auction/`; fixture được ghi rõ dưới `src/test/java/`.

## Singleton production và vòng đời Server

`server/network/Server` là final, constructor `(host, port, data, demo)` private.
`getInstance()` là static synchronized: mọi caller production nhận cùng một Server
đang hoạt động, nên cùng listener, scheduler và AuctionService. `main()` dùng accessor
này; không có public factory nào tạo thêm Server. Chỉ tạo lại khi instance cũ đã đóng.
Đây là Singleton theo vòng đời tài nguyên: không hứa một đối tượng tồn tại vĩnh viễn.
Singleton không tự làm state thread-safe; `AuctionService.execute()` vẫn phải synchronized.

`ServerStartupTest.productionConstructionIsPrivate()` kiểm tra constructor;
`defaultServerNeverSeedsEvenWhenOldDemoPropertyIsPresentAndRestartPreservesData()`
cho 16 luồng gọi accessor, kiểm cùng instance, chạy TCP, đóng rồi mở lại và đăng nhập
bằng dữ liệu đã lưu. Không dùng dữ liệu thật làm fixture.

`src/test/java/com/auction/server/network/TestServers.create()` là helper chỉ dành
cho test: reflection mở constructor private, giữ khả năng thử nhiều cổng/thư mục
mà không thêm đường tạo instance vào API production. Helper không nằm trong JAR.
Reflection có thể phá Singleton thông thường; đây không phải ranh giới bảo mật.
MockDataStore cũng có private constructor nhưng chỉ ở test, không dùng làm bằng
chứng Singleton production. File lock và bind port là bảo vệ tài nguyên, không thay
thế Singleton.

## Factory Method kết hợp Simple Factory

**Vấn đề:** tạo sáu loại sản phẩm cần tập trung ánh xạ category → implementation,
không lặp switch/new trong các controller và service.

**Vị trí:** `server/model/ItemFactory.forCategory()` và abstract `create()`;
`ProductLogic.createAuction()` là caller thật, được facade AuctionService gọi.

**Hoạt động:** `forCategory()` kiểm tra sáu mã hợp lệ, trả một anonymous subclass
của ItemFactory override `create()`. Factory Method đó trả Electronics/Art/Vehicle;
JEWELRY/REAL_ESTATE/OTHER dùng private nested `CategoryItem` có category riêng.
`createAuction()` làm việc qua kiểu Item, gọi `getCategory()` bằng đa hình.

Phân loại chính xác: static `forCategory()` là **Simple Factory chọn creator**;
abstract/override `create()` là **Factory Method tham số hóa**. Đây là biến thể
nhỏ có switch tập trung, không phải một hệ GoF có sáu concrete creator class và
workflow phức tạp. Không gọi mọi static method trả object là Factory Method.

```java
Item item = ItemFactory.forCategory("ART")
        .create("i", "Tranh", "Sơn dầu", new Seller("s", "seller", ""));
String category = item.getCategory(); // ART
```

**Lợi ích:** caller không phụ thuộc constructor mỗi loại; ItemFactoryTest kiểm tra
cả kiểu runtime và thông tin sản phẩm/owner. **Hạn chế:** thêm category vẫn cần
sửa selector/switch và danh mục validation, chưa hoàn toàn đóng với thay đổi.

**Thêm loại mới:** cập nhật ItemFactory, tập category trong
`ProductLogic.validateProduct()`, `client/model/ItemCategory` (nhãn/icon), test.
Thêm subclass Item khi có hành vi riêng; không bắt buộc một lớp cho mỗi nhãn.
JSON lưu category dạng chuỗi nên không cần đổi schema cho chỉ một mã danh mục mới,
nhưng client cũ có thể cần nâng cấp để hiển thị đúng nhãn.

**Hỏi:** “Sao không new Art ở mọi nơi?” **Đáp:** “Factory gom việc chọn lớp vào
một chỗ; service chỉ cần Item. Thêm loại mới không phải đi tìm mọi constructor call.
Đổi lại, bản đơn giản này vẫn phải sửa switch và validation tại nơi khai báo danh mục.”

## Observer và server push qua Socket

**Vấn đề:** client B đang xem phiên cần thấy bid của A mà không liên tục hỏi giá.

**Vị trí và vai trò:**

| Vai trò | Code thực tế |
|---|---|
| Nguồn sự kiện | `AuctionContext.event()` tạo buffer; `AuctionService.commit()` lưu rồi gọi publisher |
| Phân phối qua mạng | `Server.publish()`, Client.subscriptions/feed, outbound queue |
| Đọc và điều phối | `JsonTransport.read()`, executor `auction-events` |
| Subject phía client | `ServerServices.registerBidListener()`, `addChangeListener()`, `onEvent()` |
| Observer giao diện | `AuctionRoomController.onBidReceived()`, `serverChanges` |
| Chuyển sang UI thread | `ViewTasks.dispatch()` → `Platform.runLater()` |
| Gỡ đăng ký | `AuctionRoomController.detachAuction()/dispose()`, adapter.close() |

**Hoạt động:** service lưu JSON thành công trước, publish event; server lọc theo
user hoặc đăng ký rồi đưa vào queue có giới hạn 256. Writer riêng gửi TCP.
Adapter thật đăng ký `SUBSCRIBE_FEED` sau login/reconnect; listener bid được lọc
theo auctionId ngay trên client. Giao thức cũng hỗ trợ SUBSCRIBE_AUCTION riêng.
Controller nhận sự kiện, đọc snapshot authoritative và vẽ lịch sử/đồ thị trên FX.

Listener dùng ConcurrentHashMap và CopyOnWriteArrayList để có thể thêm/bớt khi
dispatch. Bản sửa cô lập RuntimeException của từng bid callback: observer hỏng
không chặn observer sau hoặc general change event. Test
`FullStackTest.failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges()`
chạy qua socket thật. Khi đổi phòng hoặc rời view phải gỡ listener, dừng Timeline
và đóng ViewTasks để callback cũ không sửa màn hình mới.

**Lợi ích:** realtime theo sự kiện, controller tách transport. **Hạn chế:** queue đầy
đóng client chậm; event không phải nhật ký giao hàng đảm bảo exactly-once. Reconnect
phải đăng ký lại và đọc snapshot. Heartbeat PING kiểm tra kết nối, không phải vòng
polling giá thay cho push. Network có thể gửi event trước response của chính lệnh
vì publish chạy trong commit rồi readLoop mới enqueue response; client phải đối soát
commandId và snapshot, không giả định ACK luôn đến trước event.

**Hỏi:** “Observer có phải cứ gọi notify là an toàn đa luồng?” **Đáp:** “Không.
Transaction lock bảo vệ state; writer queue tách socket output; listener collection
an toàn khi dispatch; ViewTasks chuyển UI update sang FX thread. Mỗi phần giải quyết
một vấn đề khác nhau. Sau mất mạng phải đọc lại state từ server.”

## Adapter, Repository và policy

`ServerServices` là Adapter thực tế: hiện thực sáu interface client, đổi DTO/call
thành action JSON và đổi response qua `ServerMapper`. `AuctionJsonMapper` đổi giá
trị domain mới sang định dạng bid cũ mà không serialize lại toàn bộ state.
`AuctionRepository` gom File I/O và file lock; không chứa luật chọn winner. Đây là
các cách phân chia trách nhiệm hữu ích, không cần tính thêm pattern để tăng điểm.

`AntiSnipingPolicy` là policy class cụ thể, kiểm thử độc lập, hiện chỉ có một cách
tính được service sử dụng. **Không tuyên bố đã có Strategy GoF hoàn chỉnh**: chưa
có interface và nhiều strategy thay thế được inject.

Action strings trong `handleAction()` và `commandId` là định tuyến và chống lặp.
**Không phải Command GoF** có object lệnh với execute/undo. Chưa đề xuất thêm vì
chưa có yêu cầu undo, hàng đợi command nghiệp vụ hoặc strategy bid thay thế. Hệ
thống không có undo bid đã commit; rollback do lưu thất bại là việc khác.

## PriorityQueue trong auto-bid Server thật

`BiddingLogic.processAutoBids()` dùng PriorityQueue ứng viên và lấy từng luật bằng
`remove()`, không duyệt iterator của heap để suy ra thứ tự. Danh sách `rules` đã
sắp ổn định theo `order`; vị trí trong danh sách là priority của heap, nên cả các
luật cũ có cùng order vẫn giữ thứ tự trước đây. Mỗi vòng chọn một bid hợp lệ,
kiểm tra trần/ngân sách và bỏ leader; vòng kế tiếp xét lại từ ưu tiên đầu. Cùng
monitor và transaction của AuctionService, không thêm queue/network thread. Đây
là cấu trúc dữ liệu theo yêu cầu PDF, không phải một Design Pattern mới. Mock
`AutoBidEngine` vẫn có PriorityQueue riêng cho fixture GUI.
