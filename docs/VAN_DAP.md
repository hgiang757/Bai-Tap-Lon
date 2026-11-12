# Đọc code và chuẩn bị vấn đáp

Mục tiêu là giải thích được phần nhóm sử dụng và tự sửa được ví dụ nhỏ.
Cấu trúc ít thư mục hơn không thay thế việc hiểu luồng xử lý.

## 1. Bắt đầu từ đâu?

| Phần việc | Đọc theo thứ tự |
|---|---|
| Giao diện | `Main` → `ViewNavigator` → FXML → controller tương ứng |
| Mạng | `JsonTransport` → `JsonProtocol` → `Server.Client.readLoop` |
| Nghiệp vụ | `AuctionService.execute` → `handleAction` → AuthLogic/ProductLogic/BiddingLogic/WalletLogic/SettlementLogic |
| Lưu trữ | `AuctionRepository.load/save` |
| Nâng cao | `processAutoBids`, `AntiSnipingPolicy`, các lớp trong `src/test/java` |

Các tên lớp nằm dưới `src/main/java/com/auction`, còn FXML ở
`src/main/resources/com/auction/client/views`.

## 2. Một lượt đặt giá đi qua những đâu?

1. Nút Đặt giá trong `auction-room-view.fxml` gọi `AuctionRoomController.handlePlaceBid`.
2. Controller đọc số tiền, kiểm tra đầu vào, gọi dịch vụ ở luồng nền qua `ViewTasks`.
3. `ServerServices.placeBid` tạo payload gồm `auctionId` và `amount`, gọi `write`.
4. `write` tạo `commandId`, ghi lệnh chờ vào máy client; `JsonTransport.call` gửi request qua Socket.
5. `JsonProtocol.writeFrame` gửi độ dài 4 byte, sau đó là JSON UTF-8. Bên nhận dùng `readFrame`.
6. `Server.Client.readLoop` nhận request, gọi `AuctionService.execute`.
7. `execute` giữ khóa chung, gọi AuthLogic xác thực token, kiểm tra lệnh trùng;
   `handleAction` chuyển `PLACE_BID` tới `BiddingLogic.placeBid`.
8. `Auction` kiểm tra phiên/quyền/giá; `WalletLogic` tính tiền khả dụng;
   `BiddingLogic.acceptBid` dùng `Auction.withBid` và mapper cập nhật người dẫn đầu,
   lịch sử, tiền giữ và thời gian kết thúc. `processAutoBids` xử lý các luật tự động đang bật.
9. `commit` lưu trạng thái thành công rồi phát sự kiện; server đưa response/event vào hàng đợi của client.
10. `JsonTransport` ghép response theo `requestId`; event đi tới listener. Controller tải/cập nhật
    dữ liệu và biểu đồ trên luồng JavaFX qua `ViewTasks`.

Response xác nhận thao tác của người gửi. Event thông báo thay đổi cho các client khác đã đăng ký.

## 3. MVC nằm ở đâu?

- **View:** FXML/CSS, thể hiện bố cục và giao diện.
- **Controller:** nhận thao tác và cập nhật thành phần JavaFX; không trực tiếp ghi file server.
- **Model:** các lớp `Auction`, `Bid`, `User`, `Item` phía client dùng để hiển thị dữ liệu.

Dịch vụ client nối controller với backend. Ở server, `AuctionService` giữ transaction,
các lớp Logic xử lý từng nghiệp vụ, `Auction` giữ luật domain và
`AuctionRepository` phụ trách lưu trữ. JSON được dùng cho thông điệp và trạng thái lưu trữ;
không phải toàn bộ dữ liệu server đều được bọc thành lớp Java riêng.

Một dự án Maven vẫn có Client–Server: hai chương trình chạy độc lập và nói chuyện qua Socket.
Việc ở chung thư mục không làm chúng trở thành cùng một tiến trình.

## 4. Vì sao hai người không cùng thắng?

`AuctionService.execute` dùng `synchronized`. Mỗi server dùng một đối tượng dịch vụ chung,
nên tại một thời điểm chỉ một request được kiểm tra và thay đổi trạng thái.

Ví dụ giá hiện tại 100.000, bước 10.000; A và B cùng đặt 110.000.
A được xử lý trước thì giá thành 110.000. Khi B vào, mức tối thiểu đã thành 120.000 nên B bị từ chối.
Các thread nhận/gửi Socket vẫn độc lập; hàng đợi gửi tránh để client chậm chặn phần xử lý nghiệp vụ.

Đây là khóa chung, dễ giải thích và phù hợp demo nhỏ. Nhiều phiên vẫn phải chờ cùng khóa;
không tuyên bố đây là kiến trúc xử lý tải lớn hay nhiều server đồng thời.

## 5. Ví, lưu file và mất kết nối

**Tiền giữ:** tổng số dư chưa bị trừ ngay khi đặt giá. Tiền đang dẫn đầu bị giữ để không thể
rút hoặc tiêu ở phiên khác. Khi bị vượt giá, khoản giữ được giải phóng.
Khi hết phiên có người thắng, `SettlementLogic.settle` trừ tiền người thắng, cộng tiền người bán và đánh dấu đã thanh toán.
Phiên không có bid kết thúc mà không chuyển tiền. Hủy phiên giải phóng khoản giữ.

**Số tiền:** backend dùng `long` theo đơn vị đồng, không dùng số thực cho phép tính tiền.
Client có thuộc tính `double` phục vụ UI; adapter chỉ nhận giá trị nguyên trước khi gửi.

**Lưu file:** trước thao tác, service giữ bản sao trạng thái. Repository ghi file tạm,
đẩy dữ liệu ra đĩa rồi thay `state.json` bằng thao tác đổi tên nguyên tử.
Nếu không lưu được, service khôi phục trạng thái cũ và trả `STORAGE_ERROR`.
Khóa file ngăn hai server ghi chung một thư mục dữ liệu.

**Gửi trùng:** `requestId` ghép request–response trong một kết nối; `commandId` nhận diện
một thao tác thay đổi dữ liệu qua nhiều lần gửi. Server lưu kết quả theo người dùng và `commandId`.
Gửi lại cùng lệnh thì nhận kết quả cũ; đổi payload nhưng dùng lại mã thì bị từ chối.

**Timeout:** chưa nhận được response không có nghĩa server chưa xử lý. Client giữ lệnh chờ,
đối soát bằng mã cũ và tải lại dữ liệu. Tránh tự tạo lệnh nạp tiền/đặt giá mới để thử lại liên tục.

## 6. Realtime và mẫu thiết kế

| Mẫu/khái niệm | Vị trí và cách dùng thật |
|---|---|
| Observer | Callback `publisher` trong service, `Server.publish`, các listener bid/change phía client |
| Factory Method | `ItemFactory.forCategory(...).create(...)` tạo lớp sản phẩm tương ứng; service đọc `getCategory()` |
| Kế thừa/đa hình | `User` có `Admin`, `Seller`, `Bidder`; `Item` có các loại sản phẩm, override phương thức |
| Đóng gói | `Entity`, `User`, `Item` giữ trường private và cung cấp getter |
| Instance mặc định dùng chung | `Server.getInstance()` dùng cho entry point; constructor cho phép test tạo server riêng trên cổng tự chọn |
| Singleton trong mock | `MockDataStore.getInstance()` giúp dịch vụ UI giả dùng chung dữ liệu |

Giao thức ở đây là **TCP Socket JSON có độ dài**, không phải REST, WebSocket hay ObjectStream.
Luồng đọc xử lý thông điệp vào; luồng ghi lấy thông điệp ra từ hàng đợi riêng cho mỗi client.
`SUBSCRIBE_FEED`/`SUBSCRIBE_AUCTION` xác định client muốn nhận cập nhật nào.
Sau kết nối lại, adapter đăng ký lại feed và tải trạng thái từ server.

## 7. Auto-bid và chống bắn tỉa

`setAuto` kiểm tra mức tối đa, bước tăng và ngân sách. `processAutoBids` sắp luật theo thứ tự đăng ký,
bỏ qua người đang dẫn đầu, chọn bid không vượt trần và đủ tiền, rồi lặp lại.
Khi cùng trần, luật đăng ký sớm được ưu tiên. Chạm trần thì luật dừng.
Có giới hạn vòng lặp; nếu quá nhiều vòng, thao tác bị từ chối và hoàn tác thay vì lưu kết quả dở dang.

`AntiSnipingPolicy` mặc định: bid hợp lệ khi còn **0 < thời gian còn lại <= 30 giây**
thì cộng **60 giây** vào thời điểm kết thúc hiện tại.
Đồng hồ server là nguồn quyết định; đồng hồ giao diện chỉ để hiển thị đếm ngược.
Scheduler gọi `tick` mỗi 250 ms để cập nhật trạng thái phiên.

## 8. Đăng nhập và quyền

Mật khẩu được băm PBKDF2 có salt trong `Passwords`; response người dùng không chứa hash.
Đăng nhập thành công cấp token trong bộ nhớ server, có hạn 12 giờ.
Server kiểm tra token, trạng thái khóa/xóa và vai trò trước thao tác cần quyền.
Ẩn nút trên UI giúp trải nghiệm; quyền thực sự vẫn được kiểm tra ở backend.
Khởi động lại server cần đăng nhập lại vì session không được lưu trong file.

## 9. Tập demo và tự kiểm tra

1. Mở server, mở hai client và đăng nhập hai tài khoản bidder.
2. Đặt cùng mức giá, quan sát một người bị báo giá thấp và cả hai màn hình cập nhật.
3. Bật auto-bid ở một client, dùng client kia tăng giá; giải thích bước giá và trần.
4. Vào Ví, chỉ ra khác nhau giữa số dư, đang giữ và khả dụng.
5. Đăng nhập Seller tạo phiên/ảnh mới, rồi dùng Admin kiểm tra quyền quản trị.
6. Dừng/mở lại server để kiểm tra dữ liệu còn, đăng nhập lại và tiếp tục.

Kiểm thử tiêu biểu: `FullStackTest` (ảnh/quản trị/ví/retry/nhiều client),
`AuctionServiceTest` (nghiệp vụ/lưu trữ), `AntiSnipingPolicyTest` (mốc thời gian),
`ClientUiTest` và `ServerUiIntegrationTest` (thao tác màn hình).
Đọc tên và nội dung test trước khi giải thích; không chỉ dựa vào số test xanh.
