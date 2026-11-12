# Báo cáo dọn dẹp và kiểm chứng dự án

Mốc trước `2026-11-12T16:14:07.192913+07:00`. Mốc sau `2026-11-12T16:41:58+07:00`. UTC+7, 12/11/2026.

Phạm vi là working tree thực tế ở `D:/Bai-Tap-Lon-gon`, HEAD `db199b48fdfbceb192c4faa21c16a20376f81dac`. Mốc trước lấy từ checkpoint trước khi sửa trong lần này, bao gồm thay đổi chưa commit và file mới. Các số liệu năm 2026 trong báo cáo lịch sử có mốc riêng.

Quy ước Git trong bảng là `tracked`, `untracked`, `ignored`, lấy từ index và danh sách working tree. Một file tracked vẫn có thể đang modified. Việc chuyển file untracked chỉ thể hiện qua manifest trước/sau vì Git chưa có bản index để nhận diện rename.

Phạm vi file dự án gồm mã nguồn, tài nguyên, cấu hình và tài liệu do nhóm duy trì. Phạm vi filesystem quan sát còn gồm `target/`, IDE, hook phát triển và `data/`. `.git/` được ghi nhận tại ranh giới, `.maven-repository/` được ghi nhận là cache tải về. Hai cây này được loại khỏi thống kê file chi tiết và LOC. ZIP backup và JAR chỉ được ghi metadata, JAR ứng dụng cuối được kiểm tra danh sách entry/manifest để xác nhận packaging. Nội dung dữ liệu người dùng, password và token được lược khỏi báo cáo.

## Checkpoint và bảo toàn trạng thái ban đầu

Checkpoint tại `C:/Users/MAIDUC~1/AppData/Local/Temp/auction-project-audit-20261112` ngoài repository, trước khi sửa. `working-tree-before.zip` chứa 140 file tracked/untracked hiện hữu gồm source, config, script, tài liệu và PDF. ZIP đã được kiểm CRC. `working-tree-before.patch` giữ diff binary so với HEAD. `before.json` ghi status, HEAD, manifest file/kích thước/hash nguồn và số liệu trước. Thư mục còn có script/AST/analysis và log kiểm chứng của lần này.

Khôi phục mã bằng cách giải nén ZIP vào thư mục riêng và đối chiếu từng file với thay đổi hiện tại. Các file untracked từ trước cũng nằm trong ZIP. Checkpoint nằm ở thư mục tạm của hệ điều hành nên tính bền vững phụ thuộc việc giữ thư mục này. ZIP này chỉ chứa file được duy trì, dữ liệu trong data và cấu hình IDE nằm ngoài ZIP. Data/backup hiện hữu được giữ tại vị trí gốc.

Working tree ban đầu có 14 file tracked modified và 23 file untracked hiện hữu. Có tổng 117 file tracked hiện hữu trước dọn. Các thay đổi đó thuộc trạng thái bắt đầu. So sánh thao tác lần này dùng bytes checkpoint và manifest, tách khỏi diff tổng so HEAD. Báo cáo không in giá trị môi trường, nội dung JSON user, password hay token.

## Danh sách quyết định trước khi thực hiện

| Nhóm/file | Bằng chứng đọc được | Đề xuất và mức rủi ro |
| --- | --- | --- |
| Ba báo cáo 11/11 | JSON_STORAGE_SAFETY_REVIEW và MULTI_CLIENT_TEST_RESULTS có nhãn lịch sử và lệnh MySQL cũ. JSON_ONLY_REFACTOR có checkpoint/số test cũ và phần quality đã được mốc 12/11 thay thế. | Chuyển archive, giữ nội dung/bằng chứng và sửa relative links. Rủi ro thấp, kiểm link sau chuyển. |
| Java/test/FXML/CSS | Có caller/implementation/FXML/route/discovery và chức năng ở bảng kiểm kê. | Giữ, bỏ file sẽ ảnh hưởng hành vi hoặc bằng chứng test. |
| VAN_DAP và bộ viva | Giao chủ đề nhưng có định dạng ôn nhanh/chuyên đề/hỏi đáp/demo khác nhau. | REVIEW việc gộp, giữ trong lần này vì còn giá trị vấn đáp. |
| PDF giảng viên | Đề bài 10 trang và hướng dẫn 7 trang, mục đích khác nhau. | Giữ cả hai. |
| IDE/modernize | Thiết lập cục bộ và hook extension có thể được gọi động. | REVIEW, giữ vì chưa chứng minh vô dụng cho workflow phát triển. |
| Maven target | Output compile/test/package/report tái sinh theo lifecycle. | Chạy clean rồi verify để có coverage/test report của một lượt mới. Output cũ bị xóa, được liệt kê từng file bên dưới. |
| Data/cache/Git | Data thật/backup và thư viện cục bộ tồn tại. | Giữ các vị trí này trong lần dọn. |

## File đã chuyển

| Đường dẫn trước | Đường dẫn sau | Lý do | Thay đổi nội dung | An toàn |
| --- | --- | --- | --- | --- |
| `docs/JSON_ONLY_REFACTOR.md` | `docs/archive/JSON_ONLY_REFACTOR.md` | Lưu mốc refactor/kiểm thử lịch sử vào docs/archive. | Giữ thông tin lịch sử, chỉ sửa link Markdown tương đối nếu cần. | Thấp, checkpoint nguồn tồn tại và link được kiểm. |
| `docs/JSON_STORAGE_SAFETY_REVIEW.md` | `docs/archive/JSON_STORAGE_SAFETY_REVIEW.md` | Lưu mốc refactor/kiểm thử lịch sử vào docs/archive. | Giữ thông tin lịch sử, chỉ sửa link Markdown tương đối nếu cần. | Thấp, checkpoint nguồn tồn tại và link được kiểm. |
| `docs/MULTI_CLIENT_TEST_RESULTS.md` | `docs/archive/MULTI_CLIENT_TEST_RESULTS.md` | Lưu mốc refactor/kiểm thử lịch sử vào docs/archive. | Giữ thông tin lịch sử, chỉ sửa link Markdown tương đối nếu cần. | Thấp, checkpoint nguồn tồn tại và link được kiểm. |

## File đã xóa hoặc gộp thuộc mã/tài liệu

Số file nguồn, test, tài nguyên hoặc tài liệu bị loại bỏ là 0. Số tài liệu gộp là 0. Các bản có giá trị lịch sử được chuyển nguyên file theo danh sách trên. Ba tài liệu chuyển đều untracked từ trước. File MySQL/SQL được nhắc trong tài liệu cũ đã vắng mặt ở mốc bắt đầu, chúng nằm ngoài danh sách thao tác xóa của lần này.

## File có bytes thay đổi hoặc được thêm trong lần này

| File sau | Thao tác | Nội dung |
| --- | --- | --- |
| `MULTI_CLIENT_SETUP.md` | Cập nhật tài liệu | Cập nhật đích docs/archive hoặc relative link tới PDF, giữ lịch sử nội dung. |
| `README.md` | Cập nhật tài liệu | Sửa link archive và thêm đường dẫn tới đủ ba báo cáo kiểm kê hiện tại. |
| `docs/CLEANUP_REPORT.md` | Thêm | Báo cáo bắt buộc từ yêu cầu, có số liệu và kiểm chứng độc lập. |
| `docs/CODE_STATISTICS.md` | Thêm | Báo cáo bắt buộc từ yêu cầu, có số liệu và kiểm chứng độc lập. |
| `docs/PROJECT_FILE_INVENTORY.md` | Thêm | Báo cáo bắt buộc từ yêu cầu, có số liệu và kiểm chứng độc lập. |
| `docs/archive/JSON_ONLY_REFACTOR.md` | Chuyển, bytes giữ nguyên | Tên/vị trí được tổ chức lại, nội dung vẫn khớp checkpoint. |
| `docs/archive/JSON_STORAGE_SAFETY_REVIEW.md` | Chuyển + sửa link | Cập nhật đích docs/archive hoặc relative link tới PDF, giữ lịch sử nội dung. |
| `docs/archive/MULTI_CLIENT_TEST_RESULTS.md` | Chuyển, bytes giữ nguyên | Tên/vị trí được tổ chức lại, nội dung vẫn khớp checkpoint. |
| `docs/viva/AUDIT_AND_RESULTS.md` | Cập nhật tài liệu | Thêm ghi chú phân biệt lượt 15:58 với lượt kiểm kê, raw log cũ đã được clean xóa, liên kết kết quả/log hiện tại. Giữ số liệu lịch sử. |

## Trước/sau

| Nhóm | File trước → sau | Dòng vật lý trước → sau | Code trước → sau | Comment-only trước → sau | Trống trước → sau |
| --- | --- | --- | --- | --- | --- |
| Java production | 79 → 79 | 11394 → 11394 | 10083 → 10083 | 275 → 275 | 1036 → 1036 |
| Java test | 27 → 27 | 3744 → 3744 | 3452 → 3452 | 10 → 10 | 282 → 282 |
| FXML | 8 → 8 | 695 → 695 | 642 → 642 | 21 → 21 | 32 → 32 |
| CSS | 1 → 1 | 245 → 245 | 245 → 245 | 0 → 0 | 0 → 0 |
| XML/POM | 2 → 2 | 160 → 160 | 158 → 158 | 2 → 2 | 0 → 0 |
| PowerShell | 1 → 1 | 36 → 36 | 36 → 36 | 0 → 0 | 0 → 0 |
| CMD | 2 → 2 | 12 → 12 | 12 → 12 | 0 → 0 | 0 → 0 |
| YAML | 1 → 1 | 25 → 25 | 25 → 25 | 0 → 0 | 0 → 0 |
| Properties | 1 → 1 | 8 → 8 | 8 → 8 | 0 → 0 | 0 → 0 |
| Git config | 2 → 2 | 19 → 19 | 19 → 19 | 0 → 0 | 0 → 0 |
| Java tổng | 106 → 106 | 15138 → 15138 | 13535 → 13535 | 285 → 285 | 1318 → 1318 |
| Giao diện tổng | 9 → 9 | 940 → 940 | 887 → 887 | 21 → 21 | 32 → 32 |
| Cấu hình/script tổng | 9 → 9 | 260 → 260 | 258 → 258 | 2 → 2 | 0 → 0 |
| Tổng nguồn/config/UI | 124 → 124 | 16338 → 16338 | 14680 → 14680 | 308 → 308 | 1350 → 1350 |

| Quy mô | Trước | Sau | Chênh lệch |
| --- | --- | --- | --- |
| File dự án được duy trì | 140 | 143 | 3 |
| File filesystem quan sát | 685 | 689 | 4 |
| Java main/test | 79 / 27 | 79 / 27 | 0 |
| Báo cáo lịch sử nằm trực tiếp docs/ | 3 | 0 | Chuyển đủ 3 vào archive |

Nguồn/config/UI giữ đủ 124 file và 16.338 dòng vật lý. Code loại comment/trống là 14.680 dòng. Java giữ 15.138 dòng vật lý, 13.535 dòng code. Mức giảm Java LOC là 0%, phần dọn nằm ở tổ chức tài liệu. Chưa đo giảm cyclomatic complexity.

## Danh sách đầy đủ output build bị Maven clean xóa

Bước clean xóa output cũ trong target để đo một lượt coverage mới. Mốc trước có 528 file target, bảng dưới ghi đủ 528 đường dẫn cùng trạng thái sau verify. File có cùng path sau build là output mới được tái tạo. Bytecode/ảnh/report được phân loại output, các bản source trong target chỉ là bản copy hoặc HTML phục vụ report.

Lượt clean đầu dùng log redirect trong target nên Windows giữ handle trên target/inventory-verify.log và clean thất bại. Đã sao chép log lỗi ra thư mục checkpoint, chạy clean lần nữa thành công rồi verify với log ngoài target. Bản log lỗi được giữ lại tại target/inventory-clean-attempt-1.log, log clean cuối tại target/inventory-clean-final.log và verify cuối tại target/inventory-verify.log. File log tạm target/inventory-verify.log phát sinh sau mốc trước cũng đã bị clean lần hai xóa rồi được thay bằng log verify cuối.

Raw log cũ target/viva-final-verify.log và các report build cũ đã bị clean xóa. Checkpoint nguồn không chứa target nên raw log cũ không được sao lưu trong checkpoint lần này. Kết quả lịch sử trong docs/viva/AUDIT_AND_RESULTS.md được giữ và báo cáo mới có log/XML của lượt hiện tại. Đây là giới hạn bảo tồn bằng chứng build cũ của lần dọn này.

| STT | Đường dẫn output trước clean | Byte trước | Trạng thái sau verify |
| --- | --- | --- | --- |
| 1 | `target/checkstyle-cachefile` | 11124 | Đã tái tạo tại cùng path |
| 2 | `target/checkstyle-checker.xml` | 714 | Đã tái tạo tại cùng path |
| 3 | `target/checkstyle-result.xml` | 11001 | Đã tái tạo tại cùng path |
| 4 | `target/digital-auction-1.0.0.jar` | 10456772 | Đã tái tạo tại cùng path |
| 5 | `target/generate_viva_map.py` | 6210 | Đã xóa, hiện không tái tạo |
| 6 | `target/jacoco.exec` | 1108338 | Đã tái tạo tại cùng path |
| 7 | `target/original-digital-auction-1.0.0.jar` | 330020 | Đã tái tạo tại cùng path |
| 8 | `target/viva-final-verify.log` | 91110 | Đã xóa, hiện không tái tạo |
| 9 | `target/classes/com/auction/client/App.class` | 437 | Đã tái tạo tại cùng path |
| 10 | `target/classes/com/auction/client/Main.class` | 5007 | Đã tái tạo tại cùng path |
| 11 | `target/classes/com/auction/client/contacts.properties` | 284 | Đã tái tạo tại cùng path |
| 12 | `target/classes/com/auction/client/controller/AdminController$1.class` | 8345 | Đã tái tạo tại cùng path |
| 13 | `target/classes/com/auction/client/controller/AdminController$2.class` | 8972 | Đã tái tạo tại cùng path |
| 14 | `target/classes/com/auction/client/controller/AdminController$Summary.class` | 2229 | Đã tái tạo tại cùng path |
| 15 | `target/classes/com/auction/client/controller/AdminController.class` | 25184 | Đã tái tạo tại cùng path |
| 16 | `target/classes/com/auction/client/controller/AuctionRoomController$1.class` | 2329 | Đã tái tạo tại cùng path |
| 17 | `target/classes/com/auction/client/controller/AuctionRoomController$2.class` | 1699 | Đã tái tạo tại cùng path |
| 18 | `target/classes/com/auction/client/controller/AuctionRoomController$3.class` | 1046 | Đã tái tạo tại cùng path |
| 19 | `target/classes/com/auction/client/controller/AuctionRoomController$RoomData.class` | 2695 | Đã tái tạo tại cùng path |
| 20 | `target/classes/com/auction/client/controller/AuctionRoomController.class` | 47448 | Đã tái tạo tại cùng path |
| 21 | `target/classes/com/auction/client/controller/AuthController.class` | 20784 | Đã tái tạo tại cùng path |
| 22 | `target/classes/com/auction/client/controller/ContactController$TeamMember.class` | 630 | Đã tái tạo tại cùng path |
| 23 | `target/classes/com/auction/client/controller/ContactController.class` | 14496 | Đã tái tạo tại cùng path |
| 24 | `target/classes/com/auction/client/controller/GuideController.class` | 13210 | Đã tái tạo tại cùng path |
| 25 | `target/classes/com/auction/client/controller/HomeController$CountdownEntry.class` | 706 | Đã tái tạo tại cùng path |
| 26 | `target/classes/com/auction/client/controller/HomeController.class` | 30886 | Đã tái tạo tại cùng path |
| 27 | `target/classes/com/auction/client/controller/SellerProductsController$1.class` | 5207 | Đã tái tạo tại cùng path |
| 28 | `target/classes/com/auction/client/controller/SellerProductsController$2.class` | 2338 | Đã tái tạo tại cùng path |
| 29 | `target/classes/com/auction/client/controller/SellerProductsController$3.class` | 6930 | Đã tái tạo tại cùng path |
| 30 | `target/classes/com/auction/client/controller/SellerProductsController$4.class` | 2080 | Đã tái tạo tại cùng path |
| 31 | `target/classes/com/auction/client/controller/SellerProductsController$Products.class` | 2324 | Đã tái tạo tại cùng path |
| 32 | `target/classes/com/auction/client/controller/SellerProductsController.class` | 43713 | Đã tái tạo tại cùng path |
| 33 | `target/classes/com/auction/client/controller/WalletController$1.class` | 1613 | Đã tái tạo tại cùng path |
| 34 | `target/classes/com/auction/client/controller/WalletController$2.class` | 1676 | Đã tái tạo tại cùng path |
| 35 | `target/classes/com/auction/client/controller/WalletController.class` | 25181 | Đã tái tạo tại cùng path |
| 36 | `target/classes/com/auction/client/css/app.css` | 23394 | Đã tái tạo tại cùng path |
| 37 | `target/classes/com/auction/client/model/AdminUser.class` | 948 | Đã tái tạo tại cùng path |
| 38 | `target/classes/com/auction/client/model/Auction.class` | 5660 | Đã tái tạo tại cùng path |
| 39 | `target/classes/com/auction/client/model/AuctionStatus.class` | 1731 | Đã tái tạo tại cùng path |
| 40 | `target/classes/com/auction/client/model/AutoBid.class` | 2299 | Đã tái tạo tại cùng path |
| 41 | `target/classes/com/auction/client/model/Bid.class` | 3327 | Đã tái tạo tại cùng path |
| 42 | `target/classes/com/auction/client/model/Item.class` | 3088 | Đã tái tạo tại cùng path |
| 43 | `target/classes/com/auction/client/model/ItemCategory.class` | 2999 | Đã tái tạo tại cùng path |
| 44 | `target/classes/com/auction/client/model/Transaction.class` | 2785 | Đã tái tạo tại cùng path |
| 45 | `target/classes/com/auction/client/model/TransactionType.class` | 1942 | Đã tái tạo tại cùng path |
| 46 | `target/classes/com/auction/client/model/User.class` | 2740 | Đã tái tạo tại cùng path |
| 47 | `target/classes/com/auction/client/model/UserRole.class` | 1551 | Đã tái tạo tại cùng path |
| 48 | `target/classes/com/auction/client/navigation/DisposableView.class` | 163 | Đã tái tạo tại cùng path |
| 49 | `target/classes/com/auction/client/navigation/ViewNavigator$Route.class` | 2513 | Đã tái tạo tại cùng path |
| 50 | `target/classes/com/auction/client/navigation/ViewNavigator$View.class` | 663 | Đã tái tạo tại cùng path |
| 51 | `target/classes/com/auction/client/navigation/ViewNavigator.class` | 9971 | Đã tái tạo tại cùng path |
| 52 | `target/classes/com/auction/client/navigation/ViewTasks$NotSubmittedException.class` | 764 | Đã tái tạo tại cùng path |
| 53 | `target/classes/com/auction/client/navigation/ViewTasks.class` | 9048 | Đã tái tạo tại cùng path |
| 54 | `target/classes/com/auction/client/service/AuctionAdminService.class` | 381 | Đã tái tạo tại cùng path |
| 55 | `target/classes/com/auction/client/service/AuctionClientService.class` | 2586 | Đã tái tạo tại cùng path |
| 56 | `target/classes/com/auction/client/service/AuctionRoomSnapshot.class` | 2123 | Đã tái tạo tại cùng path |
| 57 | `target/classes/com/auction/client/service/AutoBidException.class` | 401 | Đã tái tạo tại cùng path |
| 58 | `target/classes/com/auction/client/service/AutoBidService.class` | 349 | Đã tái tạo tại cùng path |
| 59 | `target/classes/com/auction/client/service/BidResult.class` | 1327 | Đã tái tạo tại cùng path |
| 60 | `target/classes/com/auction/client/service/BidSubmissionTracker$Entry.class` | 691 | Đã tái tạo tại cùng path |
| 61 | `target/classes/com/auction/client/service/BidSubmissionTracker$Key.class` | 1754 | Đã tái tạo tại cùng path |
| 62 | `target/classes/com/auction/client/service/BidSubmissionTracker$Phase.class` | 1480 | Đã tái tạo tại cùng path |
| 63 | `target/classes/com/auction/client/service/BidSubmissionTracker$ReadStamp.class` | 2343 | Đã tái tạo tại cùng path |
| 64 | `target/classes/com/auction/client/service/BidSubmissionTracker$Ticket.class` | 1950 | Đã tái tạo tại cùng path |
| 65 | `target/classes/com/auction/client/service/BidSubmissionTracker.class` | 5003 | Đã tái tạo tại cùng path |
| 66 | `target/classes/com/auction/client/service/ProductService.class` | 606 | Đã tái tạo tại cùng path |
| 67 | `target/classes/com/auction/client/service/ServiceRegistry$Services.class` | 4059 | Đã tái tạo tại cùng path |
| 68 | `target/classes/com/auction/client/service/ServiceRegistry.class` | 6407 | Đã tái tạo tại cùng path |
| 69 | `target/classes/com/auction/client/service/UserAdminService.class` | 481 | Đã tái tạo tại cùng path |
| 70 | `target/classes/com/auction/client/service/WalletException.class` | 398 | Đã tái tạo tại cùng path |
| 71 | `target/classes/com/auction/client/service/WalletService.class` | 915 | Đã tái tạo tại cùng path |
| 72 | `target/classes/com/auction/client/service/WalletSnapshot.class` | 2257 | Đã tái tạo tại cùng path |
| 73 | `target/classes/com/auction/client/service/mock/AutoBidEngine.class` | 6438 | Đã tái tạo tại cùng path |
| 74 | `target/classes/com/auction/client/service/mock/MockAuctionClientService.class` | 17823 | Đã tái tạo tại cùng path |
| 75 | `target/classes/com/auction/client/service/mock/MockAutoBidService.class` | 7316 | Đã tái tạo tại cùng path |
| 76 | `target/classes/com/auction/client/service/mock/MockDataStore$Hold.class` | 1728 | Đã tái tạo tại cùng path |
| 77 | `target/classes/com/auction/client/service/mock/MockDataStore.class` | 18079 | Đã tái tạo tại cùng path |
| 78 | `target/classes/com/auction/client/service/mock/MockManagementService.class` | 9599 | Đã tái tạo tại cùng path |
| 79 | `target/classes/com/auction/client/service/mock/MockWalletService.class` | 3017 | Đã tái tạo tại cùng path |
| 80 | `target/classes/com/auction/client/service/server/JsonTransport$Connection.class` | 2630 | Đã tái tạo tại cùng path |
| 81 | `target/classes/com/auction/client/service/server/JsonTransport.class` | 8842 | Đã tái tạo tại cùng path |
| 82 | `target/classes/com/auction/client/service/server/ServerError.class` | 2727 | Đã tái tạo tại cùng path |
| 83 | `target/classes/com/auction/client/service/server/ServerMapper.class` | 4831 | Đã tái tạo tại cùng path |
| 84 | `target/classes/com/auction/client/service/server/ServerServices$CommandLock.class` | 2276 | Đã tái tạo tại cùng path |
| 85 | `target/classes/com/auction/client/service/server/ServerServices.class` | 33202 | Đã tái tạo tại cùng path |
| 86 | `target/classes/com/auction/client/ui/AppFooter.class` | 7067 | Đã tái tạo tại cùng path |
| 87 | `target/classes/com/auction/client/ui/AppHeader.class` | 10229 | Đã tái tạo tại cùng path |
| 88 | `target/classes/com/auction/client/ui/Icons$1.class` | 1136 | Đã tái tạo tại cùng path |
| 89 | `target/classes/com/auction/client/ui/Icons.class` | 6933 | Đã tái tạo tại cùng path |
| 90 | `target/classes/com/auction/client/ui/PasswordFieldWithToggle.class` | 3555 | Đã tái tạo tại cùng path |
| 91 | `target/classes/com/auction/client/ui/ProductImages$1.class` | 1595 | Đã tái tạo tại cùng path |
| 92 | `target/classes/com/auction/client/ui/ProductImages.class` | 6635 | Đã tái tạo tại cùng path |
| 93 | `target/classes/com/auction/client/ui/RoundedClip.class` | 1041 | Đã tái tạo tại cùng path |
| 94 | `target/classes/com/auction/client/ui/SmoothScroll.class` | 2667 | Đã tái tạo tại cùng path |
| 95 | `target/classes/com/auction/client/ui/UserMenu.class` | 17491 | Đã tái tạo tại cùng path |
| 96 | `target/classes/com/auction/client/util/AppInfo.class` | 375 | Đã tái tạo tại cùng path |
| 97 | `target/classes/com/auction/client/util/AuctionTime.class` | 3158 | Đã tái tạo tại cùng path |
| 98 | `target/classes/com/auction/client/util/FormatUtil.class` | 3217 | Đã tái tạo tại cùng path |
| 99 | `target/classes/com/auction/client/util/Validators.class` | 3822 | Đã tái tạo tại cùng path |
| 100 | `target/classes/com/auction/client/views/admin-view.fxml` | 8003 | Đã tái tạo tại cùng path |
| 101 | `target/classes/com/auction/client/views/auction-room-view.fxml` | 13233 | Đã tái tạo tại cùng path |
| 102 | `target/classes/com/auction/client/views/auth-view.fxml` | 7305 | Đã tái tạo tại cùng path |
| 103 | `target/classes/com/auction/client/views/contact-view.fxml` | 1871 | Đã tái tạo tại cùng path |
| 104 | `target/classes/com/auction/client/views/guide-view.fxml` | 1877 | Đã tái tạo tại cùng path |
| 105 | `target/classes/com/auction/client/views/home-view.fxml` | 8207 | Đã tái tạo tại cùng path |
| 106 | `target/classes/com/auction/client/views/seller-products-view.fxml` | 2730 | Đã tái tạo tại cùng path |
| 107 | `target/classes/com/auction/client/views/wallet-view.fxml` | 2452 | Đã tái tạo tại cùng path |
| 108 | `target/classes/com/auction/server/dao/AuctionJsonMapper.class` | 1875 | Đã tái tạo tại cùng path |
| 109 | `target/classes/com/auction/server/dao/AuctionRepository.class` | 5091 | Đã tái tạo tại cùng path |
| 110 | `target/classes/com/auction/server/exception/ApiException.class` | 563 | Đã tái tạo tại cùng path |
| 111 | `target/classes/com/auction/server/model/Admin.class` | 549 | Đã tái tạo tại cùng path |
| 112 | `target/classes/com/auction/server/model/Art.class` | 630 | Đã tái tạo tại cùng path |
| 113 | `target/classes/com/auction/server/model/Auction.class` | 3398 | Đã tái tạo tại cùng path |
| 114 | `target/classes/com/auction/server/model/BidTransaction.class` | 2248 | Đã tái tạo tại cùng path |
| 115 | `target/classes/com/auction/server/model/Bidder.class` | 553 | Đã tái tạo tại cùng path |
| 116 | `target/classes/com/auction/server/model/Electronics.class` | 662 | Đã tái tạo tại cùng path |
| 117 | `target/classes/com/auction/server/model/Entity.class` | 609 | Đã tái tạo tại cùng path |
| 118 | `target/classes/com/auction/server/model/Item.class` | 858 | Đã tái tạo tại cùng path |
| 119 | `target/classes/com/auction/server/model/ItemFactory$1.class` | 1765 | Đã tái tạo tại cùng path |
| 120 | `target/classes/com/auction/server/model/ItemFactory$CategoryItem.class` | 939 | Đã tái tạo tại cùng path |
| 121 | `target/classes/com/auction/server/model/ItemFactory.class` | 1254 | Đã tái tạo tại cùng path |
| 122 | `target/classes/com/auction/server/model/Seller.class` | 553 | Đã tái tạo tại cùng path |
| 123 | `target/classes/com/auction/server/model/User.class` | 633 | Đã tái tạo tại cùng path |
| 124 | `target/classes/com/auction/server/model/Vehicle.class` | 646 | Đã tái tạo tại cùng path |
| 125 | `target/classes/com/auction/server/network/Server$Client.class` | 7703 | Đã tái tạo tại cùng path |
| 126 | `target/classes/com/auction/server/network/Server.class` | 9408 | Đã tái tạo tại cùng path |
| 127 | `target/classes/com/auction/server/service/AntiSnipingPolicy.class` | 2055 | Đã tái tạo tại cùng path |
| 128 | `target/classes/com/auction/server/service/AuctionService$Session.class` | 1775 | Đã tái tạo tại cùng path |
| 129 | `target/classes/com/auction/server/service/AuctionService.class` | 41488 | Đã tái tạo tại cùng path |
| 130 | `target/classes/com/auction/server/service/Passwords.class` | 2829 | Đã tái tạo tại cùng path |
| 131 | `target/classes/com/auction/shared/JsonProtocol.class` | 4302 | Đã tái tạo tại cùng path |
| 132 | `target/maven-archiver/pom.properties` | 61 | Đã tái tạo tại cùng path |
| 133 | `target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst` | 5588 | Đã tái tạo tại cùng path |
| 134 | `target/maven-status/maven-compiler-plugin/compile/default-compile/dependencies.lst` | 1546 | Đã tái tạo tại cùng path |
| 135 | `target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst` | 6204 | Đã tái tạo tại cùng path |
| 136 | `target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/createdFiles.lst` | 1915 | Đã tái tạo tại cùng path |
| 137 | `target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/dependencies.lst` | 2882 | Đã tái tạo tại cùng path |
| 138 | `target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/inputFiles.lst` | 2276 | Đã tái tạo tại cùng path |
| 139 | `target/qa/admin.png` | 57741 | Đã tái tạo tại cùng path |
| 140 | `target/qa/auth-register-scrolled-640.png` | 50480 | Đã tái tạo tại cùng path |
| 141 | `target/qa/home-eight-running-narrow.png` | 66230 | Đã tái tạo tại cùng path |
| 142 | `target/qa/home.png` | 88343 | Đã tái tạo tại cùng path |
| 143 | `target/qa/products.png` | 32694 | Đã tái tạo tại cùng path |
| 144 | `target/qa/room-narrow.png` | 54424 | Đã tái tạo tại cùng path |
| 145 | `target/qa/room-wide.png` | 91728 | Đã tái tạo tại cùng path |
| 146 | `target/qa/server-admin.png` | 49712 | Đã tái tạo tại cùng path |
| 147 | `target/qa/server-home.png` | 81901 | Đã tái tạo tại cùng path |
| 148 | `target/qa/server-room.png` | 90006 | Đã tái tạo tại cùng path |
| 149 | `target/qa/server-seller.png` | 52470 | Đã tái tạo tại cùng path |
| 150 | `target/qa/server-wallet.png` | 88178 | Đã tái tạo tại cùng path |
| 151 | `target/qa/wallet-long-1200.png` | 92404 | Đã tái tạo tại cùng path |
| 152 | `target/qa/wallet-long-640.png` | 47460 | Đã tái tạo tại cùng path |
| 153 | `target/qa/wallet.png` | 78534 | Đã tái tạo tại cùng path |
| 154 | `target/site/jacoco/index.html` | 13921 | Đã tái tạo tại cùng path |
| 155 | `target/site/jacoco/jacoco-sessions.html` | 356688 | Đã tái tạo tại cùng path |
| 156 | `target/site/jacoco/jacoco.csv` | 9427 | Đã tái tạo tại cùng path |
| 157 | `target/site/jacoco/jacoco.xml` | 664783 | Đã tái tạo tại cùng path |
| 158 | `target/site/jacoco/com.auction.client/App.html` | 3171 | Đã tái tạo tại cùng path |
| 159 | `target/site/jacoco/com.auction.client/App.java.html` | 1617 | Đã tái tạo tại cùng path |
| 160 | `target/site/jacoco/com.auction.client/Main.html` | 5855 | Đã tái tạo tại cùng path |
| 161 | `target/site/jacoco/com.auction.client/Main.java.html` | 7406 | Đã tái tạo tại cùng path |
| 162 | `target/site/jacoco/com.auction.client/index.html` | 3618 | Đã tái tạo tại cùng path |
| 163 | `target/site/jacoco/com.auction.client/index.source.html` | 3627 | Đã tái tạo tại cùng path |
| 164 | `target/site/jacoco/com.auction.client.controller/AdminController$1.html` | 8787 | Đã tái tạo tại cùng path |
| 165 | `target/site/jacoco/com.auction.client.controller/AdminController$2.html` | 8229 | Đã tái tạo tại cùng path |
| 166 | `target/site/jacoco/com.auction.client.controller/AdminController$Summary.html` | 2813 | Đã tái tạo tại cùng path |
| 167 | `target/site/jacoco/com.auction.client.controller/AdminController.html` | 31022 | Đã tái tạo tại cùng path |
| 168 | `target/site/jacoco/com.auction.client.controller/AdminController.java.html` | 42460 | Đã tái tạo tại cùng path |
| 169 | `target/site/jacoco/com.auction.client.controller/AuctionRoomController$1.html` | 4049 | Đã tái tạo tại cùng path |
| 170 | `target/site/jacoco/com.auction.client.controller/AuctionRoomController$2.html` | 4130 | Đã tái tạo tại cùng path |
| 171 | `target/site/jacoco/com.auction.client.controller/AuctionRoomController$RoomData.html` | 2885 | Đã tái tạo tại cùng path |
| 172 | `target/site/jacoco/com.auction.client.controller/AuctionRoomController.html` | 49472 | Đã tái tạo tại cùng path |
| 173 | `target/site/jacoco/com.auction.client.controller/AuctionRoomController.java.html` | 77128 | Đã tái tạo tại cùng path |
| 174 | `target/site/jacoco/com.auction.client.controller/AuthController.html` | 23033 | Đã tái tạo tại cùng path |
| 175 | `target/site/jacoco/com.auction.client.controller/AuthController.java.html` | 28358 | Đã tái tạo tại cùng path |
| 176 | `target/site/jacoco/com.auction.client.controller/ContactController$TeamMember.html` | 2839 | Đã tái tạo tại cùng path |
| 177 | `target/site/jacoco/com.auction.client.controller/ContactController.html` | 11152 | Đã tái tạo tại cùng path |
| 178 | `target/site/jacoco/com.auction.client.controller/ContactController.java.html` | 15246 | Đã tái tạo tại cùng path |
| 179 | `target/site/jacoco/com.auction.client.controller/GuideController.html` | 9357 | Đã tái tạo tại cùng path |
| 180 | `target/site/jacoco/com.auction.client.controller/GuideController.java.html` | 15794 | Đã tái tạo tại cùng path |
| 181 | `target/site/jacoco/com.auction.client.controller/HomeController$CountdownEntry.html` | 2840 | Đã tái tạo tại cùng path |
| 182 | `target/site/jacoco/com.auction.client.controller/HomeController.html` | 28125 | Đã tái tạo tại cùng path |
| 183 | `target/site/jacoco/com.auction.client.controller/HomeController.java.html` | 43732 | Đã tái tạo tại cùng path |
| 184 | `target/site/jacoco/com.auction.client.controller/SellerProductsController$1.html` | 6006 | Đã tái tạo tại cùng path |
| 185 | `target/site/jacoco/com.auction.client.controller/SellerProductsController$2.html` | 3558 | Đã tái tạo tại cùng path |
| 186 | `target/site/jacoco/com.auction.client.controller/SellerProductsController$3.html` | 6383 | Đã tái tạo tại cùng path |
| 187 | `target/site/jacoco/com.auction.client.controller/SellerProductsController$4.html` | 4078 | Đã tái tạo tại cùng path |
| 188 | `target/site/jacoco/com.auction.client.controller/SellerProductsController$Products.html` | 2861 | Đã tái tạo tại cùng path |
| 189 | `target/site/jacoco/com.auction.client.controller/SellerProductsController.html` | 35714 | Đã tái tạo tại cùng path |
| 190 | `target/site/jacoco/com.auction.client.controller/SellerProductsController.java.html` | 58953 | Đã tái tạo tại cùng path |
| 191 | `target/site/jacoco/com.auction.client.controller/WalletController$1.html` | 3516 | Đã tái tạo tại cùng path |
| 192 | `target/site/jacoco/com.auction.client.controller/WalletController$2.html` | 3519 | Đã tái tạo tại cùng path |
| 193 | `target/site/jacoco/com.auction.client.controller/WalletController.html` | 25629 | Đã tái tạo tại cùng path |
| 194 | `target/site/jacoco/com.auction.client.controller/WalletController.java.html` | 29255 | Đã tái tạo tại cùng path |
| 195 | `target/site/jacoco/com.auction.client.controller/index.html` | 18404 | Đã tái tạo tại cùng path |
| 196 | `target/site/jacoco/com.auction.client.controller/index.source.html` | 9245 | Đã tái tạo tại cùng path |
| 197 | `target/site/jacoco/com.auction.client.model/AdminUser.html` | 4701 | Đã tái tạo tại cùng path |
| 198 | `target/site/jacoco/com.auction.client.model/AdminUser.java.html` | 2428 | Đã tái tạo tại cùng path |
| 199 | `target/site/jacoco/com.auction.client.model/Auction.html` | 20249 | Đã tái tạo tại cùng path |
| 200 | `target/site/jacoco/com.auction.client.model/Auction.java.html` | 8148 | Đã tái tạo tại cùng path |
| 201 | `target/site/jacoco/com.auction.client.model/AuctionStatus.html` | 4245 | Đã tái tạo tại cùng path |
| 202 | `target/site/jacoco/com.auction.client.model/AuctionStatus.java.html` | 2190 | Đã tái tạo tại cùng path |
| 203 | `target/site/jacoco/com.auction.client.model/AutoBid.html` | 9902 | Đã tái tạo tại cùng path |
| 204 | `target/site/jacoco/com.auction.client.model/AutoBid.java.html` | 5204 | Đã tái tạo tại cùng path |
| 205 | `target/site/jacoco/com.auction.client.model/Bid.html` | 10847 | Đã tái tạo tại cùng path |
| 206 | `target/site/jacoco/com.auction.client.model/Bid.java.html` | 4979 | Đã tái tạo tại cùng path |
| 207 | `target/site/jacoco/com.auction.client.model/Item.html` | 10581 | Đã tái tạo tại cùng path |
| 208 | `target/site/jacoco/com.auction.client.model/Item.java.html` | 4711 | Đã tái tạo tại cùng path |
| 209 | `target/site/jacoco/com.auction.client.model/ItemCategory.html` | 5489 | Đã tái tạo tại cùng path |
| 210 | `target/site/jacoco/com.auction.client.model/ItemCategory.java.html` | 4938 | Đã tái tạo tại cùng path |
| 211 | `target/site/jacoco/com.auction.client.model/Transaction.html` | 2829 | Đã tái tạo tại cùng path |
| 212 | `target/site/jacoco/com.auction.client.model/Transaction.java.html` | 1686 | Đã tái tạo tại cùng path |
| 213 | `target/site/jacoco/com.auction.client.model/TransactionType.html` | 4255 | Đã tái tạo tại cùng path |
| 214 | `target/site/jacoco/com.auction.client.model/TransactionType.java.html` | 2393 | Đã tái tạo tại cùng path |
| 215 | `target/site/jacoco/com.auction.client.model/User.html` | 9556 | Đã tái tạo tại cùng path |
| 216 | `target/site/jacoco/com.auction.client.model/User.java.html` | 4309 | Đã tái tạo tại cùng path |
| 217 | `target/site/jacoco/com.auction.client.model/UserRole.html` | 4206 | Đã tái tạo tại cùng path |
| 218 | `target/site/jacoco/com.auction.client.model/UserRole.java.html` | 2065 | Đã tái tạo tại cùng path |
| 219 | `target/site/jacoco/com.auction.client.model/index.html` | 9985 | Đã tái tạo tại cùng path |
| 220 | `target/site/jacoco/com.auction.client.model/index.source.html` | 10093 | Đã tái tạo tại cùng path |
| 221 | `target/site/jacoco/com.auction.client.navigation/ViewNavigator$Route.html` | 4278 | Đã tái tạo tại cùng path |
| 222 | `target/site/jacoco/com.auction.client.navigation/ViewNavigator$View.html` | 2806 | Đã tái tạo tại cùng path |
| 223 | `target/site/jacoco/com.auction.client.navigation/ViewNavigator.html` | 15263 | Đã tái tạo tại cùng path |
| 224 | `target/site/jacoco/com.auction.client.navigation/ViewNavigator.java.html` | 15616 | Đã tái tạo tại cùng path |
| 225 | `target/site/jacoco/com.auction.client.navigation/ViewTasks$NotSubmittedException.html` | 2854 | Đã tái tạo tại cùng path |
| 226 | `target/site/jacoco/com.auction.client.navigation/ViewTasks.html` | 13233 | Đã tái tạo tại cùng path |
| 227 | `target/site/jacoco/com.auction.client.navigation/ViewTasks.java.html` | 9408 | Đã tái tạo tại cùng path |
| 228 | `target/site/jacoco/com.auction.client.navigation/index.html` | 5826 | Đã tái tạo tại cùng path |
| 229 | `target/site/jacoco/com.auction.client.navigation/index.source.html` | 4143 | Đã tái tạo tại cùng path |
| 230 | `target/site/jacoco/com.auction.client.service/AuctionClientService.html` | 4832 | Đã tái tạo tại cùng path |
| 231 | `target/site/jacoco/com.auction.client.service/AuctionClientService.java.html` | 3702 | Đã tái tạo tại cùng path |
| 232 | `target/site/jacoco/com.auction.client.service/AuctionRoomSnapshot.html` | 2802 | Đã tái tạo tại cùng path |
| 233 | `target/site/jacoco/com.auction.client.service/AuctionRoomSnapshot.java.html` | 1657 | Đã tái tạo tại cùng path |
| 234 | `target/site/jacoco/com.auction.client.service/AutoBidException.html` | 2777 | Đã tái tạo tại cùng path |
| 235 | `target/site/jacoco/com.auction.client.service/AutoBidException.java.html` | 1595 | Đã tái tạo tại cùng path |
| 236 | `target/site/jacoco/com.auction.client.service/BidResult.html` | 5696 | Đã tái tạo tại cùng path |
| 237 | `target/site/jacoco/com.auction.client.service/BidResult.java.html` | 2699 | Đã tái tạo tại cùng path |
| 238 | `target/site/jacoco/com.auction.client.service/BidSubmissionTracker$Entry.html` | 2816 | Đã tái tạo tại cùng path |
| 239 | `target/site/jacoco/com.auction.client.service/BidSubmissionTracker$Key.html` | 2822 | Đã tái tạo tại cùng path |
| 240 | `target/site/jacoco/com.auction.client.service/BidSubmissionTracker$Phase.html` | 2803 | Đã tái tạo tại cùng path |
| 241 | `target/site/jacoco/com.auction.client.service/BidSubmissionTracker$ReadStamp.html` | 2893 | Đã tái tạo tại cùng path |
| 242 | `target/site/jacoco/com.auction.client.service/BidSubmissionTracker$Ticket.html` | 2850 | Đã tái tạo tại cùng path |
| 243 | `target/site/jacoco/com.auction.client.service/BidSubmissionTracker.html` | 9669 | Đã tái tạo tại cùng path |
| 244 | `target/site/jacoco/com.auction.client.service/BidSubmissionTracker.java.html` | 6086 | Đã tái tạo tại cùng path |
| 245 | `target/site/jacoco/com.auction.client.service/ServiceRegistry$Services.html` | 3089 | Đã tái tạo tại cùng path |
| 246 | `target/site/jacoco/com.auction.client.service/ServiceRegistry.html` | 12732 | Đã tái tạo tại cùng path |
| 247 | `target/site/jacoco/com.auction.client.service/ServiceRegistry.java.html` | 11946 | Đã tái tạo tại cùng path |
| 248 | `target/site/jacoco/com.auction.client.service/WalletException.html` | 2772 | Đã tái tạo tại cùng path |
| 249 | `target/site/jacoco/com.auction.client.service/WalletException.java.html` | 1579 | Đã tái tạo tại cùng path |
| 250 | `target/site/jacoco/com.auction.client.service/WalletService.html` | 2764 | Đã tái tạo tại cùng path |
| 251 | `target/site/jacoco/com.auction.client.service/WalletService.java.html` | 2388 | Đã tái tạo tại cùng path |
| 252 | `target/site/jacoco/com.auction.client.service/WalletSnapshot.html` | 2786 | Đã tái tạo tại cùng path |
| 253 | `target/site/jacoco/com.auction.client.service/WalletSnapshot.java.html` | 1655 | Đã tái tạo tại cùng path |
| 254 | `target/site/jacoco/com.auction.client.service/index.html` | 11739 | Đã tái tạo tại cùng path |
| 255 | `target/site/jacoco/com.auction.client.service/index.source.html` | 8161 | Đã tái tạo tại cùng path |
| 256 | `target/site/jacoco/com.auction.client.service.mock/AutoBidEngine.html` | 4819 | Đã tái tạo tại cùng path |
| 257 | `target/site/jacoco/com.auction.client.service.mock/AutoBidEngine.java.html` | 10011 | Đã tái tạo tại cùng path |
| 258 | `target/site/jacoco/com.auction.client.service.mock/MockAuctionClientService.html` | 21801 | Đã tái tạo tại cùng path |
| 259 | `target/site/jacoco/com.auction.client.service.mock/MockAuctionClientService.java.html` | 26141 | Đã tái tạo tại cùng path |
| 260 | `target/site/jacoco/com.auction.client.service.mock/MockAutoBidService.html` | 7104 | Đã tái tạo tại cùng path |
| 261 | `target/site/jacoco/com.auction.client.service.mock/MockAutoBidService.java.html` | 7289 | Đã tái tạo tại cùng path |
| 262 | `target/site/jacoco/com.auction.client.service.mock/MockDataStore$Hold.html` | 2794 | Đã tái tạo tại cùng path |
| 263 | `target/site/jacoco/com.auction.client.service.mock/MockDataStore.html` | 16246 | Đã tái tạo tại cùng path |
| 264 | `target/site/jacoco/com.auction.client.service.mock/MockDataStore.java.html` | 29728 | Đã tái tạo tại cùng path |
| 265 | `target/site/jacoco/com.auction.client.service.mock/MockManagementService.html` | 15651 | Đã tái tạo tại cùng path |
| 266 | `target/site/jacoco/com.auction.client.service.mock/MockManagementService.java.html` | 12276 | Đã tái tạo tại cùng path |
| 267 | `target/site/jacoco/com.auction.client.service.mock/MockWalletService.html` | 6764 | Đã tái tạo tại cùng path |
| 268 | `target/site/jacoco/com.auction.client.service.mock/MockWalletService.java.html` | 5418 | Đã tái tạo tại cùng path |
| 269 | `target/site/jacoco/com.auction.client.service.mock/index.html` | 7844 | Đã tái tạo tại cùng path |
| 270 | `target/site/jacoco/com.auction.client.service.mock/index.source.html` | 7433 | Đã tái tạo tại cùng path |
| 271 | `target/site/jacoco/com.auction.client.service.server/JsonTransport$Connection.html` | 3940 | Đã tái tạo tại cùng path |
| 272 | `target/site/jacoco/com.auction.client.service.server/JsonTransport.html` | 8612 | Đã tái tạo tại cùng path |
| 273 | `target/site/jacoco/com.auction.client.service.server/JsonTransport.java.html` | 10025 | Đã tái tạo tại cùng path |
| 274 | `target/site/jacoco/com.auction.client.service.server/ServerError.html` | 4019 | Đã tái tạo tại cùng path |
| 275 | `target/site/jacoco/com.auction.client.service.server/ServerError.java.html` | 4412 | Đã tái tạo tại cùng path |
| 276 | `target/site/jacoco/com.auction.client.service.server/ServerMapper.html` | 6528 | Đã tái tạo tại cùng path |
| 277 | `target/site/jacoco/com.auction.client.service.server/ServerMapper.java.html` | 6876 | Đã tái tạo tại cùng path |
| 278 | `target/site/jacoco/com.auction.client.service.server/ServerServices$CommandLock.html` | 3328 | Đã tái tạo tại cùng path |
| 279 | `target/site/jacoco/com.auction.client.service.server/ServerServices.html` | 41178 | Đã tái tạo tại cùng path |
| 280 | `target/site/jacoco/com.auction.client.service.server/ServerServices.java.html` | 43114 | Đã tái tạo tại cùng path |
| 281 | `target/site/jacoco/com.auction.client.service.server/index.html` | 6741 | Đã tái tạo tại cùng path |
| 282 | `target/site/jacoco/com.auction.client.service.server/index.source.html` | 5725 | Đã tái tạo tại cùng path |
| 283 | `target/site/jacoco/com.auction.client.ui/AppFooter.html` | 6590 | Đã tái tạo tại cùng path |
| 284 | `target/site/jacoco/com.auction.client.ui/AppFooter.java.html` | 7333 | Đã tái tạo tại cùng path |
| 285 | `target/site/jacoco/com.auction.client.ui/AppHeader.html` | 10043 | Đã tái tạo tại cùng path |
| 286 | `target/site/jacoco/com.auction.client.ui/AppHeader.java.html` | 12253 | Đã tái tạo tại cùng path |
| 287 | `target/site/jacoco/com.auction.client.ui/Icons$1.html` | 3261 | Đã tái tạo tại cùng path |
| 288 | `target/site/jacoco/com.auction.client.ui/Icons.html` | 3950 | Đã tái tạo tại cùng path |
| 289 | `target/site/jacoco/com.auction.client.ui/Icons.java.html` | 12368 | Đã tái tạo tại cùng path |
| 290 | `target/site/jacoco/com.auction.client.ui/PasswordFieldWithToggle.html` | 4245 | Đã tái tạo tại cùng path |
| 291 | `target/site/jacoco/com.auction.client.ui/PasswordFieldWithToggle.java.html` | 4087 | Đã tái tạo tại cùng path |
| 292 | `target/site/jacoco/com.auction.client.ui/ProductImages$1.html` | 4969 | Đã tái tạo tại cùng path |
| 293 | `target/site/jacoco/com.auction.client.ui/ProductImages.html` | 6330 | Đã tái tạo tại cùng path |
| 294 | `target/site/jacoco/com.auction.client.ui/ProductImages.java.html` | 9442 | Đã tái tạo tại cùng path |
| 295 | `target/site/jacoco/com.auction.client.ui/RoundedClip.html` | 2753 | Đã tái tạo tại cùng path |
| 296 | `target/site/jacoco/com.auction.client.ui/RoundedClip.java.html` | 1968 | Đã tái tạo tại cùng path |
| 297 | `target/site/jacoco/com.auction.client.ui/SmoothScroll.html` | 4232 | Đã tái tạo tại cùng path |
| 298 | `target/site/jacoco/com.auction.client.ui/SmoothScroll.java.html` | 3991 | Đã tái tạo tại cùng path |
| 299 | `target/site/jacoco/com.auction.client.ui/UserMenu.html` | 19391 | Đã tái tạo tại cùng path |
| 300 | `target/site/jacoco/com.auction.client.ui/UserMenu.java.html` | 25291 | Đã tái tạo tại cùng path |
| 301 | `target/site/jacoco/com.auction.client.ui/index.html` | 10075 | Đã tái tạo tại cùng path |
| 302 | `target/site/jacoco/com.auction.client.ui/index.source.html` | 8769 | Đã tái tạo tại cùng path |
| 303 | `target/site/jacoco/com.auction.client.util/AuctionTime.html` | 4630 | Đã tái tạo tại cùng path |
| 304 | `target/site/jacoco/com.auction.client.util/AuctionTime.java.html` | 3660 | Đã tái tạo tại cùng path |
| 305 | `target/site/jacoco/com.auction.client.util/FormatUtil.html` | 6260 | Đã tái tạo tại cùng path |
| 306 | `target/site/jacoco/com.auction.client.util/FormatUtil.java.html` | 4605 | Đã tái tạo tại cùng path |
| 307 | `target/site/jacoco/com.auction.client.util/Validators.html` | 7514 | Đã tái tạo tại cùng path |
| 308 | `target/site/jacoco/com.auction.client.util/Validators.java.html` | 7355 | Đã tái tạo tại cùng path |
| 309 | `target/site/jacoco/com.auction.client.util/index.html` | 4898 | Đã tái tạo tại cùng path |
| 310 | `target/site/jacoco/com.auction.client.util/index.source.html` | 4918 | Đã tái tạo tại cùng path |
| 311 | `target/site/jacoco/com.auction.server.dao/AuctionJsonMapper.html` | 3281 | Đã tái tạo tại cùng path |
| 312 | `target/site/jacoco/com.auction.server.dao/AuctionJsonMapper.java.html` | 3147 | Đã tái tạo tại cùng path |
| 313 | `target/site/jacoco/com.auction.server.dao/AuctionRepository.html` | 5287 | Đã tái tạo tại cùng path |
| 314 | `target/site/jacoco/com.auction.server.dao/AuctionRepository.java.html` | 7254 | Đã tái tạo tại cùng path |
| 315 | `target/site/jacoco/com.auction.server.dao/index.html` | 3863 | Đã tái tạo tại cùng path |
| 316 | `target/site/jacoco/com.auction.server.dao/index.source.html` | 3872 | Đã tái tạo tại cùng path |
| 317 | `target/site/jacoco/com.auction.server.exception/ApiException.html` | 3252 | Đã tái tạo tại cùng path |
| 318 | `target/site/jacoco/com.auction.server.exception/ApiException.java.html` | 1735 | Đã tái tạo tại cùng path |
| 319 | `target/site/jacoco/com.auction.server.exception/index.html` | 3047 | Đã tái tạo tại cùng path |
| 320 | `target/site/jacoco/com.auction.server.exception/index.source.html` | 3045 | Đã tái tạo tại cùng path |
| 321 | `target/site/jacoco/com.auction.server.model/Admin.html` | 3220 | Đã tái tạo tại cùng path |
| 322 | `target/site/jacoco/com.auction.server.model/Admin.java.html` | 1588 | Đã tái tạo tại cùng path |
| 323 | `target/site/jacoco/com.auction.server.model/Art.html` | 3216 | Đã tái tạo tại cùng path |
| 324 | `target/site/jacoco/com.auction.server.model/Art.java.html` | 1597 | Đã tái tạo tại cùng path |
| 325 | `target/site/jacoco/com.auction.server.model/Auction.html` | 4487 | Đã tái tạo tại cùng path |
| 326 | `target/site/jacoco/com.auction.server.model/Auction.java.html` | 3349 | Đã tái tạo tại cùng path |
| 327 | `target/site/jacoco/com.auction.server.model/BidTransaction.html` | 2814 | Đã tái tạo tại cùng path |
| 328 | `target/site/jacoco/com.auction.server.model/BidTransaction.java.html` | 1626 | Đã tái tạo tại cùng path |
| 329 | `target/site/jacoco/com.auction.server.model/Bidder.html` | 3226 | Đã tái tạo tại cùng path |
| 330 | `target/site/jacoco/com.auction.server.model/Bidder.java.html` | 1594 | Đã tái tạo tại cùng path |
| 331 | `target/site/jacoco/com.auction.server.model/Electronics.html` | 3264 | Đã tái tạo tại cùng path |
| 332 | `target/site/jacoco/com.auction.server.model/Electronics.java.html` | 1645 | Đã tái tạo tại cùng path |
| 333 | `target/site/jacoco/com.auction.server.model/Entity.html` | 3694 | Đã tái tạo tại cùng path |
| 334 | `target/site/jacoco/com.auction.server.model/Entity.java.html` | 1823 | Đã tái tạo tại cùng path |
| 335 | `target/site/jacoco/com.auction.server.model/Item.html` | 3709 | Đã tái tạo tại cùng path |
| 336 | `target/site/jacoco/com.auction.server.model/Item.java.html` | 2013 | Đã tái tạo tại cùng path |
| 337 | `target/site/jacoco/com.auction.server.model/ItemFactory$1.html` | 3420 | Đã tái tạo tại cùng path |
| 338 | `target/site/jacoco/com.auction.server.model/ItemFactory$CategoryItem.html` | 3328 | Đã tái tạo tại cùng path |
| 339 | `target/site/jacoco/com.auction.server.model/ItemFactory.html` | 3336 | Đã tái tạo tại cùng path |
| 340 | `target/site/jacoco/com.auction.server.model/ItemFactory.java.html` | 3399 | Đã tái tạo tại cùng path |
| 341 | `target/site/jacoco/com.auction.server.model/Seller.html` | 3226 | Đã tái tạo tại cùng path |
| 342 | `target/site/jacoco/com.auction.server.model/Seller.java.html` | 1594 | Đã tái tạo tại cùng path |
| 343 | `target/site/jacoco/com.auction.server.model/User.html` | 3219 | Đã tái tạo tại cùng path |
| 344 | `target/site/jacoco/com.auction.server.model/User.java.html` | 1801 | Đã tái tạo tại cùng path |
| 345 | `target/site/jacoco/com.auction.server.model/Vehicle.html` | 3240 | Đã tái tạo tại cùng path |
| 346 | `target/site/jacoco/com.auction.server.model/Vehicle.java.html` | 1621 | Đã tái tạo tại cùng path |
| 347 | `target/site/jacoco/com.auction.server.model/index.html` | 10360 | Đã tái tạo tại cùng path |
| 348 | `target/site/jacoco/com.auction.server.model/index.source.html` | 9229 | Đã tái tạo tại cùng path |
| 349 | `target/site/jacoco/com.auction.server.network/Server$Client.html` | 5412 | Đã tái tạo tại cùng path |
| 350 | `target/site/jacoco/com.auction.server.network/Server.html` | 9015 | Đã tái tạo tại cùng path |
| 351 | `target/site/jacoco/com.auction.server.network/Server.java.html` | 20701 | Đã tái tạo tại cùng path |
| 352 | `target/site/jacoco/com.auction.server.network/index.html` | 4120 | Đã tái tạo tại cùng path |
| 353 | `target/site/jacoco/com.auction.server.network/index.source.html` | 3309 | Đã tái tạo tại cùng path |
| 354 | `target/site/jacoco/com.auction.server.service/AntiSnipingPolicy.html` | 6198 | Đã tái tạo tại cùng path |
| 355 | `target/site/jacoco/com.auction.server.service/AntiSnipingPolicy.java.html` | 4222 | Đã tái tạo tại cùng path |
| 356 | `target/site/jacoco/com.auction.server.service/AuctionService$Session.html` | 2809 | Đã tái tạo tại cùng path |
| 357 | `target/site/jacoco/com.auction.server.service/AuctionService.html` | 47309 | Đã tái tạo tại cùng path |
| 358 | `target/site/jacoco/com.auction.server.service/AuctionService.java.html` | 80782 | Đã tái tạo tại cùng path |
| 359 | `target/site/jacoco/com.auction.server.service/Passwords.html` | 3821 | Đã tái tạo tại cùng path |
| 360 | `target/site/jacoco/com.auction.server.service/Passwords.java.html` | 3352 | Đã tái tạo tại cùng path |
| 361 | `target/site/jacoco/com.auction.server.service/index.html` | 5080 | Đã tái tạo tại cùng path |
| 362 | `target/site/jacoco/com.auction.server.service/index.source.html` | 4628 | Đã tái tạo tại cùng path |
| 363 | `target/site/jacoco/com.auction.shared/JsonProtocol.html` | 5735 | Đã tái tạo tại cùng path |
| 364 | `target/site/jacoco/com.auction.shared/JsonProtocol.java.html` | 5111 | Đã tái tạo tại cùng path |
| 365 | `target/site/jacoco/com.auction.shared/index.html` | 3286 | Đã tái tạo tại cùng path |
| 366 | `target/site/jacoco/com.auction.shared/index.source.html` | 3284 | Đã tái tạo tại cùng path |
| 367 | `target/site/jacoco/jacoco-resources/branchfc.gif` | 91 | Đã tái tạo tại cùng path |
| 368 | `target/site/jacoco/jacoco-resources/branchnc.gif` | 91 | Đã tái tạo tại cùng path |
| 369 | `target/site/jacoco/jacoco-resources/branchpc.gif` | 91 | Đã tái tạo tại cùng path |
| 370 | `target/site/jacoco/jacoco-resources/bundle.gif` | 709 | Đã tái tạo tại cùng path |
| 371 | `target/site/jacoco/jacoco-resources/class.gif` | 586 | Đã tái tạo tại cùng path |
| 372 | `target/site/jacoco/jacoco-resources/down.gif` | 67 | Đã tái tạo tại cùng path |
| 373 | `target/site/jacoco/jacoco-resources/greenbar.gif` | 91 | Đã tái tạo tại cùng path |
| 374 | `target/site/jacoco/jacoco-resources/group.gif` | 351 | Đã tái tạo tại cùng path |
| 375 | `target/site/jacoco/jacoco-resources/method.gif` | 193 | Đã tái tạo tại cùng path |
| 376 | `target/site/jacoco/jacoco-resources/package.gif` | 227 | Đã tái tạo tại cùng path |
| 377 | `target/site/jacoco/jacoco-resources/prettify.css` | 323 | Đã tái tạo tại cùng path |
| 378 | `target/site/jacoco/jacoco-resources/prettify.js` | 59057 | Đã tái tạo tại cùng path |
| 379 | `target/site/jacoco/jacoco-resources/redbar.gif` | 91 | Đã tái tạo tại cùng path |
| 380 | `target/site/jacoco/jacoco-resources/report.css` | 4046 | Đã tái tạo tại cùng path |
| 381 | `target/site/jacoco/jacoco-resources/report.gif` | 363 | Đã tái tạo tại cùng path |
| 382 | `target/site/jacoco/jacoco-resources/session.gif` | 213 | Đã tái tạo tại cùng path |
| 383 | `target/site/jacoco/jacoco-resources/sort.gif` | 58 | Đã tái tạo tại cùng path |
| 384 | `target/site/jacoco/jacoco-resources/sort.js` | 3955 | Đã tái tạo tại cùng path |
| 385 | `target/site/jacoco/jacoco-resources/source.gif` | 354 | Đã tái tạo tại cùng path |
| 386 | `target/site/jacoco/jacoco-resources/up.gif` | 67 | Đã tái tạo tại cùng path |
| 387 | `target/surefire-reports/2026-11-12T15-54-24_574.dumpstream` | 8532 | Đã xóa, hiện không tái tạo |
| 388 | `target/surefire-reports/TEST-com.auction.client.controller.ClientUiTest.xml` | 21571 | Đã tái tạo tại cùng path |
| 389 | `target/surefire-reports/TEST-com.auction.client.navigation.ImageTasksTest.xml` | 11076 | Đã tái tạo tại cùng path |
| 390 | `target/surefire-reports/TEST-com.auction.client.navigation.ViewTasksTest.xml` | 10878 | Đã tái tạo tại cùng path |
| 391 | `target/surefire-reports/TEST-com.auction.client.service.BidSubmissionTrackerTest.xml` | 10966 | Đã tái tạo tại cùng path |
| 392 | `target/surefire-reports/TEST-com.auction.client.service.MockAuctionBidAccessTest.xml` | 10519 | Đã tái tạo tại cùng path |
| 393 | `target/surefire-reports/TEST-com.auction.client.service.MockAuthIntegrationTest.xml` | 10668 | Đã tái tạo tại cùng path |
| 394 | `target/surefire-reports/TEST-com.auction.client.service.MockAutoBidServiceTest.xml` | 11086 | Đã tái tạo tại cùng path |
| 395 | `target/surefire-reports/TEST-com.auction.client.service.MockManagementServiceTest.xml` | 10820 | Đã tái tạo tại cùng path |
| 396 | `target/surefire-reports/TEST-com.auction.client.service.MockSharedStoreTest.xml` | 10671 | Đã tái tạo tại cùng path |
| 397 | `target/surefire-reports/TEST-com.auction.client.service.MockWalletServiceTest.xml` | 10896 | Đã tái tạo tại cùng path |
| 398 | `target/surefire-reports/TEST-com.auction.client.service.ServiceRegistryTest.xml` | 10665 | Đã tái tạo tại cùng path |
| 399 | `target/surefire-reports/TEST-com.auction.client.ui.ProductImagesIoTest.xml` | 10605 | Đã tái tạo tại cùng path |
| 400 | `target/surefire-reports/TEST-com.auction.client.ui.ProductImagesTest.xml` | 19742 | Đã tái tạo tại cùng path |
| 401 | `target/surefire-reports/TEST-com.auction.client.ui.UiResourcesTest.xml` | 10464 | Đã tái tạo tại cùng path |
| 402 | `target/surefire-reports/TEST-com.auction.client.util.AuctionTimeTest.xml` | 10639 | Đã tái tạo tại cùng path |
| 403 | `target/surefire-reports/TEST-com.auction.client.util.FormatUtilTest.xml` | 10445 | Đã tái tạo tại cùng path |
| 404 | `target/surefire-reports/TEST-com.auction.client.util.ValidatorsTest.xml` | 15336 | Đã tái tạo tại cùng path |
| 405 | `target/surefire-reports/TEST-com.auction.integration.FullStackTest.xml` | 14706 | Đã tái tạo tại cùng path |
| 406 | `target/surefire-reports/TEST-com.auction.integration.ServerUiIntegrationTest.xml` | 23565 | Đã tái tạo tại cùng path |
| 407 | `target/surefire-reports/TEST-com.auction.server.dao.AuctionJsonMapperTest.xml` | 10779 | Đã tái tạo tại cùng path |
| 408 | `target/surefire-reports/TEST-com.auction.server.dao.AuctionRepositoryTest.xml` | 11744 | Đã tái tạo tại cùng path |
| 409 | `target/surefire-reports/TEST-com.auction.server.model.AuctionTest.xml` | 11197 | Đã tái tạo tại cùng path |
| 410 | `target/surefire-reports/TEST-com.auction.server.model.ItemFactoryTest.xml` | 11484 | Đã tái tạo tại cùng path |
| 411 | `target/surefire-reports/TEST-com.auction.server.network.ServerIntegrationTest.xml` | 11438 | Đã tái tạo tại cùng path |
| 412 | `target/surefire-reports/TEST-com.auction.server.service.AntiSnipingPolicyTest.xml` | 10943 | Đã tái tạo tại cùng path |
| 413 | `target/surefire-reports/TEST-com.auction.server.service.AuctionServiceTest.xml` | 13079 | Đã tái tạo tại cùng path |
| 414 | `target/surefire-reports/TEST-com.auction.server.service.AuctionStateValidationTest.xml` | 11528 | Đã tái tạo tại cùng path |
| 415 | `target/surefire-reports/com.auction.client.controller.ClientUiTest.txt` | 339 | Đã tái tạo tại cùng path |
| 416 | `target/surefire-reports/com.auction.client.navigation.ImageTasksTest.txt` | 342 | Đã tái tạo tại cùng path |
| 417 | `target/surefire-reports/com.auction.client.navigation.ViewTasksTest.txt` | 340 | Đã tái tạo tại cùng path |
| 418 | `target/surefire-reports/com.auction.client.service.BidSubmissionTrackerTest.txt` | 356 | Đã tái tạo tại cùng path |
| 419 | `target/surefire-reports/com.auction.client.service.MockAuctionBidAccessTest.txt` | 356 | Đã tái tạo tại cùng path |
| 420 | `target/surefire-reports/com.auction.client.service.MockAuthIntegrationTest.txt` | 354 | Đã tái tạo tại cùng path |
| 421 | `target/surefire-reports/com.auction.client.service.MockAutoBidServiceTest.txt` | 352 | Đã tái tạo tại cùng path |
| 422 | `target/surefire-reports/com.auction.client.service.MockManagementServiceTest.txt` | 358 | Đã tái tạo tại cùng path |
| 423 | `target/surefire-reports/com.auction.client.service.MockSharedStoreTest.txt` | 346 | Đã tái tạo tại cùng path |
| 424 | `target/surefire-reports/com.auction.client.service.MockWalletServiceTest.txt` | 350 | Đã tái tạo tại cùng path |
| 425 | `target/surefire-reports/com.auction.client.service.ServiceRegistryTest.txt` | 346 | Đã tái tạo tại cùng path |
| 426 | `target/surefire-reports/com.auction.client.ui.ProductImagesIoTest.txt` | 336 | Đã tái tạo tại cùng path |
| 427 | `target/surefire-reports/com.auction.client.ui.ProductImagesTest.txt` | 332 | Đã tái tạo tại cùng path |
| 428 | `target/surefire-reports/com.auction.client.ui.UiResourcesTest.txt` | 328 | Đã tái tạo tại cùng path |
| 429 | `target/surefire-reports/com.auction.client.util.AuctionTimeTest.txt` | 332 | Đã tái tạo tại cùng path |
| 430 | `target/surefire-reports/com.auction.client.util.FormatUtilTest.txt` | 330 | Đã tái tạo tại cùng path |
| 431 | `target/surefire-reports/com.auction.client.util.ValidatorsTest.txt` | 331 | Đã tái tạo tại cùng path |
| 432 | `target/surefire-reports/com.auction.integration.FullStackTest.txt` | 328 | Đã tái tạo tại cùng path |
| 433 | `target/surefire-reports/com.auction.integration.ServerUiIntegrationTest.txt` | 348 | Đã tái tạo tại cùng path |
| 434 | `target/surefire-reports/com.auction.server.dao.AuctionJsonMapperTest.txt` | 342 | Đã tái tạo tại cùng path |
| 435 | `target/surefire-reports/com.auction.server.dao.AuctionRepositoryTest.txt` | 343 | Đã tái tạo tại cùng path |
| 436 | `target/surefire-reports/com.auction.server.model.AuctionTest.txt` | 326 | Đã tái tạo tại cùng path |
| 437 | `target/surefire-reports/com.auction.server.model.ItemFactoryTest.txt` | 334 | Đã tái tạo tại cùng path |
| 438 | `target/surefire-reports/com.auction.server.network.ServerIntegrationTest.txt` | 350 | Đã tái tạo tại cùng path |
| 439 | `target/surefire-reports/com.auction.server.service.AntiSnipingPolicyTest.txt` | 350 | Đã tái tạo tại cùng path |
| 440 | `target/surefire-reports/com.auction.server.service.AuctionServiceTest.txt` | 345 | Đã tái tạo tại cùng path |
| 441 | `target/surefire-reports/com.auction.server.service.AuctionStateValidationTest.txt` | 360 | Đã tái tạo tại cùng path |
| 442 | `target/test-classes/com/auction/client/controller/ClientUiTest$1.class` | 1266 | Đã tái tạo tại cùng path |
| 443 | `target/test-classes/com/auction/client/controller/ClientUiTest$2.class` | 1198 | Đã tái tạo tại cùng path |
| 444 | `target/test-classes/com/auction/client/controller/ClientUiTest$3.class` | 2145 | Đã tái tạo tại cùng path |
| 445 | `target/test-classes/com/auction/client/controller/ClientUiTest$ControlledAuctionService.class` | 2762 | Đã tái tạo tại cùng path |
| 446 | `target/test-classes/com/auction/client/controller/ClientUiTest$ReadGate.class` | 830 | Đã tái tạo tại cùng path |
| 447 | `target/test-classes/com/auction/client/controller/ClientUiTest.class` | 62143 | Đã tái tạo tại cùng path |
| 448 | `target/test-classes/com/auction/client/navigation/ImageTasksTest.class` | 14411 | Đã tái tạo tại cùng path |
| 449 | `target/test-classes/com/auction/client/navigation/ViewTasksTest.class` | 6080 | Đã tái tạo tại cùng path |
| 450 | `target/test-classes/com/auction/client/service/BidSubmissionTrackerTest.class` | 4324 | Đã tái tạo tại cùng path |
| 451 | `target/test-classes/com/auction/client/service/MockAuctionBidAccessTest.class` | 3450 | Đã tái tạo tại cùng path |
| 452 | `target/test-classes/com/auction/client/service/MockAuthIntegrationTest.class` | 3741 | Đã tái tạo tại cùng path |
| 453 | `target/test-classes/com/auction/client/service/MockAutoBidServiceTest$Setup.class` | 3223 | Đã tái tạo tại cùng path |
| 454 | `target/test-classes/com/auction/client/service/MockAutoBidServiceTest.class` | 11622 | Đã tái tạo tại cùng path |
| 455 | `target/test-classes/com/auction/client/service/MockManagementServiceTest.class` | 4804 | Đã tái tạo tại cùng path |
| 456 | `target/test-classes/com/auction/client/service/MockSharedStoreTest.class` | 5083 | Đã tái tạo tại cùng path |
| 457 | `target/test-classes/com/auction/client/service/MockWalletServiceTest.class` | 8620 | Đã tái tạo tại cùng path |
| 458 | `target/test-classes/com/auction/client/service/ServiceRegistryTest.class` | 3864 | Đã tái tạo tại cùng path |
| 459 | `target/test-classes/com/auction/client/ui/ProductImagesIoTest.class` | 6370 | Đã tái tạo tại cùng path |
| 460 | `target/test-classes/com/auction/client/ui/ProductImagesTest.class` | 2242 | Đã tái tạo tại cùng path |
| 461 | `target/test-classes/com/auction/client/ui/UiResourcesTest.class` | 1679 | Đã tái tạo tại cùng path |
| 462 | `target/test-classes/com/auction/client/util/AuctionTimeTest.class` | 1744 | Đã tái tạo tại cùng path |
| 463 | `target/test-classes/com/auction/client/util/FormatUtilTest.class` | 852 | Đã tái tạo tại cùng path |
| 464 | `target/test-classes/com/auction/client/util/ValidatorsTest.class` | 3537 | Đã tái tạo tại cùng path |
| 465 | `target/test-classes/com/auction/integration/FullStackTest$DroppingProxy.class` | 5661 | Đã tái tạo tại cùng path |
| 466 | `target/test-classes/com/auction/integration/FullStackTest.class` | 15345 | Đã tái tạo tại cùng path |
| 467 | `target/test-classes/com/auction/integration/ServerUiIntegrationTest.class` | 15401 | Đã tái tạo tại cùng path |
| 468 | `target/test-classes/com/auction/server/dao/AuctionJsonMapperTest.class` | 4026 | Đã tái tạo tại cùng path |
| 469 | `target/test-classes/com/auction/server/dao/AuctionRepositoryTest.class` | 6765 | Đã tái tạo tại cùng path |
| 470 | `target/test-classes/com/auction/server/model/AuctionTest.class` | 6053 | Đã tái tạo tại cùng path |
| 471 | `target/test-classes/com/auction/server/model/ItemFactoryTest.class` | 4284 | Đã tái tạo tại cùng path |
| 472 | `target/test-classes/com/auction/server/network/ServerIntegrationTest$Peer.class` | 3498 | Đã tái tạo tại cùng path |
| 473 | `target/test-classes/com/auction/server/network/ServerIntegrationTest.class` | 8384 | Đã tái tạo tại cùng path |
| 474 | `target/test-classes/com/auction/server/service/AntiSnipingPolicyTest.class` | 3029 | Đã tái tạo tại cùng path |
| 475 | `target/test-classes/com/auction/server/service/AuctionServiceTest$MutableClock.class` | 1201 | Đã tái tạo tại cùng path |
| 476 | `target/test-classes/com/auction/server/service/AuctionServiceTest.class` | 18297 | Đã tái tạo tại cùng path |
| 477 | `target/test-classes/com/auction/server/service/AuctionStateValidationTest.class` | 4173 | Đã tái tạo tại cùng path |
| 478 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/.lock` | 0 | Đã tái tạo tại cùng path |
| 479 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-console-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 480 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-console-l1-2-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 481 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-datetime-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 482 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-debug-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 483 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-errorhandling-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 484 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-file-l1-1-0.dll` | 26232 | Đã tái tạo tại cùng path |
| 485 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-file-l1-2-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 486 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-file-l2-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 487 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-handle-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 488 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-heap-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 489 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-interlocked-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 490 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-libraryloader-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 491 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-localization-l1-2-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 492 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-memory-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 493 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-namedpipe-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 494 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-processenvironment-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 495 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-processthreads-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 496 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-processthreads-l1-1-1.dll` | 22120 | Đã tái tạo tại cùng path |
| 497 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-profile-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 498 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-rtlsupport-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 499 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-string-l1-1-0.dll` | 22112 | Đã tái tạo tại cùng path |
| 500 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-synch-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 501 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-synch-l1-2-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 502 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-sysinfo-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 503 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-timezone-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 504 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-core-util-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 505 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-conio-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 506 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-convert-l1-1-0.dll` | 26216 | Đã tái tạo tại cùng path |
| 507 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-environment-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 508 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-filesystem-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 509 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-heap-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 510 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-locale-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 511 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-math-l1-1-0.dll` | 30328 | Đã tái tạo tại cùng path |
| 512 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-multibyte-l1-1-0.dll` | 30328 | Đã tái tạo tại cùng path |
| 513 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-private-l1-1-0.dll` | 75368 | Đã tái tạo tại cùng path |
| 514 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-process-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 515 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-runtime-l1-1-0.dll` | 26216 | Đã tái tạo tại cùng path |
| 516 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-stdio-l1-1-0.dll` | 26216 | Đã tái tạo tại cùng path |
| 517 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-string-l1-1-0.dll` | 26216 | Đã tái tạo tại cùng path |
| 518 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-time-l1-1-0.dll` | 22120 | Đã tái tạo tại cùng path |
| 519 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/api-ms-win-crt-utility-l1-1-0.dll` | 22136 | Đã tái tạo tại cùng path |
| 520 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/glass.dll` | 284672 | Đã tái tạo tại cùng path |
| 521 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/javafx_font.dll` | 69632 | Đã tái tạo tại cùng path |
| 522 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/msvcp140.dll` | 575568 | Đã tái tạo tại cùng path |
| 523 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/msvcp140_1.dll` | 35920 | Đã tái tạo tại cùng path |
| 524 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/msvcp140_2.dll` | 267888 | Đã tái tạo tại cùng path |
| 525 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/prism_d3d.dll` | 180224 | Đã tái tạo tại cùng path |
| 526 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/ucrtbase.dll` | 1123944 | Đã tái tạo tại cùng path |
| 527 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/vcruntime140.dll` | 120400 | Đã tái tạo tại cùng path |
| 528 | `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64/vcruntime140_1.dll` | 49776 | Đã tái tạo tại cùng path |

### Output build có đường dẫn mới sau verify

| File | Vai trò |
| --- | --- |
| `target/inventory-clean-attempt-1.log` | Log thực thi Maven trong lần kiểm kê, gồm lần clean lỗi, clean cuối hoặc verify cuối. |
| `target/inventory-clean-final.log` | Log thực thi Maven trong lần kiểm kê, gồm lần clean lỗi, clean cuối hoặc verify cuối. |
| `target/inventory-verify.log` | Log thực thi Maven trong lần kiểm kê, gồm lần clean lỗi, clean cuối hoặc verify cuối. |
| `target/surefire-reports/2026-11-12T16-17-07_905.dumpstream` | Bằng chứng JUnit Surefire XML/TXT tự sinh, có test count và lỗi/skipped theo suite. |

## Danh sách đầy đủ file quyết định giữ

Bảng sau ghi từng file được duy trì sau dọn. Vai trò/method/caller từng Java có đầy đủ trong PROJECT_FILE_INVENTORY, số liệu từng file ở cả hai báo cáo. Các file ignored data/backup/IDE/hook cũng được ghi riêng sau bảng.

| File | Git | Quyết định | Căn cứ giữ |
| --- | --- | --- | --- |
| `.gitattributes` | tracked | Giữ | Git normalization khi checkout/add/diff. |
| `.github/workflows/ci.yml` | tracked | Giữ | GitHub Actions ở push/pull_request. Mới kiểm tra cấu hình trong lần này. |
| `.gitignore` | tracked | Giữ | Git status/add và quy trình bảo vệ dữ liệu cục bộ. |
| `MULTI_CLIENT_SETUP.md` | untracked | Giữ | Cần thiết khi demo và triển khai LAN. |
| `README.md` | tracked | Giữ | Cần thiết cho chạy và giới thiệu dự án. |
| `Start-Client.cmd` | tracked | Giữ | Người dùng mở từ File Explorer và README. |
| `Start-Server.cmd` | tracked | Giữ | Người dùng mở từ File Explorer và hướng dẫn LAN. |
| `config/checkstyle.xml` | untracked | Giữ | pom configLocation tại verify cho main và test. |
| `docs/2026-BTL.pdf` | untracked | Giữ | Giữ nguyên PDF giảng viên, dùng đối chiếu đề. |
| `docs/CLEANUP_REPORT.md` | untracked | Giữ | Một trong ba deliverable bắt buộc. |
| `docs/CODE_STATISTICS.md` | untracked | Giữ | Một trong ba deliverable bắt buộc. |
| `docs/Hướng dẫn BTL LTNC-2026.pdf` | untracked | Giữ | Giữ nguyên hướng dẫn giảng viên. |
| `docs/PROJECT_FILE_INVENTORY.md` | untracked | Giữ | Một trong ba deliverable bắt buộc. |
| `docs/VAN_DAP.md` | tracked | REVIEW | Giữ để bảo vệ. Việc bỏ cần quyết định về giá trị bản tóm tắt. |
| `docs/archive/JSON_ONLY_REFACTOR.md` | untracked | Giữ | Giữ lịch sử, tra cứu thay đổi và kết quả lỗi/thành công thời điểm cũ. |
| `docs/archive/JSON_STORAGE_SAFETY_REVIEW.md` | untracked | Giữ | Giữ bằng chứng quyết định bảo toàn dữ liệu và lịch sử kiểm thử. |
| `docs/archive/MULTI_CLIENT_TEST_RESULTS.md` | untracked | Giữ | Giữ bằng chứng lịch sử. Chuyển archive giúp nhận diện đúng phiên bản. |
| `docs/viva/ARCHITECTURE.md` | untracked | Giữ | Cần thiết để trình bày kiến trúc. |
| `docs/viva/AUDIT_AND_RESULTS.md` | untracked | Giữ | Giữ bằng chứng cải tiến và hạn chế đã ghi nhận. |
| `docs/viva/CODE_MAP.md` | untracked | Giữ | Giữ cho điều hướng khi demo/vấn đáp. |
| `docs/viva/CONCURRENCY_EXPLANATION.md` | untracked | Giữ | Cần thiết cho phần đồng thời và tính nhất quán. |
| `docs/viva/DESIGN_PATTERNS.md` | untracked | Giữ | Cần thiết cho bảo vệ patterns. |
| `docs/viva/OOP_EXPLANATION.md` | untracked | Giữ | Cần thiết để giải thích cây kế thừa/đa hình. |
| `docs/viva/PRESENTATION_GUIDE.md` | untracked | Giữ | Cần thiết cho chạy demo và bảo vệ. |
| `docs/viva/VIVA_QUESTIONS.md` | untracked | Giữ | Cần thiết cho luyện vấn đáp. |
| `pom.xml` | tracked | Giữ | Mọi lệnh Maven, run.ps1, CI và IDE import. |
| `run.ps1` | tracked | Giữ | Hai launcher CMD và lệnh hướng dẫn README/LAN. |
| `src/main/java/com/auction/client/App.java` | tracked | Giữ | Bootstrap có main chuyển args sang Main.main, phục vụ chạy client bằng classpath và tránh ràng buộc launcher JavaFX khi đóng gói. |
| `src/main/java/com/auction/client/Main.java` | tracked | Giữ | Application JavaFX khởi tạo mode server mặc định, đăng ký listener đổi phiên/quyền, thiết lập Stage tối thiểu 640×640 và mở màn hình xác thực. stop giải phóng view và bộ dịch vụ. |
| `src/main/java/com/auction/client/controller/AdminController.java` | tracked | Giữ | Điều khiển tổng quan, bảng tài khoản và bảng phiên dành cho ADMIN. Tải dữ liệu nền, lọc bảng, khóa/mở khóa/đổi vai trò/xóa người dùng và hủy/xóa phiên qua hai interface quản trị. dispose gỡ listener và đóng scope tác vụ. |
| `src/main/java/com/auction/client/controller/AuctionRoomController.java` | tracked | Giữ | Điều khiển phòng đấu giá gồm snapshot phiên/lịch sử, ảnh, biểu đồ giá, đếm ngược, bid thủ công và bật/tắt auto-bid. Dùng BidSubmissionTracker khóa thao tác qua timeout/chuyển view, đối soát pending trước khi đọc mới, đăng ký realtime và cập nhật qua FX thread. Tách scope ảnh, loại callback cũ và giải phóng timer/listener khi rời phòng. |
| `src/main/java/com/auction/client/controller/AuthController.java` | tracked | Giữ | Chuyển form đăng nhập/đăng ký, kiểm tra username/email/password/confirm và chọn BIDDER hoặc SELLER khi đăng ký. Gọi xác thực nền, điều hướng theo vai trò, ghi nhớ username bằng Preferences và hỗ trợ hiện/ẩn password. dispose ngăn kết quả cũ cập nhật view đã đóng. |
| `src/main/java/com/auction/client/controller/ContactController.java` | tracked | Giữ | Xây các thẻ liên hệ từ đúng bốn cặp khóa name/email trong contacts.properties bằng UTF-8. Đổi lưới một/hai cột theo chiều rộng, sao chép email vào clipboard, hiển thị toast và điều hướng về trang trước hoặc xác thực. |
| `src/main/java/com/auction/client/controller/GuideController.java` | tracked | Giữ | Tạo nội dung hướng dẫn sử dụng theo các mục bidder, seller, ví và quản trị trong Java, tạo mục lục cuộn tới section, điều hướng liên hệ/đăng nhập/quay lại. ViewTasks bảo vệ callback điều hướng và được đóng khi dispose. |
| `src/main/java/com/auction/client/controller/HomeController.java` | tracked | Giữ | Tải toàn bộ phiên nền, khử trùng theo ID, tìm theo tên/danh mục/người bán và chia card đang diễn ra, sắp diễn ra, kết quả. Hiển thị trạng thái loading/lỗi/rỗng, tải ảnh trên pool riêng, dùng một timer cho countdown và nạp lại khi qua ranh giới thời gian. Mở phòng hoặc trang quản lý seller. |
| `src/main/java/com/auction/client/controller/SellerProductsController.java` | tracked | Giữ | Quản lý bảng sản phẩm/phiên thuộc seller hiện tại, lọc theo trạng thái/danh mục/tên và tải ảnh nền. Form tạo/sửa kiểm tra tiền nguyên, thời gian server và ảnh, gửi saveProduct hoặc deleteProduct, mở lịch sử bid và phòng chi tiết. dispose gỡ subscription và đóng task scopes. |
| `src/main/java/com/auction/client/controller/WalletController.java` | tracked | Giữ | Đọc WalletSnapshot để hiển thị tổng số dư, khả dụng, giữ tiền, tổng nạp/rút và lịch sử. Lọc giao dịch, tạo dialog nạp/rút tiền giả lập với xác nhận và xử lý lỗi qua tác vụ nền. Đăng ký thay đổi ví, định dạng long đầy đủ và hủy listener khi dispose. |
| `src/main/java/com/auction/client/model/AdminUser.java` | tracked | Giữ | DTO quản trị bọc User cùng createdAt và cờ locked có setter, dùng để hiển thị và thao tác bảng tài khoản. |
| `src/main/java/com/auction/client/model/Auction.java` | tracked | Giữ | DTO phiên mutable cho UI, giữ Item, seller, giá hiện tại, người dẫn đầu, trạng thái, giờ bắt đầu/kết thúc, bước giá, số bid và lý do hủy. Tính mức bid kế tiếp và chuỗi tiền/thời gian hiển thị. |
| `src/main/java/com/auction/client/model/AuctionStatus.java` | tracked | Giữ | Enum OPEN, RUNNING, FINISHED, PAID, CANCELED cùng mô tả tiếng Việt, dùng cho lọc, render trạng thái và chuyển đổi JSON. |
| `src/main/java/com/auction/client/model/AutoBid.java` | tracked | Giữ | Mô hình cấu hình auto-bid gồm định danh phiên/người dùng, trần, bước tăng, thời điểm, cờ active/limitReached và số lượt tự động. copy/restoreState giữ trạng thái khi đi qua adapter/mock. |
| `src/main/java/com/auction/client/model/Bid.java` | tracked | Giữ | DTO lượt bid gồm ID, phiên, username bidder, amount, timestamp và cờ auto. Các constructor/getter/setter hỗ trợ lịch sử và realtime, getFormattedTime/toString phục vụ hiển thị. |
| `src/main/java/com/auction/client/model/Item.java` | tracked | Giữ | DTO sản phẩm mutable gồm ID, tên, mô tả, danh mục chuỗi, giá khởi điểm, seller và createdAt. Được gắn vào Auction khi adapter đọc hoặc form seller tạo/sửa. |
| `src/main/java/com/auction/client/model/ItemCategory.java` | tracked | Giữ | Enum sáu danh mục cùng nhãn và icon. from chuẩn hóa cả tên danh mục hiện tại và các tên tiếng Việt/legacy để bộ lọc và form dùng chung. |
| `src/main/java/com/auction/client/model/Transaction.java` | tracked | Giữ | Record dữ liệu một giao dịch ví gồm ID, username, loại, amount/balanceAfter kiểu long, thời điểm, trạng thái, ghi chú và auctionId. |
| `src/main/java/com/auction/client/model/TransactionType.java` | tracked | Giữ | Enum DEPOSIT, WITHDRAW, BID_HOLD, BID_RELEASE, PAYMENT, REFUND, SALE cùng nhãn tiếng Việt, dùng cho lọc lịch sử và phân biệt dòng tiền với tiền giữ. |
| `src/main/java/com/auction/client/model/User.java` | tracked | Giữ | DTO người dùng phía giao diện gồm ID, username, họ tên, email, UserRole và balance kiểu double. Adapter chuyển từ số nguyên server để giữ API client hiện hữu. |
| `src/main/java/com/auction/client/model/UserRole.java` | tracked | Giữ | Enum BIDDER, SELLER, ADMIN có tên hiển thị, dùng phân luồng UI và ánh xạ vai trò server. |
| `src/main/java/com/auction/client/navigation/DisposableView.java` | tracked | Giữ | Hợp đồng dispose cho controller giải phóng timer, listener và quyền cập nhật UI khi chuyển view hoặc thoát ứng dụng. |
| `src/main/java/com/auction/client/navigation/ViewNavigator.java` | tracked | Giữ | Quản lý Stage, Route tới FXML, lịch sử màn hình và auctionId để quay lại đúng phòng. Nạp FXMLLoader, dispose controller cũ, áp CSS cho Scene/DialogPane, quản lý focus và phân biệt keyboard navigation. |
| `src/main/java/com/auction/client/navigation/ViewTasks.java` | tracked | Giữ | Điều phối tác vụ nền theo khóa và phiên bản, chỉ giao kết quả mới nhất lên UI. Pool nghiệp vụ 4 luồng/queue 64, pool ảnh 2 luồng/queue 32. Lỗi từ chối hàng đợi có NotSubmittedException, scope ảnh được interrupt/cancel, đóng view chỉ chặn callback của lệnh nghiệp vụ đã nhận. |
| `src/main/java/com/auction/client/service/AuctionAdminService.java` | tracked | Giữ | Interface quản trị danh sách, hủy phiên kèm lý do và xóa phiên. Có implementation ServerServices và MockManagementService. |
| `src/main/java/com/auction/client/service/AuctionClientService.java` | tracked | Giữ | Interface chung cho xác thực, đọc phiên/lịch sử, bid, API sản phẩm legacy và đăng ký listener. Các default method cung cấp snapshot phòng, serverNow và hook đối soát pending cho implementation cụ thể. |
| `src/main/java/com/auction/client/service/AuctionRoomSnapshot.java` | tracked | Giữ | Record ghép Auction với lịch sử Bid của một lần đọc phòng. Compact constructor sao chép List để chặn thay đổi cấu trúc danh sách từ caller. |
| `src/main/java/com/auction/client/service/AutoBidException.java` | tracked | Giữ | RuntimeException diễn đạt lỗi cấu hình/thao tác auto-bid của mock bằng thông báo UI, được controller xử lý như từ chối đã biết. |
| `src/main/java/com/auction/client/service/AutoBidService.java` | tracked | Giữ | Interface đăng ký, hủy và đọc cấu hình auto-bid của tài khoản hiện tại. ServerServices và MockAutoBidService cùng triển khai. |
| `src/main/java/com/auction/client/service/BidResult.java` | tracked | Giữ | Đóng gói kết quả bid gồm success, message, updatedPrice và newBid. Factory ok/fail giúp controller xử lý chấp nhận/từ chối nhất quán. |
| `src/main/java/com/auction/client/service/BidSubmissionTracker.java` | tracked | Giữ | Máy trạng thái đồng bộ theo cặp username/auctionId với READY, SENDING, AWAITING_REFRESH, UNKNOWN. Ticket/read revision loại phản hồi cũ, chỉ read bắt đầu sau response rõ ràng mới mở khóa và UNKNOWN cần được đối soát. |
| `src/main/java/com/auction/client/service/ProductService.java` | tracked | Giữ | Interface liệt kê/lưu/xóa sản phẩm-phiên theo seller, đọc đường dẫn ảnh và lịch sử bid. Được controller seller, phòng và trang chủ dùng qua registry. |
| `src/main/java/com/auction/client/service/ServiceRegistry.java` | tracked | Giữ | Giữ nguyên bộ sáu service, chọn mode mock/server, tạo một ServerServices dùng chung cho các interface và từ chối trộn implementation. Thay bộ dịch vụ sẽ cấp tracker mới, validateMode quản lý chuyển mode và close đóng adapter. |
| `src/main/java/com/auction/client/service/UserAdminService.java` | tracked | Giữ | Interface liệt kê tài khoản, khóa/mở khóa, đổi vai trò và xóa. Server kiểm quyền theo token, tham số actor tồn tại cho API mock/client hiện hữu. |
| `src/main/java/com/auction/client/service/WalletException.java` | tracked | Giữ | RuntimeException báo lỗi ví mock như amount không hợp lệ, nạp dưới mức tối thiểu, tràn số hoặc thiếu tiền khả dụng. |
| `src/main/java/com/auction/client/service/WalletService.java` | tracked | Giữ | Interface đọc balance/held/available, nạp/rút giả lập và lịch sử giao dịch. Default getSnapshot ghép các phép đọc, implementation server override để lấy snapshot nhất quán bằng một request. |
| `src/main/java/com/auction/client/service/WalletSnapshot.java` | tracked | Giữ | Record số dư tổng, tiền giữ, khả dụng và danh sách giao dịch. Compact constructor dùng List.copyOf cho lịch sử. |
| `src/main/java/com/auction/client/service/mock/AutoBidEngine.java` | tracked | Giữ | Bộ xử lý auto-bid trên MockDataStore dưới khóa store. Duyệt luật theo thời điểm/ID, xét trần/bước tăng/ngân sách/ưu tiên bằng trần, giữ tiền, cập nhật phiên/lịch sử và phát listener. Có giới hạn 10.000 vòng. |
| `src/main/java/com/auction/client/service/mock/MockAuctionClientService.java` | tracked | Giữ | Triển khai AuctionClientService cho UI phát triển độc lập, xác thực fixture/đăng ký trong RAM, đọc phiên/sản phẩm/lịch sử, kiểm tra bid, cập nhật giá và gọi AutoBidEngine. Cùng dùng MockDataStore với các mock khác và đăng ký observer trên store. |
| `src/main/java/com/auction/client/service/mock/MockAutoBidService.java` | tracked | Giữ | Lấy tài khoản qua supplier AuctionClientService, kiểm tra phiên đang chạy, sở hữu, trần/bước/ngân sách, thay luật cũ, gọi engine và trả bản copy. Hủy luật hoặc đọc cấu hình trên cùng store. |
| `src/main/java/com/auction/client/service/mock/MockDataStore.java` | tracked | Giữ | Kho RAM chung cho auction/item/user, bid history, ảnh, số dư, holds, giao dịch và luật auto-bid. Singleton phục vụ mock, createForTesting cấp fixture riêng. Đồng bộ cập nhật tiền, giải phóng/settle holds, refresh status và phát observer. Có dữ liệu mẫu dành cho mode mock. |
| `src/main/java/com/auction/client/service/mock/MockManagementService.java` | tracked | Giữ | Triển khai đồng thời ProductService, AuctionAdminService, UserAdminService trên store chung. Kiểm tra seller khi sửa/xóa phiên, hạn chế phiên đã bid, quản lý ảnh/lịch sử, hủy giải phóng holds và chặn admin tự khóa/xóa mình. |
| `src/main/java/com/auction/client/service/mock/MockWalletService.java` | tracked | Giữ | Đọc số dư/held/available và lịch sử từ store, nạp tối thiểu 10.000, dùng Math.addExact chống tràn và rút theo khả dụng. Thao tác dưới khóa store cùng với bid/settlement. |
| `src/main/java/com/auction/client/service/server/JsonTransport.java` | tracked | Giữ | Vận chuyển framed JSON qua Socket, nối theo timeout, requestId ghép CompletableFuture với response. Mỗi connection có reader virtual thread, ghi đồng bộ trên output stream, event chuyển executor riêng để listener có thể chờ request khác. Generation nhận diện reconnect, fail hoàn tất các pending exceptionally. |
| `src/main/java/com/auction/client/service/server/ServerError.java` | tracked | Giữ | RuntimeException giữ errorCode của từ chối backend và ánh xạ thông báo tiếng Việt cho quyền, giá, phiên, ví, storage và xác thực. Được adapter/controller dùng để phân biệt từ chối rõ ràng với lỗi vận chuyển. |
| `src/main/java/com/auction/client/service/server/ServerMapper.java` | tracked | Giữ | Mapper package-private chuyển JsonObject/JsonElement thành User, Auction/Item, Bid, AutoBid và Transaction phía client. Chuẩn hóa thời gian, số nguyên và enum, khôi phục đầy đủ trạng thái auto-bid. |
| `src/main/java/com/auction/client/service/server/ServerServices.java` | tracked | Giữ | Adapter triển khai cả sáu interface trên một token/JsonTransport. Ánh xạ action TCP cho auth, sản phẩm/ảnh, bid, ví, auto-bid và quản trị. Lưu pending command bằng file lock/force/atomic move, replay cùng commandId, đối soát trước lệnh tiếp theo. Heartbeat 5 giây, đăng ký lại feed theo generation, đồng bộ giờ và cô lập lỗi từng observer. |
| `src/main/java/com/auction/client/ui/AppFooter.java` | tracked | Giữ | Thành phần VBox chân trang với liên kết hướng dẫn/liên hệ. install bọc content trong layout đảm bảo chân trang nằm phía dưới viewport và áp điều hướng qua ViewNavigator. |
| `src/main/java/com/auction/client/ui/AppHeader.java` | tracked | Giữ | Thành phần HBox được FXML tạo, theo dõi scene để gắn/gỡ listener, hiển thị tài khoản/menu và trạng thái kết nối. Đọc số dư nền, cập nhật theo event và dùng timer riêng trong mode mock. |
| `src/main/java/com/auction/client/ui/Icons.java` | tracked | Giữ | Bộ path SVG nhúng ngay trong Java với danh sách tên icon và factory tạo Region/SVGPath co giãn. FXML/controller và các thành phần UI dùng graphic từ đây. |
| `src/main/java/com/auction/client/ui/PasswordFieldWithToggle.java` | tracked | Giữ | Bind text hai chiều giữa PasswordField/TextField, đổi visible/managed theo nút và giữ caret/selection/focus. Thay icon, accessible text và tooltip theo trạng thái hiện mật khẩu. |
| `src/main/java/com/auction/client/ui/ProductImages.java` | tracked | Giữ | Nhập ảnh vào thư mục ảnh người dùng, tải ảnh từ path/file URI/HTTP với connect/read timeout và deadline/interrupt checks. Chỉ xóa bản ảnh thuộc thư mục ứng dụng với tiền tố auctionId, trả null khi đọc lỗi/hủy và ghi log phù hợp. |
| `src/main/java/com/auction/client/ui/RoundedClip.java` | tracked | Giữ | Gắn Rectangle bo góc làm clip cho Region và bind chiều rộng/cao theo node, phục vụ ảnh/card không tràn góc. |
| `src/main/java/com/auction/client/ui/SmoothScroll.java` | tracked | Giữ | Xử lý ScrollEvent để chuẩn hóa wheel thành dịch chuyển mượt theo pixel trên ScrollPane. Nhận biết control cuộn lồng nhau để giữ thao tác cho bảng/list/text area bên trong. |
| `src/main/java/com/auction/client/ui/UserMenu.java` | tracked | Giữ | Nút tài khoản và Popup điều hướng theo role, tên người dùng, ví, sản phẩm, admin và logout. Quản lý menu đang mở, Escape/click ngoài, scene handlers và đóng menu trước khi chuyển route. |
| `src/main/java/com/auction/client/util/AppInfo.java` | tracked | Giữ | Chứa hằng NAME của ứng dụng, được dùng khi đặt tiêu đề cửa sổ và thương hiệu UI. |
| `src/main/java/com/auction/client/util/AuctionTime.java` | tracked | Giữ | Parse giờ HH:mm bằng định dạng nghiêm ngặt, kiểm tra start/end khác null, start từ now trở đi và end sau start. Tóm tắt thời lượng ngày/giờ/phút và giờ kết thúc cho biểu mẫu seller. |
| `src/main/java/com/auction/client/util/FormatUtil.java` | tracked | Giữ | Định dạng tiền Việt từ long/double, nhãn giá rút gọn trên trục biểu đồ, thời gian biểu đồ và che username khi hiển thị bid. Overload long giữ đầy đủ chữ số của số dư. |
| `src/main/java/com/auction/client/util/Validators.java` | tracked | Giữ | Kiểm tra và chuẩn hóa username, email theo giới hạn/định dạng, password theo độ dài và chữ/số, và so khớp password xác nhận. Đây là validation hỗ trợ form client. |
| `src/main/java/com/auction/server/dao/AuctionJsonMapper.java` | untracked | Giữ | Đọc bảy trường luật phiên từ snapshot schemaVersion 1 thành server.model.Auction. Xuất BidTransaction thành đúng sáu trường legacy, ánh xạ automatic thành auto và giữ timestamp ISO. Phần JSON khác vẫn do service giữ nguyên. |
| `src/main/java/com/auction/server/dao/AuctionRepository.java` | tracked | Giữ | Quản lý một snapshot JSON và khóa file độc quyền thư mục data. load dùng Gson strict, chặn save trước load thành công hoặc sau close. save ghi đủ ByteBuffer vào state.json.tmp, force(true) rồi ATOMIC_MOVE/REPLACE_EXISTING, close giải phóng lock/channel. |
| `src/main/java/com/auction/server/exception/ApiException.java` | tracked | Giữ | RuntimeException nghiệp vụ có code ổn định để service/handler tạo response JSON lỗi, giữ tách biệt với IOException lưu trữ/vận chuyển. |
| `src/main/java/com/auction/server/model/Admin.java` | tracked | Giữ | Subclass final của User, constructor truyền thuộc tính chung và override getRoleName trả ADMIN. AuctionService dùng trong createUser. |
| `src/main/java/com/auction/server/model/Art.java` | tracked | Giữ | Subclass final của Item, override getCategory trả ART, được ItemFactory chọn khi category tương ứng. |
| `src/main/java/com/auction/server/model/Auction.java` | untracked | Giữ | Record bất biến giữ id, sellerId, status, startTime/endTime, currentPrice và minBidStep. minimumNextBid cộng currentPrice với minBidStep, requireBidder chặn ADMIN/chủ phiên rồi kiểm tra trạng thái và biên thời gian. requireMinimumBid chặn giá dưới mức kế tiếp, giới hạn định dạng tiền và số dư do service kiểm tra. |
| `src/main/java/com/auction/server/model/BidTransaction.java` | untracked | Giữ | Record bất biến của một bid đã chấp nhận gồm id, auctionId, bidderUsername, amount long, timestamp và automatic, được mapper ghi vào lịch sử bid. |
| `src/main/java/com/auction/server/model/Bidder.java` | tracked | Giữ | Subclass final của User, override getRoleName trả BIDDER, tham gia dispatch vai trò khi tạo user và test đa hình. |
| `src/main/java/com/auction/server/model/Electronics.java` | tracked | Giữ | Subclass final của Item, override getCategory trả ELECTRONICS, được Factory dùng trong đường tạo sản phẩm thật. |
| `src/main/java/com/auction/server/model/Entity.java` | tracked | Giữ | Abstract base giữ id/name private final và getter, là gốc chung của hai nhánh User và Item. |
| `src/main/java/com/auction/server/model/Item.java` | tracked | Giữ | Abstract Item kế thừa Entity, giữ description và Seller owner, cung cấp getter và hợp đồng getCategory cho các subclass sản phẩm. |
| `src/main/java/com/auction/server/model/ItemFactory.java` | tracked | Giữ | Abstract creator có create, selector forCategory trả creator anonymous override để tạo Electronics/Art/Vehicle hoặc CategoryItem cho JEWELRY/REAL_ESTATE/OTHER. Category không hỗ trợ ném lỗi, giữ đa hình getCategory tại caller. |
| `src/main/java/com/auction/server/model/Seller.java` | tracked | Giữ | Subclass final của User, override getRoleName trả SELLER. Đồng thời là kiểu owner của Item khi Factory tạo sản phẩm. |
| `src/main/java/com/auction/server/model/User.java` | tracked | Giữ | Abstract User kế thừa Entity, giữ passwordHash private final và hợp đồng getRoleName để subclass trả vai trò khi service tạo tài khoản. |
| `src/main/java/com/auction/server/model/Vehicle.java` | tracked | Giữ | Subclass final của Item, override getCategory trả VEHICLE, giữ các thuộc tính sản phẩm/owner qua constructor cha. |
| `src/main/java/com/auction/server/network/Server.java` | tracked | Giữ | TCP server bind cấu hình mặc định 0.0.0.0 và port 8888, có default instance, acceptor và virtual-thread executor. Mỗi Client có hàng đợi gửi giới hạn 256, reader/writer, subscription feed/phòng và token. Scheduler tick 250 ms, publish định tuyến event sau commit, main cài shutdown hook và close thu hồi tài nguyên. |
| `src/main/java/com/auction/server/service/AntiSnipingPolicy.java` | tracked | Giữ | Policy bất biến với threshold mặc định 30 giây và extension 60 giây. Chỉ gia hạn khi 0 < thời gian còn lại <= threshold, cộng extension vào deadline hiện tại, constructor từ chối cấu hình không hợp lệ. |
| `src/main/java/com/auction/server/service/AuctionService.java` | tracked | Giữ | Điểm vào nghiệp vụ đồng bộ cho refresh phiên, auth/session 12 giờ, phân quyền, sản phẩm/ảnh, ví/held/settlement, bid/auto-bid và quản trị. Mỗi execute deep-copy snapshot, kiểm tra commandId/fingerprint, lưu receipt và commit trước publish. Lỗi lưu rollback toàn state/events. Tiền long giới hạn 9.000.000.000.000, auto-bid ưu tiên order và giới hạn 10.000 vòng. Constructor mở repository/kiểm tra schema, seedDemo chỉ khi cấu hình demo. |
| `src/main/java/com/auction/server/service/Passwords.java` | tracked | Giữ | Tiện ích package-private tạo salt ngẫu nhiên, derive PBKDF2 và verify bằng so sánh byte an toàn thời gian. Service dùng để băm credentials và fingerprint đăng ký trước khi lưu. |
| `src/main/java/com/auction/shared/JsonProtocol.java` | tracked | Giữ | Hợp đồng chung version 1 và frame tối đa 8 MiB. readFrame đọc length int rồi đủ UTF-8 JSON object, từ chối length hoặc JSON lỗi. writeFrame ghi length/payload/flush. obj/text/number dựng JSON, đọc chuỗi và số nguyên chính xác. |
| `src/main/resources/com/auction/client/contacts.properties` | tracked | Giữ | ContactController.loadMembers, vòng lặp 1..4. |
| `src/main/resources/com/auction/client/css/app.css` | tracked | Giữ | ViewNavigator.APP_STYLESHEET, applyAppStylesheet cho Scene/DialogPane. |
| `src/main/resources/com/auction/client/views/admin-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/main/resources/com/auction/client/views/auction-room-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/main/resources/com/auction/client/views/auth-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/main/resources/com/auction/client/views/contact-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/main/resources/com/auction/client/views/guide-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/main/resources/com/auction/client/views/home-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/main/resources/com/auction/client/views/seller-products-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/main/resources/com/auction/client/views/wallet-view.fxml` | tracked | Giữ | FXML controller/handler/route đang được load, có test JavaFX trong lượt cuối. |
| `src/test/java/com/auction/client/controller/ClientUiTest.java` | tracked | Giữ | Kiểm thử toolkit JavaFX thật với UI mock, đăng ký/login/tìm kiếm/điều hướng và đủ route theo role. Kiểm tra chart/lịch sử/vòng đời phòng, queue rejection, timeout/UNKNOWN/read revision/chuyển phòng, card không trùng và loading/lỗi/rỗng, số dư long ở hai độ rộng, form auth/cửa sổ nhỏ. Dùng fixture điều khiển luồng và ghi ảnh QA. Opt-in auction.ui.tests=true. |
| `src/test/java/com/auction/client/navigation/ImageTasksTest.java` | tracked | Giữ | Kiểm tra pool ảnh đầy hoặc bị chặn vẫn để lệnh nghiệp vụ chạy, cancel scope gỡ hàng đợi/interrupt đúng read, loại ảnh cũ khi đổi filter/tái dùng cell và phục hồi sau lỗi ảnh. Kiểm tra lệnh đã nhận vẫn hoàn tất khi đóng scope. |
| `src/test/java/com/auction/client/navigation/ViewTasksTest.java` | tracked | Giữ | Kiểm tra latest-result wins, chặn callback/event sau rời view, giữ mutation đã nhận mà không gửi lại và giao lỗi về UI. Executor/UI dispatcher điều khiển được giúp tái tạo thứ tự chạy. |
| `src/test/java/com/auction/client/service/BidSubmissionTrackerTest.java` | tracked | Giữ | Kiểm tra timeout giữ UNKNOWN, read cũ hoặc bắt đầu trước response không mở khóa, read lỗi vẫn khóa, command cũ không đổi command mới, cô lập theo user/auction và chỉ queue rejection chứng minh chưa gửi. |
| `src/test/java/com/auction/client/service/MockAuctionBidAccessTest.java` | tracked | Giữ | Kiểm tra guest chưa login bị từ chối bid và seller không được bid sản phẩm của chính mình trong mock. |
| `src/test/java/com/auction/client/service/MockAuthIntegrationTest.java` | tracked | Giữ | Kiểm tra tài khoản đăng ký và login fixture dùng chung trạng thái quản trị trong MockDataStore, gồm khóa tài khoản và vai trò đã đổi. |
| `src/test/java/com/auction/client/service/MockAutoBidServiceTest.java` | tracked | Giữ | Kiểm tra ưu tiên đăng ký khi bằng trần, không tự vượt giá chính mình, dừng ở trần, listener qua nhiều service, validation/hủy và manual bid đồng thời giữ giá đơn điệu/đúng một hold. |
| `src/test/java/com/auction/client/service/MockManagementServiceTest.java` | tracked | Giữ | Kiểm tra admin không tự khóa/xóa, hủy cần lý do và trạng thái OPEN/RUNNING, seller không xóa phiên đã bid hoặc thuộc người khác. |
| `src/test/java/com/auction/client/service/MockSharedStoreTest.java` | tracked | Giữ | Kiểm tra sản phẩm mới/sửa/xóa và chuyển trạng thái hiện đồng nhất qua auction service và management service, đồng thời chuẩn hóa danh mục legacy/current. |
| `src/test/java/com/auction/client/service/MockWalletServiceTest.java` | tracked | Giữ | Kiểm tra nạp/rút/validation, giữ tiền khi bid, giải phóng khi vượt giá/hủy, thanh toán khi hết phiên và nạp đồng thời không mất cập nhật. |
| `src/test/java/com/auction/client/service/ServiceRegistryTest.java` | tracked | Giữ | Kiểm tra mode không hợp lệ gây lỗi và thay riêng auction service không âm thầm giữ các service mock còn lại. |
| `src/test/java/com/auction/client/ui/ProductImagesIoTest.java` | tracked | Giữ | Kiểm tra local read đã cancel/hết deadline dừng ngay và HTTP body bị treo kết thúc theo read timeout. Dùng TempDir và HTTP server cục bộ. |
| `src/test/java/com/auction/client/ui/ProductImagesTest.java` | tracked | Giữ | Đổi user.home tạm trong try/finally, import một file bytes fixture, kiểm tra path và bytes sao chép trong .digital-auction/images, kiểm tra load path thiếu trả null rồi phục hồi user.home. |
| `src/test/java/com/auction/client/ui/UiResourcesTest.java` | tracked | Giữ | Kiểm tra hai ảnh nền trang trí cũ và AppBackground.class vắng mặt, ProductImages.class, app.css và auth-view.fxml hiện hữu. Bảo vệ cấu trúc tài nguyên hiện tại. |
| `src/test/java/com/auction/client/util/AuctionTimeTest.java` | tracked | Giữ | Kiểm tra parse giờ nghiêm ngặt, chuỗi tóm tắt thời lượng và biên start/end so với now. |
| `src/test/java/com/auction/client/util/FormatUtilTest.java` | tracked | Giữ | Kiểm tra định dạng tiền long giữ đủ chữ số ở các mức tiền lớn, bảo vệ số dư ví khỏi rút gọn/mất chính xác. |
| `src/test/java/com/auction/client/util/ValidatorsTest.java` | tracked | Giữ | Parameterized tests cho email hợp lệ/sai/quá dài, username, password và confirm. Các nguồn ValueSource/NullAndEmptySource/MethodSource tạo nhiều lần chạy trên một method. |
| `src/test/java/com/auction/integration/FullStackTest.java` | tracked | Giữ | Integration qua ServerServices và TCP server thật trên TempDir/port 0. Kiểm tra seller ảnh/CRUD/admin, hai adapter nhận bid/ví/auto-bid, observer lỗi, auth/quyền, pending qua restart, server unavailable/reconnect. DroppingProxy làm mất ACK đầu để kiểm chứng replay cùng commandId chỉ ghi một bid. |
| `src/test/java/com/auction/integration/ServerUiIntegrationTest.java` | tracked | Giữ | Integration JavaFX và server TCP thật, seller/bidder/admin adapter cùng data tạm và port 0. Nạp các màn hình, bid/realtime/ví/auto-bid/quyền và ghi ảnh server-room/server-wallet. Opt-in auction.ui.tests=true. |
| `src/test/java/com/auction/server/dao/AuctionJsonMapperTest.java` | untracked | Giữ | Kiểm tra đọc snapshot legacy không sửa lịch sử/ảnh/trường mở rộng và ghi đúng sáu trường bid, cờ auto, timestamp và tiền nguyên long. |
| `src/test/java/com/auction/server/dao/AuctionRepositoryTest.java` | untracked | Giữ | Kiểm tra snapshot qua restart, bỏ qua file tmp chưa commit, khóa repository thứ hai, JSON sai không reset/ghi đè, lỗi ghi giữ file đã commit rồi retry và save chỉ sau load thành công/trước close. Có parameterized dữ liệu hỏng. |
| `src/test/java/com/auction/server/model/AuctionTest.java` | untracked | Giữ | Kiểm tra giá đầu phải đủ bước, cho phép nhảy bước lớn, vai trò/sở hữu bidder, biên start inclusive/end exclusive và các trạng thái ngoài RUNNING bị từ chối. |
| `src/test/java/com/auction/server/model/ItemFactoryTest.java` | untracked | Giữ | Kiểm tra đủ sáu category tạo đúng loại/thuộc tính/owner, từ chối category lạ và dispatch getRoleName theo subclass User thực tế. |
| `src/test/java/com/auction/server/network/ServerIntegrationTest.java` | tracked | Giữ | Dùng Peer socket thật kiểm tra hai client nhận push và replay một bid sau reconnect, bid bằng nhau đồng thời chỉ commit một lần, frame sai/ngắt một client không dừng server và length quá lớn bị chặn trước allocation. |
| `src/test/java/com/auction/server/service/AntiSnipingPolicyTest.java` | tracked | Giữ | Kiểm tra bid xa deadline, trong ngưỡng, đúng ngưỡng, sau deadline và constructor cấu hình sai. |
| `src/test/java/com/auction/server/service/AuctionServiceTest.java` | tracked | Giữ | Test nghiệp vụ với MutableClock/TempDir gồm credential/public DTO, đăng ký/quyền, ví/holds, bước giá/tiền nguyên, phiên theo lịch, settlement một lần qua restart, anti-sniping/hủy/auto-bid, commandId/fingerprint, cùng ví nhiều phiên và bid bằng nhau đồng thời, admin/seller CRUD, lỗi ghi rollback và không push rồi retry cùng command. |
| `src/test/java/com/auction/server/service/AuctionStateValidationTest.java` | untracked | Giữ | Parameterized test snapshot thiếu/sai schema/version/collection, yêu cầu constructor từ chối khởi động, giữ nguyên bytes file và giải phóng khóa để thử lại. |

| File cục bộ được giữ | Git | Byte | Căn cứ |
| --- | --- | --- | --- |
| `.github/modernize/java-upgrade/.gitignore` | ignored | 6 | REVIEW. Hook công cụ Java upgrade ghi tool-use JSONL từ stdin và gitignore riêng, phục vụ phát triển. Không có caller trong src/pom/CI. Có thể được extension gọi động nên giữ. |
| `.github/modernize/java-upgrade/hooks/scripts/recordToolUse.ps1` | ignored | 695 | REVIEW. Hook công cụ Java upgrade ghi tool-use JSONL từ stdin và gitignore riêng, phục vụ phát triển. Không có caller trong src/pom/CI. Có thể được extension gọi động nên giữ. |
| `.github/modernize/java-upgrade/hooks/scripts/recordToolUse.sh` | ignored | 608 | REVIEW. Hook công cụ Java upgrade ghi tool-use JSONL từ stdin và gitignore riêng, phục vụ phát triển. Không có caller trong src/pom/CI. Có thể được extension gọi động nên giữ. |
| `.idea/.gitignore` | ignored | 238 | Quy tắc bỏ qua workspace/tasks nội bộ IntelliJ. |
| `.idea/compiler.xml` | ignored | 548 | Cấu hình annotation processing/output compiler trỏ target. |
| `.idea/encodings.xml` | ignored | 267 | Thiết lập UTF-8 cho Java/resources trong IDE. |
| `.idea/jarRepositories.xml` | ignored | 864 | Danh sách Maven repositories IDE sử dụng. |
| `.idea/misc.xml` | ignored | 463 | Thông tin Maven project và JDK trong IntelliJ. |
| `.idea/vcs.xml` | ignored | 172 | Mapping thư mục project sang Git. |
| `.idea/workspace.xml` | ignored | 5179 | Trạng thái workspace/run configuration cục bộ, có cấu hình môi trường. Giá trị cấu hình được lược khỏi báo cáo. REVIEW, giữ thiết lập người dùng. |
| `.vscode/settings.json` | ignored | 70 | Thiết lập Java updateBuildConfiguration interactive cho VS Code. |
| `data/server.lock` | ignored | 0 | File lock server dùng khi mở repository, kích thước 0 vẫn có vai trò khóa độc quyền. |
| `data/state.json` | ignored | 195143 | Snapshot nghiệp vụ thật của server. Đối chiếu SHA-256 trước/sau thành công, nội dung được giữ ngoài báo cáo. |
| `data/backups/json-only-20261111-233921/checkpoint.json` | ignored | 24562 | Backup/checkpoint lịch sử cục bộ. Chỉ ghi đường dẫn/kích thước, giữ để khôi phục, chưa mở ZIP hoặc đọc nội dung dữ liệu. |
| `data/backups/json-only-20261111-233921/state.json` | ignored | 6606 | Backup/checkpoint lịch sử cục bộ. Chỉ ghi đường dẫn/kích thước, giữ để khôi phục, chưa mở ZIP hoặc đọc nội dung dữ liệu. |
| `data/backups/json-only-20261111-233921/working-tree.patch` | ignored | 26339 | Backup/checkpoint lịch sử cục bộ. Chỉ ghi đường dẫn/kích thước, giữ để khôi phục, chưa mở ZIP hoặc đọc nội dung dữ liệu. |
| `data/backups/json-only-20261111-233921/working-tree.zip` | ignored | 607007 | Backup/checkpoint lịch sử cục bộ. Chỉ ghi đường dẫn/kích thước, giữ để khôi phục, chưa mở ZIP hoặc đọc nội dung dữ liệu. |

## Build, JUnit, JaCoCo và Checkstyle

| Kiểm chứng | Kết quả và phạm vi |
| --- | --- |
| Maven clean đầu | FAILURE. Log mở trong target làm Windows chặn xóa inventory-verify.log. Lỗi thao tác ghi log, chưa vào test. |
| Maven clean lần sửa | SUCCESS. Log chuyển ra thư mục checkpoint, tạo test-tmp sau clean. |
| Maven verify cuối | BUILD SUCCESS, JDK 25.0.4.1, Maven 3.10.0, kết thúc 16:20:31 ngày 12/11/2026 UTC+7, 03:27. |
| JUnit | 164 tests, 0 failures, 0 errors, 0 skipped, 27 suite. |
| JavaFX opt-in | 12 ClientUiTest và 1 ServerUiIntegrationTest chạy trong lượt 164 test. |
| Checkstyle | 0 vi phạm, 106 Java files, baseline whitespace trong config/checkstyle.xml. |
| JaCoCo line | 4606/6061 = 75.99% |
| JaCoCo branch | 1541/2505 = 61.52% |
| JaCoCo instruction | 22806/28781 = 79.24% |
| JaCoCo method | 777/1057 = 73.51% |
| JaCoCo class | 102/106 = 96.23% |
| JAR | Manifest Server, Java-Version 25, class major 69, không có MySQL/StateStore trong entry đã kiểm tra. |
| CI | Đã đọc workflow, có Java 25/Xvfb/UI tests/report upload. Chưa chạy GitHub-hosted workflow. |
| Dữ liệu thật | SHA-256 state.json trước/sau khớp, sáu file data/backup vẫn hiện hữu cùng kích thước. |
| LAN/tải/phần cứng | Chưa chạy hai máy vật lý, tải lớn, mất điện/hỏng đĩa hoặc filesystem thiếu atomic move. |

Lệnh thực tế theo đúng thứ tự, log được ghi ra thư mục checkpoint khi thực thi. Maven dùng cache workspace ở chế độ offline. Chạy verify sau khi tạo lại target/test-tmp.

```powershell
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' clean
New-Item -ItemType Directory -Path target/test-tmp -Force
mvn -o -B '-Dmaven.repo.local=D:\Bai-Tap-Lon-gon\.maven-repository' '-DargLine=-Djava.io.tmpdir=D:\Bai-Tap-Lon-gon	arget	est-tmp' '-Dauction.ui.tests=true' verify
```

### Kết quả từng suite từ Surefire XML mới

| Suite | Tests | Failures | Errors | Skipped | Giây |
| --- | --- | --- | --- | --- | --- |
| com.auction.client.controller.ClientUiTest | 12 | 0 | 0 | 0 | 20.351 |
| com.auction.client.navigation.ImageTasksTest | 5 | 0 | 0 | 0 | 0.109 |
| com.auction.client.navigation.ViewTasksTest | 4 | 0 | 0 | 0 | 0.105 |
| com.auction.client.service.BidSubmissionTrackerTest | 4 | 0 | 0 | 0 | 0.09 |
| com.auction.client.service.MockAuctionBidAccessTest | 1 | 0 | 0 | 0 | 8.129 |
| com.auction.client.service.MockAuthIntegrationTest | 2 | 0 | 0 | 0 | 8.141 |
| com.auction.client.service.MockAutoBidServiceTest | 5 | 0 | 0 | 0 | 8.182 |
| com.auction.client.service.MockManagementServiceTest | 3 | 0 | 0 | 0 | 8.131 |
| com.auction.client.service.MockSharedStoreTest | 2 | 0 | 0 | 0 | 8.155 |
| com.auction.client.service.MockWalletServiceTest | 4 | 0 | 0 | 0 | 8.159 |
| com.auction.client.service.ServiceRegistryTest | 2 | 0 | 0 | 0 | 8.168 |
| com.auction.client.ui.ProductImagesIoTest | 2 | 0 | 0 | 0 | 8.256 |
| com.auction.client.ui.ProductImagesTest | 1 | 0 | 0 | 0 | 8.176 |
| com.auction.client.ui.UiResourcesTest | 1 | 0 | 0 | 0 | 0.075 |
| com.auction.client.util.AuctionTimeTest | 3 | 0 | 0 | 0 | 0.097 |
| com.auction.client.util.FormatUtilTest | 1 | 0 | 0 | 0 | 0.104 |
| com.auction.client.util.ValidatorsTest | 43 | 0 | 0 | 0 | 0.256 |
| com.auction.integration.FullStackTest | 8 | 0 | 0 | 0 | 10.721 |
| com.auction.integration.ServerUiIntegrationTest | 1 | 0 | 0 | 0 | 16.378 |
| com.auction.server.dao.AuctionJsonMapperTest | 3 | 0 | 0 | 0 | 0.248 |
| com.auction.server.dao.AuctionRepositoryTest | 10 | 0 | 0 | 0 | 8.393 |
| com.auction.server.model.AuctionTest | 7 | 0 | 0 | 0 | 0.182 |
| com.auction.server.model.ItemFactoryTest | 8 | 0 | 0 | 0 | 0.176 |
| com.auction.server.network.ServerIntegrationTest | 3 | 0 | 0 | 0 | 8.827 |
| com.auction.server.service.AntiSnipingPolicyTest | 5 | 0 | 0 | 0 | 0.098 |
| com.auction.server.service.AuctionServiceTest | 17 | 0 | 0 | 0 | 12.476 |
| com.auction.server.service.AuctionStateValidationTest | 7 | 0 | 0 | 0 | 8.359 |

Có 27 XML TEST-*.xml tương ứng đúng 27 file test hiện hữu. 164 là số lần chạy kể cả parameterized invocations. JaCoCo lấy counter cấp root từ target/site/jacoco/jacoco.xml của lượt clean, không cộng kết quả lịch sử. Line coverage 4.606/6.061 = 75,99%, branch 1.541/2.505 = 61,52%. Biến thiên với lượt 15:58 trong báo cáo viva có thể do đường thực thi bất đồng bộ, chưa xác định riêng từng nhánh gây chênh lệch.

Checkstyle kiểm tra tab/trailing whitespace/newline UTF-8 cho Java main/test theo cấu hình hiện hữu. Workflow giữ Java 25/Xvfb/UI opt-in và upload artifact, chưa được thực thi trên GitHub trong lần này. JAR manifest, class major và absence MySQL được kiểm tra trực tiếp qua ZIP entries của artifact cuối.

Cảnh báo quan sát trong build gồm API deprecated, JavaFX/native access và tài nguyên MANIFEST/module-info trùng khi shade, Windows Preferences từ sandbox. Một số test cố ý tạo lỗi ghi để kiểm rollback, thông báo State commit failed thuộc các case đó. Kết luận JUnit lấy từ XML và exit code cuối. Các cảnh báo này được giữ trong log hiện tại.

## File nghi ngờ và công việc còn lại

- Java không có caller Java production được kiểm tra thêm. App/Main/Server là entry point qua script/POM/bootstrap, các controller qua fx:controller, AppHeader qua FXML import. Mọi interface service có implementation mock/server và caller thực. DisposableView được navigator kiểm tra vòng đời. Java/test vẫn có trách nhiệm theo bảng kiểm kê.
- Tám FXML có route/đường load và controller tương ứng, một CSS được nạp qua APP_STYLESHEET, contacts.properties có getResourceAsStream. Không có ảnh tĩnh được ship trong src/main/resources. Ảnh sản phẩm đến từ GET_IMAGE hoặc path người dùng, vì vậy tìm kiếm basename resource không đủ mô tả ảnh runtime.
- Không tìm thấy Class.forName hoặc ServiceLoader trong src hiện tại. Reflection vẫn xuất hiện trong helper GUI test để đọc field/gọi handler, FXML dùng reflection để inject/controller và JUnit discovery nạp test. Các đường này được giữ trong đánh giá.
- POM có năm dependency trực tiếp. Gson phục vụ protocol/mapper/repository/service. javafx-controls phục vụ UI, javafx-fxml phục vụ loader/annotation. JUnit Jupiter dùng cho mọi suite và parameterized test. javafx-swing được ClientUiTest/ServerUiIntegrationTest dùng qua SwingFXUtils để screenshot. Scope compile của javafx-swing là REVIEW vì call site hiện ở test, chưa đổi scope/packaging trong lần kiểm kê này.
- Src, POM, launcher, config và CI hiện tại không còn MySQL/JDBC/AUCTION_DB hoặc StateStore. Không có SQL trong phạm vi nguồn/config quan sát. Tên MySQL và migration tồn tại trong tài liệu archive, link tham khảo PDF và có thể trong cache/backup lịch sử. JAR cuối được kiểm tra không chứa com/mysql, MysqlStateStore hoặc StateStore.class.
- So SHA-256 mọi file được duy trì trước dọn không phát hiện bản trùng nguyên byte. Các test cùng chạm bid/ví có mức kiểm chứng khác nhau ở mock/domain/service/socket/adapter/GUI, danh sách case và kết quả từng suite được giữ. Chưa thực hiện detector trùng lặp ngữ nghĩa trên mọi đoạn code.
- docs/VAN_DAP.md giao chủ đề với docs/viva, các báo cáo archive giao phần persistence/test nhưng giữ sự kiện lịch sử khác nhau. Hai PDF có mục đích đề bài và lộ trình riêng. .github/modernize có hook công cụ phát triển, IDE chứa thiết lập riêng. Các nhóm còn giá trị hoặc chưa đủ bằng chứng bỏ được giữ ở REVIEW.
- wallet-view.fxml có đoạn giải thích settlement gọi tên mock trong giao diện dùng chung server/mock. JavaDoc AutoBidService/WalletService cũng thiên về mô phỏng trong khi ServerServices có implementation thật. Đây là nội dung cần rà lại, chưa thay wording hoặc logic trong lần kiểm kê.

- Giới hạn bảo tồn raw build cũ đã ghi ở phần output clean. Chưa có bản sao raw log viva-final-verify.log trong checkpoint mới.
- Checkpoint nằm ở thư mục tạm ngoài Git. Việc lưu checkpoint lâu dài phụ thuộc vòng đời thư mục đó.
- Chưa chạy LAN hai máy vật lý, GitHub-hosted CI, tải lớn hoặc thử lỗi phần cứng/filesystem không atomic. TCP hiện truyền trong LAN, session RAM và full snapshot/global monitor giới hạn khả năng mở rộng.
- Chưa phân tích call graph theo symbol/reflection đầy đủ và chưa dùng detector duplication ngữ nghĩa. Candidate thiếu text reference được giữ theo FXML/entry point/discovery và bảng dependency.
- Tài liệu PDF được phân loại bằng text trích xuất đủ trang. Text có lỗi font dấu tiếng Việt, chưa render để đánh giá bảng điểm trong lần này.

## Đối chiếu hoàn tất

Các phép đối chiếu tự động cuối gồm đủ 106 hàng Java duy nhất, đầy đủ mô tả do đọc source và AST, mọi file duy trì và ignored trong phạm vi có trong inventory, tổng physical = code + comment + blank, 27 suite và 164 test, link Markdown cục bộ còn tồn tại, 124 file nguồn/config/UI khớp checkpoint, PDF và dữ liệu thật còn ở vị trí gốc. Số liệu filesystem và các report tự thống kê được chốt sau khi sinh đủ ba báo cáo. Chi tiết kết quả đối chiếu được đưa vào bảng dưới sau khi chạy validator.

| Phép kiểm tra cuối | Kết quả quan sát |
| --- | --- |
| Hash nguồn/config/UI | 124/124 khớp checkpoint |
| Phủ mô tả và số dòng Java | 106/106, mỗi file một hàng chính trong inventory |
| Phủ file filesystem hiện hữu | 689/689, gồm ignored trong phạm vi |
| Danh sách output clean trước dọn | 528/528 đường dẫn được ghi |
| Liên kết Markdown cục bộ | 209/209 có đích tồn tại, không kiểm URL ngoài |
| Cấu trúc bảng Markdown | Tất cả hàng có cùng số cột với header trong từng bảng |
| Whitespace ba báo cáo | Không có trailing whitespace |
| Số dòng tự thống kê báo cáo | 3/3 khớp file thực tế và tổng Markdown khớp |
| Fixture bộ đếm | 6/6 kiểm tra comment/string/char/textblock/XML/script đạt kỳ vọng |
| Đẳng thức dòng vật lý | 124/124 có physical = code + comment-only + blank |
| Data/PDF | state hash khớp, 6 file data/backup giữ kích thước, 2 PDF giữ hash |
| Đối chiếu JUnit/Checkstyle | 27 suite, 164 test, 0 failure/error/skipped và 0 vi phạm |
| Whitespace diff | git diff --check exit 0 |
