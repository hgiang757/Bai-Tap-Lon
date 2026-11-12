# Kiến trúc và luồng dữ liệu

Java release 25, JavaFX 26.0.2, Maven; một server TCP mặc định 8888, nhiều client.
Source chung trong một Maven project nhưng client/server là các tiến trình riêng.
JSON dùng cho cả thông điệp và snapshot, không có MySQL/JDBC/Spring/REST/WebSocket.

## Trách nhiệm các tầng

| Tầng | File/class | Trách nhiệm |
|---|---|---|
| View | `src/main/resources/com/auction/client/views/*.fxml`, `css/app.css` | Bố cục và kiểu hiển thị |
| Controller | `client/controller/*Controller` | Đọc form, trạng thái UI, gọi service nền, cập nhật UI |
| Model UI | `client/model/Auction`, Bid, User, Item | DTO để render, không phải nguồn dữ liệu chính thức |
| Service contract | `client/service/*Service` | Hợp đồng bid/ví/sản phẩm/admin, tách controller khỏi TCP |
| Adapter/network | ServerServices, ServerMapper, JsonTransport, JsonProtocol | Token, commandId, frame, pending request, push/reconnect |
| Request handler | `server/network/Server.Client.readLoop()` | Envelope, subscription, gọi service, ghép requestId |
| Application service | `server/service/AuctionService` | Xác thực/quyền, transaction, ví, auto-bid, settlement |
| Domain | `server/model/*` | Cây User/Item, luật Auction bất biến và bản ghi BidTransaction |
| Mapper | `server/dao/AuctionJsonMapper` | Đọc trường phiên, ghi bid đúng schema cũ, không sửa trường khác |
| Repository | `server/dao/AuctionRepository` | Load/save JSON, file lock, force và atomic move |

MVC phía server được ánh xạ như sau: readLoop là controller nhận request;
service + domain là model nghiệp vụ; repository là DAO; JSON response là biểu diễn
trả về. Không có JavaFX View trên server. Controller UI vẫn có kiểm tra định dạng,
khóa nút và báo giá tối thiểu để hỗ trợ người nhập; kiểm tra authoritative nằm ở
server. Không phát hiện controller ghi trực tiếp state.json của server.

## 1. Sơ đồ Client–Server

```mermaid
flowchart LR
  subgraph ClientA[Tiến trình Client A]
    VA[FXML và CSS] --> CA[JavaFX Controller]
    CA --> TA[ViewTasks]
    TA --> SA[Service interfaces / ServerServices]
    SA --> NA[JsonTransport]
    NA --> EA[Event dispatch và listeners]
    EA --> TA
    TA --> VA
  end
  subgraph ClientB[Tiến trình Client B]
    VB[JavaFX View / Controller] --> SB[ServerServices / JsonTransport]
    SB --> VB
  end
  subgraph Backend[Tiến trình Server - TCP 8888]
    H[Server.Client.readLoop] --> S[AuctionService - monitor chung]
    S --> D[Auction / BidTransaction / Item / User]
    S --> M[AuctionJsonMapper]
    S --> R[AuctionRepository]
    R --> J[(data/state.json)]
    S --> P[Server.publish - sau save]
    P --> Q[Queue riêng mỗi client / writeLoop]
  end
  NA <-->|TCP frame JSON UTF-8| H
  SB <-->|TCP frame JSON UTF-8| H
  Q --> NA
  Q --> SB
```

Không hiểu mũi tên ViewTasks → View là network thread sửa UI: nhánh trả kết quả
luôn được dispatch bằng Platform.runLater. Socket output chạy ở writer riêng.

## 2. Sơ đồ lớp OOP chính

```mermaid
classDiagram
  class Entity {
    <<abstract>>
    -String id
    -String name
    +getId() String
    +getName() String
  }
  class User {
    <<abstract>>
    -String passwordHash
    +getRoleName() String
  }
  class Item {
    <<abstract>>
    -String description
    -Seller seller
    +getCategory() String
  }
  Entity <|-- User
  Entity <|-- Item
  User <|-- Bidder
  User <|-- Seller
  User <|-- Admin
  Item <|-- Electronics
  Item <|-- Art
  Item <|-- Vehicle
  Item <|-- CategoryItem
  Item --> Seller : owner
  class ItemFactory {
    <<abstract>>
    +forCategory(category) ItemFactory
    +create(id, name, description, owner) Item
  }
  ItemFactory ..> Item : creates
  class Auction {
    <<record>>
    +minimumNextBid() long
    +requireBidder(userId, role, time)
    +requireMinimumBid(amount)
  }
  class BidTransaction {
    <<record>>
    +String id
    +String auctionId
    +long amount
    +LocalDateTime timestamp
    +boolean automatic
  }
  class AuctionJsonMapper
  class AuctionService
  class AuctionRepository
  AuctionService ..> ItemFactory
  AuctionService ..> User
  AuctionService ..> Auction
  AuctionService ..> BidTransaction
  AuctionService --> AuctionRepository
  AuctionService ..> AuctionJsonMapper
  AuctionJsonMapper ..> Auction
  AuctionJsonMapper ..> BidTransaction
```

Auction/BidTransaction ở đây là **server.model**, không phải DTO cùng tên ở client.
Record components là private final; dấu `+` trong diagram BidTransaction chỉ biểu
diễn accessor công khai. CategoryItem là nested private class của ItemFactory.
Service không giữ một collection Auction Java lâu dài; mapper dựng giá trị từ
snapshot trong transaction. Sơ đồ dùng dependency `..>` cho việc sử dụng tạm đó.

## 3. Sequence đặt giá

```mermaid
sequenceDiagram
  actor U as Người dùng
  participant C as AuctionRoomController
  participant V as ViewTasks
  participant A as ServerServices
  participant T as JsonTransport
  participant H as Server.Client
  participant S as AuctionService
  participant D as Auction / BidTransaction
  participant R as AuctionRepository
  participant B as Client B listeners
  U->>C: Nhập số tiền, nhấn Đặt giá
  C->>V: submit công việc nền và giữ ticket
  V->>A: placeBid(auctionId, amount)
  A->>A: Lưu pending với commandId ổn định
  A->>T: PLACE_BID với requestId mới
  T->>H: Frame độ dài + JSON
  H->>S: execute(action, token, commandId, payload)
  Note over S: Giữ monitor tới khi commit/publish xong
  S->>S: refresh, xác thực, kiểm tra commandId
  S->>D: requireBidder / requireMinimumBid
  S->>S: Kiểm tra ví, acceptBid, auto-bid, ghi receipt
  S->>D: Tạo BidTransaction rồi mapper ra JSON cũ
  S->>R: save(snapshot)
  R-->>S: force và atomic move hoàn tất
  S->>H: publish event vào outbound queues
  H-->>B: writeLoop gửi BID_PLACED
  S-->>H: response thành công
  H-->>T: writeLoop gửi response cùng requestId
  T-->>A: Hoàn tất future
  A->>A: Xóa pending sau kết quả rõ ràng
  A-->>V: BidResult
  V-->>C: Platform.runLater callback
  C->>V: Tải snapshot mới và vẽ UI
  Note over B: dispatch qua ViewTasks rồi tải snapshot/vẽ FX
```

Sự kiện có thể đến trước ACK. Nếu save thất bại, service khôi phục `before`, xóa
events, trả STORAGE_ERROR; không có nhánh push thành công. Nếu cùng commandId đã
có receipt, trả kết quả cũ; không tạo lại BidTransaction. Với tiền tự động, có thể
có nhiều bid và nhiều event trong cùng một transaction.

## 4. Sequence hai client đặt cùng giá

```mermaid
sequenceDiagram
  participant A as Client A / reader A
  participant B as Client B / reader B
  participant S as AuctionService monitor
  participant R as AuctionRepository
  Note over A,B: Giá 1.000.000, bước 100.000, đủ tiền, không auto-bid
  par Gửi gần đồng thời
    A->>S: PLACE_BID 1.200.000 / commandA
  and
    B->>S: PLACE_BID 1.200.000 / commandB
  end
  Note over S: Giả sử A lấy monitor trước; thứ tự không được bảo đảm
  S->>S: A đọc mức tối thiểu 1.100.000, cập nhật giá 1.200.000
  S->>R: Lưu bid A, hold, receipt A
  R-->>S: Thành công
  S-->>A: Thành công; publish tới các subscriber
  Note over S: A nhả monitor; B mới được đọc và kiểm tra state
  S->>S: B thấy mức tối thiểu mới 1.300.000
  S->>S: Từ chối BID_TOO_LOW, ghi receipt REJECTED
  S->>R: Lưu receipt B, không thêm bid B
  R-->>S: Thành công
  S-->>B: BID_TOO_LOW
```

Nếu B lấy khóa trước thì vai trò đảo lại. Nếu A không lưu được, A bị rollback và
B có thể thành công; không được kết luận “luôn A thắng”. Người dẫn đầu hiện tại
chưa phải winner cuối cùng; scheduler quyết định settlement khi hết giờ.

## Snapshot và compatibility

SchemaVersion vẫn 1: users, auctions, rules, commands, transactions. Phiên cũ giữ
id/itemId/highestBidder/history/image/extension fields; bid mới giữ đúng các tên
`id`, `auctionId`, `bidderUsername`, `amount`, `timestamp`, `auto`. Repository vẫn
lưu cây JSON gốc. Mapper không tạo một snapshot mới chỉ từ bảy trường Auction,
nên không làm rơi lịch sử hoặc commandId.

Client chỉ lưu pending và cache ảnh dưới thư mục người dùng, không mở JSON server.
Session token chỉ ở RAM, restart cần đăng nhập lại. JSON là lựa chọn File I/O rõ
ràng cho quy mô bài tập; không thay thế transaction, lock hoặc kiểm thử.
