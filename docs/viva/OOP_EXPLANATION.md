# Giải thích OOP bằng code của dự án

Đọc cùng [sơ đồ lớp](ARCHITECTURE.md). Dưới đây luôn phân biệt model server với
`client/model` là DTO cho giao diện. Tên file/method nằm trong [CODE_MAP.md](CODE_MAP.md);
tên method là điểm tìm kiếm ổn định nếu số dòng thay đổi.

## 1. Encapsulation — đóng gói

Đóng gói là giữ dữ liệu bên trong đối tượng và cho bên ngoài thao tác qua hợp đồng
rõ ràng. Không bắt buộc phải có setter cho mọi thuộc tính.

- [server/model/Entity.java](../../src/main/java/com/auction/server/model/Entity.java),
  thuộc tính `id`, `name` là private, đọc/đổi qua getter/setter; setter đánh dấu cập nhật.
- [Auction](../../src/main/java/com/auction/server/model/Auction.java): record có
  thành phần private final; không có setter; `requireBidder()` và `requireMinimumBid()`
  diễn đạt luật nghiệp vụ bằng tên có nghĩa. `withBid()` tạo snapshot mới với một
  leader; `heldBy()`, `requireEditable()`, `requireAutoBid()` và các luật thời gian
  được WalletLogic/ProductLogic/BiddingLogic/SettlementLogic gọi thật.
- [AuctionService](../../src/main/java/com/auction/server/service/AuctionService.java):
  giữ `context`, các helper và `repository` private; AuthLogic giữ sessions private.
  `execute()` trả deepCopy, tránh caller
  sửa cây JSON đang thuộc transaction.

```java
Auction auction = new Auction("a", "seller", 1_000_000, 100_000,
        "RUNNING", start, end);
auction.requireMinimumBid(1_200_000); // hợp lệ
long minimum = auction.minimumNextBid(); // 1.100.000
```

Trả lời 30–60 giây: “Trong Entity, id và name là private và truy cập qua getter/setter;
không tuyên bố chúng bất biến. Auction là record bất biến, các luật kiểm tra bidder và
bước giá được gọi bằng method của domain. Ở service, snapshot thật và phiên đăng
nhập không được public; phản hồi được deepCopy. Đóng gói ở đây giúp kiểm soát ai
được thay đổi trạng thái, chứ không chỉ thêm getter/setter cho đủ.”

## 2. Inheritance — kế thừa

Kế thừa biểu diễn quan hệ ‘là một’. Lớp con nhận phần chung và hiện thực phần khác.

- [User](../../src/main/java/com/auction/server/model/User.java) extends Entity;
  [Bidder](../../src/main/java/com/auction/server/model/Bidder.java),
  [Seller](../../src/main/java/com/auction/server/model/Seller.java),
  [Admin](../../src/main/java/com/auction/server/model/Admin.java) extends User.
  Constructor con gọi `super(...)`; mỗi lớp override `getRoleName()`.
- [Item](../../src/main/java/com/auction/server/model/Item.java) extends Entity;
  [Electronics](../../src/main/java/com/auction/server/model/Electronics.java),
  [Art](../../src/main/java/com/auction/server/model/Art.java),
  [Vehicle](../../src/main/java/com/auction/server/model/Vehicle.java) override `getCategory()`.
- Item **có một** Seller (`getSeller()`); đây là composition/association, không phải
  Item kế thừa Seller. Auction không extends Entity vì record chỉ cần trạng thái
  kiểm tra bid, không cần ép có name.

```java
Seller seller = new Seller("s", "người_bán", "hash");
Item item = ItemFactory.forCategory("ART").create("i", "Tranh", "Sơn dầu", seller);
String id = item.getId(); // method kế thừa từ Entity
```

Trả lời 30–60 giây: “Dự án có hai nhánh từ Entity là User và Item. User chia thành
Bidder, Seller, Admin; Item chia thành Electronics, Art, Vehicle. Entity giữ định
danh và tên dùng chung, lớp con chỉ cần cung cấp vai trò hoặc danh mục. Quan hệ
Item với Seller là sản phẩm có người bán nên dùng thuộc tính, không dùng kế thừa.”

## 3. Polymorphism — đa hình

Cùng một kiểu tham chiếu và lời gọi method có thể thực thi hành vi khác theo lớp
thực tế lúc chạy. Có override chưa đủ làm ví dụ thuyết phục: cần chỉ ra nơi gọi.

- `AuthLogic.createUser()`: biến `User account` nhận Admin/Seller/Bidder;
  `account.getRoleName()` dispatch đến implementation lớp con trước khi ghi JSON.
- `ProductLogic.createAuction()`: `ItemFactory.forCategory(...).create(...)` trả
  `Item`; `item.getCategory()` gọi Electronics/Art/Vehicle/CategoryItem tương ứng.
- `AuctionRoomController.handlePlaceBid()`: gọi `AuctionClientService.placeBid()`;
  runtime production luôn là ServerServices; test có thể tiêm fixture
  MockAuctionClientService từ src/test. Không cần controller tự mở Socket.

```java
List<User> users = List.of(new Bidder("b", "buyer", "hash"),
        new Seller("s", "seller", "hash"), new Admin("a", "admin", "hash"));
List<String> roles = users.stream().map(User::getRoleName).toList();
// [BIDDER, SELLER, ADMIN] — được kiểm tra trong ItemFactoryTest
```

Trả lời 30–60 giây: “Ở createUser, biến account có kiểu User nhưng đối tượng thực
là một trong ba lớp con. Khi gọi getRoleName, Java chọn method override theo lớp
thực tế, nên cùng một lời gọi trả ba vai trò khác nhau. Một ví dụ khác là controller
làm việc với AuctionClientService, trong khi ServerServices và Mock có cách thực
hiện khác nhau. Trong chế độ thật chỉ dùng ServerServices, không tự fallback mock.”

## 4. Abstraction — trừu tượng

Trừu tượng là mô tả điều bên dùng cần biết, che chi tiết thực hiện phía sau.

- Entity/User/Item là abstract class, không tạo trực tiếp bằng `new User(...)`.
  `User.getRoleName()` và `Item.getCategory()` là abstract method buộc lớp con cung cấp.
- [AuctionClientService](../../src/main/java/com/auction/client/service/AuctionClientService.java)
  định nghĩa đăng nhập, đặt giá, đọc snapshot và đăng ký listener; các interface ví,
  sản phẩm, admin tách hợp đồng theo nhu cầu màn hình.
- [ItemFactory](../../src/main/java/com/auction/server/model/ItemFactory.java)
  định nghĩa `create(...)`; caller nhận Item mà không phải chọn constructor mọi nơi.

```java
AuctionClientService service = ServiceRegistry.getService();
BidResult result = service.placeBid(auctionId, 1_200_000);
// Controller biết hợp đồng; ServerServices lo JSON, Socket và commandId.
```

Trả lời 30–60 giây: “Controller chỉ cần biết dịch vụ có placeBid trả BidResult,
không cần biết frame TCP hay cách lưu JSON. Interface AuctionClientService là hợp
đồng trừu tượng cho việc đó. Trong domain, User và Item là lớp trừu tượng chứa phần
chung, lớp con phải hiện thực vai trò hoặc danh mục. Trừu tượng giúp tập trung vào
hành vi cần dùng, còn đóng gói giúp kiểm soát dữ liệu bên trong.”

## Những giới hạn cần hiểu

User/Item server hiện chủ yếu cung cấp thông tin vai trò/danh mục; luật phân quyền
và ví vẫn do service kiểm tra. Role trong JSON có thể đổi bởi admin, không phải thay
đổi Java runtime class của một User đang được cache. Các phương thức ví `double`
trong User và giá `double` trong Item là API cũ, không được gọi để tính tiền ở backend.
Đợt tối giản JavaFX đã xóa ba model server cũ `AuctionDraft`, `Bid` và `Transaction`
sau khi kiểm tra caller, FXML, reflection và JSON persistence. `BidTransaction` ghi một
bid đã chấp nhận, không tự chuyển tiền. `Auction` mới là domain quy tắc bất biến,
không phải toàn bộ state manager. `AuctionService` vẫn là nơi điều phối giao dịch.

Kiểm chứng: `AuctionTest`, `ItemFactoryTest`, `AuctionJsonMapperTest` và
`AuctionServiceTest`. Không tuyên bố mọi setter của DTO UI bảo vệ được toàn bộ
invariant nghiệp vụ; server mới là nơi thực thi luật chính thức.

Xem [báo cáo refactor](../archive/OOP_SERVICE_REFACTOR_REPORT.md) để đối chiếu PDF,
trách nhiệm service, dữ liệu thật và kết quả test sau sửa.

## Composition tại phòng đấu giá

Controller sở hữu `AuctionBidHistoryView`, truyền các control đã inject từ FXML
vào constructor. `render(Auction, List<Bid>, LocalDateTime)` chỉ lo biểu diễn một
snapshot; không biết Controller, ServerServices hay repository. Đây là composition
với trách nhiệm hiển thị rõ ràng, không thêm cây kế thừa để giảm LOC. Luật giá,
quyền và ví vẫn nằm tại Server. Giữ Mock phục vụ test GUI có điều khiển và ví dụ
Singleton trong nguồn test, không trình bày nó là production; ứng dụng chính chỉ dùng TCP.

Seller dùng composition tương tự: SellerProductFormView (FXML + bản nháp) sở hữu
AuctionScheduleInput và ProductImagePicker. Controller nhận Draft qua Consumer,
gọi service nền rồi trả trạng thái busy/lỗi cho View. closeForm/dispose đóng scope
preview. Các helper không giữ session, transport, quyền hoặc transaction Server.
