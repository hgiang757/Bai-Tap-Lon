# Digital Auction Client UI - Design System

Giao diện dùng chung FXML và stylesheet `src/main/resources/com/auction/client/css/app.css` với một bảng màu sáng thống nhất.

## Bảng màu

| Token | Giá trị | Công dụng |
|---|---|---|
| `-c-bg` | `#F5F5F7` | Nền ứng dụng |
| `-c-surface` | `#FFFFFF` | Card, input, bảng |
| `-c-surface-2` | `#F5F5F7` | Bề mặt phụ |
| `-c-text` | `#1D1D1F` | Nội dung chính |
| `-c-muted` | `#6E6E73` | Nội dung phụ |
| `-c-primary` | `#0066CC` | Nút và thành phần nhấn |
| `-c-primary-hover` | `#004FA3` | Trạng thái hover |
| `-c-on-primary` | `#FFFFFF` | Nội dung trên màu nhấn |
| `-c-border` | `#D2D2D7` | Viền |
| `-c-border-input` | `#86868B` | Viền trường nhập |
| `-c-success` | `#1D7F3A` | Thành công/đang diễn ra |
| `-c-warning` | `#B64400` | Cảnh báo |
| `-c-danger` | `#D70015` | Lỗi/đếm ngược |

Màu được khai báo bằng looked-up color trong `app.css`; không đặt màu inline trong controller. Card bo góc lớn, button pill, chữ dùng Segoe UI để hỗ trợ tiếng Việt.

Màn Đăng nhập/Đăng ký dùng ảnh nền phủ kín cửa sổ với lớp phủ tối và thẻ form sáng. Ảnh nền không bị làm mờ; thông tin nguồn được quản lý tại [IMAGE_CREDITS.md](../IMAGE_CREDITS.md).

## Thành phần và tương tác

- Định nghĩa button, badge/chip, form field, alert, toast, skeleton, card, bảng và biểu đồ trong `app.css`.
- `AuctionClientService` là cổng giao tiếp giữa MVC controller và backend/mock.
- Cập nhật bid từ listener phải được điều phối trên JavaFX Application Thread.
