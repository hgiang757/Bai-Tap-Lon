# Backend đấu giá

Java 25, Maven, TCP Socket với JSON UTF-8. Backend xử lý tài khoản, phân quyền, sản phẩm/ảnh, đấu giá, ví giả lập, auto-bid, anti-sniping, quản trị, lưu trữ và push realtime. Module JavaFX đã có adapter cho cả 6 dịch vụ và mặc định kết nối server. Xem README gốc để chạy nhiều client.

## Build và chạy

Tại `auction-server`:

```powershell
mvn -B package
java -Dauction.demo=true -jar target/auction-server-1.0.0.jar
```

Mặc định lắng nghe `127.0.0.1:8888`, dữ liệu ở `data/state.json`. Có thể cấu hình:

```powershell
java -Dauction.host=0.0.0.0 -Dauction.port=8888 -Dauction.data=D:/auction-data -Dauction.demo=true -jar target/auction-server-1.0.0.jar
```

Chế độ demo chỉ seed khi chưa có state. Tài khoản: `admin_hieu / Admin12345`, `seller_hieu / Seller12345`, `bidder_hieu / Bidder12345`, `bidder_vip99 / Bidder12345`. Hai bidder có số dư 100 và 300 triệu đồng. Mật khẩu demo khác bản mock cũ.

Khi không bật demo, lần đầu cần biến môi trường `AUCTION_ADMIN_PASSWORD` (8–128 ký tự, có chữ và số); tạo `admin_hieu`. Đăng ký mới có số dư 0; nạp tiền qua lệnh giả lập. Nếu đã có dữ liệu, không tạo lại user hoặc thay mật khẩu từ môi trường. Không xóa thư mục data để cập nhật chương trình.

`Ctrl+C` đóng kết nối và nhả khóa thư mục dữ liệu. Không chạy hai server dùng cùng thư mục. File dữ liệu hỏng hoặc schema khác sẽ báo lỗi thay vì seed đè dữ liệu.

## Quy tắc nghiệp vụ

- Tiền là số nguyên VND; giá/lệnh nạp/rút tối đa 9 nghìn tỷ. Không chuyển `double` trực tiếp từ UI nếu có phần lẻ.
- Bid đầu và các bid sau phải >= giá hiện tại + bước tối thiểu. Cho phép người dẫn đầu tăng giá; phần tiền giữ trước đó được tính lại, không giữ hai lần.
- Bidder và Seller được đấu sản phẩm người khác; Admin và chủ sản phẩm không được đấu. Không nhận giá trước giờ mở hoặc khi đã đóng/hủy.
- Chỉ Seller sở hữu sản phẩm được sửa/xóa khi OPEN chưa có bid; server kiểm tra tại thời điểm ghi.
- Ví chỉ có thao tác cho chính tài khoản trong session; mọi `username`, `actorId`, số dư hoặc seller giả mạo trong payload không cấp quyền.
- Bid giữ tiền người dẫn đầu, nhả tiền người cũ. Rút không dùng được tiền đang giữ. Hai phiên cùng ví được kiểm soát trong cùng khóa giao dịch.
- Scheduler chạy mỗi 250 ms, không phụ thuộc client. Hết giờ: không bid → FINISHED; có bid → chốt, trừ người mua, cộng người bán, ghi PAYMENT/SALE và PAID đúng một lần. Khi restart, xử lý phiên quá hạn ở tick đầu.
- Anti-sniping: bid trong 30 giây cuối kéo dài endTime thêm 60 giây. Client phải dùng endTime mới.
- Auto-bid ưu tiên thứ tự đăng ký, không vượt max hoặc tiền khả dụng; bước cuối có thể nhỏ hơn increment nếu vẫn đạt bước tối thiểu. Đăng ký lại mất ưu tiên cũ. Giới hạn 10.000 vòng; nếu vượt thì từ chối và rollback cả lệnh. Rule tồn tại sau khi client đóng và sau server restart.
- Khóa user áp dụng ngay cho request tiếp theo, dừng rule của user. Xóa user dùng soft delete để giữ lịch sử. Không tự khóa/xóa/đổi vai trò của Admin đang thao tác. Không xóa/đổi vai trò khi tài khoản còn nghĩa vụ đấu giá hoạt động.
- Hủy phiên cần Admin và lý do; nhả giữ tiền, dừng rule, lưu lý do và phát sự kiện. Xóa phiên Admin chỉ cho CANCELED; dữ liệu được đánh dấu xóa, giữ chứng cứ.
- Ảnh gửi base64 trong CREATE/UPDATE, dưới 5 MB; kiểm tra định dạng bằng ImageIO và tối đa 40 triệu pixel. Bỏ ảnh bằng chuỗi rỗng; không truyền trường ảnh để giữ ảnh cũ. GET_IMAGE trả byte dùng chung giữa các máy, không trả đường dẫn ổ đĩa server.

## Kiến trúc và phạm vi

`network/Server` quản lý kết nối/framing/router và subscription. `application/AuctionApplication` là application service duy nhất cho request và scheduler; `JsonRepository` commit một snapshot nguyên tử gồm user, phiên, bid, ví, rule và kết quả lệnh. Các transaction được tuần tự hóa bằng khóa ứng dụng để bảo vệ cả một ví trên nhiều phiên. Đây là lựa chọn cho BTL quy mô nhỏ; không tuyên bố throughput của hệ thống thương mại.

PBKDF2-HMAC-SHA256 với salt ngẫu nhiên, 120.000 vòng. Session ngẫu nhiên hết hạn sau 12 giờ, lưu trong RAM; restart yêu cầu đăng nhập lại. Snapshot không lưu token, response profile không chứa hash. Giao thức TCP hiện không có TLS: cấu hình mặc định loopback, dùng môi trường demo/LAN tin cậy hoặc TLS tunnel nếu cần truy cập từ xa.

Factory Method nằm trong `model/ItemFactory`, dùng các lớp Item kế thừa cho loại sản phẩm. Observer được chuyển qua event Socket. Một instance AuctionApplication sở hữu kho dữ liệu và vòng đời tập trung; không dùng Singleton static cho state để các test có thể chạy kho độc lập.

`service/BidProcessor` và `service/AutoBidEngine` cũ được giữ tương thích cho bài học/kiểm thử thuật toán, không phải entry point giao dịch tiền của server. Workflow chạy thật dùng integer VND trong AuctionApplication và dùng lại AntiSnipingPolicy. Không gọi trực tiếp các thuật toán double cũ từ handler hoặc frontend vì quy tắc bid đầu/leader khác.

## Kiểm thử

```powershell
mvn -B test
```

Các kiểm thử mới dùng thư mục tạm: xác thực/role/khóa user, CRUD, giá lẻ/thấp/sở hữu, lịch mở, hai phiên cùng ví, tạm giữ/rút, settlement một lần, restart, anti-sniping, auto-bid, replay commandId, lỗi ghi file rollback; kiểm thử hai socket thật nhận push và reconnect không nhân đôi bid. Test cũ vẫn chạy.

Giao thức đầy đủ và các trường request ở [PROTOCOL.md](PROTOCOL.md). File `Client.java` cũ ở gốc module là ví dụ echo trước đây và không tương thích giao thức mới; không dùng nó để kiểm chứng server này.

Giới hạn còn có chủ ý: state và command log nằm trong RAM và snapshot toàn file; chưa phân trang các danh sách, frame tối đa 8 MB, không tải lịch sử khổng lồ qua một request. Mục tiêu là demo nhiều client và dữ liệu BTL. Không có giao nhận hàng hoặc thanh toán tiền thật. REFUND không có lệnh công khai vì phiên thắng được thanh toán tự động và phiên đang chạy bị hủy chỉ giải phóng hold.
