# Bàn giao chỉnh giao diện — 03/11/2026

Phạm vi source chỉ trong `auction-client-ui/`. Không có `AGENTS.md` hoặc metadata `.git` trong workspace. Không commit/push/reset, không sửa backend hoặc Java/dependency. Nghiệp vụ vẫn chạy mock trong bộ nhớ.

## File sửa/xóa và lý do

| File trong module | Thay đổi |
|---|---|
| `src/main/java/com/auction/client/controller/HomeController.java` | Bỏ chọn/loại phiên ở khu vực riêng; khử trùng ID khi nạp, lọc rồi chia RUNNING/OPEN; thông báo riêng cho từng nhóm; card theo chiều rộng thật, giữ timer chung và scope ảnh. |
| `src/main/resources/com/auction/client/views/home-view.fxml` | Bỏ khối cũ và điều khiển trái/phải; giữ tìm kiếm/danh mục/Làm mới/lịch/hướng dẫn/CTA/footer; thêm nhãn trống cho OPEN. |
| `src/main/java/com/auction/client/controller/AuthController.java`, `src/main/resources/com/auction/client/views/auth-view.fxml` | Bỏ cache, WeakReference, listener/clip/cover/fallback chỉ phục vụ trang trí; giữ xác thực nền, khóa submit, quản lý mật khẩu, Preferences và form cuộn. |
| `src/main/java/com/auction/client/navigation/ViewNavigator.java` | Gắn trực tiếp root FXML vào scene, giữ dispose/lịch sử/phục hồi phiên/stylesheet và focus. |
| **Xóa** `src/main/java/com/auction/client/ui/AppBackground.java` | Chức năng duy nhất là trang trí; không còn người gọi. |
| **Xóa** `src/main/resources/com/auction/client/images/app-bg.jpg`, `login-bg.jpg` | Đã xác minh không còn tham chiếu runtime. Bản build sinh tự động cũ cũng được dọn riêng. |
| `src/main/java/com/auction/client/controller/WalletController.java`, `src/main/resources/com/auction/client/views/wallet-view.fxml` | Nhãn mới, giải thích giữ/giải phóng/thanh toán, thẻ thống kê co giãn, nhãn/số xuống dòng, tooltip/accessibility chứa đầy đủ giá trị; có trạng thái sẵn sàng cho SnapshotApp. |
| `src/main/java/com/auction/client/model/TransactionType.java` | Đổi nhãn hiển thị BID_HOLD/BID_RELEASE; giữ nguyên tên enum/API. |
| `src/main/java/com/auction/client/util/FormatUtil.java` | Overload `formatCurrency(long)` để không mất chữ số ví khi vượt 2^53; giữ overload double cho giá phiên. Không đổi công thức/kiểu tiền hoặc dữ liệu gốc. |
| `src/main/java/com/auction/client/controller/AuctionRoomController.java`, `GuideController.java` | Thống nhất câu chữ về số tiền có thể sử dụng và giải thích ví đúng mock. Không sửa cơ chế đặt giá/listener/UNKNOWN. |
| `src/main/java/com/auction/client/service/mock/MockAuctionClientService.java`, `MockAutoBidService.java`, `MockWalletService.java` | Chỉ đổi câu thông báo lỗi số tiền có thể sử dụng; điều kiện, công thức và nghiệp vụ giữ nguyên. |
| `src/main/resources/com/auction/client/css/app.css` | Token sáng trắng ngà/xanh ngọc/đồng; loại selector không dùng, gộp selector trùng; đồng bộ popup/dialog/tooltip/badge/table/chart/focus/selection. |
| `src/main/resources/com/auction/client/views/seller-products-view.fxml` | Bộ lọc FlowPane xuống hàng; bảng giữ cột và cuộn ngang ở cửa sổ nhỏ. |
| `src/main/resources/com/auction/client/views/auction-room-view.fxml`, `auth-view.fxml`, `contact-view.fxml`, `guide-view.fxml`, `home-view.fxml`, `wallet-view.fxml`, `src/main/resources/com/auction/client/dev/uikit-view.fxml` | Sửa danh sách `styleClass` thành phân cách bằng dấu phẩy để các lớp typography thực sự được áp dụng. |
| `src/main/java/com/auction/client/dev/SnapshotApp.java` | Chọn chiều rộng bằng biến môi trường/property; chờ dữ liệu và FX pulse; thêm ảnh ví với số tiền dài, vẫn giữ ImageIO/SwingFXUtils. |
| `src/test/java/com/auction/client/controller/ClientUiTest.java` | Thêm 4 bài JavaFX cho đủ RUNNING/OPEN, trùng ID/lọc, trạng thái tải/lỗi/rỗng, ví dài và form xác thực nhỏ. Giữ 8 bài hồi quy về thao tác nền/khóa giá/vòng đời. |
| `src/test/java/com/auction/client/service/MockAutoBidServiceTest.java`, `MockWalletServiceTest.java` | Cập nhật assertion thông báo theo thuật ngữ mới. |
| **Mới** `src/test/java/com/auction/client/util/FormatUtilTest.java` | Giữ đủ chữ số số tiền long, kể cả Long.MAX_VALUE. |
| **Mới** `src/test/java/com/auction/client/ui/UiResourcesTest.java` | Tài nguyên trang trí không còn trên classpath; giữ ảnh sản phẩm và resource ứng dụng. |
| `scripts/check-docs.py` | Quét thêm liên kết tài liệu thiết kế, bàn giao UI mới và nguồn hình ảnh. |
| **Mới** `scripts/check-contrast.py` | Đo 30 cặp màu từ CSS; ngưỡng chữ 4.5:1, viền/focus 3:1. |
| `README.md`, `docs/CODE_MAP.md`, `docs/CLIENT_HANDOVER.md`, `docs/IMAGE_CREDITS.md`, `docs/ui/DESIGN.md`, `docs/VALIDATION.md`, **mới** `docs/UI_REFRESH.md` | Cấu trúc file, màu/thuật ngữ, báo cáo kiểm thử mới và bàn giao. Tài liệu/ảnh lịch sử giữ nguyên. |

Ảnh sản phẩm, chọn/import/tải ảnh, `ProductImages`, `RoundedClip`, icon SVG, `SnapshotApp`, ImageIO, `java.desktop`, `javafx.swing` được giữ. `ViewTasks`, `BidSubmissionTracker`, `ServiceRegistry` và thuật toán giữ/hoàn/thanh toán không bị thay đổi.

## Đo số dòng trước/sau

Không có Git để tính diff. Đã lưu nội dung UTF-8 **trước khi sửa** ở `target/ui-refresh/before.json`; các khối gỡ được ghi trong `target/ui-refresh/removed-blocks.json`. Đếm `splitlines()` bao gồm dòng trống/comment/import; không đếm ảnh nhị phân thành dòng. So sánh file dùng `difflib.SequenceMatcher(autojunk=False)`, không chỉ dùng chênh lệch tổng số dòng.

- Phần nền ảnh: **258 dòng khối gốc → 0 dòng** (class, cache, cover/clip/resize/fallback, wrapper, import/inject/FXML và quy tắc riêng).
- Phần danh sách nổi bật/carousel: **111 dòng khối gốc → 0 dòng** (FXML, state, chọn/loại phiên, animation, listener, handler, import và quy tắc riêng).
- Tổng khối cũ đã bỏ: **369 dòng**. Các chỉnh sửa selector dùng chung và code thay thế để chia nhóm/co giãn được tính riêng trong diff file dưới đây.

Đối chiếu đầy đủ bảy file chứa hai phần này; số dòng xóa trong diff còn bao gồm thay thế định dạng CSS và bố cục, nên **không được gọi tất cả số dòng xóa là code trang trí**:

| File | Trước | Sau | Dòng xóa | Dòng thêm | Thay đổi ròng |
|---|---:|---:|---:|---:|---:|
| `AuthController.java` | 429 | 327 | 105 | 3 | -102 |
| `HomeController.java` | 591 | 498 | 147 | 54 | -93 |
| `ViewNavigator.java` | 235 | 226 | 13 | 4 | -9 |
| `AppBackground.java` | 138 | 0 | 138 | 0 | -138 |
| `app.css` | 429 | 245 | 302 | 118 | -184 |
| `auth-view.fxml` | 105 | 101 | 7 | 3 | -4 |
| `home-view.fxml` | 102 | 86 | 26 | 10 | -16 |
| **Tổng bảy file** | **2029** | **1483** | **738** | **192** | **-546** |

Đây là thay đổi ròng **của bảy file trên**, không phải toàn module: test và tài liệu mới thêm dòng để giải thích/kiểm tra hành vi.

## Ảnh mới đã xem

SnapshotApp tạo 11 ảnh mỗi kích thước, scene **1200×800** và **640×800**. 12 ảnh bắt buộc dưới đây đã được mở xem; bản lưu trong tài liệu được sao chép từ lần chụp mới. Ảnh tham khảo cũ trong `docs/ui/screenshots/` được giữ nguyên.

| Màn hình | Rộng | Hẹp 640 px |
|---|---|---|
| Đăng nhập | [Ảnh rộng](ui/refresh-2026-11-03/wide/01_auth_login.png) | [Ảnh hẹp](ui/refresh-2026-11-03/narrow/01_auth_login.png) |
| Đăng ký | [Ảnh rộng](ui/refresh-2026-11-03/wide/02_auth_register.png) | [Ảnh hẹp](ui/refresh-2026-11-03/narrow/02_auth_register.png) |
| Trang chủ | [Ảnh rộng](ui/refresh-2026-11-03/wide/03_dashboard.png) | [Ảnh hẹp](ui/refresh-2026-11-03/narrow/03_dashboard.png) |
| Phòng đấu giá | [Ảnh rộng](ui/refresh-2026-11-03/wide/04_realtime_bidding_running.png) | [Ảnh hẹp](ui/refresh-2026-11-03/narrow/04_realtime_bidding_running.png) |
| Ví số tiền dài | [Ảnh rộng](ui/refresh-2026-11-03/wide/11_wallet_long_amount.png) | [Ảnh hẹp](ui/refresh-2026-11-03/narrow/11_wallet_long_amount.png) |
| Quản lý sản phẩm | [Ảnh rộng](ui/refresh-2026-11-03/wide/06_product_management.png) | [Ảnh hẹp](ui/refresh-2026-11-03/narrow/06_product_management.png) |

[Đăng ký cuộn tới cuối ở 640 px](ui/refresh-2026-11-03/auth-register-scrolled-640.png) cho thấy đủ nút tạo tài khoản/chuyển chế độ/hướng dẫn. [Kiểm tra tám phiên RUNNING với tên dài](ui/refresh-2026-11-03/home-eight-running-narrow.png) đi cùng assertion ID/countdown, chiều rộng card và chiều cao tên/nút.

Ví hiển thị đúng `9.123.456.789.012.345 ₫` trong cả hai ảnh, không thành dấu ba chấm; tooltip/accessibility giữ toàn bộ số. Phòng đấu giá xuống một cột khi hẹp. Trang sản phẩm xuống hàng các bộ lọc và cuộn ngang bảng để xem đủ cột/thao tác. Đăng ký dài cuộn theo chiều dọc.

## Kiểm thử và giới hạn

Kết quả lệnh mới nhất, giờ chạy và các lỗi môi trường trung gian được ghi tại [VALIDATION.md](VALIDATION.md). Các bài JavaFX kiểm tra toàn bộ route/FXML và UI kit bằng toolkit thật; giữ kiểm tra khóa gửi, UNKNOWN, callback cũ và dispose/listener. Palette đo được 30 cặp, không cặp nào dưới ngưỡng. Xem [DESIGN.md](ui/DESIGN.md) cho tỷ lệ và nguồn tham khảo.

Chưa kiểm tra backend thật, broadcast hai tiến trình, toàn bộ định dạng ảnh/lỗi mạng/tải lớn, toàn bộ DPI/độ phân giải hoặc mọi thao tác bằng trình đọc màn hình. Kiểm tra tooltip so khớp text binding và giá trị, không tự động kiểm thử thời gian hover trên mọi hệ điều hành. Không có tuyên bố chứng nhận WCAG toàn ứng dụng.

## Gợi ý vấn đáp

1. **Chia nhóm phiên:** tải một danh sách qua service trên luồng nền, khử trùng theo ID, áp dụng tìm kiếm/danh mục, sau đó RUNNING vào Đang diễn ra và OPEN vào Sắp diễn ra. Không trích riêng một nhóm làm mất phiên RUNNING. Một timer cập nhật toàn bộ countdown; khi lọc/rời trang, hủy công việc ảnh cũ và chặn callback lỗi thời.
2. **Tiền tạm giữ:** tiền tương ứng giá đang dẫn đầu. Tổng số dư chưa bị trừ; số tiền có thể sử dụng bằng tổng số dư trừ khoản đang tạm giữ. Bị vượt giá hoặc hủy thì giải phóng. Khi xử lý kết thúc phiên, mock trừ khoản giữ của người thắng để thanh toán và chuyển PAID. Đây không phải cơ chế đặt cọc riêng.
3. **Nền màu CSS:** chỉ cần token nền trên root. Không cần class bọc, cache ảnh, WeakReference, listener resize, clip, cover hay fallback; Java tập trung xác thực, điều hướng và nghiệp vụ. Ảnh sản phẩm vẫn có luồng tải riêng vì đó là nội dung ứng dụng.
