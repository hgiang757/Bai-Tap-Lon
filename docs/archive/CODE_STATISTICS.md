# Thống kê code và file của dự án

> Thống kê dưới đây là mốc lịch sử. LOC sau refactor OOP/service nằm trong [OOP_SERVICE_REFACTOR_REPORT.md](OOP_SERVICE_REFACTOR_REPORT.md). Số liệu mới nhất sau tối giản JavaFX nằm trong [FINAL_SIMPLIFICATION_REPORT.md](FINAL_SIMPLIFICATION_REPORT.md).

Mốc trước `2026-11-12T16:14:07.192913+07:00`, mốc sau `2026-11-12T16:41:58+07:00`. UTC+7, ngày 12/11/2026.

Phạm vi là working tree thực tế ở `D:/Bai-Tap-Lon-gon`, HEAD `db199b48fdfbceb192c4faa21c16a20376f81dac`. Mốc trước lấy từ checkpoint trước khi sửa trong lần này, bao gồm thay đổi chưa commit và file mới. Các số liệu năm 2026 trong báo cáo lịch sử có mốc riêng.

Quy ước Git trong bảng là `tracked`, `untracked`, `ignored`, lấy từ index và danh sách working tree. Một file tracked vẫn có thể đang modified. Việc chuyển file untracked chỉ thể hiện qua manifest trước/sau vì Git chưa có bản index để nhận diện rename.

Phạm vi file dự án gồm mã nguồn, tài nguyên, cấu hình và tài liệu do nhóm duy trì. Phạm vi filesystem quan sát còn gồm `target/`, IDE, hook phát triển và `data/`. `.git/` được ghi nhận tại ranh giới, `.maven-repository/` được ghi nhận là cache tải về. Hai cây này được loại khỏi thống kê file chi tiết và LOC. ZIP backup và JAR chỉ được ghi metadata, JAR ứng dụng cuối được kiểm tra danh sách entry/manifest để xác nhận packaging. Nội dung dữ liệu người dùng, password và token được lược khỏi báo cáo.

## Cách đếm và ranh giới

Python 3.12.12 đọc UTF-8/UTF-8-BOM và dùng splitlines để đếm dòng vật lý, kể cả dòng cuối thiếu newline. Java/CSS có lexer trạng thái cho block comment, line comment Java, string/char/escape và Java text block. XML/FXML bỏ comment <!-- -->. PowerShell nhận # và <# #> ngoài quote, CMD nhận REM/:: đầu dòng, YAML/properties/Git config xử lý dấu comment ngoài quote theo cú pháp đang có.

Một dòng có code cùng comment được tính code một lần. Dòng chỉ chứa comment tính comment-only, dòng trắng tính blank kể cả nằm trong comment/text block. Với định nghĩa này, physical = code + comment-only + blank. Dấu ngoặc/khai báo/import cũng là code, số dòng này đo SLOC vật lý. Cấu hình/script được tính trong nhóm riêng. Markdown/TXT, PDF/ảnh, JSON dữ liệu thật, binary và output tự sinh nằm ngoài tổng source SLOC.

Java production có nghĩa mọi Java trong src/main, gồm cả JavaFX client và sáu file mock được đóng gói cùng ứng dụng. Java test là src/test. Nhóm giao diện riêng trong bảng tổng là tám FXML và một CSS, Java controller/UI đã được tính một lần trong Java production và được phân tách theo package ở bảng sau.

Lexer là công cụ tự viết phù hợp cú pháp các file đã đọc. Nó chưa bao phủ Java Unicode-escape trước lexical translation hoặc mọi biến thể PowerShell here-string/YAML block scalar và comment inline properties tổng quát. Nguồn hiện tại không có Java Unicode escape tạo delimiter hoặc here-string PowerShell. Đây là giới hạn cho tái sử dụng trên project khác. Số type/method lấy bằng JavacTask/TreeScanner --release 25, không dùng regex đếm từ khóa class trong comment/string. AST parse thành công 106 file, build cuối xác nhận compilation.

Số liệu trước được lấy trực tiếp từ ZIP checkpoint. Mọi Java, FXML/CSS, POM, config và script được đối chiếu SHA-256 sau dọn, đủ 124 file nguồn/config/UI đều khớp. Số liệu sau đọc lại từ working tree. Source copy trong target/classes, HTML JaCoCo và annotation/build output được loại bằng path trước khi đếm.

## File và LOC trước/sau theo nhóm

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

## Quy mô file dự án và filesystem

| Phạm vi | Trước | Sau | Chênh lệch |
| --- | --- | --- | --- |
| File dự án được duy trì | 140 | 143 | 3 |
| File filesystem quan sát | 685 | 689 | 4 |
| Thư mục quan sát gồm root, loại hai cây cache/Git | 131 | 132 | 1 |
| File build target | 528 | 529 | 1 |
| Tracked hiện hữu trong phạm vi | 117 | 117 | 0 |
| Untracked hiện hữu trong phạm vi | 23 | 26 | 3 |
| Ignored trong phạm vi | 545 | 546 | 1 |

Trạng thái Git tính trên file hiện hữu. Danh sách ignored chỉ gồm cây đã quan sát, không cộng nội bộ cache Maven/.git. Ba báo cáo mới tăng số file duy trì thêm 3. Ba tài liệu chuyển archive giữ số file, bước clean làm thay đổi số output build.

## Số file theo định dạng

| Định dạng | File duy trì trước | File duy trì sau | Filesystem trước | Filesystem sau |
| --- | --- | --- | --- | --- |
| `(không đuôi)` | 2 | 2 | 6 | 6 |
| `.class` | 0 | 0 | 149 | 149 |
| `.cmd` | 2 | 2 | 2 | 2 |
| `.css` | 1 | 1 | 4 | 4 |
| `.csv` | 0 | 0 | 1 | 1 |
| `.dll` | 0 | 0 | 50 | 50 |
| `.dumpstream` | 0 | 0 | 1 | 1 |
| `.exec` | 0 | 0 | 1 | 1 |
| `.fxml` | 8 | 8 | 16 | 16 |
| `.gif` | 0 | 0 | 16 | 16 |
| `.html` | 0 | 0 | 211 | 211 |
| `.jar` | 0 | 0 | 2 | 2 |
| `.java` | 106 | 106 | 106 | 106 |
| `.js` | 0 | 0 | 2 | 2 |
| `.json` | 0 | 0 | 4 | 4 |
| `.lock` | 0 | 0 | 1 | 1 |
| `.log` | 0 | 0 | 1 | 3 |
| `.lst` | 0 | 0 | 6 | 6 |
| `.md` | 14 | 17 | 14 | 17 |
| `.patch` | 0 | 0 | 1 | 1 |
| `.pdf` | 2 | 2 | 2 | 2 |
| `.png` | 0 | 0 | 15 | 15 |
| `.properties` | 1 | 1 | 3 | 3 |
| `.ps1` | 1 | 1 | 2 | 2 |
| `.py` | 0 | 0 | 1 | 0 |
| `.sh` | 0 | 0 | 1 | 1 |
| `.txt` | 0 | 0 | 27 | 27 |
| `.xml` | 2 | 2 | 38 | 38 |
| `.yml` | 1 | 1 | 1 | 1 |
| `.zip` | 0 | 0 | 1 | 1 |

## Dòng văn bản ngoài source

Dòng Markdown được ghi riêng để tránh cộng nội dung giải thích thành code. Ba báo cáo mới cũng là tài liệu. PDF/ZIP/JAR/ảnh được tính file, không tính physical text hoặc code. JSON thật chỉ ghi metadata trong inventory.

| Chỉ tiêu | Trước | Sau |
| --- | --- | --- |
| File Markdown/TXT | 14 | 17 |
| Dòng Markdown/TXT | 2156 | 4798 |
| Tổng dòng text được duy trì gồm source/config/UI và Markdown/TXT | 18494 | 21136 |

| Tài liệu hiện hữu sau dọn | Dòng trước ở đường gốc | Dòng sau |
| --- | --- | --- |
| `MULTI_CLIENT_SETUP.md` | 87 | 87 |
| `README.md` | 81 | 85 |
| `docs/CLEANUP_REPORT.md` | 0 | 904 |
| `docs/CODE_STATISTICS.md` | 0 | 669 |
| `docs/PROJECT_FILE_INVENTORY.md` | 0 | 1059 |
| `docs/VAN_DAP.md` | 131 | 131 |
| `docs/archive/JSON_ONLY_REFACTOR.md` | 132 | 132 |
| `docs/archive/JSON_STORAGE_SAFETY_REVIEW.md` | 204 | 204 |
| `docs/archive/MULTI_CLIENT_TEST_RESULTS.md` | 85 | 85 |
| `docs/viva/ARCHITECTURE.md` | 220 | 220 |
| `docs/viva/AUDIT_AND_RESULTS.md` | 187 | 193 |
| `docs/viva/CODE_MAP.md` | 180 | 180 |
| `docs/viva/CONCURRENCY_EXPLANATION.md` | 126 | 126 |
| `docs/viva/DESIGN_PATTERNS.md` | 134 | 134 |
| `docs/viva/OOP_EXPLANATION.md` | 122 | 122 |
| `docs/viva/PRESENTATION_GUIDE.md` | 141 | 141 |
| `docs/viva/VIVA_QUESTIONS.md` | 326 | 326 |

## Kiểu khai báo Java trước/sau

Đếm type theo AST trước dọn. Hash Java giữ nguyên nên số sau bằng số trước. CLASS bao gồm abstract class. Cột abstract là tập con của CLASS, tránh cộng thêm lần thứ hai. Anonymous class được báo riêng, named types loại anonymous. Nested/local named type được tính trong bảng mọi khai báo.

| Phạm vi | Cách tính | Class trước → sau | Abstract thuộc class | Interface | Enum | Record | Anonymous được nhận diện | Tổng type sau |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Production | Top-level | 63 → 63 | 4 → 4 | 7 → 7 | 4 → 4 | 5 → 5 | 0 → 0 | 79 |
| Production | Mọi type có tên | 71 → 71 | 4 → 4 | 7 → 7 | 6 → 6 | 15 → 15 | 13 → 13 | 99 |
| Production | Gồm anonymous | 84 → 84 | 4 → 4 | 7 → 7 | 6 → 6 | 15 → 15 | 13 → 13 | 112 |
| Test | Top-level | 27 → 27 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 27 |
| Test | Mọi type có tên | 32 → 32 | 0 → 0 | 0 → 0 | 0 → 0 | 1 → 1 | 3 → 3 | 33 |
| Test | Gồm anonymous | 35 → 35 | 0 → 0 | 0 → 0 | 0 → 0 | 1 → 1 | 3 → 3 | 36 |
| Tổng | Top-level | 90 → 90 | 4 → 4 | 7 → 7 | 4 → 4 | 5 → 5 | 0 → 0 | 106 |
| Tổng | Mọi type có tên | 103 → 103 | 4 → 4 | 7 → 7 | 6 → 6 | 16 → 16 | 16 → 16 | 132 |
| Tổng | Gồm anonymous | 119 → 119 | 4 → 4 | 7 → 7 | 6 → 6 | 16 → 16 | 16 → 16 | 148 |

Record được tính theo khai báo nguồn, không cộng record thêm vào class. Class trong báo cáo JaCoCo là tập class có instruction được đo, vì vậy có mẫu số riêng 106, độc lập tổng type AST.

## Thống kê package Java

| Phạm vi | Package | File trước=sau | Vật lý trước=sau | Code | Comment-only | Trống | Named class | Interface | Enum | Record |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Production | com.auction.client | 2 | 97 | 75 | 11 | 11 | 2 | 0 | 0 | 0 |
| Production | com.auction.client.controller | 8 | 4165 | 3839 | 66 | 260 | 10 | 0 | 0 | 3 |
| Production | com.auction.client.model | 11 | 778 | 598 | 41 | 139 | 6 | 0 | 4 | 1 |
| Production | com.auction.client.navigation | 3 | 407 | 344 | 13 | 50 | 4 | 1 | 1 | 0 |
| Production | com.auction.client.service | 13 | 550 | 389 | 50 | 111 | 6 | 6 | 1 | 6 |
| Production | com.auction.client.service.mock | 6 | 1362 | 1222 | 28 | 112 | 6 | 0 | 0 | 1 |
| Production | com.auction.client.service.server | 4 | 995 | 894 | 8 | 93 | 5 | 0 | 0 | 1 |
| Production | com.auction.client.ui | 8 | 973 | 893 | 7 | 73 | 8 | 0 | 0 | 0 |
| Production | com.auction.client.util | 4 | 197 | 165 | 3 | 29 | 4 | 0 | 0 | 0 |
| Production | com.auction.server.dao | 2 | 136 | 120 | 3 | 13 | 2 | 0 | 0 | 0 |
| Production | com.auction.server.exception | 1 | 15 | 11 | 1 | 3 | 1 | 0 | 0 | 0 |
| Production | com.auction.server.model | 12 | 223 | 171 | 14 | 38 | 11 | 0 | 0 | 2 |
| Production | com.auction.server.network | 1 | 279 | 255 | 3 | 21 | 2 | 0 | 0 | 0 |
| Production | com.auction.server.service | 3 | 1154 | 1055 | 25 | 74 | 3 | 0 | 0 | 1 |
| Production | com.auction.shared | 1 | 63 | 52 | 2 | 9 | 1 | 0 | 0 | 0 |
| Test | com.auction.client.controller | 1 | 1200 | 1155 | 3 | 42 | 3 | 0 | 0 | 0 |
| Test | com.auction.client.navigation | 2 | 269 | 253 | 0 | 16 | 2 | 0 | 0 | 0 |
| Test | com.auction.client.service | 8 | 593 | 534 | 6 | 53 | 8 | 0 | 0 | 1 |
| Test | com.auction.client.ui | 3 | 129 | 115 | 0 | 14 | 3 | 0 | 0 | 0 |
| Test | com.auction.client.util | 3 | 151 | 129 | 0 | 22 | 3 | 0 | 0 | 0 |
| Test | com.auction.integration | 2 | 481 | 443 | 1 | 37 | 3 | 0 | 0 | 0 |
| Test | com.auction.server.dao | 2 | 134 | 120 | 0 | 14 | 2 | 0 | 0 | 0 |
| Test | com.auction.server.model | 2 | 97 | 80 | 0 | 17 | 2 | 0 | 0 | 0 |
| Test | com.auction.server.network | 1 | 162 | 147 | 0 | 15 | 2 | 0 | 0 | 0 |
| Test | com.auction.server.service | 3 | 528 | 476 | 0 | 52 | 4 | 0 | 0 | 0 |

## Số dòng từng file nguồn, giao diện và cấu hình

Bảng liệt kê đầy đủ 124 file của mẫu số LOC. Trước/sau khớp từng byte. Số type là named type tường minh của AST, anonymous được đếm riêng trong bảng type phía trên.

| File | Nhóm | Vật lý trước=sau | Code trước=sau | Comment-only | Trống | Named class | Interface | Enum | Record |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `.gitattributes` | Git config | 9 | 9 | 0 | 0 | 0 | 0 | 0 | 0 |
| `.github/workflows/ci.yml` | YAML | 25 | 25 | 0 | 0 | 0 | 0 | 0 | 0 |
| `.gitignore` | Git config | 10 | 10 | 0 | 0 | 0 | 0 | 0 | 0 |
| `Start-Client.cmd` | CMD | 6 | 6 | 0 | 0 | 0 | 0 | 0 | 0 |
| `Start-Server.cmd` | CMD | 6 | 6 | 0 | 0 | 0 | 0 | 0 | 0 |
| `config/checkstyle.xml` | XML/POM | 15 | 13 | 2 | 0 | 0 | 0 | 0 | 0 |
| `pom.xml` | XML/POM | 145 | 145 | 0 | 0 | 0 | 0 | 0 | 0 |
| `run.ps1` | PowerShell | 36 | 36 | 0 | 0 | 0 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/App.java` | Java production | 11 | 6 | 4 | 1 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/Main.java` | Java production | 86 | 69 | 7 | 10 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/controller/AdminController.java` | Java production | 514 | 491 | 2 | 21 | 1 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/controller/AuctionRoomController.java` | Java production | 1070 | 977 | 21 | 72 | 1 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/controller/AuthController.java` | Java production | 395 | 359 | 6 | 30 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/controller/ContactController.java` | Java production | 226 | 209 | 0 | 17 | 2 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/controller/GuideController.java` | Java production | 213 | 195 | 0 | 18 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/controller/HomeController.java` | Java production | 627 | 536 | 32 | 59 | 2 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/controller/SellerProductsController.java` | Java production | 732 | 705 | 3 | 24 | 1 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/controller/WalletController.java` | Java production | 388 | 367 | 2 | 19 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/model/AdminUser.java` | Java production | 36 | 23 | 6 | 7 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/model/Auction.java` | Java production | 180 | 143 | 2 | 35 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/model/AuctionStatus.java` | Java production | 25 | 19 | 1 | 5 | 0 | 0 | 1 | 0 |
| `src/main/java/com/auction/client/model/AutoBid.java` | Java production | 108 | 75 | 16 | 17 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/model/Bid.java` | Java production | 105 | 82 | 4 | 19 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/model/Item.java` | Java production | 101 | 79 | 2 | 20 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/model/ItemCategory.java` | Java production | 56 | 45 | 4 | 7 | 0 | 0 | 1 | 0 |
| `src/main/java/com/auction/client/model/Transaction.java` | Java production | 17 | 14 | 1 | 2 | 0 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/model/TransactionType.java` | Java production | 27 | 21 | 2 | 4 | 0 | 0 | 1 | 0 |
| `src/main/java/com/auction/client/model/User.java` | Java production | 100 | 80 | 2 | 18 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/model/UserRole.java` | Java production | 23 | 17 | 1 | 5 | 0 | 0 | 1 | 0 |
| `src/main/java/com/auction/client/navigation/DisposableView.java` | Java production | 6 | 4 | 1 | 1 | 0 | 1 | 0 | 0 |
| `src/main/java/com/auction/client/navigation/ViewNavigator.java` | Java production | 252 | 219 | 0 | 33 | 2 | 0 | 1 | 0 |
| `src/main/java/com/auction/client/navigation/ViewTasks.java` | Java production | 149 | 121 | 12 | 16 | 2 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/AuctionAdminService.java` | Java production | 17 | 8 | 4 | 5 | 0 | 1 | 0 | 0 |
| `src/main/java/com/auction/client/service/AuctionClientService.java` | Java production | 69 | 37 | 10 | 22 | 0 | 1 | 0 | 0 |
| `src/main/java/com/auction/client/service/AuctionRoomSnapshot.java` | Java production | 12 | 9 | 0 | 3 | 0 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/service/AutoBidException.java` | Java production | 9 | 6 | 2 | 1 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/AutoBidService.java` | Java production | 15 | 7 | 4 | 4 | 0 | 1 | 0 | 0 |
| `src/main/java/com/auction/client/service/BidResult.java` | Java production | 42 | 32 | 1 | 9 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/BidSubmissionTracker.java` | Java production | 97 | 73 | 6 | 18 | 2 | 0 | 1 | 3 |
| `src/main/java/com/auction/client/service/ProductService.java` | Java production | 24 | 11 | 6 | 7 | 0 | 1 | 0 | 0 |
| `src/main/java/com/auction/client/service/ServiceRegistry.java` | Java production | 189 | 163 | 3 | 23 | 1 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/service/UserAdminService.java` | Java production | 21 | 10 | 5 | 6 | 0 | 1 | 0 | 0 |
| `src/main/java/com/auction/client/service/WalletException.java` | Java production | 9 | 6 | 2 | 1 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/WalletService.java` | Java production | 34 | 18 | 7 | 9 | 0 | 1 | 0 | 0 |
| `src/main/java/com/auction/client/service/WalletSnapshot.java` | Java production | 12 | 9 | 0 | 3 | 0 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/service/mock/AutoBidEngine.java` | Java production | 101 | 92 | 4 | 5 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/mock/MockAuctionClientService.java` | Java production | 380 | 341 | 8 | 31 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/mock/MockAutoBidService.java` | Java production | 92 | 82 | 2 | 8 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/mock/MockDataStore.java` | Java production | 505 | 461 | 8 | 36 | 1 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/service/mock/MockManagementService.java` | Java production | 199 | 175 | 3 | 21 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/mock/MockWalletService.java` | Java production | 85 | 71 | 3 | 11 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/server/JsonTransport.java` | Java production | 152 | 137 | 1 | 14 | 2 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/server/ServerError.java` | Java production | 39 | 34 | 1 | 4 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/server/ServerMapper.java` | Java production | 98 | 87 | 0 | 11 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/service/server/ServerServices.java` | Java production | 706 | 636 | 6 | 64 | 1 | 0 | 0 | 1 |
| `src/main/java/com/auction/client/ui/AppFooter.java` | Java production | 97 | 88 | 0 | 9 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/ui/AppHeader.java` | Java production | 139 | 133 | 1 | 5 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/ui/Icons.java` | Java production | 153 | 143 | 1 | 9 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/ui/PasswordFieldWithToggle.java` | Java production | 41 | 36 | 1 | 4 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/ui/ProductImages.java` | Java production | 127 | 112 | 3 | 12 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/ui/RoundedClip.java` | Java production | 17 | 14 | 0 | 3 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/ui/SmoothScroll.java` | Java production | 43 | 38 | 1 | 4 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/ui/UserMenu.java` | Java production | 356 | 329 | 0 | 27 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/util/AppInfo.java` | Java production | 7 | 5 | 0 | 2 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/util/AuctionTime.java` | Java production | 42 | 35 | 1 | 6 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/util/FormatUtil.java` | Java production | 66 | 53 | 2 | 11 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/client/util/Validators.java` | Java production | 82 | 72 | 0 | 10 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/dao/AuctionJsonMapper.java` | Java production | 37 | 30 | 1 | 6 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/dao/AuctionRepository.java` | Java production | 99 | 90 | 2 | 7 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/exception/ApiException.java` | Java production | 15 | 11 | 1 | 3 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Admin.java` | Java production | 12 | 10 | 0 | 2 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Art.java` | Java production | 12 | 10 | 0 | 2 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Auction.java` | Java production | 40 | 28 | 6 | 6 | 0 | 0 | 0 | 1 |
| `src/main/java/com/auction/server/model/BidTransaction.java` | Java production | 12 | 9 | 1 | 2 | 0 | 0 | 0 | 1 |
| `src/main/java/com/auction/server/model/Bidder.java` | Java production | 12 | 10 | 0 | 2 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Electronics.java` | Java production | 12 | 10 | 0 | 2 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Entity.java` | Java production | 20 | 15 | 1 | 4 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Item.java` | Java production | 23 | 17 | 1 | 5 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/ItemFactory.java` | Java production | 39 | 30 | 4 | 5 | 2 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Seller.java` | Java production | 12 | 10 | 0 | 2 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/User.java` | Java production | 17 | 12 | 1 | 4 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/model/Vehicle.java` | Java production | 12 | 10 | 0 | 2 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/network/Server.java` | Java production | 279 | 255 | 3 | 21 | 2 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/service/AntiSnipingPolicy.java` | Java production | 55 | 37 | 8 | 10 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/server/service/AuctionService.java` | Java production | 1057 | 983 | 16 | 58 | 1 | 0 | 0 | 1 |
| `src/main/java/com/auction/server/service/Passwords.java` | Java production | 42 | 35 | 1 | 6 | 1 | 0 | 0 | 0 |
| `src/main/java/com/auction/shared/JsonProtocol.java` | Java production | 63 | 52 | 2 | 9 | 1 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/contacts.properties` | Properties | 8 | 8 | 0 | 0 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/css/app.css` | CSS | 245 | 245 | 0 | 0 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/admin-view.fxml` | FXML | 126 | 126 | 0 | 0 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/auction-room-view.fxml` | FXML | 226 | 182 | 21 | 23 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/auth-view.fxml` | FXML | 101 | 99 | 0 | 2 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/contact-view.fxml` | FXML | 39 | 37 | 0 | 2 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/guide-view.fxml` | FXML | 39 | 37 | 0 | 2 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/home-view.fxml` | FXML | 91 | 88 | 0 | 3 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/seller-products-view.fxml` | FXML | 49 | 49 | 0 | 0 | 0 | 0 | 0 | 0 |
| `src/main/resources/com/auction/client/views/wallet-view.fxml` | FXML | 24 | 24 | 0 | 0 | 0 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/controller/ClientUiTest.java` | Java test | 1200 | 1155 | 3 | 42 | 3 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/navigation/ImageTasksTest.java` | Java test | 199 | 191 | 0 | 8 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/navigation/ViewTasksTest.java` | Java test | 70 | 62 | 0 | 8 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/service/BidSubmissionTrackerTest.java` | Java test | 64 | 55 | 1 | 8 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/service/MockAuctionBidAccessTest.java` | Java test | 37 | 32 | 1 | 4 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/service/MockAuthIntegrationTest.java` | Java test | 41 | 36 | 0 | 5 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/service/MockAutoBidServiceTest.java` | Java test | 159 | 147 | 1 | 11 | 1 | 0 | 0 | 1 |
| `src/test/java/com/auction/client/service/MockManagementServiceTest.java` | Java test | 56 | 49 | 1 | 6 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/service/MockSharedStoreTest.java` | Java test | 98 | 91 | 1 | 6 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/service/MockWalletServiceTest.java` | Java test | 99 | 90 | 1 | 8 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/service/ServiceRegistryTest.java` | Java test | 39 | 34 | 0 | 5 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/ui/ProductImagesIoTest.java` | Java test | 73 | 67 | 0 | 6 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/ui/ProductImagesTest.java` | Java test | 36 | 31 | 0 | 5 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/ui/UiResourcesTest.java` | Java test | 20 | 17 | 0 | 3 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/util/AuctionTimeTest.java` | Java test | 33 | 27 | 0 | 6 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/util/FormatUtilTest.java` | Java test | 14 | 11 | 0 | 3 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/client/util/ValidatorsTest.java` | Java test | 104 | 91 | 0 | 13 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/integration/FullStackTest.java` | Java test | 281 | 258 | 1 | 22 | 2 | 0 | 0 | 0 |
| `src/test/java/com/auction/integration/ServerUiIntegrationTest.java` | Java test | 200 | 185 | 0 | 15 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/dao/AuctionJsonMapperTest.java` | Java test | 46 | 40 | 0 | 6 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/dao/AuctionRepositoryTest.java` | Java test | 88 | 80 | 0 | 8 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/model/AuctionTest.java` | Java test | 56 | 45 | 0 | 11 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/model/ItemFactoryTest.java` | Java test | 41 | 35 | 0 | 6 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/network/ServerIntegrationTest.java` | Java test | 162 | 147 | 0 | 15 | 2 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/service/AntiSnipingPolicyTest.java` | Java test | 53 | 43 | 0 | 10 | 1 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/service/AuctionServiceTest.java` | Java test | 432 | 394 | 0 | 38 | 2 | 0 | 0 | 0 |
| `src/test/java/com/auction/server/service/AuctionStateValidationTest.java` | Java test | 43 | 39 | 0 | 4 | 1 | 0 | 0 | 0 |

## Thống kê mọi thư mục

File trực tiếp chỉ tính ở đúng thư mục cha, mỗi file đóng góp một lần. Hai cột đệ quy mô tả kích thước cây để đọc cấu trúc, tổng các cột đệ quy qua nhiều cấp sẽ cộng trùng. LOC trực tiếp chỉ cộng nhóm nguồn/config/UI. Thư mục mất đi sau clean vẫn có dòng với số sau bằng 0.

| Thư mục | File trực tiếp trước | Sau | File đệ quy trước | Sau | LOC nguồn trực tiếp trước | Sau |
| --- | --- | --- | --- | --- | --- | --- |
| `.` | 8 | 8 | 685 | 689 | 212 | 212 |
| `.github` | 0 | 0 | 4 | 4 | 0 | 0 |
| `.github/modernize` | 0 | 0 | 3 | 3 | 0 | 0 |
| `.github/modernize/java-upgrade` | 1 | 1 | 3 | 3 | 0 | 0 |
| `.github/modernize/java-upgrade/hooks` | 0 | 0 | 2 | 2 | 0 | 0 |
| `.github/modernize/java-upgrade/hooks/scripts` | 2 | 2 | 2 | 2 | 0 | 0 |
| `.github/workflows` | 1 | 1 | 1 | 1 | 25 | 25 |
| `.idea` | 7 | 7 | 7 | 7 | 0 | 0 |
| `.vscode` | 1 | 1 | 1 | 1 | 0 | 0 |
| `config` | 1 | 1 | 1 | 1 | 15 | 15 |
| `data` | 2 | 2 | 6 | 6 | 0 | 0 |
| `data/backups` | 0 | 0 | 4 | 4 | 0 | 0 |
| `data/backups/json-only-20261111-233921` | 4 | 4 | 4 | 4 | 0 | 0 |
| `docs` | 6 | 6 | 14 | 17 | 0 | 0 |
| `docs/archive` | 0 | 3 | 0 | 3 | 0 | 0 |
| `docs/viva` | 8 | 8 | 8 | 8 | 0 | 0 |
| `src` | 0 | 0 | 116 | 116 | 0 | 0 |
| `src/main` | 0 | 0 | 89 | 89 | 0 | 0 |
| `src/main/java` | 0 | 0 | 79 | 79 | 0 | 0 |
| `src/main/java/com` | 0 | 0 | 79 | 79 | 0 | 0 |
| `src/main/java/com/auction` | 0 | 0 | 79 | 79 | 0 | 0 |
| `src/main/java/com/auction/client` | 2 | 2 | 59 | 59 | 97 | 97 |
| `src/main/java/com/auction/client/controller` | 8 | 8 | 8 | 8 | 4165 | 4165 |
| `src/main/java/com/auction/client/model` | 11 | 11 | 11 | 11 | 778 | 778 |
| `src/main/java/com/auction/client/navigation` | 3 | 3 | 3 | 3 | 407 | 407 |
| `src/main/java/com/auction/client/service` | 13 | 13 | 23 | 23 | 550 | 550 |
| `src/main/java/com/auction/client/service/mock` | 6 | 6 | 6 | 6 | 1362 | 1362 |
| `src/main/java/com/auction/client/service/server` | 4 | 4 | 4 | 4 | 995 | 995 |
| `src/main/java/com/auction/client/ui` | 8 | 8 | 8 | 8 | 973 | 973 |
| `src/main/java/com/auction/client/util` | 4 | 4 | 4 | 4 | 197 | 197 |
| `src/main/java/com/auction/server` | 0 | 0 | 19 | 19 | 0 | 0 |
| `src/main/java/com/auction/server/dao` | 2 | 2 | 2 | 2 | 136 | 136 |
| `src/main/java/com/auction/server/exception` | 1 | 1 | 1 | 1 | 15 | 15 |
| `src/main/java/com/auction/server/model` | 12 | 12 | 12 | 12 | 223 | 223 |
| `src/main/java/com/auction/server/network` | 1 | 1 | 1 | 1 | 279 | 279 |
| `src/main/java/com/auction/server/service` | 3 | 3 | 3 | 3 | 1154 | 1154 |
| `src/main/java/com/auction/shared` | 1 | 1 | 1 | 1 | 63 | 63 |
| `src/main/resources` | 0 | 0 | 10 | 10 | 0 | 0 |
| `src/main/resources/com` | 0 | 0 | 10 | 10 | 0 | 0 |
| `src/main/resources/com/auction` | 0 | 0 | 10 | 10 | 0 | 0 |
| `src/main/resources/com/auction/client` | 1 | 1 | 10 | 10 | 8 | 8 |
| `src/main/resources/com/auction/client/css` | 1 | 1 | 1 | 1 | 245 | 245 |
| `src/main/resources/com/auction/client/views` | 8 | 8 | 8 | 8 | 695 | 695 |
| `src/test` | 0 | 0 | 27 | 27 | 0 | 0 |
| `src/test/java` | 0 | 0 | 27 | 27 | 0 | 0 |
| `src/test/java/com` | 0 | 0 | 27 | 27 | 0 | 0 |
| `src/test/java/com/auction` | 0 | 0 | 27 | 27 | 0 | 0 |
| `src/test/java/com/auction/client` | 0 | 0 | 17 | 17 | 0 | 0 |
| `src/test/java/com/auction/client/controller` | 1 | 1 | 1 | 1 | 1200 | 1200 |
| `src/test/java/com/auction/client/navigation` | 2 | 2 | 2 | 2 | 269 | 269 |
| `src/test/java/com/auction/client/service` | 8 | 8 | 8 | 8 | 593 | 593 |
| `src/test/java/com/auction/client/ui` | 3 | 3 | 3 | 3 | 129 | 129 |
| `src/test/java/com/auction/client/util` | 3 | 3 | 3 | 3 | 151 | 151 |
| `src/test/java/com/auction/integration` | 2 | 2 | 2 | 2 | 481 | 481 |
| `src/test/java/com/auction/server` | 0 | 0 | 8 | 8 | 0 | 0 |
| `src/test/java/com/auction/server/dao` | 2 | 2 | 2 | 2 | 134 | 134 |
| `src/test/java/com/auction/server/model` | 2 | 2 | 2 | 2 | 97 | 97 |
| `src/test/java/com/auction/server/network` | 1 | 1 | 1 | 1 | 162 | 162 |
| `src/test/java/com/auction/server/service` | 3 | 3 | 3 | 3 | 528 | 528 |
| `target` | 8 | 9 | 528 | 529 | 0 | 0 |
| `target/classes` | 0 | 0 | 123 | 123 | 0 | 0 |
| `target/classes/com` | 0 | 0 | 123 | 123 | 0 | 0 |
| `target/classes/com/auction` | 0 | 0 | 123 | 123 | 0 | 0 |
| `target/classes/com/auction/client` | 3 | 3 | 99 | 99 | 0 | 0 |
| `target/classes/com/auction/client/controller` | 24 | 24 | 24 | 24 | 0 | 0 |
| `target/classes/com/auction/client/css` | 1 | 1 | 1 | 1 | 0 | 0 |
| `target/classes/com/auction/client/model` | 11 | 11 | 11 | 11 | 0 | 0 |
| `target/classes/com/auction/client/navigation` | 6 | 6 | 6 | 6 | 0 | 0 |
| `target/classes/com/auction/client/service` | 19 | 19 | 32 | 32 | 0 | 0 |
| `target/classes/com/auction/client/service/mock` | 7 | 7 | 7 | 7 | 0 | 0 |
| `target/classes/com/auction/client/service/server` | 6 | 6 | 6 | 6 | 0 | 0 |
| `target/classes/com/auction/client/ui` | 10 | 10 | 10 | 10 | 0 | 0 |
| `target/classes/com/auction/client/util` | 4 | 4 | 4 | 4 | 0 | 0 |
| `target/classes/com/auction/client/views` | 8 | 8 | 8 | 8 | 0 | 0 |
| `target/classes/com/auction/server` | 0 | 0 | 23 | 23 | 0 | 0 |
| `target/classes/com/auction/server/dao` | 2 | 2 | 2 | 2 | 0 | 0 |
| `target/classes/com/auction/server/exception` | 1 | 1 | 1 | 1 | 0 | 0 |
| `target/classes/com/auction/server/model` | 14 | 14 | 14 | 14 | 0 | 0 |
| `target/classes/com/auction/server/network` | 2 | 2 | 2 | 2 | 0 | 0 |
| `target/classes/com/auction/server/service` | 4 | 4 | 4 | 4 | 0 | 0 |
| `target/classes/com/auction/shared` | 1 | 1 | 1 | 1 | 0 | 0 |
| `target/generated-sources` | 0 | 0 | 0 | 0 | 0 | 0 |
| `target/generated-sources/annotations` | 0 | 0 | 0 | 0 | 0 | 0 |
| `target/generated-test-sources` | 0 | 0 | 0 | 0 | 0 | 0 |
| `target/generated-test-sources/test-annotations` | 0 | 0 | 0 | 0 | 0 | 0 |
| `target/maven-archiver` | 1 | 1 | 1 | 1 | 0 | 0 |
| `target/maven-status` | 0 | 0 | 6 | 6 | 0 | 0 |
| `target/maven-status/maven-compiler-plugin` | 0 | 0 | 6 | 6 | 0 | 0 |
| `target/maven-status/maven-compiler-plugin/compile` | 0 | 0 | 3 | 3 | 0 | 0 |
| `target/maven-status/maven-compiler-plugin/compile/default-compile` | 3 | 3 | 3 | 3 | 0 | 0 |
| `target/maven-status/maven-compiler-plugin/testCompile` | 0 | 0 | 3 | 3 | 0 | 0 |
| `target/maven-status/maven-compiler-plugin/testCompile/default-testCompile` | 3 | 3 | 3 | 3 | 0 | 0 |
| `target/qa` | 15 | 15 | 15 | 15 | 0 | 0 |
| `target/site` | 0 | 0 | 233 | 233 | 0 | 0 |
| `target/site/jacoco` | 4 | 4 | 233 | 233 | 0 | 0 |
| `target/site/jacoco/com.auction.client` | 6 | 6 | 6 | 6 | 0 | 0 |
| `target/site/jacoco/com.auction.client.controller` | 33 | 33 | 33 | 33 | 0 | 0 |
| `target/site/jacoco/com.auction.client.model` | 24 | 24 | 24 | 24 | 0 | 0 |
| `target/site/jacoco/com.auction.client.navigation` | 9 | 9 | 9 | 9 | 0 | 0 |
| `target/site/jacoco/com.auction.client.service` | 26 | 26 | 26 | 26 | 0 | 0 |
| `target/site/jacoco/com.auction.client.service.mock` | 15 | 15 | 15 | 15 | 0 | 0 |
| `target/site/jacoco/com.auction.client.service.server` | 12 | 12 | 12 | 12 | 0 | 0 |
| `target/site/jacoco/com.auction.client.ui` | 20 | 20 | 20 | 20 | 0 | 0 |
| `target/site/jacoco/com.auction.client.util` | 8 | 8 | 8 | 8 | 0 | 0 |
| `target/site/jacoco/com.auction.server.dao` | 6 | 6 | 6 | 6 | 0 | 0 |
| `target/site/jacoco/com.auction.server.exception` | 4 | 4 | 4 | 4 | 0 | 0 |
| `target/site/jacoco/com.auction.server.model` | 28 | 28 | 28 | 28 | 0 | 0 |
| `target/site/jacoco/com.auction.server.network` | 5 | 5 | 5 | 5 | 0 | 0 |
| `target/site/jacoco/com.auction.server.service` | 9 | 9 | 9 | 9 | 0 | 0 |
| `target/site/jacoco/com.auction.shared` | 4 | 4 | 4 | 4 | 0 | 0 |
| `target/site/jacoco/jacoco-resources` | 20 | 20 | 20 | 20 | 0 | 0 |
| `target/surefire-reports` | 55 | 55 | 55 | 55 | 0 | 0 |
| `target/test-classes` | 0 | 0 | 36 | 36 | 0 | 0 |
| `target/test-classes/com` | 0 | 0 | 36 | 36 | 0 | 0 |
| `target/test-classes/com/auction` | 0 | 0 | 36 | 36 | 0 | 0 |
| `target/test-classes/com/auction/client` | 0 | 0 | 23 | 23 | 0 | 0 |
| `target/test-classes/com/auction/client/controller` | 6 | 6 | 6 | 6 | 0 | 0 |
| `target/test-classes/com/auction/client/navigation` | 2 | 2 | 2 | 2 | 0 | 0 |
| `target/test-classes/com/auction/client/service` | 9 | 9 | 9 | 9 | 0 | 0 |
| `target/test-classes/com/auction/client/ui` | 3 | 3 | 3 | 3 | 0 | 0 |
| `target/test-classes/com/auction/client/util` | 3 | 3 | 3 | 3 | 0 | 0 |
| `target/test-classes/com/auction/integration` | 3 | 3 | 3 | 3 | 0 | 0 |
| `target/test-classes/com/auction/server` | 0 | 0 | 10 | 10 | 0 | 0 |
| `target/test-classes/com/auction/server/dao` | 2 | 2 | 2 | 2 | 0 | 0 |
| `target/test-classes/com/auction/server/model` | 2 | 2 | 2 | 2 | 0 | 0 |
| `target/test-classes/com/auction/server/network` | 2 | 2 | 2 | 2 | 0 | 0 |
| `target/test-classes/com/auction/server/service` | 4 | 4 | 4 | 4 | 0 | 0 |
| `target/test-tmp` | 0 | 0 | 51 | 51 | 0 | 0 |
| `target/test-tmp/.openjfx_MAI DUC HIEU` | 0 | 0 | 51 | 51 | 0 | 0 |
| `target/test-tmp/.openjfx_MAI DUC HIEU/cache` | 0 | 0 | 51 | 51 | 0 | 0 |
| `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3` | 0 | 0 | 51 | 51 | 0 | 0 |
| `target/test-tmp/.openjfx_MAI DUC HIEU/cache/26.0.2+3/amd64` | 51 | 51 | 51 | 51 | 0 | 0 |

## Top 15 file Java dài nhất

Xếp theo dòng vật lý sau dọn, cùng thứ tự trước dọn vì Java giữ nguyên bytes.

| Hạng | File | Phạm vi | Vật lý trước=sau | Code | Comment-only | Trống |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `src/test/java/com/auction/client/controller/ClientUiTest.java` | Java test | 1200 | 1155 | 3 | 42 |
| 2 | `src/main/java/com/auction/client/controller/AuctionRoomController.java` | Java production | 1070 | 977 | 21 | 72 |
| 3 | `src/main/java/com/auction/server/service/AuctionService.java` | Java production | 1057 | 983 | 16 | 58 |
| 4 | `src/main/java/com/auction/client/controller/SellerProductsController.java` | Java production | 732 | 705 | 3 | 24 |
| 5 | `src/main/java/com/auction/client/service/server/ServerServices.java` | Java production | 706 | 636 | 6 | 64 |
| 6 | `src/main/java/com/auction/client/controller/HomeController.java` | Java production | 627 | 536 | 32 | 59 |
| 7 | `src/main/java/com/auction/client/controller/AdminController.java` | Java production | 514 | 491 | 2 | 21 |
| 8 | `src/main/java/com/auction/client/service/mock/MockDataStore.java` | Java production | 505 | 461 | 8 | 36 |
| 9 | `src/test/java/com/auction/server/service/AuctionServiceTest.java` | Java test | 432 | 394 | 0 | 38 |
| 10 | `src/main/java/com/auction/client/controller/AuthController.java` | Java production | 395 | 359 | 6 | 30 |
| 11 | `src/main/java/com/auction/client/controller/WalletController.java` | Java production | 388 | 367 | 2 | 19 |
| 12 | `src/main/java/com/auction/client/service/mock/MockAuctionClientService.java` | Java production | 380 | 341 | 8 | 31 |
| 13 | `src/main/java/com/auction/client/ui/UserMenu.java` | Java production | 356 | 329 | 0 | 27 |
| 14 | `src/test/java/com/auction/integration/FullStackTest.java` | Java test | 281 | 258 | 1 | 22 |
| 15 | `src/main/java/com/auction/server/network/Server.java` | Java production | 279 | 255 | 3 | 21 |

## Điểm cân nhắc đơn giản hóa về sau

| File | Trách nhiệm quan sát được | Hướng rà soát có căn cứ |
| --- | --- | --- |
| AuctionRoomController.java | 1.070 dòng, chart/countdown/history/image, bid/auto và tracker trong một controller. | Có thể tách phần render chart hoặc phần form auto sau khi xác định lifecycle listener/task và giữ test UNKNOWN/read revision. |
| AuctionService.java | 1.057 dòng, auth, sản phẩm, ví, bid, auto-bid, settlement và persist. | Có thể tách luật thuần như Auction hiện tại. Cần giữ ranh giới monitor/rollback/receipt/ví chung khi đổi tổ chức. |
| SellerProductsController.java | 732 dòng, table/image/filter và dialog form/time/money. | Xem khả năng tách phần dựng form, dùng lại validation/time helper đang có. |
| ServerServices.java | 706 dòng, sáu interface, transport/pending/subscription/mapping. | Rà khối pending-command có FileLock/atomic replacement trước khi tách, giữ token/transport thống nhất. |
| HomeController.java | 627 dòng, card/filter/countdown và image task. | Xem phần dựng card trong khi giữ shared timer, dedup ID và chống ảnh cũ. |
| ClientUiTest.java | 1.200 dòng, nhiều route và helper điều khiển bất đồng bộ. | Có thể nhóm test theo màn hình sau khi bảo toàn fixture toolkit, isolation và đầy đủ discovery. |
| AdminController.java, WalletController.java, MockDataStore.java | Các table/dialog hoặc seed fixture dài. | Chỉ tách khi giảm coupling thật và giữ hành vi caller. Lần này chưa refactor hoặc xóa test. |

Độ phức tạp code chưa được đo lại bằng metric cyclomatic trước/sau. Java physical/code và cấu trúc type giữ nguyên nên mức giảm source LOC là 0%. Mức thay đổi tổ chức là ba báo cáo mốc cũ chuyển archive.

## Kiểm chứng chất lượng và phạm vi

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

## Mã tái lập phép đếm dòng

Đoạn Python dưới đây có thể lưu ngoài repository thành count_source.py rồi chạy ở root. Nó chỉ đếm nguồn/config/UI, không đọc JSON thật, cache hoặc backup. Công cụ checkpoint/AST của phiên làm việc nằm ở thư mục tạm nêu trong CLEANUP_REPORT.

```python
import collections
from pathlib import Path
import re

def line_counts(text, language):
    """Classify physical lines with code precedence over comments."""
    result = collections.Counter(total=0, code=0, comment=0, blank=0)
    state = 'normal'
    for line in text.splitlines():
        result['total'] += 1
        if not line.strip():
            result['blank'] += 1
            continue
        code = False
        comment = state in ('block', 'xmlcomment', 'psblock')
        index = 0
        while index < len(line):
            ch = line[index]
            rest = line[index:]
            if state == 'block':
                comment = True
                if rest.startswith('*/'):
                    state = 'normal'; index += 2
                else:
                    index += 1
            elif state == 'xmlcomment':
                comment = True
                if rest.startswith('-->'):
                    state = 'normal'; index += 3
                else:
                    index += 1
            elif state == 'psblock':
                comment = True
                if rest.startswith('#>'):
                    state = 'normal'; index += 2
                else:
                    index += 1
            elif state == 'textblock':
                code = True
                if rest.startswith('"""'):
                    state = 'normal'; index += 3
                elif ch == '\\':
                    index += 2
                else:
                    index += 1
            elif state in ('single', 'double'):
                code = True
                delimiter = "'" if state == 'single' else '"'
                if ch == '\\' and language in ('java', 'css'):
                    index += 2
                elif ch == '`' and language == 'powershell':
                    index += 2
                elif ch == delimiter:
                    if language in ('powershell', 'yaml') and index + 1 < len(line) and line[index + 1] == delimiter:
                        index += 2
                    else:
                        state = 'normal'; index += 1
                else:
                    index += 1
            elif ch.isspace():
                index += 1
            elif language in ('java', 'css') and rest.startswith('/*'):
                comment = True; state = 'block'; index += 2
            elif language == 'java' and rest.startswith('//'):
                comment = True; break
            elif language == 'xml' and rest.startswith('<!--'):
                comment = True; state = 'xmlcomment'; index += 4
            elif language == 'powershell' and rest.startswith('<#'):
                comment = True; state = 'psblock'; index += 2
            elif language in ('powershell', 'yaml', 'properties', 'gitconfig') and ch == '#':
                comment = True; break
            elif language == 'cmd' and re.match(r'(?i)^\s*@?(rem(?:\s|$)|::)', line):
                comment = True; break
            elif language == 'java' and rest.startswith('"""'):
                code = True; state = 'textblock'; index += 3
            elif ch in ('"', "'") and language in ('java', 'css', 'xml', 'powershell', 'yaml'):
                code = True; state = 'double' if ch == '"' else 'single'; index += 1
            else:
                code = True; index += 1
        result['code' if code else 'comment' if comment else 'blank'] += 1
        if state in ('single', 'double') and language in ('java', 'css', 'xml', 'yaml'):
            state = 'normal'
    assert result['total'] == result['code'] + result['comment'] + result['blank']
    return dict(result)

def category(path):
    if path.startswith(('target/', 'data/', '.idea/', '.vscode/', '.github/modernize/')):
        return None
    if path.endswith('.java'):
        return ('Java production' if path.startswith('src/main/') else 'Java test', 'java')
    ext = Path(path).suffix
    return {'.fxml': ('FXML', 'xml'), '.css': ('CSS', 'css'), '.xml': ('XML/POM', 'xml'),
            '.ps1': ('PowerShell', 'powershell'), '.cmd': ('CMD', 'cmd'),
            '.yml': ('YAML', 'yaml'), '.yaml': ('YAML', 'yaml'),
            '.properties': ('Properties', 'properties')}.get(ext, ('Git config', 'gitconfig') if path in ('.gitignore', '.gitattributes') else None)

groups = collections.defaultdict(collections.Counter)
for base in [Path('src'), Path('config'), Path('.github/workflows')]:
    for path in sorted(base.rglob('*')):
        if path.is_file():
            name = path.as_posix()
            item = category(name)
            if item:
                group, language = item
                groups[group].update(line_counts(path.read_text(encoding='utf-8-sig'), language))
                groups[group]['files'] += 1
for name in ['pom.xml', 'run.ps1', 'Start-Client.cmd', 'Start-Server.cmd', '.gitignore', '.gitattributes']:
    path = Path(name)
    group, language = category(name)
    groups[group].update(line_counts(path.read_text(encoding='utf-8-sig'), language))
    groups[group]['files'] += 1
for group, totals in groups.items():
    print(group, dict(totals))
```

## Mã tái lập AST Java

Lưu SourceAudit.java ngoài repository, chạy bằng JDK 25 với hai đối số root và đường dẫn JSON đầu ra. Đoạn code parse mọi Java trong src bằng compiler API, ghi từng type/method/line/import/identifier để đếm theo các định nghĩa trong bảng.

```java
import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;
public class SourceAudit {
 static String quote(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n").replace("\t","\\t")+"\""; }
 static String array(Collection<String> values) { return "["+String.join(",",values.stream().map(SourceAudit::quote).toList())+"]"; }
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args[0]);
  List<Path> paths;
  try(var walk=Files.walk(root.resolve("src"))) { paths=walk.filter(p->p.toString().endsWith(".java")).sorted().toList(); }
  var compiler=ToolProvider.getSystemJavaCompiler();
  var fm=compiler.getStandardFileManager(null,null,java.nio.charset.StandardCharsets.UTF_8);
  var task=(JavacTask)compiler.getTask(null,fm,null,List.of("-proc:none","--release","25"),null,fm.getJavaFileObjectsFromPaths(paths));
  var trees=Trees.instance(task);
  List<String> output=new ArrayList<>();
  for(var unit:task.parse()) {
   List<String> imports=unit.getImports().stream().map(i->i.getQualifiedIdentifier().toString()).toList();
   List<String> classes=new ArrayList<>(),methods=new ArrayList<>(),fields=new ArrayList<>();
   Set<String> names=new TreeSet<>();
   new TreeScanner<Void,String>() {
    long line(Tree n) { return unit.getLineMap().getLineNumber(trees.getSourcePositions().getStartPosition(unit,n)); }
    public Void visitClass(ClassTree n,String owner) {
     String name=n.getSimpleName().toString();
     String qualified=owner==null?name:owner+"."+(name.isEmpty()?"<anonymous@"+line(n)+">":name);
     classes.add("{\"name\":"+quote(qualified)+",\"kind\":"+quote(n.getKind().toString())+",\"modifiers\":"+quote(n.getModifiers().getFlags().toString())+",\"line\":"+line(n)+"}");
     return super.visitClass(n,qualified);
    }
    public Void visitMethod(MethodTree n,String owner) {
     String methodName=n.getName().toString();
     String signature=(n.getReturnType()==null?"":n.getReturnType()+" ")+methodName+"("+String.join(", ",n.getParameters().stream().map(p->p.getType()+" "+p.getName()).toList())+")";
     methods.add("{\"owner\":"+quote(owner==null?"":owner)+",\"name\":"+quote(methodName)+",\"signature\":"+quote(signature)+",\"line\":"+line(n)+",\"annotations\":"+array(n.getModifiers().getAnnotations().stream().map(Object::toString).toList())+"}");
     return super.visitMethod(n,owner);
    }
    public Void visitVariable(VariableTree n,String owner) {
     if (n.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.STATIC)) fields.add(n.getName().toString());
     return super.visitVariable(n,owner);
    }
    public Void visitIdentifier(IdentifierTree n,String owner) { names.add(n.getName().toString()); return super.visitIdentifier(n,owner); }
    public Void visitMemberSelect(MemberSelectTree n,String owner) { names.add(n.toString()); return super.visitMemberSelect(n,owner); }
   }.scan(unit,null);
   String path=root.relativize(Path.of(unit.getSourceFile().toUri())).toString().replace('\\','/');
   output.add("{\"path\":"+quote(path)+",\"package\":"+quote(unit.getPackageName().toString())+",\"imports\":"+array(imports)+",\"types\":["+String.join(",",classes)+"],\"methods\":["+String.join(",",methods)+"],\"names\":"+array(names)+",\"staticFields\":"+array(fields)+"}");
  }
  Files.writeString(Path.of(args[1]),"["+String.join(",\n",output)+"]");
 }
}

```
