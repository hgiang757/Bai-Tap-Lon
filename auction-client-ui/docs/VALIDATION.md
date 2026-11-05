# Kiểm tra client và tích hợp


## Kết quả chạy lại ngày 05/11/2026 — kết nối backend

- Build sạch toàn dự án bằng `mvn -B clean verify "-Dauction.ui.tests=true"` thành công.
- Backend: 59 test đạt; client: 95 test đạt (gồm 12 GUI test); tích hợp: 8 test đạt (gồm 1 kịch bản JavaFX thật đi qua nhiều màn hình). Tổng 162, không lỗi, không bỏ qua trong lần bật GUI.
- Tích hợp kiểm tra đăng ký/đăng nhập, ví/nạp/rút, sản phẩm/sửa/hủy/xóa, ảnh do client khác tải, auto-bid và push giữa hai adapter độc lập.
- Proxy cố ý làm mất response đặt giá: client reconnect/replay cùng commandId, chỉ ghi nhận 1 bid và 1 khoản giữ tiền. Pending command được giữ qua restart client; restart server yêu cầu login lại và giữ dữ liệu ví.
- JavaFX dùng backend thật: thao tác đăng nhập và đặt giá từ nút UI, nhận giá người khác, mở ví/Seller/Admin. Ảnh kiểm tra ở `auction-integration-tests/target/qa/server-*.png` sau khi bật GUI.
- Kiểm tra encoding: 0 match; bản đồ tài liệu: 94 file code/cấu hình và link hợp lệ; tương phản: 30 cặp đạt theo phạm vi script.
- Thử nghiệm chạy tại máy Windows hiện tại. CI Linux/Xvfb đã được cấu hình nhưng chưa được xác nhận bởi một lần chạy trên GitHub trong phiên này. Chưa đo throughput hoặc chứng nhận mọi DPI/OS.
- Mặc định ứng dụng chạy SERVER; MOCK vẫn có để kiểm tra độc lập. Ví dùng tiền giả lập, dữ liệu BTL quy mô nhỏ, không thanh toán tiền thật.

Các kết quả và giới hạn dưới đây là lịch sử trước khi tích hợp, không mô tả trạng thái hiện hành.

## Kết quả đã ghi nhận ngày 03/11/2026

Bảng dưới tóm tắt báo cáo có sẵn, không phải kết quả chạy lại khi rút gọn tài liệu ngày 04/11/2026.

| Kiểm tra | Kết quả ghi trong báo cáo trước |
|---|---|
| `mvn -B test "-Dauction.ui.tests=true"` | 95 test đạt, gồm 12 test JavaFX; không lỗi/bỏ qua |
| `mvn -B test` | 83 test đạt, 12 test UI bỏ qua theo cấu hình |
| `python scripts/check-encoding.py` | Không phát hiện lỗi trong phạm vi script |
| `python scripts/check-docs.py` | 88 file code/cấu hình có trong bản đồ; liên kết hợp lệ |
| `python scripts/check-contrast.py` | 30 cặp màu đạt ngưỡng của script |
| `SnapshotApp` tại 1200×800 và 640×800 | Mỗi kích thước tạo 11 ảnh mock |

Phạm vi đã ghi nhận: phân nhóm RUNNING/OPEN và lọc không trùng; trạng thái tải/lỗi; hiển thị số tiền dài; nạp FXML sau khi bỏ nền ảnh; khóa lệnh đặt giá; callback/listener và hủy tác vụ ảnh. Chi tiết đợt giao diện nằm trong [UI_REFRESH.md](UI_REFRESH.md).

## Chạy lại

Thực hiện các lệnh trong [README](../README.md). Test UI cần desktop; kiểm tra `mvn -version` nếu Maven dùng sai JDK. Nếu cache Maven mặc định không ghi được, chỉ định `-Dmaven.repo.local` tới cache có quyền ghi trên máy đang dùng.

## Giới hạn

- Chưa xác minh tích hợp backend thật hoặc broadcast giữa hai tiến trình.
- Chưa kiểm tra mọi DPI, hệ điều hành, định dạng ảnh và lỗi mạng; hủy tải ảnh là best effort.
- Script tương phản không chứng nhận toàn bộ ứng dụng đạt WCAG.
- Các kết quả cũ chỉ phản ánh bản được kiểm tra tại thời điểm đó.
