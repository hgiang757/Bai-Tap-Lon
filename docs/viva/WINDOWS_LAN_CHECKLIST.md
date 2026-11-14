# Checklist hai máy Windows thật

Trạng thái ban đầu: **NOT TESTED**. Socket loopback trong JUnit không thay thế
thử hai máy. Điền ngày, người thử, revision, IP/port và kết quả từng hàng; không
đánh PASS theo kỳ vọng. Hướng dẫn lệnh đầy đủ: [MULTI_CLIENT_SETUP](../../MULTI_CLIENT_SETUP.md).

## Chuẩn bị

1. Nhóm tự review/commit revision muốn thử; ghi `git rev-parse HEAD` và mọi diff
   chưa commit nếu còn. Chạy Maven verify có bật GUI, giữ log và báo cáo lỗi.
2. Máy A chạy Server JDK 25, bind 0.0.0.0:8888; máy B chạy Client với IPv4 LAN của A.
   Có thể chạy Client thứ hai trên A. Cùng mạng; cho phép Java/port TCP trên profile
   mạng riêng theo quy định máy. 0.0.0.0 là địa chỉ bind, không nhập làm host Client.
3. Dùng thư mục QA riêng cố định qua `-Dauction.data=...`; đặt
   AUCTION_ADMIN_PASSWORD khi tạo lần đầu. Tạo Seller và hai Bidder qua GUI, nạp
   tiền giả lập. Không copy data production sang Client, không reset state.json.
4. Tạo phiên OPEN bắt đầu đúng buổi thử; một phiên ngắn để kết thúc/anti-sniping,
   một phiên dài để restart, một phiên tương lai để sửa/xóa. Tạo ảnh fixture không
   chứa dữ liệu cá nhân. Đồng hồ Server quyết định thời gian.

## Ghi kết quả thực tế

| Tình huống | Kết quả cần quan sát | Thực tế / bằng chứng |
|---|---|---|
| Kết nối B tới A | Đăng ký/login thật; hai Client thấy cùng danh sách | NOT TESTED |
| Seller tạo, sửa, xóa phiên tương lai | Tên/mô tả/ảnh/giá/lịch đúng; Client khác nhận cập nhật; chỉ đúng owner được sửa/xóa | NOT TESTED |
| Bid hợp lệ rồi bid quá thấp | Giá/leader/history/ví nhất quán; bid thấp bị từ chối, không thêm history | NOT TESTED |
| Hai bidder gửi cùng giá gần đồng thời | Một giá được nhận, một bị từ chối theo giá mới; không hai leader | NOT TESTED |
| Một ví bid hai phiên | Tổng hold không vượt số dư; rút phần đang hold bị từ chối | NOT TESTED |
| Auto-bid hai người | Không vượt max/tiền, increment đúng; trần bằng nhau ưu tiên đăng ký sớm | NOT TESTED |
| Bid trong 30 giây cuối | Deadline cũ tăng 60 giây ở cả hai máy; chart/history/countdown nhận push | NOT TESTED |
| Không bid khi hết giờ | FINISHED, không chuyển tiền, không winner | NOT TESTED |
| Có bid khi hết giờ | Một winner, PAID; buyer trừ, seller cộng đúng một lần; hold về đúng mức | NOT TESTED |
| Admin hủy phiên có bid | Có lý do, CANCELED, release hold, auto-bid dừng, không nhận bid mới | NOT TESTED |
| Ngắt Wi-Fi B khi đang gửi | UI báo trạng thái chưa rõ; kết nối/login lại và đối soát pending, không tự gửi bid thứ hai | NOT TESTED |
| Đóng một Client đột ngột | Client kia và Server tiếp tục hoạt động; vào/ra phòng không listener/timer trùng | NOT TESTED |
| Đóng Server sạch rồi restart cùng thư mục | Tài khoản/ảnh/phiên/bid/rules/ví/receipt còn; login lại; phiên hết hạn xử lý một lần | NOT TESTED |
| Mở Server thứ hai cùng data | Bị chặn file lock; dữ liệu Server đang chạy không bị reset | NOT TESTED |
| Menu tài khoản | Nút mở/đóng, Space chuẩn, chọn mục/logout đóng; ẩn owner/rời trang cleanup; không có cửa sổ nổi khi Alt+Tab | NOT TESTED |
| Seller 640 px và DPI của hai máy | Ngày/giờ/ảnh/Lưu/Hủy dùng được, lỗi giữ draft; tới được cột thao tác bảng | NOT TESTED |

Mất ACK cần đối chiếu history/commandId, không chỉ thông báo trên UI. Không sửa
JSON hoặc chép file .tmp lên state.json để tạo tình huống. Không thử rollback bằng
phá quyền/ổ đĩa của dữ liệu thật; các lỗi lưu đã có JUnit với thư mục cô lập.

## Kết thúc

Dừng Server trước khi sao lưu; lưu SHA-256 và bản sao dữ liệu QA cùng log/ảnh
fixture. Đăng nhập Seller kiểm ví và kết quả lần cuối. Ghi PASS/FAIL/NOT TESTED
riêng từng hàng. Không đưa snapshot, mật khẩu, pending command hoặc ảnh cá nhân
lên GitHub. Nếu có lỗi, giữ log đúng revision và tái hiện trước khi sửa.
