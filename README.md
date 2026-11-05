# Hệ thống đấu giá trực tuyến

Ứng dụng JavaFX kết nối server Java qua TCP Socket, trao đổi JSON.
Một dự án Maven, một thư mục `src`; client và server vẫn chạy thành hai tiến trình.

## Chạy trên Windows

Cài **JDK 25 trở lên**, **Maven** và **Git** nếu lấy code bằng Git. Kiểm tra trong terminal:

```powershell
java -version
mvn -version
```

Mở thư mục dự án bằng **File Explorer**:

1. Nhấp đúp `Start-Server.cmd`, chờ dòng `Auction JSON server listening on ...`.
2. Giữ cửa sổ server mở, nhấp đúp `Start-Client.cmd`.
3. Mở thêm `Start-Client.cmd` để thử hai người đấu giá.

Nhấp đúp file trong VS Code chỉ mở nội dung để sửa. Để chạy ngay trong terminal VS Code:

```powershell
.\Start-Server.cmd
```

Mở terminal thứ hai, cùng thư mục chứa `pom.xml`:

```powershell
.\Start-Client.cmd
```

Lần đầu Maven tải thư viện nên cần mạng và có thể mất vài phút.
Nếu có lỗi, cửa sổ `.cmd` giữ lại thông báo để đọc.

## Tài khoản demo của server

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Người đấu giá | bidder_hieu | Bidder12345 |
| Người đấu giá 2 | bidder_vip99 | Bidder12345 |
| Người bán | seller_hieu | Seller12345 |
| Quản trị viên | admin_hieu | Admin12345 |

Tài khoản và sáu phiên mẫu chỉ được tạo khi chưa có dữ liệu.
Nếu phiên mẫu đã hết giờ, đăng nhập người bán để tạo phiên mới.
Tài khoản đăng ký mới có số dư 0; vào Ví để nạp tiền giả lập.

## Chức năng

- Đăng nhập, đăng ký, phân quyền người bán/người đấu giá/quản trị viên.
- Danh sách phiên, lọc/tìm kiếm, chi tiết và ảnh sản phẩm.
- Tạo/sửa/xóa sản phẩm và phiên sắp diễn ra của người bán.
- Đặt giá, cập nhật giá và lịch sử/biểu đồ trực tiếp cho nhiều client.
- Đấu giá tự động theo giá tối đa, bước giá và thứ tự đăng ký.
- Gia hạn 60 giây khi có bid hợp lệ trong 30 giây cuối phiên.
- Ví: nạp/rút giả lập, giữ tiền khi đang dẫn đầu, trả tiền cho người bán khi hết phiên.
- Quản trị tài khoản, khóa/mở khóa, đổi vai trò và hủy/lưu trữ phiên.
- Lưu trạng thái server ra JSON; gửi lại cùng mã lệnh không trừ tiền hai lần.

## Cấu trúc

```text
pom.xml                         Cấu hình build duy nhất
src/main/java/com/auction/
  client/
    controller/                 Xử lý thao tác giao diện
    model/                      Dữ liệu dùng để hiển thị
    service/server/             Gọi server và chuyển JSON thành model
    service/mock/               Dữ liệu giả để phát triển UI độc lập
    navigation/, ui/, util/     Chuyển màn hình và tiện ích giao diện
  server/
    network/Server.java         Nhận kết nối và điều phối request
    service/AuctionService.java Xác thực, sản phẩm, đấu giá, ví, quản trị
    service/AntiSnipingPolicy.java
    dao/AuctionRepository.java  Đọc/ghi trạng thái JSON
    model/                      Entity, User, Item và Factory
    exception/                  Lỗi nghiệp vụ
  shared/JsonProtocol.java      Đóng gói thông điệp dùng chung
src/main/resources/             FXML và CSS
src/test/java/                  Kiểm thử client, server và kết nối
```

Đọc [luồng xử lý và câu hỏi vấn đáp](docs/VAN_DAP.md).

## Lệnh khác

Từ thư mục chứa `pom.xml`:

```powershell
# Kiểm thử nghiệp vụ và kết nối
mvn -B verify

# Kiểm thử thêm màn hình JavaFX (cần desktop; Linux dùng Xvfb)
mvn -B verify "-Dauction.ui.tests=true"

# Chạy UI độc lập, không cần server (mock dùng mật khẩu 123456)
powershell -NoProfile -ExecutionPolicy Bypass -File .un.ps1 Client -Mock

# Đổi địa chỉ khi client và server ở hai máy khác nhau
powershell -NoProfile -ExecutionPolicy Bypass -File .un.ps1 Server -ServerHost 0.0.0.0
powershell -NoProfile -ExecutionPolicy Bypass -File .un.ps1 Client -ServerHost IP_MAY_SERVER
```

Các lệnh server và client ở hai dòng cuối chạy trên hai máy/terminal tương ứng.
Máy server cần cho phép kết nối TCP cổng 8888.

Không dùng script cũng được: terminal thứ nhất chạy `mvn -B package -DskipTests`,
rồi `java -Dauction.demo=true -jar target/digital-auction-1.0.0.jar`.
Terminal thứ hai chạy `mvn javafx:run`.

## Dữ liệu và phạm vi

Server lưu `data/state.json` tại thư mục dự án, không dùng MySQL.
Thư mục `data/` đã được loại khỏi Git. Ví dùng tiền giả lập, chưa tích hợp thanh toán ngân hàng.
Các lệnh client chưa rõ kết quả được lưu ở thư mục người dùng `.digital-auction/server` để đối soát khi kết nối lại.

Nếu chuyển dữ liệu từ bản nhiều module: tắt server cũ, sao lưu `auction-server/data/state.json`,
rồi chép file đó vào `data/state.json` của bản này **trước lần chạy đầu**.
Định dạng dữ liệu vẫn là `schemaVersion = 1`.

Bản gọn giữ giao thức và các chức năng đang chạy. Đã bỏ bản ObjectStream tham khảo,
thuật toán/DAO cũ không được server sử dụng và màn hình trưng bày UI dành cho phát triển.
Lịch sử Git của bản đầy đủ trước đó vẫn còn.

## Làm việc trên nhánh riêng

Lấy bản gọn sau khi nhánh đã được đăng lên GitHub:

```powershell
git clone --branch codex/simple-project https://github.com/hgiang757/Bai-Tap-Lon.git Bai-Tap-Lon-gon
cd Bai-Tap-Lon-gon
```

Để đưa thay đổi tiếp theo lên đúng nhánh này:

```powershell
git branch --show-current
git add -A
git commit -m "Update simplified auction application"
git push origin codex/simple-project
```

Dòng đầu cần hiển thị `codex/simple-project`. Push nhánh riêng chưa thay đổi `main`.
`main` nhận code mới khi có thao tác merge vào `main` hoặc push trực tiếp lên `main`.
