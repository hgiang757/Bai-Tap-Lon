# Tối giản UI và đối chiếu yêu cầu giảng viên

14/11/2026, working tree trên HEAD `f832295`. **Windows local: BUILD SUCCESS,
237/237 test PASS, 0 failure/error/skipped**, clean verify kết thúc 14:56:32 +07.
**Chưa hoàn tất nghiệm thu toàn bộ:** GitHub CI đúng revision, Linux/Xvfb và LAN
hai máy vẫn **NOT TESTED**. Không commit/push/merge; không tự chấm điểm.

## UI đã rà soát và quyết định

A = bắt buộc; B = nâng cao có điểm/nghiệp vụ quan trọng; C = hỗ trợ cơ bản;
D = trang trí/thao tác dư. Căn cứ: đề `2026-BTL.pdf` tr. PDF 4–10 và
`Hướng dẫn BTL LTNC-2026.pdf` tr. 1–6; chi tiết yêu cầu ở [audit trước](TEACHER_REQUIREMENTS_FINAL_AUDIT.md).

| Màn hình | A/B giữ nguyên | C giữ vì giúp sử dụng | D đã bỏ |
|---|---|---|---|
| Login/Register | Đăng ký/login, vai trò, validation và lỗi | Hiện mật khẩu, xác nhận, ghi nhớ username, cuộn form | Icon cúp, ba nhãn quảng bá; field/listener/helper chỉ phục vụ chúng |
| Home | Danh sách/chi tiết/realtime | Tìm kiếm, loại/trạng thái, tab lịch sử, tải lại | Không thêm phần phải bỏ; không mất cách xem phiên cũ |
| Seller | CRUD/quyền/lịch/giá | Ảnh, bản nháp khi lỗi, busy, Lưu/Hủy/ESC | Không bỏ helper đã có trách nhiệm rõ |
| Auction Room | Bid/leader/state/push; auto-bid, anti-sniping, chart | Countdown, bước giá nhanh, tải lại, lỗi/UNKNOWN | Không bỏ trạng thái bảo vệ lệnh đang gửi |
| Wallet | Hold/release, thanh toán và tiền khả dụng | Nạp/rút giả lập, lịch sử/lọc, giải thích số dư | Không bỏ phần đang bảo đảm bid đúng |
| Admin | Phân quyền/quản lý user và phiên | Tìm/lọc, lý do hủy, logout trực tiếp, cuộn bảng | Không đổi quyền hoặc thao tác quản trị |
| Guide/Contact | Không thay nghiệp vụ | Hướng dẫn, thông tin nhóm, copy, quay lại | Giữ nội dung ngắn hiện có |
| Header/Menu | Các mục điều hướng/logout theo role | Nút toggle, số dư ví và trạng thái kết nối | Popup riêng, auto-hide/focus/ESC handler và Robot; timer Mock không thể chạy |

Ngoài ra bỏ CSS `badge-dev`, `sidebar-card`, style của nhãn/icon đã xóa và shadow
thừa; xóa bản copy cũ `server/service/docs/reference/AuctionService.txt` không có
caller và không được biên dịch. Không tính file `.txt` này vào giảm Java LOC.
Các phần D trên không được hai PDF yêu cầu; lỗi quan trọng, form/dialog và ESC
chuẩn của chúng vẫn giữ. Không tạo framework hoặc class UI mới.

## Menu trước/sau và code đã đơn giản hóa

- `UserMenu.openMenu/hideMenu` thay Popup/tọa độ màn hình/focus/auto-hide/ESC bằng gắn/gỡ ScrollPane trong StackPane chính. `isOpen` lấy từ parent; `activeMenu` chỉ phục vụ cleanup.
- Nút/Space mở–đóng; chọn mục/logout đóng ngay; click ngoài/focus không tự đóng. Menu cuộn khi thiếu chiều cao, không đổi kích thước trang hoặc sinh Window.
- `ViewNavigator.loadView/disposeCurrentView/setPrimaryStage` quản lý container và một handler WINDOW_HIDDEN; tháo header, logout và rời trang đều gỡ pane/unbind.
- `AppHeader` bỏ timer Mock, giữ Observer ví; `AuthController` bỏ updateFeatureVisibility/setIcon. Các helper Seller/chart và Home/AuctionRoom/ServerServices giữ nguyên: callback nền, recovery, commandId và rollback cần cho tính đúng.

| Java, dòng vật lý gồm comment/dòng trống | Trước → Sau |
|---|---:|
| UserMenu | 320 → 252 |
| AppHeader | 153 → 138 |
| ViewNavigator | 314 → 330 |
| AuthController | 409 → 378 |
| Tổng production, 89 file | **11.488 → 11.390 (-98)** |
| Tổng test, 38 file | 7.743 → 7.586 (-157) |

## Đối chiếu lại PDF

PASS chỉ trong phạm vi code/test đã chạy; không suy từ test tổng thể sang mọi môi trường.

| Tiêu chí | Mức | Bằng chứng production và test thực tế |
|---|---|---|
| OOP/cây User và Item | PASS | AuthLogic.createUser, ProductLogic.createAuction; ItemFactoryTest/AuctionTest |
| Singleton production | PASS | Server constructor private/getInstance; ServerStartupTest (16 caller, restart) |
| Factory Method/Observer | PASS | ItemFactory.create; commit → publish → onEvent; ItemFactoryTest, FullStackTest.failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges |
| MVC Client/Server | PASS | FXML/controller/model, readLoop → service/domain → repository; allRoutesLoadWithAuthorizedRoles và ServerUiIntegrationTest |
| User/quyền/Seller CRUD | PASS | AuthLogic/ProductLogic; registerValidatesRoleUniquenessAndPassword, productCrudChecksOwnerAndPreservesAuctionHistory |
| Manual bid/đóng phiên/winner | PASS | BiddingLogic/SettlementLogic; firstBidRequiresStepAndIntegerAndSellerCannotBidOwnProduct, settlementPaysSellerExactlyOnceAndSurvivesRestart |
| Custom exception | PASS | Ba subclass ApiException được dùng thật; CustomExceptionTest 3 case |
| Concurrent bidding | PASS loopback | AuctionService.execute synchronized; equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice, concurrentAuctionsCannotOverspendOneWallet và ServerIntegrationTest |
| Realtime Socket | PASS loopback | Server.publish/JsonTransport/ServerServices; twoRealSocketsReceivePushAndReconnectReplaysOnlyOneBid |
| Serialization/File I/O | PASS | Gson JSON, force + atomic move, load/validate; AuctionRepositoryTest/AuctionStateValidationTest; không đổi sang ObjectOutputStream/MySQL |
| Idempotency/rollback/ví | PASS | Receipt + snapshot cùng commit; FullStackTest.lostBidAcknowledgementRetriesTheSameCommandNotAnotherBid; failedSettlementSaveRestoresBothWalletsAndRetryPaysExactlyOnce |
| Maven/Google Style/JUnit/JaCoCo | PASS Windows | Clean verify bật GUI; Google Checkstyle 0; LINE ≥60% |
| Auto-bid/PriorityQueue | PASS, tùy chọn | processAutoBids; autoBidRespectsLimitBudgetAndRegistrationOrder |
| Anti-sniping/LineChart | PASS, tùy chọn | AntiSnipingPolicyTest; antiSnipingExtendsAndDoesNotSettleAtOldDeadline; historySnapshotOrdersCapsAndReplacesPointsAndExtensionClearsUrgency |
| GitHub Actions | PARTIAL cấu hình / NOT TESTED run | Workflow giữ nguyên: Ubuntu/JDK 25/Xvfb, GUI + quality, upload artifact; chưa có run đúng revision mới |

## Kiểm thử trước/sau và tính toàn vẹn

Baseline từ ZIP bất biến: **237 test, 1 failure Robot click ngoài popup**, 0 error/skip.
Ba test cũ được thay đúng yêu cầu UI mới, không giảm số test (237 → 237):

| Test cũ | Hành vi thay thế được kiểm tra |
|---|---|
| accountPopupClosesWhenItsHeaderIsRemoved | accountMenuCleansUpOnOwnerHideHeaderRemovalAndLogout: detach/hide/logout, unbind |
| accountPopupClosesOnFocusLossEscapeAndOwnerHide | accountMenuToggleStaysInSceneAndLeavesOtherControlsUsable: toggle/Space, cùng Scene, bounds 640 px, không chặn focus/tìm kiếm |
| accountPopupSupportsOutsideClickKeyboardAndNavigation | accountMenuNavigationAndRepeatedUseDoNotDuplicateContent: chọn mục, 10 lần điều hướng, không pane/binding trùng |

Ba test menu chạy trước, rồi **3 JVM/Maven độc lập tiếp theo: 9/9 PASS**; mỗi case
toggle còn lặp 20 lần. Bỏ assertion click ngoài/focus/ESC chỉ của menu; giữ toàn
bộ assertion nghiệp vụ, bảo mật, Socket, concurrency và ESC form/dialog khác.
Lượt chốt **clean verify PASS 237/237**, có 25 ClientUiTest và 2 ServerUiIntegrationTest.
Đã sửa 3 lỗi Style ở test mới bằng final/braces; không skip/ignore failure hoặc đổi assertion.

JaCoCo LINE **4.742/5.741 = 82,60%**; BRANCH **1.706/2.357 = 72,38%**;
Google Checkstyle **0 vi phạm**; gate LINE ≥60% PASS. Không loại UI/model khỏi
coverage, không đổi POM/style/workflow. JAR không có mock/fixture/TestServers.

Checkpoint và log/ảnh/XML/manifest ở `.maven-repository/simplification-20261114/`;
raw cuối ở `target/surefire-reports`, `target/site/jacoco`, `target/checkstyle-result.xml`.
Đã xem ảnh menu 640 px và chạy lại toàn bộ layout/form/dialog tests.
**1.979/1.979 file data giữ nguyên SHA-256, 0 thêm/mất/thay đổi**; state.json trước/sau:
`577b2de3d73209689bf0e148416ef525b3a5fe45ec53c630c638c4d08aa250a9`.
Hash xác nhận 89 file backend/service/build/test lõi nguyên byte; 21 case cũ còn lại
của ClientUiTest giữ nguyên body (Auth UI chỉ thêm assertion thiếu trang trí).
Test ghi @TempDir/QA riêng, không seed hay sửa dữ liệu thật.

## Việc còn phải xác minh trước bảo vệ

GitHub CI đúng commit, Linux/Xvfb và LAN hai máy: **NOT TESTED**. WSL chưa cài,
không có Docker/Podman/Xvfb khả dụng; loopback không thay LAN. Nhóm tự review/commit/
push rồi xem artifact đúng SHA; chạy [checklist LAN đã cập nhật](viva/WINDOWS_LAN_CHECKLIST.md).
Không tuyên bố CI xanh hoặc nghiệm thu hoàn tất từ kết quả Windows này. Alt+Tab,
native FileChooser/DPI nhiều màn hình chưa kiểm lại thủ công; menu được chứng minh
cùng Scene và không sinh Window trong GUI test. Mỗi thành viên vẫn phải diễn tập
giải thích toàn source; đọc [kiến trúc](viva/ARCHITECTURE.md) và [code map](viva/CODE_MAP.md).
