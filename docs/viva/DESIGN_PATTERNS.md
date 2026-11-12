# Design Patterns đang được sử dụng

Vị trí cụ thể và số dòng: [CODE_MAP.md](CODE_MAP.md). Các file Java trong tài liệu
này nằm dưới `src/main/java/com/auction/`.

## Singleton: phân biệt ví dụ nghiêm ngặt và instance mặc định

**Vấn đề:** các dịch vụ mock phải nhìn thấy cùng một danh sách phiên, ví và listener.
`client/service/mock/MockDataStore` có `private static final INSTANCE`, constructor
`private MockDataStore()` và `public static getInstance()`. JVM khởi tạo class một
lần, công bố INSTANCE an toàn; việc sửa các collection vẫn cần `synchronized(store)`.
Singleton không tự làm tất cả dữ liệu bên trong thread-safe.

**Sử dụng thực tế:** `ServiceRegistry.mockServices()` đưa cùng store vào auction,
wallet, auto-bid và management service. Đây là ví dụ Singleton hoàn chỉnh ở chế độ
phát triển, không phải kho JSON của server thật. Mock singleton mang trạng thái
dùng chung nên test mock phải quản lý fixture và dọn tài nguyên; khó cô lập hơn
đối tượng được tạo mới cho từng test.

`server/network/Server.getInstance()` dùng `static synchronized` để cấp một server
mặc định và tạo lại khi instance trước đã đóng. Entry point `main()` dùng accessor
này. Constructor cấu hình `(host, port, data, demo)` vẫn **public** để integration
test tạo nhiều server độc lập, cổng 0, dữ liệu tạm. Vì vậy không nói rằng Server
ngăn tuyệt đối mọi `new Server`, cũng không chỉ ra một private constructor không
tồn tại. Đây là lựa chọn vòng đời default instance có chủ ý, không Singleton GoF
nghiêm ngặt. File lock ngăn hai server dùng cùng thư mục dữ liệu, còn bind ngăn hai
listener trùng địa chỉ/cổng; hai cơ chế đó không phải Singleton.

**Lợi ích:** chia sẻ trạng thái mock nhất quán; entry point có đầu mối vòng đời rõ.
**Hạn chế:** global state ảnh hưởng cô lập test; không dùng Singleton thay transaction
lock, file lock hoặc phân quyền. AuctionService và AuctionRepository vẫn là đối
tượng cấu hình theo từng server, không bị biến thành Singleton.

**Hỏi:** “Constructor private của Singleton ở đâu, vì sao Server vẫn new được?”
**Đáp:** “Ví dụ nghiêm ngặt là MockDataStore, constructor private và INSTANCE được
khởi tạo một lần khi nạp class. Server chỉ cung cấp instance mặc định bằng accessor
đồng bộ; constructor public giữ khả năng tạo server cô lập khi test. Nhóm không
tuyên bố nó cấm mọi instance khác.”

## Factory Method kết hợp Simple Factory

**Vấn đề:** tạo sáu loại sản phẩm cần tập trung ánh xạ category → implementation,
không lặp switch/new trong các controller và service.

**Vị trí:** `server/model/ItemFactory.forCategory()` và abstract `create()`;
`AuctionService.createAuction()` là caller thật.

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
`AuctionService.validateProduct()`, `client/model/ItemCategory` (nhãn/icon), test.
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
| Nguồn sự kiện | `AuctionService.event()`, `commit()` và `Consumer<JsonObject> publisher` |
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
