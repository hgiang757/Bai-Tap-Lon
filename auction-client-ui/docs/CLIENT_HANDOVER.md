# Lịch sử bàn giao phần client JavaFX

> Cập nhật 05/11/2026: tài liệu dưới đây ghi trạng thái trước tích hợp. Hiện `service/server` đã nối 6 dịch vụ vào backend thật; xem [PROTOCOL](../../auction-server/PROTOCOL.md) và [README](../../README.md) để chạy bản hiện hành. Các giới hạn chỉ sửa client/không sửa backend bên dưới là phạm vi của đợt bàn giao cũ.

Giao diện hiện tại dùng nền CSS trắng ngà, màu xanh ngọc, hai nhóm RUNNING/OPEN và thuật ngữ ví mới; xem [UI_REFRESH](UI_REFRESH.md). Bản mô tả tổng quan và bảng chức năng đầy đủ hiện ở [README](../README.md) và [CODE_MAP](CODE_MAP.md). Đợt sửa tiếp ngày 03/11/2026 thêm `BidSubmissionTracker`, tách hàng đợi ảnh và cập nhật xử lý kết quả chưa rõ. Phần đối chiếu backend bên dưới vẫn áp dụng; kết quả kiểm tra mới được tách riêng trong [VALIDATION](VALIDATION.md).

## Phạm vi và hiện trạng

Module thực tế ở `Bai-Tap-Lon-main/auction-client-ui/`, cạnh `auction-server/`, bên trong workspace bị lồng tên thư mục. Không tìm thấy `AGENTS.md` hay metadata `.git`; không thể đối chiếu bằng `git status`/`git diff`. Chỉ sửa trong module client, không sửa backend, `.github/` hay cấu hình ở ngoài module; không commit/push/reset.

Source ban đầu đã có MVC, các dịch vụ mock và một kho dữ liệu dùng chung. Những lỗi thực tế gồm Home không được navigator dừng timer, callback/tác vụ nền còn cập nhật màn hình cũ, đăng nhập và một số lần đọc dữ liệu/ảnh chạy trên FX thread, thay riêng dịch vụ có thể trộn mock với adapter, tiếng Việt của `ProductImages` bị thay thành dấu hỏi, cùng một số lời hướng dẫn mô tả server chưa tồn tại.

## Cấu trúc giữ lại và bổ sung

```text
auction-client-ui/
  pom.xml
  src/main/java/
    module-info.java
    com/auction/client/
      Main.java, App.java           Khởi chạy
      controller/                  Điều khiển từng màn hình
      model/                       DTO/model độc lập phía client
      service/                     Hợp đồng dịch vụ, ServiceRegistry, BidSubmissionTracker
        mock/                      Dữ liệu, ví, auto-bid, quản lý giả lập
      navigation/
        ViewNavigator.java         Điều hướng và lịch sử kèm auctionId
        DisposableView.java        Hợp đồng giải phóng màn hình (mới)
        ViewTasks.java             Pool nghiệp vụ và ảnh riêng, chặn kết quả cũ
      ui/                          Header, menu, ảnh, icon, tiện ích giao diện
      util/                        Định dạng, xác thực dữ liệu, thời gian
      dev/                         UI kit và công cụ chụp ảnh
  src/main/resources/com/auction/client/
    views/, css/, dev/, contacts.properties
  src/test/java/com/auction/client/
    controller/, navigation/, service/, ui/, util/
  scripts/check-encoding.py
  docs/
```

Không di chuyển hoặc đổi tên file/package public nào nên không phát sinh bảng đường dẫn cũ → mới. Không tạo `auction-common` hoặc package `network` rỗng. `AdminUser` vẫn là DTO client; việc cần adapter sau này không yêu cầu gộp module.

## Thay đổi chính

| Thành phần | Thay đổi và lý do |
|---|---|
| `navigation/DisposableView`, `ViewNavigator` | Giải phóng mọi controller có vòng đời khi chuyển màn hình; xử lý cả controller nạp FXML thất bại. Lưu và phục hồi đúng ID phiên; phục hồi dữ liệu trên luồng nền. |
| `navigation/ViewTasks` | Điều phối thao tác dài vào nhóm luồng daemon; kết quả về FX thread. Hai pool giới hạn riêng: nghiệp vụ 4 luồng/64 chờ, ảnh 2 luồng/32 chờ. Scope ảnh bỏ/hủy tác vụ lỗi thời, có timeout I/O; lệnh nghiệp vụ đã nhận không bị hủy. Rejection được đưa về callback lỗi. Mỗi khóa chỉ nhận kết quả mới nhất. Đóng scope chặn cả callback đã xếp hàng; không hủy hay gửi lại lệnh nghiệp vụ đã nhận. |
| Home | Tải danh sách phiên và ảnh sản phẩm trên luồng nền, chia trực tiếp RUNNING/OPEN sau lọc, khử trùng ID; phân biệt tải/lỗi/chưa có/không khớp. Timer chỉ cập nhật countdown và tải lại khi qua mốc. Dừng timer, bỏ ảnh đến muộn sau lọc/rời trang; giữ nút Làm mới. |
| AuctionRoom | Tách scope cho từng lần chọn phiên; hủy observer cũ. Tải lại chi tiết/lịch sử qua service khi có bid, giữ tối đa 30 điểm biểu đồ và tránh nhân đôi thông báo theo ID bid. Chặn bấm đặt giá hai lần khi đang xử lý; không tự retry. Lỗi đặt giá không rõ kết quả giữ khóa UNKNOWN qua lần mở phòng; đọc lại lịch sử không mở khóa khi chưa có API tra trạng thái lệnh. Countdown sắp mở theo startTime; trạng thái kết thúc/hủy/đã thanh toán được mô tả riêng. Có nút Tải lại. |
| Auth, Seller, Wallet, Admin | Dùng scope tác vụ nền; không cập nhật giao diện cũ. Đăng nhập/đăng ký có khóa gửi trong lúc chờ và báo lỗi. Ảnh bảng/ảnh xem trước sản phẩm được đọc nền. |
| AppHeader, UserMenu | Dừng timer và chặn callback ví khi header tách khỏi scene; không tạo nhiều lượt đọc ví đồng thời. Đăng xuất chạy nền. Cửa sổ nhỏ giữ rõ nút thao tác; có thể mở Ví trong menu tài khoản. |
| `ServiceRegistry` | Một bộ dịch vụ được thay nguyên tử bằng `setServices`; giữ tên setter cũ nhưng từ chối trộn dịch vụ mock và dịch vụ khác. Mặc định `auction.mode=mock`, từ chối mode không hỗ trợ. |
| Mock xác thực | Tài khoản đăng ký mới được thêm vào danh sách quản trị cùng kho dữ liệu. Đăng nhập nhận đúng trạng thái khóa/xóa/vai trò của kho đó, kể cả kho kiểm thử riêng. Dùng UUID để ID tài khoản không trùng theo chu kỳ mili giây. |
| FXML, tài liệu, UTF-8 | Giữ phong cách hiện tại; phòng đấu giá xuống hàng khi hẹp và tên sản phẩm không bị giới hạn chiều cao hai dòng. Sửa tiếng Việt ảnh và hướng dẫn còn mô tả server/nhận hàng thật. Kiểm tra encoding cả source, test và docs. |
| Dev snapshots | Chờ tác vụ nền thay vì chụp khi dữ liệu còn trống; dùng navigator để giải phóng timer/listener; xuất vào `target/snapshots` và trả mã lỗi khi thất bại. |

Không đổi phiên bản Java/dependency, không đổi thuật toán `AutoBidEngine`, giữ tiền, thanh toán hay quy tắc quản lý phiên. `pom.xml` chỉ thêm tham số chế độ và lựa chọn main class cho công cụ dev; `module-info.java` cho JavaFX khởi chạy `SnapshotApp`.

## Đối chiếu với backend có thật

Đã đọc toàn bộ source server. `auction-server/src/main/java/com/auction/server/network/Server.java` chỉ mở cổng **8888**, gửi lời chào bằng `DataOutputStream.writeUTF`, đọc `DataInputStream.readUTF`, xử lý chuỗi `exit` và echo lại `com.auction.server.network.Server received: ...`. Không có handler nghiệp vụ, định dạng request/response nghiệp vụ, subscription hoặc broadcast giá. `auction-server/Client.java` là client console minh họa echo.

| Nhóm chức năng | Backend hiện có | Phía client hiện dùng | Cần thống nhất trước khi ánh xạ |
|---|---|---|---|
| Đăng nhập/đăng ký/đăng xuất | Chưa có handler, response hay token/session | `AuctionClientService`, mock | User ID, username, fullName, email, role, trạng thái khóa, phiên đăng nhập và lỗi xác thực. Server `User` hiện có `Entity.id/name`, password, balance, `getRoleName`; không tương đương `client.model.User`. |
| Danh sách/chi tiết phiên | Có entity `Auction`, chưa có API | `AuctionClientService`, mock | `auctionId → id`, `stepPrice → minBidStep`, thông tin item/seller/người dẫn đầu, số lượt đặt giá, startTime/endTime và trạng thái. Server chưa có đủ getter/DTO để xuất dữ liệu này. |
| Đặt giá/lịch sử/realtime | Có entity `Bid`, chưa có lệnh đặt giá, lịch sử hay broadcast | `AuctionClientService` + `BidResult` + observer mock | `bidId → id`, ID phiên còn thiếu trong server Bid, `bidder` object → username, `bidAmount → amount`, timestamp và cờ auto. Cần kết quả được chấp nhận/từ chối và snapshot authoritative sau mỗi giá. |
| Sản phẩm người bán | Có Item abstract và owner kiểu Seller, chưa có CRUD/API ảnh | `ProductService`, mock; giữ các hàm legacy trong `AuctionClientService` | Item ID, loại, mô tả, seller ID/username, giá khởi điểm của phiên, lịch và ảnh. Client Item cụ thể có category/startingPrice/sellerUsername; server Item chưa có các trường tương ứng. Đường dẫn ảnh cục bộ không phải ID ảnh server. |
| Ví | Chỉ có User.balance, chưa có ledger/hold/API | `WalletService`, mock | Balance/held/available, giao dịch, lỗi và trạng thái thanh toán. Ví client dùng long đồng; giá phiên và entity server đang dùng double, cần chốt kiểu tiền/đơn vị/làm tròn. |
| Auto-bid | Chưa có handler hoặc entity quy tắc auto-bid | `AutoBidService`, mock | Quy tắc của người dùng theo phiên, giới hạn/bước giá, active/limitReached/autoCount, lỗi và sự kiện cập nhật. |
| Quản trị | Có lớp Admin theo vai trò, chưa có API quản trị | `UserAdminService`, `AuctionAdminService`, mock | Danh sách user, khóa/xóa/đổi vai trò; hủy/xóa phiên, lý do hủy và quyền thao tác. |

Client có enum `OPEN`, `RUNNING`, `FINISHED`, `CANCELED`, `PAID`; chú thích entity server mới nêu ba trạng thái đầu dưới dạng String. Chưa có hợp đồng cho hai trạng thái còn lại. Tên package và cấu trúc các lớp khác nhau, nên **không thể gửi trực tiếp hai bộ entity qua Object streams chỉ vì cùng Serializable/serialVersionUID**. Server hiện cũng không dùng Object streams.

Tất cả chức năng trong bảng dùng mock; **chưa có chức năng nghiệp vụ nào kết nối thật**. Không tạo adapter echo giả thành công, không tự chuyển sang mock khi lỗi. Host/timeout nghiệp vụ chưa có cấu hình vì chưa có transport nghiệp vụ để cấu hình; cổng 8888 chỉ là minh họa echo hiện tại.

## Điểm bàn giao cho các thành viên

- **Thành viên 1 — mạng:** cung cấp handler thực, framing/encoding/version, cấu trúc request/response/error và cách xác thực phiên. Cần cách phân biệt response với push event, request ID, timeout/ngắt kết nối, subscribe/unsubscribe theo auctionId và thứ tự/version sự kiện. Đây là danh mục cần thống nhất, chưa phải giao thức được triển khai. Cần cơ chế tra kết quả hoặc chống gửi trùng trước khi có bất kỳ retry đặt giá nào.
- **Thành viên 2 — dữ liệu/nghiệp vụ:** cung cấp DTO cho các hàng trong bảng, quy tắc và tên trạng thái, ID ổn định, kiểu tiền/đơn vị, timestamp/múi giờ và dữ liệu lỗi. Quy tắc đặt giá, thời gian kết thúc, giữ tiền và quyền truy cập phải do backend xác nhận. Client sẽ ánh xạ qua adapter vào các interface trong `service/`, không import entity server vào controller.
- **Thành viên 4 — nâng cao/QA:** cung cấp API ví/auto-bid/quản trị nếu thuộc phạm vi backend; phối hợp kiểm tra lỗi timeout, kết quả đặt giá chưa xác định, thứ tự/nhân đôi sự kiện và hai client cùng phiên. Khi chưa có API cho một khả năng, adapter phải báo chưa hỗ trợ thay vì cấp lại dữ liệu mock.

`ServiceRegistry.setServices(...)` là điểm cắm bộ adapter đồng bộ. Bộ setter cũ vẫn tồn tại cho tương thích, nhưng không được dùng để thay một dịch vụ thật trong khi các dịch vụ khác vẫn là mock. Chỉ mở chế độ server sau khi adapter, mapper và kiểm thử giao thức thực đã có.

## Kiểm tra tại workspace

Kết quả chi tiết và giới hạn được ghi trong [VALIDATION.md](VALIDATION.md). Có kiểm thử giao diện thật và ảnh kiểm tra; mở cửa sổ thành công không đồng nghĩa đã tích hợp backend. Các bài unit cũ về auto-bid/ví/phiên vẫn được giữ để kiểm tra hồi quy.

## Hợp đồng còn thiếu để giải quyết lệnh chưa xác định

`BidSubmissionTracker` lưu số thứ tự/token cục bộ, không phải request ID được server biết. Chỉ phản hồi rõ ràng (`BidResult`, hoặc lỗi kiểm tra cấu hình của đúng implementation auto-bid mock hiện có) cộng lượt đọc mới sau phản hồi mới cho UI mở khóa. Timeout/ngoại lệ bất thường giữ UNKNOWN dù đọc snapshot thành công, kể cả sau đóng/mở phòng; không gửi lại lệnh.

Backend cần cung cấp request ID và phép tra trạng thái cuối cùng của chính lệnh (bao gồm còn xử lý), cùng quy tắc/version để biết lượt đọc phản ánh trạng thái sau khi lệnh hoàn tất. Không tìm thấy bid trong lịch sử không đủ để kết luận thất bại. Nếu cần retry còn phải chốt chống gửi trùng. Rule auto-bid đã chạy trên server không thể bị client dừng chắc chắn khi mất kết nối. Chưa có các hợp đồng này trong server hiện tại.
