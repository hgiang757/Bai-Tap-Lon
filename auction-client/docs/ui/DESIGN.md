# Tài Liệu Thiết Kế Giao Diện (Design System) — Auction Client

Hệ thống giao diện được thiết kế theo phong cách tối giản, trang nhã (Warm Paper Light Theme), lấy cảm hứng từ các nhà đấu giá cổ điển và sàn giao dịch uy tín.

---

## 1. Bảng Màu Chuẩn (Color Tokens)

Toàn bộ biến màu được định nghĩa tại selector `.root` trong tệp `src/main/resources/com/hieu/css/style.css`:

| Tên Token CSS | Mã Màu Hex | Mô Tả & Mục Đích Sử Dụng |
| :--- | :--- | :--- |
| `-color-bg-canvas` | `#F6F3EE` | Màu nền tổng thể (màu giấy ấm tự nhiên, dịu mắt) |
| `-color-surface` | `#FFFFFF` | Nền các khối bề mặt chính, ô nhập liệu, danh sách |
| `-color-surface-subtle` | `#F0ECE4` | Nền thanh tiêu đề bảng, trạng thái hover nhẹ |
| `-color-border` | `#E6E1D8` | Đường viền mảnh ngăn cách các phần tử |
| `-color-border-focus` | `#7A1F3D` | Viền nổi bật khi trường nhập liệu được chọn |
| `-color-text-primary` | `#1F2430` | Màu chữ chính (độ tương phản cao, dễ đọc) |
| `-color-text-secondary` | `#6B6F7B` | Màu chữ phụ, nhãn hướng dẫn, chú thích |
| `-color-text-muted` | `#8E93A0` | Màu chữ gợi ý mờ (placeholder), trạng thái vô hiệu |
| `-color-accent-wine` | `#7A1F3D` | **Màu nhấn chính (Đỏ rượu vang)** cho nút bấm chính, giá tiền nổi bật |
| `-color-accent-wine-hover` | `#631730` | Màu đỏ rượu vang đậm khi rê chuột |
| `-color-success` | `#1E7B4F` | Trạng thái thành công, nhãn "Đang diễn ra" |
| `-color-warning` | `#B45309` | Trạng thái cảnh báo, huy hiệu chế độ thử nghiệm |
| `-color-error` | `#B3261E` | Trạng thái lỗi, thông báo từ chối đặt giá |

> **Nguyên tắc**: Tuyệt đối không dùng dải màu biến thiên (Gradient) hoặc các màu cyan rực rỡ. Toàn bộ nút và bề mặt dùng màu phẳng (Solid Color).

---

## 2. Kiểu Chữ (Typography)

- **Phông chữ giao diện chung**: `Segoe UI`, `-apple-system`, `Roboto`, `sans-serif` (đảm bảo tính sắc nét, cân đối trên mọi hệ điều hành).
- **Phông chữ tên sản phẩm**: `Georgia`, `serif` (mang nét cổ điển, sang trọng, định danh sản phẩm cao cấp).
- **Con số giá**: Cỡ chữ lớn (`32px`), định dạng đậm (`bold`), màu đỏ rượu vang (`#7A1F3D`), luôn căn thẳng hàng và có định dạng tiền chuẩn Việt Nam (`185.000.000 ₫`).

---

## 3. Hệ Thống Khoảng Cách & Bo Góc (Spacing & Radii)

Quy chuẩn phân bậc khoảng cách lũy tiến:
- **4px (`-space-xs`)**: Khoảng cách giữa nhãn phụ và dữ liệu.
- **8px (`-space-sm`)**: Khoảng cách giữa các nút tăng giá nhanh, padding ô nhập liệu.
- **12px (`-space-md`)**: Khoảng cách giữa các dòng form, thanh công cụ.
- **16px (`-space-lg`)**: Padding bên trong khối thẻ, khoảng cách giữa các phần tử chính.
- **20px - 24px (`-space-xl`)**: Khoảng cách lề nội dung chính.
- **32px (`-space-xxl`)**: Khoảng cách phân đoạn lớn.

Bo góc phần tử:
- **8px (`-radius-sm`)**: Nút bấm, ô nhập văn bản, ô chọn (ComboBox).
- **10px - 12px (`-radius-md`, `-radius-lg`)**: Các khung thẻ (Card panel).
- **Bóng đổ**: Rất nhẹ (`rgba(0, 0, 0, 0.03)` bán kính 8px) hoặc không dùng bóng đổ để giữ nét phẳng trang nhã.

---

## 4. Thứ Bậc Thị Giác (Visual Hierarchy)

1. **Màn hình Đấu giá trực tiếp (Realtime Bidding)**:
   - Thành phần nổi bật số 1: **Khối Mức giá hiện tại** (chữ số to 32px màu đỏ vang, viền trái đỏ vang 4px) cùng thời gian kết thúc.
   - Thành phần thứ 2: **Biểu đồ biến động giá (LineChart)** với trục Y tự động co giãn (`autoRanging=true`, `forceZeroInRange=false`).
   - Các trường phụ: Thông tin người bán, mô tả được xếp gọn bên trên, ngăn cách bằng đường kẻ mảnh thay vì đóng khung hộp.
2. **Khóa chức năng khi hết phiên**:
   - Tự động vô hiệu hóa toàn bộ ô nhập giá, nút đặt giá và các nút tăng nhanh (+1M, +5M, +10M) khi phiên kết thúc hoặc chưa bắt đầu, đồng thời hiển thị thông báo rõ ràng về trạng thái.
3. **Chế độ Dev**:
   - Huy hiệu `CHẾ ĐỘ DEV — DỮ LIỆU GIẢ` và nút mô phỏng chỉ hiện thị khi chạy với `MockAuctionClientService`.
