# Digital Auction

Ứng dụng đấu giá trực tuyến viết bằng Java 25, JavaFX 26.0.2 và Maven. Người dùng có thể đăng ký, đăng nhập, đặt giá và xem biểu đồ realtime. Seller quản lý sản phẩm và phiên, Admin quản trị hệ thống. Server xử lý ví giả lập, auto-bid và gia hạn phiên qua TCP JSON. Dữ liệu được lưu tập trung trong `data/state.json` bằng Gson.

## Chạy trên Windows

Cài JDK 25 và Maven, thêm vào PATH, rồi kiểm tra bằng `java -version` và `mvn -version`. Mở PowerShell hoặc terminal VS Code tại thư mục có `pom.xml`:

```powershell
# Cửa sổ 1
.\Start-Server.cmd
# Cửa sổ 2 sau khi server hiện Auction JSON server listening
.\Start-Client.cmd
```

Nhấn Enter khi client hỏi host để dùng `127.0.0.1` trên cùng máy. Giữ cửa sổ server mở. Maven lần đầu có thể cần mạng để tải dependency.

Server mặc định bind `0.0.0.0:8888`. Khi thư mục dữ liệu hoàn toàn mới, cần cấp `AUCTION_ADMIN_PASSWORD` bằng lời nhắc ẩn hoặc môi trường của terminal để tạo `admin_hieu`. Mật khẩu dài 8–128 ký tự, có chữ và số. Nếu đã có `state.json` thì dùng tài khoản hiện có. Production không seed tài khoản hoặc phiên demo.

```powershell
# Chỉ thiết lập khi tạo thư mục data mới
$env:AUCTION_ADMIN_PASSWORD = [Net.NetworkCredential]::new(
  '', (Read-Host 'Mat khau admin ban dau' -AsSecureString)).Password
```

## Chạy nhiều client qua LAN

1. Máy server giữ một thư mục dữ liệu. Lấy IPv4 LAN của máy này bằng `ipconfig`, rồi chạy server. Dùng Windows network profile Private trên LAN tin cậy.
2. Máy client kiểm tra đến IP/cổng thực của server, rồi chạy `Start-Client.cmd` và nhập IPv4 hoặc hostname. Client và server dùng cùng cổng. Một IP `0.0.0.0` chỉ có nghĩa khi bind server.
3. Đăng nhập bằng hai tài khoản khác nhau và mở cùng một phiên. Đặt giá ở một client để quan sát giá, người dẫn đầu, lịch sử và biểu đồ ở client kia. Kiểm tra hai bid đồng thời, rút phần tiền đang hold, reconnect và restart theo [hướng dẫn diễn tập](docs/PROJECT_CONTEXT.md#diễn-tập-trên-dữ-liệu-riêng).

```powershell
# Máy client, thay IP ví dụ bằng IPv4 thực của server
Test-NetConnection -ComputerName 192.168.1.100 -Port 8888
.\Start-Client.cmd -ServerHost 192.168.1.100 -Port 8888
# Máy server, chỉ định IP bind hoặc cổng nếu cần
.\Start-Server.cmd -ServerHost 192.168.1.100 -Port 8888
Get-NetTCPConnection -LocalPort 8888 -State Listen
```

Nếu firewall chặn, quản trị viên trên máy server có thể cho phép TCP trên profile Private với phạm vi LocalSubnet:

```powershell
New-NetFirewallRule -DisplayName 'Auction LAN TCP 8888' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8888 -Profile Private -RemoteAddress LocalSubnet
```

Client truy cập qua socket, dữ liệu server ở lại trên máy server. TCP ứng dụng chưa TLS; tránh port-forward ra Internet. Wi-Fi khách có thể chặn kết nối giữa các máy và DHCP có thể đổi IP. Kiểm thử loopback trên một máy khác với kiểm thử hai máy vật lý.

### Chuẩn bị demo, sao lưu và khôi phục

- Dùng thư mục thử riêng và tạo Seller, hai Bidder, ảnh và tiền giả lập qua UI. Chuẩn bị phiên `OPEN` vào đúng giờ demo, có thời gian dự phòng. Phiên **Bắt đầu ngay** dùng giờ server tại lúc lưu. Chỉ sửa/xóa phiên sắp bắt đầu, chưa có bid và đúng chủ sở hữu.
- Để thử restart, chạy JAR với `-Dauction.data=...` đến cùng thư mục thử mỗi lần. Đóng server trước khi sao lưu. Sao chép `state.json` sang nơi riêng ngoài Git, lưu ngày/giờ và so SHA-256 bằng `Get-FileHash` cho hai file.
- Để phục hồi, dừng server, sao lưu bản đang có trước, rồi chép bản snapshot đã kiểm hash vào đúng thư mục. Đăng nhập lại các client và kiểm sản phẩm, ảnh, lịch sử, ví và hold. Khi server mở lại, phiên đã hết hạn có thể được tất toán theo giờ thực. Chỉ dùng bản `state.json` đã xác minh; giữ `.tmp`, `server.lock` và journal client đúng vai trò của chúng.

```powershell
mvn -B package -DskipTests
java '-Dauction.bind=127.0.0.1' '-Dauction.port=8888' '-Dauction.data=target/data-demo' -jar target/digital-auction-1.0.0.jar
# Client từ JAR, build trên OS thích hợp với JavaFX native
java '-Dauction.mode=server' '-Dauction.host=127.0.0.1' '-Dauction.port=8888' -cp target/digital-auction-1.0.0.jar com.auction.client.App
```

## Gemini

**Khi tải ZIP từ GitHub:** Cấu hình Gemini API key trên máy người gửi không đi kèm ZIP.

- **Chạy server và client để đăng nhập, đăng ký hoặc đấu giá:** Không cần Gemini API key. Chạy `Start-Server.cmd` và `Start-Client.cmd` như hướng dẫn ở trên.
- **Chỉ chạy client và kết nối vào server của người khác:** Không cần nhập key trên máy client. Chatbot Gemini dùng key đã cấu hình trên server.
- **Tự chạy server và muốn dùng chatbot Gemini:** Chạy `.\Gemini.cmd -ConfigureGemini` trên máy server rồi nhập Gemini API key của bạn khi được hỏi.

Trên máy server, chạy `.\Gemini.cmd -ConfigureGemini` và dán khóa vào lời nhắc ẩn. Lệnh lưu khóa bằng DPAPI theo tài khoản Windows hiện tại rồi chạy server. Dùng lại cùng lệnh khi cần thay khóa. Thêm `-GeminiFromClipboard` nếu terminal không dán được vào lời nhắc ẩn.

`.\Start-Server.cmd` ưu tiên `GEMINI_API_KEY` trong môi trường tiến trình, rồi dùng khóa DPAPI nếu có (có thể đặt thêm `GEMINI_MODEL` nếu muốn đổi mô hình mặc định). Khi thiếu khóa, server vẫn mở bình thường và các mục **Thông tin phiên** và **Giá thị trường** vẫn trả lời từ dữ liệu server. Khi đã có khóa và chưa có nguồn giá xác minh nội bộ, mục **Giá thị trường** gọi AI để đưa ra khoảng giá tham khảo chưa xác minh bằng VND. Chạy `.\Gemini.cmd` để được nhắc nhập và lưu khóa nếu chưa có. Khóa chỉ dùng ở server qua biến môi trường.

Mở phòng đấu giá và bấm biểu tượng chat. Trạng thái `configured` xác nhận cấu hình cục bộ. `.\Gemini.cmd -VerifyGemini` kiểm tra Gemini thật qua JavaFX với cổng và dữ liệu thử riêng rồi thoát. Test Maven mặc định dùng provider giả lập trên loopback. [Luồng chat, mã lỗi và giới hạn](docs/PROJECT_CONTEXT.md#gemini-và-chatbot)

## Build và test

```powershell
mvn -B test
mvn -B verify
mvn -B verify '-Dauction.ui.tests=true'
# Khi đã có cache .maven-repository và cần chạy offline
mvn -o -B '-Dmaven.repo.local=.maven-repository' verify
```

Bật `auction.ui.tests=true` khi có desktop. CI Linux cấu hình Xvfb. `verify` kiểm JUnit, JaCoCo với ngưỡng 60% và Checkstyle. Kết quả nằm dưới `target/surefire-reports` và `target/site/jacoco`. Fixture dùng data tạm. Khi cần số coverage riêng cho lượt chạy, giữ lại output cần thiết trước khi làm mới thư mục build hoặc file JaCoCo.

Nếu Maven cần dùng cache có sẵn trong workspace để chạy các launcher Windows, đặt `$env:MAVEN_ARGS = '-o -Dmaven.repo.local=.maven-repository'` trong terminal trước khi chạy lệnh.

## Lỗi thường gặp

| Dấu hiệu | Cách kiểm tra |
|---|---|
| Client báo refused hoặc timeout | Xem server đã listening đúng IP/cổng chưa. Kiểm tra `Test-NetConnection`, firewall, profile mạng và IP sau khi đổi Wi-Fi. |
| Mất mạng sau khi gửi bid | Đăng nhập lại khi cần, tải snapshot và đối soát journal lệnh. Gửi lại cùng `commandId`; tránh đổi mã lệnh hoặc xóa journal để thử lại. |
| `STORAGE_ERROR` hoặc JSON lỗi | Kiểm tra log, quyền thư mục, đĩa và hỗ trợ atomic move. Chỉ một server giữ file lock. Giữ JSON để điều tra và phục hồi từ bản snapshot đã xác minh. |
| Clipboard rỗng hoặc terminal chuyển Ctrl+V thành ký tự điều khiển | Sao chép lại khóa dưới dạng văn bản rồi dùng `.\Gemini.cmd -ConfigureGemini -GeminiFromClipboard`. Dấu sao ở lời nhắc chỉ che phần nhập. |
| Gemini báo `AI_KEY_FORMAT`, `AI_AUTH_FAILED` hoặc timeout | Phân biệt Java chặn trước HTTP, Google từ chối và lỗi kết nối. Ưu tiên mã lỗi đã lọc; không chép khóa hoặc phản hồi thô vào chat. |

## Tài liệu

- [Kiến trúc, source code và ngữ cảnh cho Codex](docs/PROJECT_CONTEXT.md)
- [Luyện vấn đáp và diễn tập](docs/PROJECT_CONTEXT.md#luyện-vấn-đáp-và-diễn-tập)
- [Đề bài](docs/2026-BTL.pdf) và [hướng dẫn giảng viên](docs/H%C6%B0%E1%BB%9Bng%20d%E1%BA%ABn%20BTL%20LTNC-2026.pdf)
