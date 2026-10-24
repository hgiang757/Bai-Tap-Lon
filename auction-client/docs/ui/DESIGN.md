# VaultAuction Client UI - Design System

Giao diện dùng chung FXML và stylesheet `src/main/resources/com/hieu/css/base.css`, cùng theme sáng/tối để đổi bộ màu mà không thay đổi controller.

## Màu sắc

| Token | Sáng | Tối | Công dụng |
|---|---|---|---|
| `-c-bg` | `#F5F5F7` | `#11131E` | Nền ứng dụng |
| `-c-nav` | `#FFFFFF` | `#151827` | Header/navigation |
| `-c-surface` | `#FFFFFF` | `#1B1E2D` | Card, input, bảng |
| `-c-surface-2` | `#EEEDF5` | `#24283A` | Bề mặt phụ |
| `-c-border` | `#E0DEEC` | `#30354B` | Viền |
| `-c-primary` | `#6256DD` | `#8B80F9` | Nút và thành phần nhấn |
| `-c-panel` | `#AAA1FF` | `#9990FA` | Nhấn nhẹ/hover |
| `-c-success` | `#15803D` | `#4ADE80` | Thành công/đang diễn ra |
| `-c-warning` | `#B45309` | `#FBBF24` | Cảnh báo |
| `-c-danger` | `#B91C1C` | `#F87171` | Lỗi/đếm ngược |

Màu được khai báo bằng looked-up color trong theme CSS; không đặt màu inline trong controller. Card bo góc lớn, button pill, chữ dùng Segoe UI để hỗ trợ tiếng Việt.

## Thành phần và tương tác

- Định nghĩa button, badge/chip, form field, alert, toast, skeleton, card, bảng và biểu đồ trong `base.css`.
- `ThemeManager` đổi theme ở runtime; theme sáng là mặc định lần đầu chạy.
- `AuctionClientService` là cổng giao tiếp giữa MVC controller và backend/mock.
- Cập nhật bid từ listener phải được điều phối trên JavaFX Application Thread.
