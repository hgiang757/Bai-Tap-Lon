# Bản đồ file client

Các đường dẫn dưới đây tính từ `auction-client-ui/`. Model là dữ liệu độc lập phía client; interface service là hợp đồng, mock là implementation đang chạy. Không có adapter nghiệp vụ server trong source hiện tại.

## Khởi chạy

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/App.java](../src/main/java/com/auction/client/App.java) | Launcher chuyển tiếp cho IDE hoặc cách chạy gọi App.main. | Main.main | Đang dùng khi chọn launcher App |
| [src/main/java/com/auction/client/Main.java](../src/main/java/com/auction/client/Main.java) | Khởi động JavaFX, kiểm tra auction.mode, tạo Stage và dọn controller khi thoát. | ViewNavigator; ServiceRegistry; auth-view.fxml | Đang dùng |

## Controller

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/controller/AdminController.java](../src/main/java/com/auction/client/controller/AdminController.java) | Tổng quan, bảng người dùng/phiên; khóa, vai trò, xóa, hủy phiên và đăng xuất theo quyền admin. | admin-view.fxml; UserAdminService; AuctionAdminService | Đang dùng, dữ liệu từ mock |
| [src/main/java/com/auction/client/controller/AuctionRoomController.java](../src/main/java/com/auction/client/controller/AuctionRoomController.java) | Chọn đúng phiên, gửi giá/auto-bid có khóa trạng thái, đọc đối chiếu, lịch sử/biểu đồ và dọn observer/timer. | auction-room-view.fxml; BidSubmissionTracker; AuctionClientService; AutoBidService; WalletService | Đang dùng, dữ liệu từ mock |
| [src/main/java/com/auction/client/controller/AuthController.java](../src/main/java/com/auction/client/controller/AuthController.java) | Điều khiển đăng nhập/đăng ký, kiểm tra ô nhập, ghi nhớ username, nền đăng nhập và chuyển trang theo vai trò. | auth-view.fxml; AuctionClientService; Validators; PasswordFieldWithToggle; ViewTasks | Đang dùng, dữ liệu từ mock |
| [src/main/java/com/auction/client/controller/ContactController.java](../src/main/java/com/auction/client/controller/ContactController.java) | Dựng thẻ thành viên từ properties, co lưới 1/2 cột và sao chép địa chỉ email. | contact-view.fxml; contacts.properties; AppFooter | Đang dùng |
| [src/main/java/com/auction/client/controller/GuideController.java](../src/main/java/com/auction/client/controller/GuideController.java) | Dựng mục lục/hướng dẫn và cuộn tới từng mục; dừng animation khi rời trang. | guide-view.fxml; AppFooter; ViewNavigator | Đang dùng |
| [src/main/java/com/auction/client/controller/HomeController.java](../src/main/java/com/auction/client/controller/HomeController.java) | Lọc/tìm phiên, dựng lưới RUNNING/OPEN, countdown; hủy lượt tải ảnh cũ khi đổi bộ lọc/rời trang. | home-view.fxml; AuctionClientService; ProductService; ViewTasks.images | Đang dùng, dữ liệu từ mock |
| [src/main/java/com/auction/client/controller/SellerProductsController.java](../src/main/java/com/auction/client/controller/SellerProductsController.java) | Bảng sản phẩm người bán; form lịch/ảnh; thêm, sửa, xóa; scope ảnh riêng cho mỗi ô bảng và hộp thoại. | seller-products-view.fxml; ProductService; AuctionTime; ProductImages | Đang dùng, dữ liệu từ mock |
| [src/main/java/com/auction/client/controller/WalletController.java](../src/main/java/com/auction/client/controller/WalletController.java) | Hiển thị số dư/tiền giữ, lọc giao dịch; form nạp/rút mô phỏng trên nền. | wallet-view.fxml; WalletService; Transaction; ViewTasks | Đang dùng, tiền mô phỏng |

## Điều hướng và tác vụ

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/navigation/DisposableView.java](../src/main/java/com/auction/client/navigation/DisposableView.java) | Hợp đồng dispose để controller dừng timer/observer và đóng scope cập nhật UI. | ViewNavigator; các controller có tác vụ/vòng đời | Đang dùng |
| [src/main/java/com/auction/client/navigation/ViewNavigator.java](../src/main/java/com/auction/client/navigation/ViewNavigator.java) | Nạp FXML/CSS, ghép nền, quản lý lịch sử kèm auctionId; gọi dispose khi thay màn hình hoặc nạp lỗi. | Main; DisposableView; AuctionRoomController; UserMenu | Đang dùng |
| [src/main/java/com/auction/client/navigation/ViewTasks.java](../src/main/java/com/auction/client/navigation/ViewTasks.java) | Điều phối hai pool có hàng đợi giới hạn; chỉ nhận callback mới nhất. Scope ảnh có cancelAll; scope nghiệp vụ giữ lệnh đã nhận. | Platform.runLater; controllers; AppHeader; UserMenu | Đang dùng |

## Model và DTO

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/model/AdminUser.java](../src/main/java/com/auction/client/model/AdminUser.java) | Bọc User với createdAt và trạng thái locked cho bảng quản trị. | AdminController; UserAdminService; MockManagementService | DTO client đang dùng |
| [src/main/java/com/auction/client/model/Auction.java](../src/main/java/com/auction/client/model/Auction.java) | Thông tin phiên: item/seller, giá/người dẫn đầu, trạng thái, lịch, bước giá và số lượt; cung cấp giá tối thiểu/định dạng. | Home/AuctionRoom/Seller/Admin controllers; AuctionStatus | Model client đang dùng |
| [src/main/java/com/auction/client/model/AuctionStatus.java](../src/main/java/com/auction/client/model/AuctionStatus.java) | Enum OPEN, RUNNING, FINISHED, CANCELED, PAID cùng mô tả UI. | Auction; các controller; MockDataStore | Model client đang dùng; server chưa chốt đủ trạng thái |
| [src/main/java/com/auction/client/model/AutoBid.java](../src/main/java/com/auction/client/model/AutoBid.java) | Rule của một user-phiên: maxBid/increment, createdAt, active/limitReached/autoCount; copy trạng thái cho UI. | AutoBidService; MockAutoBidService; AutoBidEngine | Model client đang dùng cho mock |
| [src/main/java/com/auction/client/model/Bid.java](../src/main/java/com/auction/client/model/Bid.java) | Một lượt trả giá với ID, auctionId, username, amount, timestamp và cờ auto. | BidResult; observer; AuctionRoomController; MockDataStore | Model client đang dùng |
| [src/main/java/com/auction/client/model/Item.java](../src/main/java/com/auction/client/model/Item.java) | Sản phẩm client cụ thể: ID/tên/mô tả, category, startingPrice, sellerUsername; chuẩn hóa danh mục. | Auction; ProductService; SellerProductsController; ItemCategory | Model client đang dùng |
| [src/main/java/com/auction/client/model/ItemCategory.java](../src/main/java/com/auction/client/model/ItemCategory.java) | Enum danh mục, nhãn Việt/icon và chuyển tên dữ liệu cũ hoặc mã enum về danh mục thống nhất. | Item; HomeController; Icons; SellerProductsController | Model client đang dùng |
| [src/main/java/com/auction/client/model/Transaction.java](../src/main/java/com/auction/client/model/Transaction.java) | Bản ghi giao dịch ví: số tiền/số dư sau giao dịch, loại, thời điểm, ghi chú và phiên liên quan. | WalletService; WalletController; MockDataStore | Model client đang dùng cho ví mock |
| [src/main/java/com/auction/client/model/TransactionType.java](../src/main/java/com/auction/client/model/TransactionType.java) | Enum nạp, rút, giữ/hoàn tiền giữ, thanh toán và hoàn tiền. | Transaction; WalletController; MockDataStore | Model client đang dùng cho ví mock |
| [src/main/java/com/auction/client/model/User.java](../src/main/java/com/auction/client/model/User.java) | Dữ liệu tài khoản phía UI: ID, username, họ tên, email, vai trò và số dư. | AuthController; UserMenu; MockDataStore; UserRole | Model client đang dùng |
| [src/main/java/com/auction/client/model/UserRole.java](../src/main/java/com/auction/client/model/UserRole.java) | Enum Người đấu giá, Người bán, Admin và tên hiển thị. | User; AuthController; AdminController | Model client đang dùng |

## Hợp đồng và điều phối dịch vụ

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/service/AuctionAdminService.java](../src/main/java/com/auction/client/service/AuctionAdminService.java) | Hợp đồng danh sách phiên, hủy có lý do và xóa phiên đủ điều kiện. | Implementation MockManagementService; AdminController | Interface đang dùng; chưa có adapter server |
| [src/main/java/com/auction/client/service/AuctionClientService.java](../src/main/java/com/auction/client/service/AuctionClientService.java) | Hợp đồng xác thực, phiên, đặt giá/lịch sử, observer và các hàm sản phẩm legacy. Chưa có API tra trạng thái lệnh. | Implementation MockAuctionClientService; Auth/Home/AuctionRoom | Interface đang dùng; chưa có adapter server |
| [src/main/java/com/auction/client/service/AutoBidException.java](../src/main/java/com/auction/client/service/AutoBidException.java) | Lỗi cấu hình auto-bid; implementation mock hiện kiểm tra các điều kiện này trước khi sửa rule. | MockAutoBidService; AuctionRoomController | Đang dùng cho mock; không dùng thay lỗi transport |
| [src/main/java/com/auction/client/service/AutoBidService.java](../src/main/java/com/auction/client/service/AutoBidService.java) | Hợp đồng đọc/bật/tắt cấu hình auto-bid cho tài khoản hiện tại. | Implementation MockAutoBidService; AuctionRoomController | Interface đang dùng; hiện mô phỏng |
| [src/main/java/com/auction/client/service/BidResult.java](../src/main/java/com/auction/client/service/BidResult.java) | Phản hồi đặt giá rõ ràng: success/message, updatedPrice và newBid; khác lỗi kết nối/ngoại lệ. | AuctionClientService.placeBid; MockAuctionClientService; AuctionRoomController | Đang dùng |
| [src/main/java/com/auction/client/service/BidSubmissionTracker.java](../src/main/java/com/auction/client/service/BidSubmissionTracker.java) | Giữ READY/SENDING/AWAITING_REFRESH/UNKNOWN theo user-phiên. Token/revision loại đọc cũ; snapshot không mở khóa UNKNOWN. | ServiceRegistry; AuctionRoomController; ViewTasks | Đang dùng, token chỉ cục bộ |
| [src/main/java/com/auction/client/service/ProductService.java](../src/main/java/com/auction/client/service/ProductService.java) | Hợp đồng danh sách/lưu/xóa sản phẩm-phiên, đường dẫn ảnh và lịch sử phiên người bán. | Implementation MockManagementService; SellerProductsController; card/ảnh phòng | Interface đang dùng; chưa có adapter server |
| [src/main/java/com/auction/client/service/ServiceRegistry.java](../src/main/java/com/auction/client/service/ServiceRegistry.java) | Cấp bộ dịch vụ nhất quán, từ chối trộn mock/adapter; giữ tracker giá theo vòng đời AuctionClientService. | Tất cả interface service; các MockService; BidSubmissionTracker | Đang dùng; chỉ cấu hình mode mock |
| [src/main/java/com/auction/client/service/UserAdminService.java](../src/main/java/com/auction/client/service/UserAdminService.java) | Hợp đồng liệt kê user, khóa, đổi vai trò, xóa theo actorId. | Implementation MockManagementService; AdminController | Interface đang dùng; chưa có adapter server |
| [src/main/java/com/auction/client/service/WalletException.java](../src/main/java/com/auction/client/service/WalletException.java) | Lỗi điều kiện nạp/rút hoặc số dư trong dịch vụ ví mock. | MockWalletService; WalletController | Chỉ phục vụ mock hiện tại |
| [src/main/java/com/auction/client/service/WalletService.java](../src/main/java/com/auction/client/service/WalletService.java) | Hợp đồng số dư, held/available, danh sách giao dịch và nạp/rút. | Implementation MockWalletService; WalletController; AppHeader; AuctionRoomController | Interface đang dùng; hiện tiền mô phỏng |

## Implementation mock

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/service/mock/AutoBidEngine.java](../src/main/java/com/auction/client/service/mock/AutoBidEngine.java) | Xử lý lượt đặt tự động, thứ tự ưu tiên/giới hạn và thông báo bid khi đang khóa kho. Thuật toán giữ nguyên trong đợt này. | MockDataStore; AutoBid; Bid; MockAuctionClientService | Chỉ phục vụ mock |
| [src/main/java/com/auction/client/service/mock/MockAuctionClientService.java](../src/main/java/com/auction/client/service/mock/MockAuctionClientService.java) | Xác thực tài khoản mẫu/đăng ký, CRUD legacy, phiên/lịch sử, đặt giá và observer đồng bộ kho; trả BidResult. | MockDataStore; AutoBidEngine; AuctionClientService | Chỉ phục vụ mock |
| [src/main/java/com/auction/client/service/mock/MockAutoBidService.java](../src/main/java/com/auction/client/service/mock/MockAutoBidService.java) | Đọc/bật/tắt rule; kiểm tra cấu hình/số dư rồi gọi engine trên cùng kho. | MockDataStore; AutoBidService; supplier AuctionClientService; AutoBidEngine | Chỉ phục vụ mock |
| [src/main/java/com/auction/client/service/mock/MockDataStore.java](../src/main/java/com/auction/client/service/mock/MockDataStore.java) | Kho bộ nhớ chung cho item, auction, user, lịch sử, listeners, số dư/hold và rule; tạo dữ liệu mẫu và kho kiểm thử riêng. | Tất cả MockService; AutoBidEngine | Chỉ phục vụ mock |
| [src/main/java/com/auction/client/service/mock/MockManagementService.java](../src/main/java/com/auction/client/service/mock/MockManagementService.java) | Cùng một implementation cho sản phẩm và hai dịch vụ admin; áp dụng điều kiện sửa/xóa/hủy trong mock. | MockDataStore; ProductService; UserAdminService; AuctionAdminService | Chỉ phục vụ mock |
| [src/main/java/com/auction/client/service/mock/MockWalletService.java](../src/main/java/com/auction/client/service/mock/MockWalletService.java) | Nạp/rút/đọc số dư-giao dịch trên kho được chỉ định, kiểm tra số tiền và available. | MockDataStore; WalletService | Chỉ phục vụ mock |

## Thành phần UI dùng chung

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/ui/AppFooter.java](../src/main/java/com/auction/client/ui/AppFooter.java) | Footer hướng dẫn/liên hệ; install bố trí nội dung và khoảng trống để footer cuộn đúng. | Home/AuctionRoom/Wallet/Guide/Contact; ViewNavigator | Đang dùng |
| [src/main/java/com/auction/client/ui/AppHeader.java](../src/main/java/com/auction/client/ui/AppHeader.java) | Header tài khoản; đọc số dư trên nền, không chồng lượt đọc; dọn scope/timer khi tách scene. | FXML các trang; UserMenu; WalletService; ViewTasks | Đang dùng |
| [src/main/java/com/auction/client/ui/Icons.java](../src/main/java/com/auction/client/ui/Icons.java) | Tạo Region biểu tượng vector từ tên và liệt kê bộ icon; không tải thư viện icon ngoài. | Các controller; header/footer/menu; UiKitController | Đang dùng |
| [src/main/java/com/auction/client/ui/PasswordFieldWithToggle.java](../src/main/java/com/auction/client/ui/PasswordFieldWithToggle.java) | Liên kết ô PasswordField/TextField, giữ caret và đổi hiển thị mật khẩu bằng nút icon. | AuthController; auth-view.fxml | Đang dùng |
| [src/main/java/com/auction/client/ui/ProductImages.java](../src/main/java/com/auction/client/ui/ProductImages.java) | Import/xóa bản sao ảnh do ứng dụng quản lý; đọc file/HTTP với connect/read timeout, kiểm tra interrupt/deadline và đóng stream. | Home/AuctionRoom/Seller; ViewTasks.images; thư mục ảnh user.home | Đang dùng; đường dẫn ảnh thuộc mock hiện tại |
| [src/main/java/com/auction/client/ui/RoundedClip.java](../src/main/java/com/auction/client/ui/RoundedClip.java) | Gắn clip bo góc và cập nhật kích thước theo node. | Các card, ảnh sản phẩm, lịch sử và chart | Đang dùng |
| [src/main/java/com/auction/client/ui/SmoothScroll.java](../src/main/java/com/auction/client/ui/SmoothScroll.java) | Chuẩn hóa lăn chuột cho ScrollPane; nhường cuộn cho ListView/TableView/TextArea bên trong. | Các controller trang cuộn | Đang dùng |
| [src/main/java/com/auction/client/ui/UserMenu.java](../src/main/java/com/auction/client/ui/UserMenu.java) | Popup tài khoản/vai trò, chuyển chế độ người bán, mở Ví/hướng dẫn/liên hệ và đăng xuất trên nền. | AppHeader; ViewNavigator; AuctionClientService; ViewTasks | Đang dùng |

## Tiện ích

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/util/AppInfo.java](../src/main/java/com/auction/client/util/AppInfo.java) | Tên thương hiệu dùng chung, tránh lặp literal trong nhiều màn hình. | Main; ViewNavigator; controllers; header/footer | Đang dùng |
| [src/main/java/com/auction/client/util/AuctionTime.java](../src/main/java/com/auction/client/util/AuctionTime.java) | Parse HH:mm nghiêm ngặt, kiểm tra lịch bắt đầu/kết thúc và diễn giải thời lượng. | SellerProductsController; AuctionTimeTest | Đang dùng |
| [src/main/java/com/auction/client/util/FormatUtil.java](../src/main/java/com/auction/client/util/FormatUtil.java) | Định dạng tiền Việt, trục giá rút gọn, giờ biểu đồ và che username trong lịch sử. | Model; controllers; mock service hiển thị lỗi | Đang dùng |
| [src/main/java/com/auction/client/util/Validators.java](../src/main/java/com/auction/client/util/Validators.java) | Chuẩn hóa username NFC/khoảng trắng, kiểm tra username/email, mật khẩu và nhập lại. | AuthController; ValidatorsTest | Đang dùng; không thay xác nhận của dịch vụ |

## Công cụ dev

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/java/com/auction/client/dev/SnapshotApp.java](../src/main/java/com/auction/client/dev/SnapshotApp.java) | Chụp 11 ảnh mock ở chiều rộng chọn trước, gồm ví số tiền dài; chờ FX pulse; theo thứ tự, chờ dữ liệu nền và dùng navigator để dọn màn hình; mã lỗi khác 0 nếu không hoàn tất. | ViewNavigator; Home/AuctionRoom/Seller controllers; target/snapshots | Công cụ dev |
| [src/main/java/com/auction/client/dev/UiKitController.java](../src/main/java/com/auction/client/dev/UiKitController.java) | Dựng gallery icon và tương tác toast mẫu để đối chiếu thành phần giao diện. | dev/uikit-view.fxml; Icons; app.css | Công cụ dev |

## Các file kiểm thử

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/test/java/com/auction/client/controller/ClientUiTest.java](../src/test/java/com/auction/client/controller/ClientUiTest.java) | 12 bài JavaFX thật: mọi route/FXML/UI kit, auth/tìm kiếm/điều hướng, khóa giá/timeout/callback/vòng đời, CRUD sản phẩm, hai nhóm RUNNING/OPEN và lọc trùng, trạng thái tải/lỗi/rỗng, ví số tiền dài và cuộn xác thực. | FXML thật; ControlledAuctionService; BidSubmissionTracker | Kiểm thử desktop, bật auction.ui.tests |
| [src/test/java/com/auction/client/navigation/ImageTasksTest.java](../src/test/java/com/auction/client/navigation/ImageTasksTest.java) | Ảnh bị chặn không giữ lệnh nghiệp vụ; hàng đợi đầy; cancel scope/key, tái dùng ô và phục hồi sau lỗi. | ViewTasks; pool 1 luồng/hàng đợi nhỏ; CountDownLatch | Kiểm thử unit |
| [src/test/java/com/auction/client/navigation/ViewTasksTest.java](../src/test/java/com/auction/client/navigation/ViewTasksTest.java) | Kết quả đọc đảo thứ tự, callback UI đã xếp hàng sau close, mutation vẫn chạy đúng một lần và lỗi về UI. | ViewTasks; executor/dispatcher giả có thể điều khiển | Kiểm thử unit |
| [src/test/java/com/auction/client/service/BidSubmissionTrackerTest.java](../src/test/java/com/auction/client/service/BidSubmissionTrackerTest.java) | UNKNOWN không mở khóa bởi snapshot; loại đọc trước lệnh/phản hồi; callback cũ và khóa theo user-phiên; rejection chưa gửi. | BidSubmissionTracker | Kiểm thử unit |
| [src/test/java/com/auction/client/service/MockAuctionBidAccessTest.java](../src/test/java/com/auction/client/service/MockAuctionBidAccessTest.java) | Kiểm tra điều kiện truy cập đặt giá của người bán và phiên đăng nhập. | MockAuctionClientService; kho kiểm thử riêng | Kiểm thử mock |
| [src/test/java/com/auction/client/service/MockAuthIntegrationTest.java](../src/test/java/com/auction/client/service/MockAuthIntegrationTest.java) | Đăng ký xuất hiện trong admin; khóa/đổi vai trò/xóa ảnh hưởng đăng nhập; kho riêng không đọc nhầm registry. | MockAuctionClientService; MockManagementService | Kiểm thử mock |
| [src/test/java/com/auction/client/service/MockAutoBidServiceTest.java](../src/test/java/com/auction/client/service/MockAutoBidServiceTest.java) | Đăng ký/giới hạn/bước giá/số dư, xử lý đấu tự động, ưu tiên và dừng rule theo điều kiện phiên. | MockAutoBidService; AutoBidEngine; MockWalletService | Kiểm thử mock, hồi quy thuật toán giữ nguyên |
| [src/test/java/com/auction/client/service/MockManagementServiceTest.java](../src/test/java/com/auction/client/service/MockManagementServiceTest.java) | Admin không thao tác chính mình; hủy cần lý do/đúng trạng thái; người bán không xóa sai phiên. | MockManagementService | Kiểm thử mock |
| [src/test/java/com/auction/client/service/MockSharedStoreTest.java](../src/test/java/com/auction/client/service/MockSharedStoreTest.java) | CRUD sản phẩm phản ánh trên danh sách phiên; chuyển trạng thái/thanh toán mock; nhận diện danh mục cũ/mới. | MockManagementService; MockAuctionClientService; ItemCategory | Kiểm thử mock |
| [src/test/java/com/auction/client/service/MockWalletServiceTest.java](../src/test/java/com/auction/client/service/MockWalletServiceTest.java) | Nạp/rút và điều kiện số tiền, tiền giữ/hoàn khi bị vượt giá, thanh toán và số dư qua kho dùng chung. | MockWalletService; MockDataStore; MockAuctionClientService | Kiểm thử mock, hồi quy tiền giữ/thanh toán |
| [src/test/java/com/auction/client/service/ServiceRegistryTest.java](../src/test/java/com/auction/client/service/ServiceRegistryTest.java) | Mode server không fallback; thay một adapter giữa các mock hoặc null bị từ chối và giữ dịch vụ cũ. | ServiceRegistry; proxy AuctionClientService | Kiểm thử unit |
| [src/test/java/com/auction/client/ui/ProductImagesIoTest.java](../src/test/java/com/auction/client/ui/ProductImagesIoTest.java) | Đọc file đã hủy/deadline hết; HTTP cục bộ gửi header rồi giữ body phải hết read timeout. | ProductImages.open; ServerSocket loopback; latch | Kiểm thử I/O cục bộ |
| [src/test/java/com/auction/client/ui/ProductImagesTest.java](../src/test/java/com/auction/client/ui/ProductImagesTest.java) | Import đúng thư mục ứng dụng và nội dung; đường dẫn không tồn tại trả fallback null. | ProductImages; thư mục tạm JUnit | Kiểm thử unit |
| [src/test/java/com/auction/client/util/AuctionTimeTest.java](../src/test/java/com/auction/client/util/AuctionTimeTest.java) | Giờ HH:mm hợp lệ/không hợp lệ, lịch phiên và mô tả thời lượng. | AuctionTime | Kiểm thử unit |
| [src/test/java/com/auction/client/util/FormatUtilTest.java](../src/test/java/com/auction/client/util/FormatUtilTest.java) | Hiển thị long vượt 2^53 và Long.MAX_VALUE đúng từng chữ số. | FormatUtil | Kiểm thử unit |
| [src/test/java/com/auction/client/ui/UiResourcesTest.java](../src/test/java/com/auction/client/ui/UiResourcesTest.java) | Tài nguyên trang trí đã bỏ không có trên classpath; giữ hỗ trợ ảnh sản phẩm và CSS/FXML. | ClassLoader; ProductImages | Kiểm thử resource |
| [src/test/java/com/auction/client/util/ValidatorsTest.java](../src/test/java/com/auction/client/util/ValidatorsTest.java) | Các trường hợp username/email/mật khẩu, chuẩn hóa và nhập lại theo ràng buộc hiện có. | Validators | Kiểm thử unit |

## FXML, CSS và resource

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [src/main/resources/com/auction/client/dev/uikit-view.fxml](../src/main/resources/com/auction/client/dev/uikit-view.fxml) | Trang mẫu button, form, badge/toast, icon và thành phần design system. | UiKitController; app.css | Công cụ dev |
| [src/main/resources/com/auction/client/views/admin-view.fxml](../src/main/resources/com/auction/client/views/admin-view.fxml) | Ba pane tổng quan/người dùng/phiên, bảng và bộ lọc; hiển thị progress/lỗi. | AdminController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/views/auction-room-view.fxml](../src/main/resources/com/auction/client/views/auction-room-view.fxml) | Thông tin sản phẩm, nhập giá, nhãn trạng thái lệnh, auto-bid, chart/lịch sử và nút Tải lại. | AuctionRoomController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/views/auth-view.fxml](../src/main/resources/com/auction/client/views/auth-view.fxml) | Form đăng nhập/đăng ký, chọn vai trò khi đăng ký, mật khẩu và cuộn trên cửa sổ nhỏ. | AuthController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/views/contact-view.fxml](../src/main/resources/com/auction/client/views/contact-view.fxml) | Khung trang liên hệ; thẻ thành viên được dựng từ properties. | ContactController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/views/guide-view.fxml](../src/main/resources/com/auction/client/views/guide-view.fxml) | Khung trang hướng dẫn/cuộn; các mục được GuideController dựng khi initialize. | GuideController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/views/home-view.fxml](../src/main/resources/com/auction/client/views/home-view.fxml) | Trang tìm kiếm/danh mục, hai lưới phiên RUNNING/OPEN và CTA người bán. | HomeController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/views/seller-products-view.fxml](../src/main/resources/com/auction/client/views/seller-products-view.fxml) | Bộ lọc và bảng sản phẩm người bán; controller dựng hộp thoại thêm/sửa/xóa. | SellerProductsController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/views/wallet-view.fxml](../src/main/resources/com/auction/client/views/wallet-view.fxml) | Bộ lọc, bảng giao dịch và các nút nạp/rút; controller dựng thẻ thống kê và dialog. | WalletController; app.css | Đang dùng |
| [src/main/resources/com/auction/client/css/app.css](../src/main/resources/com/auction/client/css/app.css) | Looked-up color, typography, button, bảng/card, layout, trạng thái và chart dùng chung. | ViewNavigator.applyAppStylesheet; tất cả FXML và UI kit | Đang dùng |
| [src/main/resources/com/auction/client/contacts.properties](../src/main/resources/com/auction/client/contacts.properties) | Tên/email của bốn thành viên được nạp bằng UTF-8. | ContactController | Đang dùng |

## Cấu hình và scripts

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [pom.xml](../pom.xml) | Java release 25, JavaFX/JUnit và plugin compiler/surefire/javafx; chuyển auction.mode và lựa chọn main.class. | Maven; Main; SnapshotApp; test | Cấu hình đang dùng |
| [src/main/java/module-info.java](../src/main/java/module-info.java) | Khai báo module JavaFX/Swing/JDK; mở package controller/UI cho FXML, dev cho JavaFX launcher. | FXMLLoader; Main; SnapshotApp | Cấu hình đang dùng |
| [scripts/check-encoding.py](../scripts/check-encoding.py) | Quét UTF-8/dấu hỏi hỏng trong source, test, docs và README; trả mã lỗi nếu phát hiện. | src/; docs/; README.md | Script kiểm tra |
| [scripts/check-contrast.py](../scripts/check-contrast.py) | Đo 30 cặp màu chữ/viền/focus thực tế bằng công thức WCAG; trả mã lỗi nếu dưới ngưỡng. | app.css; DESIGN.md | Script kiểm tra |
| [scripts/check-docs.py](../scripts/check-docs.py) | Đối chiếu đủ file code/config trong CODE_MAP và kiểm tra liên kết Markdown cục bộ/fenced block. Không thay thế trình render Mermaid. | CODE_MAP; README; CLIENT_HANDOVER; VALIDATION; UI_REFRESH; DESIGN; IMAGE_CREDITS | Script kiểm tra |

## Tài liệu và ảnh tham khảo

| File | Chức năng cụ thể | Liên kết chính | Tình trạng |
|---|---|---|---|
| [README.md](../README.md) | Điểm bắt đầu: kiến trúc, cây file, sơ đồ Mermaid, lệnh chạy/test, tài khoản và giới hạn backend. | CODE_MAP; CLIENT_HANDOVER; VALIDATION | Tài liệu hiện hành |
| [docs/CODE_MAP.md](../docs/CODE_MAP.md) | Bảng chức năng và liên kết của toàn bộ file Java/test, FXML/CSS, resource và cấu hình. | Source thực tế; check-docs.py | Tài liệu hiện hành |
| [docs/CLIENT_HANDOVER.md](../docs/CLIENT_HANDOVER.md) | Đối chiếu model/interface với backend echo; trách nhiệm thành viên 1/2/4 và lịch sử thay đổi trước. | auction-server chỉ đọc; README | Tài liệu bàn giao |
| [docs/UI_REFRESH.md](../docs/UI_REFRESH.md) | File sửa/xóa, số dòng trước/sau, ảnh mới và ghi chú vấn đáp cho đợt chỉnh giao diện. | Source; target/ui-refresh | Báo cáo đợt mới |
| [docs/VALIDATION.md](../docs/VALIDATION.md) | Kết quả kiểm thử từng đợt, lỗi quyền Maven và giới hạn; tách kết quả mới khỏi lịch sử. | Maven/Surefire; scripts kiểm tra | Tài liệu kết quả |
| [docs/DEV_ACCOUNTS.md](../docs/DEV_ACCOUNTS.md) | Ba tài khoản mẫu bidder/seller/admin và mật khẩu mock. | MockAuctionClientService | Tài liệu mock |
| [docs/IMAGE_CREDITS.md](../docs/IMAGE_CREDITS.md) | Ghi chú nguồn tài nguyên và ảnh tham khảo được giữ lại. | ProductImages; docs/ui/screenshots | Tài liệu resource |
| [docs/ui/DESIGN.md](../docs/ui/DESIGN.md) | Bảng màu và nguyên tắc thành phần của UI hiện tại. | app.css; Icons; FXML | Tài liệu thiết kế |
| [docs/ui/DESIGN-apple.md](../docs/ui/DESIGN-apple.md) | Đặc tả tham khảo thiết kế Apple; không phải danh sách lớp/API đã triển khai. | DESIGN.md; app.css để kiểm tra hiện trạng | Tài liệu tham khảo |
| [docs/ui/PROMPT_FIX_UI_1.md](../docs/ui/PROMPT_FIX_UI_1.md) | Yêu cầu chỉnh UI được lưu từ trước; dùng làm bối cảnh, không phải giao thức backend. | docs/ui; source hiện tại | Tài liệu lịch sử |

Các ảnh sau là ảnh tham khảo có sẵn trong `docs/ui/screenshots/`; không phải báo cáo kiểm thử mới và không bị ghi đè bởi công cụ chụp ảnh hiện tại:

- [01_auth_login.png](../docs/ui/screenshots/01_auth_login.png)
- [02_auth_register.png](../docs/ui/screenshots/02_auth_register.png)
- [03_dashboard.png](../docs/ui/screenshots/03_dashboard.png)
- [04_realtime_bidding_running.png](../docs/ui/screenshots/04_realtime_bidding_running.png)
- [05_realtime_bidding_finished.png](../docs/ui/screenshots/05_realtime_bidding_finished.png)
- [06_product_management.png](../docs/ui/screenshots/06_product_management.png)
- [07_uikit_showcase.png](../docs/ui/screenshots/07_uikit_showcase.png)
- [08_icon_gallery.png](../docs/ui/screenshots/08_icon_gallery.png)

Ảnh chụp mới của đợt giao diện được giữ trong `docs/ui/refresh-2026-11-03/wide/`, `narrow/` và hai ảnh QA đi kèm; xem bảng liên kết tại [UI_REFRESH.md](UI_REFRESH.md). Các thư mục ảnh tham khảo cũ ở trên vẫn giữ nguyên.
