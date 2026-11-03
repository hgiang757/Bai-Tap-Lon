# Thiết kế giao diện client JavaFX

Ứng dụng dùng một theme sáng trong `src/main/resources/com/auction/client/css/app.css`. Java điều khiển hành vi; FXML định nghĩa bố cục; màu được gán qua looked-up colors. `styleClass` trong FXML là danh sách phân cách bằng dấu phẩy, ví dụ `styleClass="page-title, t-display"`.

## Bảng màu đang dùng

| Token | Giá trị | Vai trò |
|---|---|---|
| `-c-bg` | `#F7F6F2` | Nền trang |
| `-c-surface` | `#FFFFFF` | Card, bảng, form, dialog, tooltip |
| `-c-surface-2` | `#EEF4F2` | Hover, badge, vùng ảnh sản phẩm |
| `-c-text` | `#18332F` | Chữ chính |
| `-c-muted` | `#52645E` | Chữ phụ, gợi ý nhập liệu |
| `-c-primary` | `#0F766E` | Nút chính, liên kết, focus, trạng thái chọn |
| `-c-primary-hover` | `#115E59` | Hover nút chính |
| `-c-on-primary` | `#FFFFFF` | Chữ trên nút chính/ô chọn |
| `-c-border`, `-c-hairline` | `#D6DFDA` | Viền phân cách trang trí |
| `-c-border-input` | `#7A8B84` | Viền control cần nhận diện, thanh cuộn |
| `-c-accent` | `#946200` | Điểm nhấn đồng: biểu tượng thương hiệu, badge mô phỏng/đã thanh toán |
| `-c-success` | `#166534` | Thành công, đang diễn ra |
| `-c-warning` | `#92400E` | Cảnh báo |
| `-c-danger` | `#B91C1C` | Lỗi, nguy hiểm, đếm ngược khẩn cấp |

Không đặt mã màu trong controller/FXML. Bo góc nhẹ và bóng nhẹ; ảnh sản phẩm và SVG vẫn được giữ. Không thêm lớp theme hoặc lớp Java chỉ để bọc node.

## Tương phản

Chạy `python scripts/check-contrast.py` để tính độ sáng tuyến tính sRGB và tỷ lệ `(L_sáng + 0.05) / (L_tối + 0.05)` trực tiếp từ token CSS. Script đo 30 cặp đang dùng; quyết định đạt/ngưỡng dựa trên số chưa làm tròn.

| Cặp màu thực tế | Tỷ lệ |
|---|---|
| Chữ chính trên nền/card/bề mặt phụ | 12.140–13.519:1 |
| Chữ phụ trên ba nền sáng | 5.641–6.281:1 |
| Màu tương tác trên ba nền sáng | 4.915–5.473:1 |
| Màu đồng trên ba nền sáng (cặp chữ thấp nhất) | 4.707–5.242:1 |
| Chữ trắng trên nút chính / hover | 5.473 / 7.584:1 |
| Viền input trên ba nền sáng | 3.222–3.587:1 |
| Focus / dấu chọn xanh ngọc trên ba nền sáng | 4.915–5.473:1 |

Tất cả cặp chữ được đo đạt 4.5:1, kể cả tiêu đề lớn; input/focus đạt 3:1. Viền phân cách `-c-border` chỉ dùng để tổ chức nội dung, không thay viền input hoặc dấu focus. Trạng thái còn có chữ: Đang diễn ra, Sắp diễn ra, Đã thanh toán, Đã hủy, lỗi nhập liệu và trạng thái lệnh. Focus bàn phím dùng viền 2 px; hàng bảng và tab được chọn có đường viền xanh.

Tham khảo: [Carbon — color roles/tokens](https://v10.carbondesignsystem.com/guidelines/color/overview/) và [W3C — contrast minimum](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html). Phép đo palette không thay thế một cuộc đánh giá khả năng tiếp cận toàn ứng dụng.

## Bố cục và hành vi

- Nền trang là màu CSS; form xác thực căn giữa, cuộn được khi nhỏ hoặc đăng ký dài. Chữ thương hiệu, slogan và lợi ích dùng màu đọc được trên nền sáng.
- Trang chủ chia trực tiếp RUNNING vào Đang diễn ra, OPEN vào Sắp diễn ra sau tìm kiếm/danh mục. Khử trùng ID một lần khi tải. Mỗi nhóm có thông báo riêng cho đang tải, tải thất bại, chưa có phiên và không khớp bộ lọc. Hướng dẫn, CTA người bán và footer được giữ.
- Card chia đều chiều rộng theo số cột vừa vùng hiển thị, làm tròn xuống và chừa 1 px cho DPI Windows để tránh một cột bị đẩy xuống hàng. Chiều cao theo nội dung; tên dài xuống dòng; badge/countdown có thể xuống hàng; nút có khoảng đệm. Timer dùng chung; tác vụ ảnh cũ vẫn bị hủy khi lọc hoặc rời trang.
- Ví hiển thị Tổng số dư, Có thể sử dụng, Tiền tạm giữ cho đấu giá; tối đa ba cột, tự giảm số cột khi hẹp. Nhãn và số được xuống dòng, tooltip và accessible text chứa đúng toàn bộ số. Định dạng long không đổi sang double.
- Tiền tạm giữ là khoản của lượt dẫn đầu, không phải một khoản đặt cọc riêng. Mock giải phóng khi bị vượt giá hoặc hủy phiên. Khi xử lý kết thúc phiên, mock trừ khoản tạm giữ của người thắng khỏi tổng số dư và chuyển trạng thái PAID; giữ/giải phóng không đổi tổng số dư. Nhãn giao dịch dùng Tạm giữ cho đấu giá và Giải phóng tiền tạm giữ.
- Bộ lọc trang sản phẩm xuống hàng, bảng giữ chiều rộng cột và cho cuộn ngang trên cửa sổ nhỏ. Popup, dialog, tooltip, badge, bảng và chart kế thừa cùng palette.
- Khóa gửi giá, UNKNOWN, callback cũ, listener và pool tác vụ nền giữ nguyên. Tất cả dữ liệu nghiệp vụ vẫn là mock.

Ảnh hiện hành và báo cáo tại [UI_REFRESH.md](../UI_REFRESH.md). Ảnh trong `docs/ui/screenshots/`, yêu cầu cũ `PROMPT_FIX_UI_1.md` và tham khảo `DESIGN-apple.md` giữ nguyên lịch sử.
