# Kịch bản bảo vệ và demo

Mỗi thành viên luyện cùng một luồng, sau đó đổi người trình bày để bảo đảm hiểu
toàn bộ. Không chỉ chia mỗi người học một thư mục. Số liệu cuối và phạm vi đã
kiểm thử nằm trong [AUDIT_AND_RESULTS.md](AUDIT_AND_RESULTS.md).

## Giới thiệu — 1 phút

“Nhóm xây dựng hệ thống đấu giá trực tuyến bằng Java 25, JavaFX 26.0.2 và Maven.
Seller đăng và quản lý sản phẩm, người tham gia đặt giá, Admin quản lý tài khoản
và phiên. Nhiều client kết nối một server TCP; server kiểm tra giá và ví, lưu JSON
tập trung rồi thông báo realtime. Ngoài chức năng chính, hệ thống có auto-bid,
anti-sniping và biểu đồ lịch sử giá. Trọng tâm là dữ liệu đúng khi nhiều người
đặt giá cùng lúc và mỗi thành viên giải thích được đường đi của một lệnh.”

## Kiến trúc — 2 phút

Mở [ARCHITECTURE.md](ARCHITECTURE.md), chỉ sơ đồ Client–Server. Khoảng 40 giây đầu
nói FXML/CSS → Controller → interface service → ServerServices/JsonTransport.
40 giây tiếp nói TCP frame → Server.Client.readLoop → AuctionService → domain →
repository. 40 giây cuối nói chỉ server mở state.json; save trước push, UI nhận
listener qua ViewTasks/FX thread. Mở `handlePlaceBid`, `execute`, `commit`, `save`
bằng bảng [CODE_MAP.md](CODE_MAP.md), không cuộn ngẫu nhiên cả nghìn dòng.

## OOP — 2 phút

30 giây cho Entity với thuộc tính private, getter/setter và đóng gói state. 30 giây cho hai nhánh kế thừa
User/Item. 30 giây chỉ call site `createUser` gọi getRoleName và `createAuction`
gọi getCategory bằng đa hình. 30 giây cho service interfaces và Auction record
chứa luật có kiểu dữ liệu. Nhấn mạnh `BidTransaction` là bid history, không phải
chuyển tiền; backend còn JSON làm transaction snapshot vì cần giữ dữ liệu cũ.

## Design Patterns — 2 phút

40 giây giải thích Factory Method create và Simple Factory forCategory; chỉ caller
thật. 50 giây đi theo Observer: event → commit/save → publish/queue → transport
→ listener → runLater; chỉ dispose gỡ listener. 30 giây nói Singleton nghiêm ngặt
ở MockDataStore, Server chỉ là default instance có constructor cho test. Không
nhận mình có Strategy/Command GoF; policy và commandId có mục đích khác.

## Chuẩn bị dữ liệu thử riêng

Không mở server trên `data/state.json` thật để diễn tập: scheduler có thể đóng và
thanh toán phiên cũ ngay khi khởi động. Dùng một thư mục hoàn toàn mới, Demo tắt.
Giữ cổng mặc định 8888; nếu cổng đang có server thật thì dừng phần diễn tập và xác
định môi trường trước, không kill tiến trình không rõ nguồn gốc.

Trong PowerShell ở thư mục dự án, build trước; đặt mật khẩu admin test riêng khi
storage mới yêu cầu. Ví dụ sau dùng nhập ẩn, không ghi mật khẩu thật trong tài liệu:

```powershell
mvn -B package -DskipTests
$testData = Join-Path $env:TEMP ('auction-viva-' + [guid]::NewGuid().ToString('N'))
$testAdminSecret = Read-Host 'Mat khau admin test (8-128 ky tu, gom chu va so)' -AsSecureString
$env:AUCTION_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $testAdminSecret).Password
# Giữ lại đường dẫn này để đọc JSON thử và restart đúng thư mục
Write-Output $testData
& "$env:JAVA_HOME\bin\java.exe" "-Dauction.data=$testData" '-Dauction.demo=false' '-Dauction.bind=127.0.0.1' '-Dauction.port=8888' -jar target/digital-auction-1.0.0.jar
```

Lệnh trên yêu cầu JAVA_HOME trỏ JDK 25. Tài khoản admin ban đầu là `admin_hieu`,
mật khẩu là giá trị vừa nhập. Với test LAN hai máy, bind `0.0.0.0`, client dùng IPv4
LAN thực của server theo [MULTI_CLIENT_SETUP.md](../../MULTI_CLIENT_SETUP.md).
Không bật Mock trong demo Client–Server. Không có mật khẩu mặc định cho storage thật.

Ở terminal khác, mở ba client bằng `Start-Client.cmd`, Enter để dùng 127.0.0.1.
Đăng ký seller thử và hai bidder thử bằng UI; nạp tiền giả lập đủ cho cả hai ví.
Seller tạo phiên tên “Phiên bảo vệ”, giá khởi điểm 1.000.000, bước 100.000, thời gian
bắt đầu sắp tới và kết thúc cách vài phút. Chờ RUNNING, không dùng phiên thật của
người dùng. Không thêm sẵn tài khoản hay sửa JSON để tạo fixture thủ công.

## Demo nhiều client, đồng thời và realtime — 3 phút

1. Hai bidder mở cùng phiên ở hai cửa sổ. Chỉ ra giá 1.000.000 và minimum 1.100.000.
2. Cùng nhập 1.200.000 rồi nhấn gần đồng thời. Một người thành công, người kia báo
   mức giá thấp khi server đã kiểm tra lại. Nếu UI nhận push trước khi người sau
   nhấn, UI có thể chặn trước khi gửi; đây không phải bằng chứng hai request đã
   đồng thời vào server. Test latch dưới đây chứng minh phía server rõ hơn.
3. Không refresh thủ công ở client còn lại: quan sát giá, leader, lịch sử và chart
   thay đổi. Mở Ví, chỉ ra total/held/available; bidder thua không bị giữ tiền.
4. Chuyển phòng rồi quay lại, chỉ ra không nhận callback lặp. Đóng một client,
   client kia vẫn tham gia được.

Mở terminal chạy tình huống đồng thời có latch, dữ liệu tạm tự quản lý:

```powershell
mvn -B '-Dtest=AuctionServiceTest#equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice,ServerIntegrationTest#simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer' test
```

Đọc assertion: đúng một success, BID_TOO_LOW phía sau, một bid history và ví đúng.
Test socket không có nghĩa đã thử hai máy LAN. Test fixture cũ có dữ liệu mẫu trong
@TempDir; nó không bật chế độ Demo của server đang trình bày.

## Demo nâng cao — 2 phút

Tạo hoặc dùng phiên thử khác để không lẫn kết quả cuộc bid đồng thời. A bật auto-bid
với trần đủ tiền, increment >=100.000; B đặt giá. Quan sát bid auto, không vượt trần
và không tự vượt giá chính mình. Nếu thử cùng trần, cho A đăng ký sớm hơn và chỉ
luật order trong service; không nói server dùng PriorityQueue như mock.

Với phiên kết thúc trong khoảng cuối 30 giây, đặt một bid hợp lệ; endTime phải tăng
thêm 60 giây từ deadline cũ, cả hai client cập nhật. Chỉ chạy trên dữ liệu thử.
Nếu không đủ thời gian diễn trực tiếp, mở AntiSnipingPolicyTest và test service
với MutableClock; nói rõ đó là kiểm thử tự động, không giả vờ vừa demo UI.

## Demo persistence và restart — 2 phút

Đọc riêng `$testData\state.json` bằng trình soạn thảo hoặc ConvertFrom-Json để chỉ
ra giá, bids, commands. Không sửa file khi server đang chạy, không chiếu hash mật
khẩu hoặc token để “chứng minh”. `schemaVersion` vẫn là 1.

Ghi lại số dư/giá và dùng Ctrl+C dừng server thử, rồi khởi động lại **cùng $testData**
với chính lệnh Java trước. Không đổi thư mục hoặc xóa JSON. Đăng nhập lại vì session
RAM mất; kiểm tra history/giá/ví còn đúng. Phiên đã hết giờ có thể được settle sau
restart, nên giải thích biến động số dư hợp lệ từ PAYMENT/SALE thay vì hứa mọi
trường đều bất biến. Test `settlementPaysSellerExactlyOnceAndSurvivesRestart` và
idempotency test chứng minh không thanh toán hoặc tạo bid lặp.

Nếu cần sao lưu, dừng server trước rồi copy state vào thư mục riêng; không để backup
có dữ liệu người dùng trong Git. Sau demo đóng các client/server thử, xóa biến môi
trường mật khẩu trong terminal bằng `Remove-Item Env:AUCTION_ADMIN_PASSWORD`.
Không xóa dữ liệu thật hoặc bất kỳ checkpoint nào của dự án.

## Vì sao JSON và những hạn chế — 1 phút

“Nhóm chọn JSON tập trung vì đề cho phép File I/O, dữ liệu bài tập nhỏ và dễ quan
sát để giải thích. Tính đúng không đến từ JSON: service khóa toàn bộ giao dịch,
repository ghi tạm rồi atomic replace, commandId giữ kết quả qua restart. Nhược
điểm là mỗi lần lưu ghi cả snapshot, các phiên chờ khóa chung, chưa hỗ trợ nhiều
server ghi đồng thời. TCP chưa TLS, chỉ dùng LAN tin cậy. Test local không thay
cho kiểm thử hai máy hay mất điện thật. Checkstyle hiện mới kiểm quy tắc cơ bản,
coverage chỉ đo code được chạy và không chứng minh không còn bug.”

## Khi bị hỏi sâu

- Người dẫn đầu khác winner cuối; auto-bid có thể làm thay đổi leader ngay trong lệnh.
- Phân biệt total balance với hold, cùng commandId với requestId mới, rejection với UNKNOWN.
- Không hứa thứ tự lấy monitor theo lúc nhấn nút; nếu save đầu tiên lỗi người sau có thể thành công.
- Không hứa event luôn đến sau ACK: event được enqueue trước khi service trả response.
- Không hứa Server constructor private hoặc backend đã bỏ toàn bộ JsonObject.
- Khi cần minh chứng, mở test cụ thể và assertion thay vì trả lời bằng tên pattern.
