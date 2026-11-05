# Thành viên 1 — Hệ thống và mạng

Phần thay đổi của nhánh này: TCP Socket, request/response JSON, push/Observer,
quản lý kết nối, Singleton vòng đời server và điều phối request đồng thời.
Không thay đổi model/DAO/service nghiệp vụ hiện có trên main.

## Chạy lớp mạng

Tại `auction-server`, cần JDK 25 và Maven:

```powershell
mvn -B package
java -jar target/auction-server-1.0.0.jar
```

Mặc định 127.0.0.1:8888. Đổi bằng `-Dauction.host=...` và `-Dauction.port=...`
trước `-jar`. PING hoạt động khi chưa có provider. Mọi thao tác nghiệp vụ trả
BACKEND_NOT_CONFIGURED: nhánh này không bổ sung dữ liệu, tài khoản mẫu, ví,
CRUD, auto-bid, anti-sniping hoặc thanh toán server của thành viên 2/4.

## Nối với phần thành viên 2

Cài interface `com.auction.server.network.BackendGateway`, đóng gói lớp provider
cùng các service/DAO của thành viên 2. Đăng ký tên lớp đầy đủ ở tài nguyên:
`META-INF/services/com.auction.server.network.BackendGateway`.

- execute(action, token, commandId, payload): xác thực/phân quyền, gọi service,
  trả success/result hoặc success=false/errorCode/message.
- authenticatedUserId(token): xác thực phiên, trả userId; không tin ID từ client.
- setEventSink(sink): lưu Observer callback; phát event sau commit thành công.
- tick(): điểm gọi scheduler của backend; mạng không tự quyết định ai thắng.
- close(): đóng tài nguyên do provider tạo.

ServerRequestDispatcher dùng một ReentrantLock công bằng để tuần tự hóa request,
kiểm tra phiên và tick qua gateway. Điều này tránh hai kết nối cùng ghi qua entry
point cùng lúc. Provider vẫn chịu trách nhiệm transaction/rollback, commandId,
quy tắc giá, số dư và quyền; lớp mạng không thể bảo đảm các điều đó thay service.

ServerRuntime.INSTANCE là Singleton quản lý start/stop. Mỗi socket có reader và
hàng đợi writer giới hạn riêng; không gửi socket trực tiếp trong khóa nghiệp vụ.
Các source Object streams từ main được giữ nguyên trong reference/.java.txt,
không giả lập các lớp shared/service chưa tồn tại để làm build xanh.

Hợp đồng và cách tích hợp ở [NETWORK_PROTOCOL.md](NETWORK_PROTOCOL.md).
Kiểm thử mạng riêng nằm trong network/TransportTest; các test nghiệp vụ sẵn có
của nhóm được giữ nguyên.
