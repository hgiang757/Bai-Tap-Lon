# Bản đồ file client

Đường dẫn tính từ `auction-client-ui/`. FXML/CSS dựng giao diện; controller xử lý thao tác; model chứa dữ liệu; service cung cấp nghiệp vụ. `service/mock` dùng cho thử độc lập; `service/server` kết nối backend thật.

| File | Chức năng |
|---|---|
| [src/main/java/com/auction/client/App.java](../src/main/java/com/auction/client/App.java) | Launcher chuyển tiếp cho IDE hoặc cách chạy gọi App.main. |
| [src/main/java/com/auction/client/Main.java](../src/main/java/com/auction/client/Main.java) | Khởi động JavaFX, kiểm tra auction.mode, tạo Stage và dọn controller khi thoát. |
| [src/main/java/com/auction/client/controller/AdminController.java](../src/main/java/com/auction/client/controller/AdminController.java) | Tổng quan, bảng người dùng/phiên |
| [src/main/java/com/auction/client/controller/AuctionRoomController.java](../src/main/java/com/auction/client/controller/AuctionRoomController.java) | Chọn đúng phiên, gửi giá/auto-bid có khóa trạng thái, đọc đối chiếu, lịch sử/biểu đồ và dọn observer/timer. |
| [src/main/java/com/auction/client/controller/AuthController.java](../src/main/java/com/auction/client/controller/AuthController.java) | Đăng nhập/đăng ký, kiểm tra dữ liệu và chuyển trang theo vai trò. |
| [src/main/java/com/auction/client/controller/ContactController.java](../src/main/java/com/auction/client/controller/ContactController.java) | Dựng thẻ thành viên từ properties, co lưới 1/2 cột và sao chép địa chỉ email. |
| [src/main/java/com/auction/client/controller/GuideController.java](../src/main/java/com/auction/client/controller/GuideController.java) | Dựng mục lục/hướng dẫn và cuộn tới từng mục |
| [src/main/java/com/auction/client/controller/HomeController.java](../src/main/java/com/auction/client/controller/HomeController.java) | Lọc/tìm phiên, dựng lưới RUNNING/OPEN, countdown |
| [src/main/java/com/auction/client/controller/SellerProductsController.java](../src/main/java/com/auction/client/controller/SellerProductsController.java) | Bảng sản phẩm người bán |
| [src/main/java/com/auction/client/controller/WalletController.java](../src/main/java/com/auction/client/controller/WalletController.java) | Hiển thị số dư/tiền giữ, lọc giao dịch |
| [src/main/java/com/auction/client/navigation/DisposableView.java](../src/main/java/com/auction/client/navigation/DisposableView.java) | Hợp đồng dispose để controller dừng timer/observer và đóng scope cập nhật UI. |
| [src/main/java/com/auction/client/navigation/ViewNavigator.java](../src/main/java/com/auction/client/navigation/ViewNavigator.java) | Nạp FXML/CSS, chuyển trang và dọn controller khi rời màn hình. |
| [src/main/java/com/auction/client/navigation/ViewTasks.java](../src/main/java/com/auction/client/navigation/ViewTasks.java) | Điều phối hai pool có hàng đợi giới hạn |
| [src/main/java/com/auction/client/model/AdminUser.java](../src/main/java/com/auction/client/model/AdminUser.java) | Bọc User với createdAt và trạng thái locked cho bảng quản trị. |
| [src/main/java/com/auction/client/model/Auction.java](../src/main/java/com/auction/client/model/Auction.java) | Thông tin phiên: item/seller, giá/người dẫn đầu, trạng thái, lịch, bước giá và số lượt |
| [src/main/java/com/auction/client/model/AuctionStatus.java](../src/main/java/com/auction/client/model/AuctionStatus.java) | Enum OPEN, RUNNING, FINISHED, CANCELED, PAID cùng mô tả UI. |
| [src/main/java/com/auction/client/model/AutoBid.java](../src/main/java/com/auction/client/model/AutoBid.java) | Rule của một user-phiên: maxBid/increment, createdAt, active/limitReached/autoCount |
| [src/main/java/com/auction/client/model/Bid.java](../src/main/java/com/auction/client/model/Bid.java) | Một lượt trả giá với ID, auctionId, username, amount, timestamp và cờ auto. |
| [src/main/java/com/auction/client/model/Item.java](../src/main/java/com/auction/client/model/Item.java) | Sản phẩm client cụ thể: ID/tên/mô tả, category, startingPrice, sellerUsername |
| [src/main/java/com/auction/client/model/ItemCategory.java](../src/main/java/com/auction/client/model/ItemCategory.java) | Enum danh mục, nhãn Việt/icon và chuyển tên dữ liệu cũ hoặc mã enum về danh mục thống nhất. |
| [src/main/java/com/auction/client/model/Transaction.java](../src/main/java/com/auction/client/model/Transaction.java) | Bản ghi giao dịch ví: số tiền/số dư sau giao dịch, loại, thời điểm, ghi chú và phiên liên quan. |
| [src/main/java/com/auction/client/model/TransactionType.java](../src/main/java/com/auction/client/model/TransactionType.java) | Enum nạp, rút, giữ/hoàn tiền giữ, thanh toán và hoàn tiền. |
| [src/main/java/com/auction/client/model/User.java](../src/main/java/com/auction/client/model/User.java) | Dữ liệu tài khoản phía UI: ID, username, họ tên, email, vai trò và số dư. |
| [src/main/java/com/auction/client/model/UserRole.java](../src/main/java/com/auction/client/model/UserRole.java) | Enum Người đấu giá, Người bán, Admin và tên hiển thị. |
| [src/main/java/com/auction/client/service/AuctionAdminService.java](../src/main/java/com/auction/client/service/AuctionAdminService.java) | Hợp đồng danh sách phiên, hủy có lý do và xóa phiên đủ điều kiện. |
| [src/main/java/com/auction/client/service/AuctionClientService.java](../src/main/java/com/auction/client/service/AuctionClientService.java) | Hợp đồng xác thực, phiên, đặt giá/lịch sử, observer và các hàm sản phẩm legacy. Chưa có API tra trạng thái lệnh. |
| [src/main/java/com/auction/client/service/AutoBidException.java](../src/main/java/com/auction/client/service/AutoBidException.java) | Lỗi cấu hình auto-bid |
| [src/main/java/com/auction/client/service/AutoBidService.java](../src/main/java/com/auction/client/service/AutoBidService.java) | Hợp đồng đọc/bật/tắt cấu hình auto-bid cho tài khoản hiện tại. |
| [src/main/java/com/auction/client/service/BidResult.java](../src/main/java/com/auction/client/service/BidResult.java) | Phản hồi đặt giá rõ ràng: success/message, updatedPrice và newBid |
| [src/main/java/com/auction/client/service/BidSubmissionTracker.java](../src/main/java/com/auction/client/service/BidSubmissionTracker.java) | Giữ READY/SENDING/AWAITING_REFRESH/UNKNOWN theo user-phiên. Token/revision loại đọc cũ |
| [src/main/java/com/auction/client/service/ProductService.java](../src/main/java/com/auction/client/service/ProductService.java) | Hợp đồng danh sách/lưu/xóa sản phẩm-phiên, đường dẫn ảnh và lịch sử phiên người bán. |
| [src/main/java/com/auction/client/service/ServiceRegistry.java](../src/main/java/com/auction/client/service/ServiceRegistry.java) | Cấp bộ dịch vụ nhất quán, từ chối trộn mock/adapter |
| [src/main/java/com/auction/client/service/UserAdminService.java](../src/main/java/com/auction/client/service/UserAdminService.java) | Hợp đồng liệt kê user, khóa, đổi vai trò, xóa theo actorId. |
| [src/main/java/com/auction/client/service/WalletException.java](../src/main/java/com/auction/client/service/WalletException.java) | Lỗi điều kiện nạp/rút hoặc số dư trong dịch vụ ví mock. |
| [src/main/java/com/auction/client/service/WalletService.java](../src/main/java/com/auction/client/service/WalletService.java) | Hợp đồng số dư, held/available, danh sách giao dịch và nạp/rút. |
| [src/main/java/com/auction/client/service/mock/AutoBidEngine.java](../src/main/java/com/auction/client/service/mock/AutoBidEngine.java) | Xử lý lượt đặt tự động, thứ tự ưu tiên/giới hạn và thông báo bid khi đang khóa kho. Thuật toán giữ nguyên trong đợt này. |
| [src/main/java/com/auction/client/service/mock/MockAuctionClientService.java](../src/main/java/com/auction/client/service/mock/MockAuctionClientService.java) | Xác thực tài khoản mẫu/đăng ký, CRUD legacy, phiên/lịch sử, đặt giá và observer đồng bộ kho |
| [src/main/java/com/auction/client/service/mock/MockAutoBidService.java](../src/main/java/com/auction/client/service/mock/MockAutoBidService.java) | Đọc/bật/tắt rule |
| [src/main/java/com/auction/client/service/mock/MockDataStore.java](../src/main/java/com/auction/client/service/mock/MockDataStore.java) | Kho bộ nhớ chung cho item, auction, user, lịch sử, listeners, số dư/hold và rule |
| [src/main/java/com/auction/client/service/mock/MockManagementService.java](../src/main/java/com/auction/client/service/mock/MockManagementService.java) | Cùng một implementation cho sản phẩm và hai dịch vụ admin |
| [src/main/java/com/auction/client/service/mock/MockWalletService.java](../src/main/java/com/auction/client/service/mock/MockWalletService.java) | Nạp/rút/đọc số dư-giao dịch trên kho được chỉ định, kiểm tra số tiền và available. |
| [src/main/java/com/auction/client/ui/AppFooter.java](../src/main/java/com/auction/client/ui/AppFooter.java) | Footer hướng dẫn/liên hệ |
| [src/main/java/com/auction/client/ui/AppHeader.java](../src/main/java/com/auction/client/ui/AppHeader.java) | Header tài khoản |
| [src/main/java/com/auction/client/ui/Icons.java](../src/main/java/com/auction/client/ui/Icons.java) | Tạo Region biểu tượng vector từ tên và liệt kê bộ icon |
| [src/main/java/com/auction/client/ui/PasswordFieldWithToggle.java](../src/main/java/com/auction/client/ui/PasswordFieldWithToggle.java) | Liên kết ô PasswordField/TextField, giữ caret và đổi hiển thị mật khẩu bằng nút icon. |
| [src/main/java/com/auction/client/ui/ProductImages.java](../src/main/java/com/auction/client/ui/ProductImages.java) | Import/xóa bản sao ảnh do ứng dụng quản lý |
| [src/main/java/com/auction/client/ui/RoundedClip.java](../src/main/java/com/auction/client/ui/RoundedClip.java) | Gắn clip bo góc và cập nhật kích thước theo node. |
| [src/main/java/com/auction/client/ui/SmoothScroll.java](../src/main/java/com/auction/client/ui/SmoothScroll.java) | Chuẩn hóa lăn chuột cho ScrollPane |
| [src/main/java/com/auction/client/ui/UserMenu.java](../src/main/java/com/auction/client/ui/UserMenu.java) | Popup tài khoản/vai trò, chuyển chế độ người bán, mở Ví/hướng dẫn/liên hệ và đăng xuất trên nền. |
| [src/main/java/com/auction/client/util/AppInfo.java](../src/main/java/com/auction/client/util/AppInfo.java) | Tên thương hiệu dùng chung, tránh lặp literal trong nhiều màn hình. |
| [src/main/java/com/auction/client/util/AuctionTime.java](../src/main/java/com/auction/client/util/AuctionTime.java) | Parse HH:mm nghiêm ngặt, kiểm tra lịch bắt đầu/kết thúc và diễn giải thời lượng. |
| [src/main/java/com/auction/client/util/FormatUtil.java](../src/main/java/com/auction/client/util/FormatUtil.java) | Định dạng tiền Việt, trục giá rút gọn, giờ biểu đồ và che username trong lịch sử. |
| [src/main/java/com/auction/client/util/Validators.java](../src/main/java/com/auction/client/util/Validators.java) | Chuẩn hóa username NFC/khoảng trắng, kiểm tra username/email, mật khẩu và nhập lại. |
| [src/main/java/com/auction/client/dev/SnapshotApp.java](../src/main/java/com/auction/client/dev/SnapshotApp.java) | Chụp 11 ảnh mock ở chiều rộng chọn trước, gồm ví số tiền dài |
| [src/main/java/com/auction/client/dev/UiKitController.java](../src/main/java/com/auction/client/dev/UiKitController.java) | Dựng gallery icon và tương tác toast mẫu để đối chiếu thành phần giao diện. |
| [src/test/java/com/auction/client/controller/ClientUiTest.java](../src/test/java/com/auction/client/controller/ClientUiTest.java) | 12 bài JavaFX thật: mọi route/FXML/UI kit, auth/tìm kiếm/điều hướng, khóa giá/timeout/callback/vòng đời, CRUD sản phẩm, hai nhóm RUNNING/OPEN và lọc trùng, trạng thái tải/lỗi/rỗng, ví số tiền dài và cuộn xác thực. |
| [src/test/java/com/auction/client/navigation/ImageTasksTest.java](../src/test/java/com/auction/client/navigation/ImageTasksTest.java) | Ảnh bị chặn không giữ lệnh nghiệp vụ |
| [src/test/java/com/auction/client/navigation/ViewTasksTest.java](../src/test/java/com/auction/client/navigation/ViewTasksTest.java) | Kết quả đọc đảo thứ tự, callback UI đã xếp hàng sau close, mutation vẫn chạy đúng một lần và lỗi về UI. |
| [src/test/java/com/auction/client/service/BidSubmissionTrackerTest.java](../src/test/java/com/auction/client/service/BidSubmissionTrackerTest.java) | UNKNOWN không mở khóa bởi snapshot |
| [src/test/java/com/auction/client/service/MockAuctionBidAccessTest.java](../src/test/java/com/auction/client/service/MockAuctionBidAccessTest.java) | Kiểm tra điều kiện truy cập đặt giá của người bán và phiên đăng nhập. |
| [src/test/java/com/auction/client/service/MockAuthIntegrationTest.java](../src/test/java/com/auction/client/service/MockAuthIntegrationTest.java) | Đăng ký xuất hiện trong admin |
| [src/test/java/com/auction/client/service/MockAutoBidServiceTest.java](../src/test/java/com/auction/client/service/MockAutoBidServiceTest.java) | Đăng ký/giới hạn/bước giá/số dư, xử lý đấu tự động, ưu tiên và dừng rule theo điều kiện phiên. |
| [src/test/java/com/auction/client/service/MockManagementServiceTest.java](../src/test/java/com/auction/client/service/MockManagementServiceTest.java) | Admin không thao tác chính mình |
| [src/test/java/com/auction/client/service/MockSharedStoreTest.java](../src/test/java/com/auction/client/service/MockSharedStoreTest.java) | CRUD sản phẩm phản ánh trên danh sách phiên |
| [src/test/java/com/auction/client/service/MockWalletServiceTest.java](../src/test/java/com/auction/client/service/MockWalletServiceTest.java) | Nạp/rút và điều kiện số tiền, tiền giữ/hoàn khi bị vượt giá, thanh toán và số dư qua kho dùng chung. |
| [src/test/java/com/auction/client/service/ServiceRegistryTest.java](../src/test/java/com/auction/client/service/ServiceRegistryTest.java) | Mode server không fallback |
| [src/test/java/com/auction/client/ui/ProductImagesIoTest.java](../src/test/java/com/auction/client/ui/ProductImagesIoTest.java) | Đọc file đã hủy/deadline hết |
| [src/test/java/com/auction/client/ui/ProductImagesTest.java](../src/test/java/com/auction/client/ui/ProductImagesTest.java) | Import đúng thư mục ứng dụng và nội dung |
| [src/test/java/com/auction/client/util/AuctionTimeTest.java](../src/test/java/com/auction/client/util/AuctionTimeTest.java) | Giờ HH:mm hợp lệ/không hợp lệ, lịch phiên và mô tả thời lượng. |
| [src/test/java/com/auction/client/util/FormatUtilTest.java](../src/test/java/com/auction/client/util/FormatUtilTest.java) | Hiển thị long vượt 2^53 và Long.MAX_VALUE đúng từng chữ số. |
| [src/test/java/com/auction/client/ui/UiResourcesTest.java](../src/test/java/com/auction/client/ui/UiResourcesTest.java) | Tài nguyên trang trí đã bỏ không có trên classpath |
| [src/test/java/com/auction/client/util/ValidatorsTest.java](../src/test/java/com/auction/client/util/ValidatorsTest.java) | Các trường hợp username/email/mật khẩu, chuẩn hóa và nhập lại theo ràng buộc hiện có. |
| [src/main/resources/com/auction/client/dev/uikit-view.fxml](../src/main/resources/com/auction/client/dev/uikit-view.fxml) | Trang mẫu button, form, badge/toast, icon và thành phần design system. |
| [src/main/resources/com/auction/client/views/admin-view.fxml](../src/main/resources/com/auction/client/views/admin-view.fxml) | Ba pane tổng quan/người dùng/phiên, bảng và bộ lọc |
| [src/main/resources/com/auction/client/views/auction-room-view.fxml](../src/main/resources/com/auction/client/views/auction-room-view.fxml) | Thông tin sản phẩm, nhập giá, nhãn trạng thái lệnh, auto-bid, chart/lịch sử và nút Tải lại. |
| [src/main/resources/com/auction/client/views/auth-view.fxml](../src/main/resources/com/auction/client/views/auth-view.fxml) | Form đăng nhập/đăng ký, chọn vai trò khi đăng ký, mật khẩu và cuộn trên cửa sổ nhỏ. |
| [src/main/resources/com/auction/client/views/contact-view.fxml](../src/main/resources/com/auction/client/views/contact-view.fxml) | Khung trang liên hệ |
| [src/main/resources/com/auction/client/views/guide-view.fxml](../src/main/resources/com/auction/client/views/guide-view.fxml) | Khung trang hướng dẫn/cuộn |
| [src/main/resources/com/auction/client/views/home-view.fxml](../src/main/resources/com/auction/client/views/home-view.fxml) | Trang tìm kiếm/danh mục, hai lưới phiên RUNNING/OPEN và CTA người bán. |
| [src/main/resources/com/auction/client/views/seller-products-view.fxml](../src/main/resources/com/auction/client/views/seller-products-view.fxml) | Bộ lọc và bảng sản phẩm người bán |
| [src/main/resources/com/auction/client/views/wallet-view.fxml](../src/main/resources/com/auction/client/views/wallet-view.fxml) | Bộ lọc, bảng giao dịch và các nút nạp/rút |
| [src/main/resources/com/auction/client/css/app.css](../src/main/resources/com/auction/client/css/app.css) | Looked-up color, typography, button, bảng/card, layout, trạng thái và chart dùng chung. |
| [src/main/resources/com/auction/client/contacts.properties](../src/main/resources/com/auction/client/contacts.properties) | Tên/email của bốn thành viên được nạp bằng UTF-8. |
| [pom.xml](../pom.xml) | Java release 25, JavaFX/JUnit và plugin compiler/surefire/javafx |
| [src/main/java/module-info.java](../src/main/java/module-info.java) | Khai báo module JavaFX/Swing/JDK |
| [scripts/check-encoding.py](../scripts/check-encoding.py) | Quét UTF-8/dấu hỏi hỏng trong source, test, docs và README |
| [scripts/check-contrast.py](../scripts/check-contrast.py) | Đo 30 cặp màu chữ/viền/focus thực tế bằng công thức WCAG |
| [scripts/check-docs.py](../scripts/check-docs.py) | Đối chiếu đủ file code/config trong CODE_MAP và kiểm tra liên kết Markdown cục bộ/fenced block. Không thay thế trình render Mermaid. |
| [README.md](../README.md) | Điểm bắt đầu: kiến trúc, cây file, sơ đồ Mermaid, lệnh chạy/test, tài khoản và giới hạn backend. |


## Tích hợp Socket (05/11/2026)

| File | Chức năng |
|---|---|
| [src/main/java/com/auction/client/service/AuctionRoomSnapshot.java](../src/main/java/com/auction/client/service/AuctionRoomSnapshot.java) | Hợp đồng snapshot, adapter hoặc transport cho backend thật |
| [src/main/java/com/auction/client/service/WalletSnapshot.java](../src/main/java/com/auction/client/service/WalletSnapshot.java) | Hợp đồng snapshot, adapter hoặc transport cho backend thật |
| [src/main/java/com/auction/client/service/server/ServerServices.java](../src/main/java/com/auction/client/service/server/ServerServices.java) | Hợp đồng snapshot, adapter hoặc transport cho backend thật |
| [src/main/java/com/auction/client/service/server/ServerMapper.java](../src/main/java/com/auction/client/service/server/ServerMapper.java) | Hợp đồng snapshot, adapter hoặc transport cho backend thật |
| [src/main/java/com/auction/client/service/server/ServerError.java](../src/main/java/com/auction/client/service/server/ServerError.java) | Hợp đồng snapshot, adapter hoặc transport cho backend thật |
| [src/main/java/com/auction/client/service/server/JsonTransport.java](../src/main/java/com/auction/client/service/server/JsonTransport.java) | Hợp đồng snapshot, adapter hoặc transport cho backend thật |
