# Báo cáo sửa kiểm thử popup tài khoản trên CI

## Phạm vi và checkpoint

Thực hiện trên nhánh `codex/final-javafx-simplification`, HEAD ban đầu
`9da23dc9af17ea81e06beba0ea2d33d37f442a8b`, ngày 12/11/2026 theo giờ Việt Nam.

Checkpoint trước khi sửa nằm tại `target/ci-fix-checkpoint-20261112-233208/`.
Checkpoint chứa bản gốc của UserMenu, ClientUiTest, workflow và pom, HEAD, trạng thái
Git, patch working tree/index và SHA-256 của các file trong `data/`. Năm file tài
liệu đã ở trạng thái deleted khi bắt đầu được ghi lại trong checkpoint.

Bản vá gồm hai file sau.

- `src/test/java/com/auction/client/controller/ClientUiTest.java`
- `docs/CI_FIX_REPORT.md`

## Artifact đầu vào

Đã đọc trực tiếp mọi Surefire XML trong hai ZIP, cộng số liệu ở cấp testsuite và
kiểm tra testcase lỗi cùng properties môi trường.

| Artifact | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| `test-and-quality-reports.zip` | 218 | 0 | 1 | 0 |
| `test-and-quality-reports (1).zip` | 218 | 1 | 1 | 0 |

Cả hai ghi Linux, JDK Temurin 25.0.5, JavaFX 26.0.2. Cả hai lỗi ở
`accountPopupClosesOnFocusLossEscapeAndOwnerHide`, dòng gốc 1279, qua `awaitFx`
dòng 566. ZIP thứ hai còn lỗi ở
`accountPopupSupportsOutsideClickKeyboardAndNavigation`, dòng gốc 1401.
Stack trace đầy đủ và SHA-256 ZIP được lưu trong
`target/ci-fix-evidence/artifact-summary.json`.

## Nguyên nhân và bằng chứng

### Phụ thuộc đăng nhập giữa các test

Test `@Order(14)` dùng phiên đăng nhập do test `@Order(13)` thiết lập. Chạy riêng
hai test được báo lỗi trên mã gốc làm test 14 lỗi NullPointerException khi đọc
`popup` từ menu null. Bằng chứng nằm trong `windows-baseline.log` và
`windows-baseline.xml` ở thư mục evidence. Đây là phụ thuộc thứ tự đã tái hiện
trực tiếp trên Windows. Helper `prepareAccountMenu()` đăng nhập và chờ dashboard
tải xong cho từng test popup, bao gồm test tháo header ở order 13.

### Mở popup khi trạng thái native đang chuyển tiếp

Mã gốc gọi `stage.toFront()`, `stage.requestFocus()` và `trigger.fire()` trong cùng
một lượt FX. Hai lời gọi đầu gửi yêu cầu tới hệ cửa sổ. Trạng thái focus/layout
được quan sát sau khi hệ cửa sổ xử lý sự kiện mới là tiền điều kiện phù hợp cho
một thao tác kích hoạt từ người dùng.

`UserMenu.openMenu()` lấy `trigger.localToScreen`, có nhánh return khi bounds hoặc
owner null, rồi gọi `popup.show`. Listener focus lên lịch kiểm tra owner và popup
bằng `Platform.runLater`. JavaFX `PopupWindow` 26.0.2 trong sources JAR cục bộ đồng
bộ focused property của popup theo owner khi hiển thị. Vì vậy, hai focused
property cùng false có thể phản ánh giai đoạn mất focus của owner.

Mã native GTK xử lý mất focus bằng ungrab, có thể dẫn tới auto-hide. Yêu cầu
focus dùng `gtk_window_present`. Đây là cơ sở cho việc chờ focus thật trước khi
mở và thu thập cả log auto-hide lẫn stack của hide. Tham khảo mã nguồn OpenJFX
GTK, nhánh master. [glass_window.cpp](https://github.com/openjdk/jfx/blob/master/modules/javafx.graphics/src/main/native-glass/gtk/glass_window.cpp)

Artifact gốc chỉ ghi timeout tại phép quan sát `isShowing` trên pulse. Nhánh bỏ
qua show vì bounds null và trường hợp show rồi hide đều phù hợp với thông tin
đó. Nguyên nhân chính xác của lần đóng/bỏ qua mở trên Linux còn cần xác minh
bằng trace mới trên GitHub Actions.

### Click ngoài và vị trí chuột native

Mã gốc đưa chuột tới góc root, click và đưa chuột về ngay trong cùng một lượt FX.
Trên GTK, Robot dùng XWarpPointer và XTestFakeButtonEvent. Việc server X xử lý
request và việc JavaFX xử lý sự kiện đi vào cần được quan sát riêng qua vòng lặp
sự kiện. [GlassRobot.cpp](https://github.com/openjdk/jfx/blob/master/modules/javafx.graphics/src/main/native-glass/gtk/GlassRobot.cpp)

Lần chạy Windows trong sandbox tái hiện timeout sau click ngoài. Trace cho thấy
`ownerFocused=true`, popup mở được, nhưng vị trí chuột sau mouseMove gần
`(0.4, 0.4)`. Chạy lại cùng test với quyền desktop cho Robot thì PASS. Hai log
`windows-diagnostic-original.log` và `windows-diagnostic-desktop.log` lưu bằng
chứng này. Đây là giới hạn của lần chạy sandbox Windows. Liên hệ với lỗi click
trên Linux cần log Robot từ runner.

Điểm click của test nằm trong nội dung owner và ngoài popup tài khoản. Chuyển
focus sang cửa sổ khác được kiểm tra riêng bằng một Stage thứ hai.

## Thay đổi kiểm thử

- Mỗi test popup tự thiết lập phiên seller, dashboard và trạng thái loading.
- Trước mỗi lần mở, chờ owner showing/focused, layout và bounds của trigger có
  kích thước dương. CSS/layout được thực hiện lại ngay trước activation.
- Kiểm tra `Popup.show` mở đồng bộ ngay sau `trigger.fire`, rồi kiểm tra popup
  còn mở và focused ở pulse tiếp theo.
- Ghi log `ownerShowing`, `ownerFocused`, `needsLayout`, `triggerBounds`,
  `popupShowing`, `popupFocused`, hình học popup và danh sách window/focus.
- Ghi `WINDOW_SHOWING`, `WINDOW_SHOWN`, `WINDOW_HIDING`, `WINDOW_HIDDEN`,
  `AUTO_HIDE` và stack tại hide. Stack giúp phân biệt đường gọi từ UserMenu,
  auto-hide native, Escape, tháo header và đóng owner.
- Robot di chuyển trước, chờ vị trí thực khớp điểm click với sai số dưới 2 logical
  pixels, rồi click ở lượt FX tiếp theo. Điểm click phải nằm trong owner, ngoài
  popup và trên màn hình. Ngay trước click, kiểm tra popup vẫn mở, bộ đếm
  auto-hide bằng 0 và chuột vẫn đúng vị trí. Sau click, chờ cả popup đóng và
  callback `AUTO_HIDE`.
- Khôi phục chuột sau khi auto-hide hoàn thành. Khối finally nhả phím/nút chuột
  và khôi phục vị trí kể cả khi assertion lỗi.
- Giữ kiểm tra Escape, chuyển focus, owner hide, tháo header, keyboard-nav,
  first-item focus, điều hướng ví và tháo menuOwner/menuScene. Bổ sung kiểm tra
  Escape trả focus về trigger và phím Tab thật chuyển tới mục kế tiếp.
- Cleanup của ba test popup đóng active menu, tháo các listener chẩn đoán và
  kiểm tra popup/owner/scene đã được giải phóng. Class keyboard-nav được gỡ trên
  đúng root ban đầu kể cả sau navigation.
- Khi timeout, assertion ghi tên giai đoạn cùng trạng thái cửa sổ. `awaitFx`
  tiếp tục dùng deadline 8 giây.

Đã kiểm tra trực tiếp `UserMenu.java` và `.github/workflows/ci.yml`. Các lần chạy
desktop cho thấy UserMenu đóng qua auto-hide khi click ngoài, đóng khi mất focus,
Escape, hide owner, tháo header và navigation. Bản vá tập trung vào các tiền điều
kiện và việc đồng bộ thao tác trong test. Workflow hiện dùng `ubuntu-latest`,
Temurin 25 và `xvfb-run -a mvn -B verify -Dauction.ui.tests=true`. Log chẩn đoán đi
vào Surefire XML mà bước upload artifact hiện tại thu thập được. Cảnh báo Node.js
20/setup-java v4 được xem xét độc lập với các stack trace Surefire nêu trên.

## Kết quả kiểm thử

Maven Windows dùng Oracle JDK 25.0.4.1, Maven 3.10.0 và JavaFX 26.0.2. `java` trên
PATH dùng JDK 26, còn `JAVA_HOME` mà Maven sử dụng trỏ tới JDK 25. Bằng chứng
trong `target/ci-fix-evidence/maven-environment.log`.

| Lần chạy | Kết quả |
| --- | --- |
| Hai test popup, bản cuối, 10 lần Maven độc lập liên tiếp | 20/20 testcase PASS, 0 failure/error/skip |
| Từng test popup chạy riêng, gồm tháo header | 3/3 PASS, 0 failure/error/skip |
| Toàn bộ `mvn verify -Dauction.ui.tests=true`, lần 1 | 218/218 PASS, 0 failure/error/skip, Checkstyle 0 lỗi |
| Toàn bộ `mvn verify -Dauction.ui.tests=true`, lần 2 | 218/218 PASS, 0 failure/error/skip, Checkstyle 0 lỗi |
| Toàn bộ verify sau assertion giữ popup mở ngay trước click, lần 3 | 218/218 PASS, 0 failure/error/skip, Checkstyle 0 lỗi |
| Linux/Xvfb, JDK 25, bản vá hiện tại | Cần chạy trên GitHub Actions |

Log và XML cho 10 lần chạy bản cuối nằm tại
`target/ci-fix-evidence/windows-final-01` tới `windows-final-10`, đuôi `.log` và
`.xml`. Mỗi lần dùng lệnh PowerShell sau, với quyền desktop cho Robot.

```powershell
mvn -o -B '-Dmaven.repo.local=.maven-repository' '-Dauction.ui.tests=true' '-Dtest=ClientUiTest#accountPopupClosesOnFocusLossEscapeAndOwnerHide+accountPopupSupportsOutsideClickKeyboardAndNavigation' test
```

Full verify dùng cùng các cờ Maven, thay bộ chọn test bằng goal `verify`.
Log toàn bộ nằm tại `windows-verify-1.log`, `windows-verify-2.log`,
`windows-verify-3.log`. Mỗi lần có thư mục XML riêng mang hậu tố `-reports`.
Các suite bao gồm 16 ClientUiTest, 1 ServerUiIntegrationTest, 11 FullStackTest,
21 AuctionServiceTest, 27 AuctionStateValidationTest, 12 AuctionRepositoryTest,
3 ServerIntegrationTest và các suite còn lại. Hai lần đầu chạy trước khi bổ
sung ba assertion ngay trước Robot click. Mười lần popup bản cuối chạy sau
phần bổ sung này. Các lần chạy riêng từng test nằm trong `windows-solo-*.log`
và `.xml`.

Ví dụ trace của lần chạy bản cuối số 1 ghi `after trigger.fire` với
`ownerShowing=true`, `ownerFocused=true`, `popupShowing=true`, `popupFocused=true`
và `triggerBounds` có kích thước dương. Sau Robot click là `WINDOW_HIDING` với
đường gọi `PopupWindow.doAutoHide`, tiếp theo có `AUTO_HIDE`. Mọi XML của 10 lần
đều ghi `tests="2" failures="0" errors="0" skipped="0"`.

## Xác minh Linux còn cần thực hiện

Máy hiện tại chạy Windows 11. `wsl --list --verbose` báo WSL chưa được cài đặt.
Docker/Podman và môi trường Linux/Xvfb khả dụng chưa được tìm thấy. Vì vậy, kết
quả của bản vá trên Linux và độ ổn định CI còn chờ xác minh. JDK Windows và
Temurin trong artifact có patch version/vendor khác nhau như đã ghi ở trên.
Kết quả dò môi trường được lưu trong `target/ci-fix-evidence/environment.json`.

Các lệnh sau dùng cùng cấu hình Xvfb/JDK của workflow hiện tại.

```bash
java -version
for run in $(seq 1 10); do
  xvfb-run -a mvn -B test -Dauction.ui.tests=true \
    '-Dtest=ClientUiTest#accountPopupClosesOnFocusLossEscapeAndOwnerHide+accountPopupSupportsOutsideClickKeyboardAndNavigation' || exit 1
done
xvfb-run -a mvn -B verify -Dauction.ui.tests=true
```

Khi xem artifact mới, kiểm tra giai đoạn `owner focused and header laid out`,
`after trigger.fire`, `WINDOW_SHOWN`, stack `WINDOW_HIDING`, `AUTO_HIDE` và Robot
target/click. Nếu native focus hoặc mouse movement thất bại, assertion mới sẽ
chỉ ra đúng tiền điều kiện. Nếu popup đóng sớm sau show, stack hide và thứ tự
focus/auto-hide sẽ cung cấp bằng chứng để sửa production khi cần.

## Dữ liệu và trạng thái làm việc

Các test nghiệp vụ được kiểm tra dùng fixture/mock trong bộ nhớ và thư mục tạm.
Kiểm tra SHA-256 trước và sau được lưu tại
`target/ci-fix-evidence/data-integrity.json`. Bản checkpoint dùng encoding mặc
định PowerShell cho hai đường dẫn backup có dấu, nên hai file đó được đối chiếu
theo thư mục cha và SHA-256. Việc đối chiếu nội dung cho toàn bộ 1.436 file trong
`data/` cho kết quả giống checkpoint tại thời điểm kiểm tra.

Lần verify cuối kết thúc lúc 23 giờ 47 phút 43 giây ngày 12/11/2026, giờ Việt Nam.
Sau verify, HEAD đối chiếu với checkpoint là
`9da23dc9af17ea81e06beba0ea2d33d37f442a8b`. Thay đổi của phiên làm việc gồm test
và báo cáo này. Kết quả kiểm tra Git, hash nguồn và tổng hợp XML được lưu cùng
thư mục evidence. Việc xác minh Linux/Xvfb và kết luận cuối cùng về nguyên nhân
đóng sớm trên runner là phần còn chờ GitHub Actions.
