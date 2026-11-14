# Một Server và nhiều Client Windows qua LAN/Wi-Fi

## Mô hình JSON-only

Java 25, JavaFX 26.0.2, Maven, MVC, TCP Socket JSON port 8888. Một server lưu tập
trung data/state.json. Client chỉ truy cập qua TCP, không chia sẻ/mở thư mục data
server và không cần MySQL, JDBC URL hoặc biến database.

## Máy server

Cài JDK 25 và Maven. Kiểm tra `java -version`, `mvn -version`, `ipconfig`.
Giữ nguyên dữ liệu đã xác minh; dừng server trước khi sao lưu hoặc khôi phục.
Storage hoàn toàn mới cần đặt AUCTION_ADMIN_PASSWORD để cấp admin ban đầu;
không tạo tài khoản/phiên mẫu. Dữ liệu hiện có không cần cấp lại admin.

```powershell
.\Start-Server.cmd
# Hoặc bind IPv4 thực của server (IP ví dụ phải thay)
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 Server -ServerHost 192.168.1.100 -Port 8888
```

Mặc định bind 0.0.0.0:8888, JSON thật, Demo tắt. Trên LAN tin cậy đặt profile
Private. Nếu firewall chặn, quản trị viên có thể chạy rule sau cho subnet nội bộ:

```powershell
New-NetFirewallRule -DisplayName 'Auction LAN TCP 8888' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8888 -Profile Private -RemoteAddress LocalSubnet
Get-NetTCPConnection -LocalPort 8888 -State Listen
```

Không port-forward ra Internet. Không mở cổng database.

## Máy client

Cài JDK 25 và Maven nếu chạy từ source. Không chép thư mục dữ liệu server.

```powershell
Test-NetConnection -ComputerName 192.168.1.100 -Port 8888
.\Start-Client.cmd
# Hoặc chỉ định IP thực từ ipconfig của server
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 Client -ServerHost 192.168.1.100
```

CMD hỏi IP/hostname; Enter dùng 127.0.0.1 nếu cùng máy. Không nhập URL, IP:port hoặc
0.0.0.0 vào host. Client luôn dùng TCP; tham số `-Mock` đã bỏ và `auction.mode=mock` không hợp lệ.
Đăng nhập hai bidder khác nhau, mở cùng phiên.
Build JAR bằng `mvn -B verify`, server chạy `java -jar target/digital-auction-1.0.0.jar`.
Client JAR dùng bootstrap sau vì manifest chính là server:

```powershell
java '-Dauction.mode=server' '-Dauction.host=192.168.1.100' '-Dauction.port=8888' -cp target/digital-auction-1.0.0.jar com.auction.client.App
```

JavaFX native phụ thuộc OS build; build trên Windows cho các máy Windows.

## Xử lý lỗi và bảo vệ dữ liệu

- Refused: kiểm tra server đã chạy, đúng cổng và log listening.
- Timeout: kiểm tra IP, firewall Private/LocalSubnet, cùng LAN. Wi-Fi khách/AP
  isolation có thể chặn liên lạc; DHCP có thể đổi IP.
- Mạng chạy nền qua ViewTasks; timeout mặc định 8 giây, heartbeat mỗi 5 giây.
  Adapter báo lỗi, đăng ký lại feed và tải lại khi reconnect; restart cần login lại.
- Lệnh chưa rõ kết quả phải đối soát cùng commandId; không đổi mã rồi gửi mới,
  không xóa sổ pending để thử lại.
- STORAGE_ERROR: xem log, quyền thư mục và dung lượng đĩa. Không báo thành công/push
  trước khi lưu. Không chép .tmp lên dữ liệu chính hoặc reset JSON lỗi.
- Không hỗ trợ atomic move: từ chối giao dịch; dùng filesystem local có hỗ trợ,
  không fallback ghi đè không nguyên tử. Chỉ một server giữ khóa thư mục data.
- Restart có thể làm scheduler đóng/thanh toán phiên đã hết giờ. Kiểm thử bằng dữ
  liệu riêng, không khởi động thử trên dữ liệu thật.

## Checklist trên ít nhất hai máy Windows

Chỉ ghi PASS sau khi quan sát, dùng dữ liệu thử và sao lưu trước:

| Bài | Kết quả mong đợi |
|---|---|
| Hai client | A/B đăng nhập riêng, cùng tải danh sách phiên. |
| Realtime | A bid, B đổi giá/lịch sử/biểu đồ không cần refresh. |
| Đồng thời | Bid cùng mức chỉ một thành công; ví và lịch sử khớp. |
| Ngắt client | Tắt A đột ngột, B vẫn bid được. |
| Mất mạng | Server dừng, UI báo lỗi và vẫn phản hồi; reconnect/login lại được. |
| Restart | Ví, bid, người thắng, commandId giữ đúng, không nhân đôi. |
| Nâng cao | Auto-bid, ưu tiên trần bằng nhau và anti-sniping đúng. |
| Phân quyền | Seller CRUD, Admin quản trị; chặn thao tác trái quyền. |

[Báo cáo hiện tại](docs/archive/JSON_ONLY_REFACTOR.md) phân biệt test socket một máy và LAN
nhiều máy chưa thực hiện. Khóa chung và snapshot toàn bộ phù hợp bài tập nhỏ;
TCP chưa TLS, chưa kiểm chứng tải lớn. Các báo cáo cũ được giữ làm lịch sử.

## Chuẩn bị và giữ bộ dữ liệu demo do nhóm tự tạo

Đăng ký Seller và ít nhất hai Bidder qua giao diện thật, nạp tiền giả lập vào ví,
rồi Seller tạo sản phẩm có ảnh, giá khởi điểm, bước giá và lịch. Những dữ liệu này
là dữ liệu nghiệp vụ JSON trên Server, khác với Mock trong RAM và fixture JUnit
trong `@TempDir`. Client thoát/logout không xóa dữ liệu này.

Chuẩn bị trước buổi bảo vệ bằng các phiên **OPEN có lịch bắt đầu đúng ngày/giờ
bảo vệ**, kết thúc sau thời lượng trình bày với khoảng dự phòng. Không bật “Bắt đầu
ngay” từ vài ngày trước: ô này dùng giờ Server lúc gửi Lưu, và phiên hết giờ sẽ
được scheduler đóng/thanh toán khi Server chạy hoặc restart. Trước buổi bảo vệ
kiểm tra ngày, giờ Việt Nam trên Server, start/end và trạng thái từng phiên. Chỉ
sửa lịch qua Seller khi phiên OPEN chưa có bid; nếu đã chạy/hết giờ, tạo phiên mới
qua UI, không sửa timestamp trong JSON hoặc reset snapshot.

Sao lưu sau khi chuẩn bị xong:

1. Đóng Client và dừng Server bằng Ctrl+C; kiểm tra tiến trình Server đã thoát.
2. Sao chép `data/state.json` vào thư mục riêng ngoài Git, đặt tên kèm ngày/giờ.
   Nếu chạy với `-Dauction.data=...`, dùng đúng thư mục đó thay cho `data`.
3. Dùng `Get-FileHash -Algorithm SHA256` cho file gốc và bản sao; chỉ chấp nhận
   khi hash khớp. Ghi đường dẫn và hash riêng; không đưa nội dung snapshot lên Git.
4. Khi khôi phục, dừng Server, sao lưu hiện trạng trước, rồi chép snapshot đã xác
   minh vào đúng thư mục dữ liệu. Không khôi phục file `.tmp`, file pending của
   Client hoặc `server.lock` thay cho `state.json`.
5. Mở Server lại cùng thư mục, đăng nhập lại các Client; kiểm tra tên phiên, ảnh,
   giá, bid history, ví và tiền đang giữ. Không kỳ vọng bản sao lưu giữ phiên còn
   RUNNING mãi: deadline vẫn là giờ thực, phiên hết giờ được xử lý bình thường.

Khi diễn tập, dùng thư mục QA riêng qua `-Dauction.data=<thư-mục-thử>` với
`-Dauction.demo=false`; giữ lại thư mục đó để restart. Không dùng dữ liệu thật làm
fixture JUnit, không khởi động một Server thử trên `data/state.json`. Dữ liệu cá
nhân, backup, mật khẩu và ảnh cache không được commit. Giữ `data/` trong `.gitignore`.

## Giao diện Seller và lịch sử sau cập nhật

Form Thêm/Sửa nằm trong trang Quản lý sản phẩm; chọn Lưu, Hủy, ← Danh sách hoặc ESC.
Lỗi lưu giữ bản nháp để sửa; rời trang giải phóng các tác vụ ảnh. Chọn ảnh bằng hộp
thoại hệ thống có owner là cửa sổ chính. Trên trang chủ chọn Kết quả / Lịch sử để
xem phiên đã kết thúc, PAID hoặc CANCELED; không chỉnh JSON để ẩn/xóa các phiên này.
Server entry point không chạy seed khi restart, kể cả cấu hình demo=true cũ.
Constructor Server đã private; TestServers trong src/test dùng reflection cho fixture tích hợp.
Chi tiết bằng chứng và giới hạn: [báo cáo Seller/Mock](docs/SELLER_UI_MOCK_CLEANUP_REPORT.md).
