# PROMPT SỬA GIAO DIỆN ĐỢT 1 — icon lỗi, cuộn chậm, bố cục trang chủ

Dán vào Copilot Agent (chế độ Interactive, Manual permissions) ở thư mục `D:\repo-nhom` (hoặc thư mục dự án đang dùng). Dán MỘT khối, đợi xong, chạy `mvn clean javafx:run`, rồi gửi ảnh chụp cho Claude.

````
Bạn là kỹ sư JavaFX. Dự án: auction-client (JavaFX + FXML + CSS). Đã có app.css. KHÔNG sửa interface AuctionClientService, models, MockAuctionClientService. Thêm file mới khi có thể; chỉ sửa file cũ khi cần thiết và nói rõ bạn sửa gì.

QUY TẮC CHUNG: màu CHỈ dùng looked-up colors -c-* trong app.css (không thêm hex). Không thêm thư viện. Mọi chuỗi tiếng Việt có dấu, UTF-8. Dùng bảng màu sáng thống nhất.

Triệu chứng tôi thấy khi chạy màn Trang chủ (dashboard):
(1) Ở khối "Cách hoạt động", cạnh số 1, 2, 3 hiện ký tự ô vuông "▯▯" (glyph thiếu).
(2) Lăn chuột rất chậm: mỗi nấc chỉ nhích vài pixel, phải lăn rất lâu mới xuống cuối trang dù trang không dài.
(3) Bố cục xấu: chữ phụ xám nhạt trên khối màu nhấn khó đọc; thẻ "Phiên đấu giá nổi bật" trùng nội dung với thẻ đầu trong lưới bên dưới; cột phải để trống một khoảng lớn giữa khối "Sắp diễn ra" và khối "Bạn có sản phẩm để bán?"; 3 thẻ "Cách hoạt động" không giãn đều chiều ngang và lệch nhau; nút "Đặt giá" dính sát đáy thẻ, không có padding/pill; lưới thẻ chỉ có 2 thẻ nằm lệch trái, để trống bên phải.

LÀM THEO THỨ TỰ, mỗi việc xong báo ngắn:

A. LỖI ICON Ô VUÔNG (gốc rễ, sửa toàn app, không chỉ khối này)
 1. Tìm mọi ký tự biểu tượng/emoji trong .fxml, .java, .css: chạy grep với regex các ký tự ngoài Latin/Vietnamese, ví dụ [\u2190-\u2BFF\u{1F000}-\u{1FFFF}] (liệt kê file:dòng đã tìm thấy).
 2. Tạo class `ui/Icons` (hoặc util/Icons) chứa các hằng số SVGPath content (đường vẽ icon đơn giản: trophy, clock, watch, laptop, car, home, palette, user, search, calendar, bag, check, arrow-right, moon, sun, hammer/gavel, book...) và hàm `static Region icon(String name, double size)` trả về Region có -fx-shape, tô màu bằng CSS class `.icon` (-fx-background-color: -c-text hoặc kế thừa -c-muted/-c-primary-text qua class phụ). KHÔNG dùng font icon, KHÔNG dùng emoji, KHÔNG thêm thư viện.
 3. Thay MỌI ký tự biểu tượng bằng icon SVG tương ứng (chip danh mục, thẻ phiên, avatar loại sản phẩm, "Cách hoạt động", banner bán hàng, ô tìm kiếm). Mũi tên "→" trong nút có thể giữ chữ vì nó nằm trong font, nhưng nếu thấy glyph thiếu thì thay bằng icon arrow-right.
 4. Sau khi xong, grep lại: không còn ký tự biểu tượng nào (báo kết quả grep thật).

B. CUỘN CHẬM
 1. Tạo `util/SmoothScroll` với `static void apply(ScrollPane sp, double speed)` (mặc định speed = 3.0):
    - Thêm event filter ScrollEvent.SCROLL trên ScrollPane; bỏ qua nếu deltaY == 0.
    - Tính pixel cuộn: contentHeight = content.getBoundsInLocal().getHeight(); viewport = sp.getViewportBounds().getHeight(); scrollable = contentHeight - viewport; nếu scrollable <= 0 thì thoát.
    - sp.setVvalue(clamp(sp.getVvalue() - e.getDeltaY() * speed / scrollable, 0, 1)); e.consume().
    - Không áp dụng nếu sự kiện bắt nguồn từ bên trong một ListView/TableView/TextArea đang có thể cuộn riêng (kiểm tra e.getTarget() là Node nằm trong các control đó thì return để control đó tự cuộn).
 2. Gọi SmoothScroll.apply cho ScrollPane ở Trang chủ, và mọi ScrollPane chính khác (Quản lý sản phẩm, Phòng đấu giá nếu có).
 3. Đặt `fitToWidth=true`, `hbarPolicy=NEVER`, và `-fx-background-color: transparent` cho ScrollPane (viền xám mặc định làm xấu).
 4. Kiểm tra hiệu năng (cuộn giật): tìm và liệt kê mọi -fx-effect (dropshadow/gaussian blur) trong app.css áp dụng cho thẻ lặp lại. Giảm: thẻ chỉ dùng dropshadow nhẹ (radius ≤ 12, không offset lớn), TUYỆT ĐỐI không dùng blur trên node lớn. Với container danh sách thẻ gọi setCache(true) và setCacheHint(CacheHint.SPEED) CHỈ khi không có animation chạy trên thẻ đó; nếu gây lỗi mờ chữ thì bỏ.
 5. Kiểm tra đồng hồ đếm ngược: mỗi thẻ KHÔNG được tự tạo một Timeline riêng. Dùng MỘT Timeline dùng chung (1 giây/lần) cập nhật tất cả CountdownLabel đang hiển thị; dừng khi rời màn.
 6. Báo: nguyên nhân đã tìm thấy là gì (có bằng chứng file:dòng).

C. BỐ CỤC TRANG CHỦ
 1. Chữ trên khối .panel: dùng -c-on-panel cho TOÀN BỘ chữ trên panel (tiêu đề, mô tả, "Tình trạng", số lượt đặt giá). Phân cấp bằng cỡ chữ/độ đậm và bảo đảm tương phản.
 2. Thẻ nổi bật: thêm padding trên 20px (tiêu đề "Phiên đấu giá nổi bật" không được dính mép trên). Phiên được chọn làm nổi bật phải BỊ LOẠI khỏi lưới "Đang diễn ra" bên dưới để không trùng.
 3. Lưới "Đang diễn ra": dùng TilePane/FlowPane với prefTileWidth ≈ 280, hgap/vgap 20, tự giãn theo bề ngang (bind prefColumns hoặc prefWrapLength theo width của cha), canh đầu (TOP_LEFT) nhưng các thẻ cùng chiều cao. Mỗi thẻ: padding 16, ô ảnh bo góc trên, nút "Đặt giá" là .btn-primary pill, có margin trên 12 và margin dưới 16 (không sát đáy), full width trong thẻ.
 4. Cột phải: xếp VBox spacing 20, khối "Sắp diễn ra" rồi NGAY DƯỚI là khối "Bạn có sản phẩm để bán?". Không dùng Region VBox.vgrow đẩy khối bán hàng xuống tận đáy. Cả cột nằm cùng ScrollPane với nội dung chính (cuộn chung), chiều rộng cố định ~300.
 5. "Cách hoạt động": HBox 3 thẻ có HBox.hgrow=ALWAYS, maxWidth=Infinity, cùng chiều cao (fillHeight=true), gồm: vòng tròn số (1/2/3, nền -c-primary, chữ -c-on-primary) + icon SVG + tiêu đề + mô tả. Bỏ các mũi tên "→" giữa các thẻ (hoặc thay bằng đường kẻ nối mảnh bằng Region), căn giữa nội dung. Khối này rộng bằng toàn bộ phần nội dung chính.
 6. Lề: toàn trang padding trái/phải 24, khoảng cách giữa các khối lớn 28, tiêu đề mục (Đang diễn ra, Cách hoạt động) cỡ 20 đậm với icon SVG nhỏ cùng hàng, canh giữa theo chiều dọc.
 7. Footer nhỏ cuối trang ("Bài tập lớn Lập trình nâng cao") và khoảng đệm dưới 24px để thẻ cuối không dính mép cửa sổ.

D. NGHIỆM THU (báo kết quả THẬT, không khẳng định nếu chưa chạy)
 - `mvn -q clean compile` không lỗi (dán output nếu lỗi).
 - Chạy app, đăng nhập bằng tài khoản mock: Trang chủ dùng bảng màu sáng thống nhất, không còn ô vuông; lăn chuột 1 nấc nhích ≥ ~80px; trang chủ cuộn từ đầu đến cuối bằng 5–8 nấc; phiên nổi bật không lặp trong lưới; cột phải không còn khoảng trống lớn; 3 thẻ "Cách hoạt động" đều nhau.
 - Thu nhỏ cửa sổ 1000×650: không vỡ, không cuộn ngang.
 - grep báo cáo: không emoji; không hex mới trong app.css.
 - Nếu KHÔNG chạy được app trong môi trường của bạn, ghi rõ "chưa chạy" cho từng mục cần chạy.

BÁO CÁO: liệt kê file mới, file sửa (kèm lý do từng file), nguyên nhân gốc của lỗi cuộn chậm và lỗi ô vuông, rủi ro còn lại, và 3 câu hỏi người bảo vệ có thể hỏi (vd: vì sao ScrollPane cuộn theo vvalue 0..1 nên cần tự nhân tốc độ).
````

## Sau khi AI làm xong
1. Chạy `mvn clean javafx:run` và chụp: Trang chủ Sáng, Trang chủ Tối, ảnh kéo xuống cuối.
2. Nếu vẫn **giật** (không chỉ chậm): gửi mình ảnh và nói rõ giật khi lăn hay khi di chuột. Khả năng cao còn một hiệu ứng đổ bóng hoặc `Timeline` lặp, mình sẽ đọc code để chỉ chính xác.
3. Làm Trang chủ xong mới sang màn Phòng đấu giá, đừng sửa nhiều màn một lúc.
