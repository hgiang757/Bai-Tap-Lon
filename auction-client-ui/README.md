# Thành viên 3 — JavaFX client

Nhánh này bàn giao UI/MVC và phần mạng của thành viên 1. Không triển khai lại dữ liệu,
DAO, logic nghiệp vụ của thành viên 2 hoặc thuật toán nâng cao/CI của thành viên 4.

## Chạy UI độc lập

Tại `auction-client-ui`, cần JDK 25 và Maven:

```powershell
mvn javafx:run
```

Mặc định là MOCK: `bidder_hieu`, `seller_hieu`, `admin_hieu`, mật khẩu `123456`.
Dữ liệu nằm trong RAM, ảnh cục bộ và ví giả lập; đây không phải backend nghiệp vụ thật.

UI có đăng ký/đăng nhập, danh sách/tìm/lọc phiên, chi tiết/đặt giá, lịch sử/biểu đồ,
ví, các màn hình auto-bid/sản phẩm/quản trị, hướng dẫn và liên hệ. Mock đi kèm để
thử các màn hình; không thay thế thuật toán server của thành viên 2/4.

## Kết nối do thành viên 1 cung cấp

`service/server/JsonTransport` đọc response và push độc lập, ghép requestId, timeout,
reconnect. `ServerServices` ánh xạ DTO vào các interface UI, tải ảnh và giữ commandId
khi mất phản hồi. Đây là adapter client; không chứa DAO hoặc thuật toán server.

Khi thành viên 2 đã cài BackendGateway và thống nhất DTO theo hợp đồng mạng:

```powershell
mvn javafx:run "-Dauction.mode=server" "-Dauction.host=127.0.0.1" "-Dauction.port=8888"
```

Server mạng chưa có provider nghiệp vụ sẽ trả BACKEND_NOT_CONFIGURED. Không tự
chuyển mock và không xác nhận đăng nhập/đặt giá thành công giả.

## Kiểm thử UI

```powershell
mvn -B test "-Dauction.ui.tests=true"
```

Hai file Object-stream cũ từ main được giữ nguyên ở `reference/object-stream-prototype`
với đuôi .java.txt vì các lớp shared và service mà chúng cần chưa được đưa vào repo.
