> Nhánh member1-member3: UI/mock đạt 95 test gồm 12 GUI; mạng đạt 4 test mới và 42 test backend sẵn có vẫn đạt. Bản này chỉ kiểm chứng mạng với gateway fixture trong test; chưa tích hợp nghiệp vụ thật của thành viên 2/4.

> Bản frontend-only chạy mock độc lập. Backend do thành viên backend tích hợp.

# Kết quả kiểm tra client

## Đợt giao diện ngày 03/11/2026 — kết quả mới nhất

Chỉ source trong module client được chỉnh. Java release 25, JavaFX 26.0.2 và dependency giữ nguyên. Maven dùng JDK 25.0.4.1. Giờ bên dưới theo Asia/Saigon. Các kết quả này đến từ lệnh chạy mới, không đọc lại XML cũ làm bằng chứng.

Cache Maven mặc định của sandbox trỏ tới `C:\.m2\repository` và không tạo được repository. Dùng cache đã có bằng tham số `"-Dmaven.repo.local=C:\Users\MAI DUC HIEU\.m2\repository"` và quyền chạy Maven được cho phép; không sửa cấu hình Maven/Java hoặc pom. Bảng dưới lược bỏ tham số đường dẫn máy này cho dễ đọc.

| Lệnh/kiểm tra trên bản cuối | Kết quả |
|---|---|
| `mvn -B test "-Dauction.ui.tests=true"` — 22:45:11 | **95 test đạt**, 0 failure/error/skip; gồm **12 bài JavaFX** với desktop thật. |
| `mvn -B test` — 22:45:18 | **83 test chạy đạt**, 12 bài UI skip theo cờ mặc định; BUILD SUCCESS. |
| `python scripts/check-encoding.py` | `Encoding scan: 0 matches`. |
| `python scripts/check-docs.py` | **88 file code/cấu hình** có trong CODE_MAP, liên kết cục bộ hợp lệ (gồm cả UI_REFRESH, DESIGN, IMAGE_CREDITS). |
| `python scripts/check-contrast.py` | **30 cặp**, 0 failure; chữ thường tối thiểu 4.707:1, viền input tối thiểu 3.222:1, focus xanh tối thiểu 4.915:1. |
| `mvn -B compile javafx:run "-Dmain.class=com.auction.client.dev.SnapshotApp"` — scene 1200×800, 22:43:20 | **11 ảnh mới**, BUILD SUCCESS, `SNAPSHOTS_SUCCESS (mock only)`. |
| Cùng lệnh SnapshotApp — scene 640×800, 22:43:30 | **11 ảnh mới**, BUILD SUCCESS, `SNAPSHOTS_SUCCESS (mock only)`. |

12 ảnh của đăng nhập/đăng ký, trang chủ, phòng đấu giá, ví với số tiền dài và quản lý sản phẩm đã được mở xem ở cả hai kích thước. Đã xem thêm ảnh đăng ký cuộn xuống cuối ở cửa sổ 640×640. Danh mục ảnh bền vững, file sửa/xóa, cách đo số dòng trước/sau và ghi chú vấn đáp ở [UI_REFRESH.md](UI_REFRESH.md).

### Các hồi quy được kiểm tra

- Tám RUNNING đều xuất hiện, hai OPEN chỉ thuộc Sắp diễn ra; FINISHED/PAID/CANCELED không lọt vào hai nhóm. Nguồn trả trùng ID không tạo card trùng. Kết hợp tìm kiếm/danh mục lặp lại vẫn đúng ID và số countdown.
- Chặn lượt đọc bằng latch để xác nhận Đang tải; lỗi vẫn là lỗi khi người dùng gõ tìm kiếm; Làm mới phục hồi. Phân biệt chưa có phiên với không khớp bộ lọc ở từng nhóm.
- Card tên dài/countdown/nút không bị cắt ở cửa sổ 640 px; lưới rộng chứa ba card một hàng khi có đủ dữ liệu. Chừa sai số 1 px cho pixel snapping ở DPI Windows.
- Ví có đủ ba nhãn mới; `9.123.456.789.012.345 ₫` giữ đúng chữ số ở 1200/640; tooltip và accessible text khớp. Unit test kiểm tra thêm Long.MAX_VALUE, không đổi tiền sang double để định dạng.
- Mọi route/FXML và UI kit nạp được sau khi bỏ wrapper/inject; ảnh trang trí và class bị xóa vắng trên classpath. Ảnh sản phẩm, import/tải ảnh, icon SVG và công cụ ImageIO/Swing vẫn có.
- Giữ các test cũ về khóa lệnh giá, không gửi lại UNKNOWN, lượt đọc/callback cũ, dispose/listener và pool/hủy ảnh.

### Lần chạy trung gian và giới hạn

- Một lần biên dịch đầu thiếu biến hiển thị ví đã được sửa. Một assertion thông báo auto-bid còn thuật ngữ cũ đã được cập nhật theo thông báo mới; không đổi điều kiện nghiệp vụ để làm test đạt.
- Có lần Maven tái sử dụng class do IDE tạo với `Unresolved compilation problems`; đã xóa riêng bytecode sinh tự động trong `target/classes`, `target/test-classes` rồi biên dịch javac lại. Không sửa cấu hình IDE ngoài module. Các lần chốt trong bảng đều đạt.
- JavaFX có cảnh báo native access/deprecated API đang có; không có lỗi phân tích CSS/FXML trong log chốt. Test ảnh thiếu cố ý tạo cảnh báo ProductImages, không phải ảnh nền bị lỗi.
- Backend thật/broadcast hai tiến trình chưa kiểm tra vì ứng dụng vẫn dùng mock. Chưa kiểm tra toàn bộ định dạng ảnh/lỗi DNS/TLS/tải lớn, mọi DPI/độ phân giải, mọi luồng trợ năng hoặc thời gian hiển thị tooltip trên mọi hệ điều hành. Phép đo palette không phải chứng nhận WCAG toàn bộ ứng dụng.
- Workspace không có Git để xem diff. Báo cáo dùng bản text trước khi sửa và `difflib`, phân biệt 369 dòng khối nền/carousel bị loại với số dòng xóa/thêm và thay đổi ròng của bảy file liên quan.

## Báo cáo lưu trước đợt giao diện — giữ nguyên để đối chiếu


## Đợt sửa tiếp ngày 03/11/2026

Đây là các kết quả chạy mới cho đợt sửa khóa lệnh/ảnh và README, không lấy lại kết quả từ các XML báo cáo cũ. Kết quả chốt nằm trong bảng dưới đây; các lần chạy trung gian được ghi sau bảng để đối chiếu.

| Lệnh/kiểm tra mới trên bản cuối | Kết quả |
|---|---|
| `mvn -B test "-Dauction.ui.tests=true"` — 00:36:50, 03/11/2026 (Asia/Saigon) | **89 test đạt**, 0 failure/error/skip, gồm **8 test JavaFX**. |
| `mvn -B test` — 00:39:18 | BUILD SUCCESS; **81 test chạy đạt**, 8 test desktop skip theo cờ mặc định. |
| `python scripts/check-encoding.py` | `Encoding scan: 0 matches`, có quét README mới. |
| `python scripts/check-docs.py` | **86 file code/cấu hình** có trong CODE_MAP; liên kết cục bộ và fenced block hợp lệ. |
| `mvn javafx:run "-Dmain.class=com.auction.client.dev.SnapshotApp"` — 00:37:39 | BUILD SUCCESS; tạo **10 ảnh mới**, `SNAPSHOTS_SUCCESS (mock only)`. |

Không nâng Java/dependency hoặc thay cấu hình toàn máy. Mọi bài hồi quy cũ được giữ nguyên assertion. Các bài JavaFX mới còn kiểm tra: từ chối lệnh ngay tại executor không gọi placeBid và phục hồi nút; auto-bid mock sai cấu hình được sửa và bật/tắt lại bình thường.

- Lần đầu chạy `mvn -B test` trong sandbox bị chặn trước kiểm thử: `Could not create local repository at C:\.m2\repository` (`LocalRepositoryNotAccessibleException`). Chạy lại với quyền dùng cache Maven đã thực thi được kiểm thử; không đổi cấu hình Java/Maven toàn máy.
- Lần chạy mới `mvn -B test "-Dauction.ui.tests=true"` lúc 00:23:28 (Asia/Saigon): **87 test đạt**, 0 lỗi/skip, gồm sáu bài UI. Sau mốc này còn bổ sung kiểm tra vòng UI auto-bid và tài liệu, nên dùng bảng kết quả chốt ở trên để đánh giá bản cuối.
- Ca lỗi dùng CountDownLatch/CompletableFuture/FX pulse giữ đúng thời điểm gửi và đọc, không chỉ dùng sleep để hy vọng tạo race. Snapshot cũ không mở khóa; timeout/đọc lỗi vẫn khóa; chuyển phòng không quên lệnh đang chạy. Không sửa assertion hồi quy cũ.
- Ca tác vụ ảnh: giữ ảnh đang chạy rồi chứng minh lệnh nghiệp vụ vẫn thực thi; hàng đợi đầy báo chưa nhận; đóng scope bỏ ảnh chờ và interrupt ảnh chạy đúng phạm vi; kết quả ô cũ không được gắn, lỗi ảnh không chặn ảnh tiếp theo. HTTP loopback giữ body để xác nhận read timeout.

### Giới hạn đợt này

- Không có kiểm thử backend thật/tra trạng thái lệnh hoặc broadcast hai tiến trình: server chưa có API đó. Mock không được dùng để giả lập xác nhận một lệnh timeout đã thất bại.
- Interrupt tải ảnh là best effort, không phải cam kết decoder JavaFX dừng tức thì. Đã kiểm thử timeout đọc HTTP và hủy scope bằng fake/gate; chưa stress mọi định dạng ảnh lớn hoặc toàn bộ tình huống DNS/TLS/redirect.
- Sơ đồ Mermaid được đối chiếu thủ công với source và kiểm tra fenced block/link; chưa chạy trình render GitHub thực tế. Script `check-docs.py` không phải trình kiểm chứng cú pháp Mermaid.
- Workspace không có metadata Git; chưa kiểm tra diff Git. Chỉ sửa trong module client, không commit/push/reset.

## Kết quả đợt trước — chỉ giữ làm lịch sử

Các số 62/73 test và kiểm tra khởi chạy bên dưới thuộc đợt trước; không phải kết quả chạy lại cho đợt sửa hiện tại.

Môi trường: Windows, Maven 3.10.0; Maven dùng JDK 25.0.4.1. `pom.xml` giữ release 25, JavaFX 26.0.2 và các dependency ban đầu. `java` trên PATH có thể khác JDK Maven dùng; kiểm tra bằng `mvn -version`.

## Đã kiểm tra

- Bộ test ban đầu: 62 test đạt.
- `mvn -B test`: BUILD SUCCESS; 70 test chạy đạt, ba test giao diện bỏ qua theo cấu hình mặc định.
- `mvn -B clean test "-Dauction.ui.tests=true"`: 73 test đạt, 0 failure/error/skip, gồm ba bài kiểm tra JavaFX có desktop thật.
- Kiểm thử giao diện: đăng ký rồi đăng nhập lại, tìm kiếm có/không có kết quả, quay lại đúng màn hình/phiên, gửi giá liên tiếp chỉ tạo một bid khi còn đang chờ, số dòng lịch sử và điểm biểu đồ khớp service.
- Vào–ra phòng đấu giá ba lần: observer của phiên là 1 khi mở và 0 khi rời, timer phòng dừng; Home cũng dừng timer khi rời. Callback giả lập đến muộn sau dispose bị bỏ qua.
- Sản phẩm: mở form và thêm, sửa, xác nhận xóa qua giao diện; dữ liệu dịch vụ và bảng được đối chiếu sau mỗi thao tác. Các trang ví, quản trị, hướng dẫn, liên hệ và UI kit nạp được.
- Ảnh `target/qa/home.png`, `room-wide.png`, `room-narrow.png`, `products.png`, `wallet.png`, `admin.png` được tạo từ kiểm thử. Đã xem ảnh Home và phòng đấu giá ở cửa sổ rộng/hẹp; nút thanh đầu trang ở 640 px được sửa để không bị rút thành dấu ba chấm.
- Test bổ sung kiểm tra callback đảo thứ tự, callback đã xếp hàng khi đóng màn hình, không hủy/gửi lại mutation, chuyển lỗi về UI, từ chối mode server hoặc bộ dịch vụ trộn mock/adapter, và đồng bộ tài khoản mock với quản trị.
- `mvn javafx:run`: đã khởi chạy cửa sổ `Digital Auction [MOCK] - Đăng nhập / Đăng ký`, kết thúc với BUILD SUCCESS. Công cụ computer-use nhận diện được cửa sổ nhưng lần chụp bị lỗi `foreground window did not report a process id`; kiểm tra hình ảnh dựa trên ảnh tạo bằng JavaFX trong bộ test.
- `mvn javafx:run "-Dmain.class=com.auction.client.dev.SnapshotApp"`: BUILD SUCCESS, tạo đủ 10 ảnh trong `target/snapshots/` và in `SNAPSHOTS_SUCCESS (mock only)` sau khi chờ dữ liệu nền.
- `python scripts/check-encoding.py`: không phát hiện lỗi sau khi sửa tiếng Việt. Phạm vi quét gồm cả source, test và docs.

Các cảnh báo native access của JavaFX và deprecated API TableView đang có không làm kiểm thử thất bại. Bài kiểm tra đường dẫn ảnh không tồn tại cố ý tạo log cảnh báo; đó là ca lỗi được kiểm tra.

## Giới hạn

- Chưa kiểm tra nghiệp vụ với server thật hoặc hai tiến trình client nhận cùng broadcast: backend chỉ echo chuỗi, chưa có handler nghiệp vụ/broadcast để kiểm tra. Không có kết quả tích hợp thật được tuyên bố.
- Chưa kiểm tra ma trận đầy đủ mọi độ phân giải, mọi loại ảnh/lỗi mạng hay tải lớn. Kiểm thử tự động đã kiểm tra luồng chính và ảnh mẫu, không phải chứng nhận mọi tình huống giao diện.
- Các bài UI cần desktop; khi chạy `mvn -B test` thông thường, ba bài này được skip có chủ đích. Lệnh bật `auction.ui.tests` mới thực thi chúng.
- Workspace không có `.git`, nên chưa kiểm tra diff Git. Không commit, push hoặc sửa file ngoài module client.
