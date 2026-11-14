# Seller CI layout fix — 14/11/2026

Nhánh: `codex/final-javafx-simplification`. Đã đọc `ClientUiTest`,
`SellerProductFormView`, hai FXML Seller và `app.css`.

## Nguyên nhân và số đo

Tái hiện trên **Windows 11, JDK 25.0.4.1, JavaFX 26.0.2, scale 1** khi chạy
baseline 20 lần trong cùng JVM: 17 PASS, 3 errors. Hai error đo bounds ở
**960×720**; một error kiểm tra hai cột trước khi layout ổn định.
Đây là bằng chứng tái hiện cục bộ, chưa phải số đo của runner Linux bị lỗi.

Stage/Scene đã nhận kích thước mới nhưng `editorHost` vẫn rộng 1122 px từ
cửa sổ 1200 px trước đó. Form/footer còn rộng 1000 px; `needsLayout=true`.
`productForm().layout()` chỉ layout cây con, không cập nhật kích thước từ
Scene qua toàn bộ parent. `awaitFx` dùng AnimationTimer chỉ kiểm tra Stage.width;
nó không bảo đảm CSS/layout đã hoàn tất. Test còn đổi nội dung validation rồi
đo trong cùng callback FX, trước pulse tiếp theo.

Số đo thực tế, đơn vị logical px; bounds Lưu là `localToScene(boundsInLocal)`:

| Lượt | Stage | Scene | Form/footer/ScrollPane width | Viewport width | Lưu [minX,minY → maxX,maxY] | Layout pending |
|---|---|---|---|---|---|---|
| Baseline lỗi | 960×720 | 942×673 | 1000/1000/1000 | 1000 | [1030,667 → 1089,708] | true |
| Sau sửa | 1200×800 | 1182×753 | 1000/1000/1000 | 1000 | [1030,584 → 1089,625] | false |
| Sau sửa | 960×720 | 942×673 | 882/882/882 | 882 | [851,584 → 910,625] | false |
| Sau sửa | 640×640 | 622×593 | 562/562/562 | 545 | [531,528 → 590,569] | false |

## Thay đổi

Chỉ sửa `src/test/java/com/auction/client/controller/ClientUiTest.java` và báo cáo này.
Test đợi Scene nhận đúng kích thước nội dung, trừ phần viền cửa sổ đo trên nền
tảng hiện tại; sau đó đợi hai **post-layout pulses** có cây layout sạch. Áp dụng
sau resize, mở form và thay đổi validation/busy. Có timeout và gỡ listener trong
`finally`; không thêm sleep, không chờ điều kiện “nút đã vừa Scene” để cho qua test.
[JavaFX xác định post-layout listener chạy sau CSS/layout](https://openjfx.io/javadoc/26/javafx.graphics/javafx/scene/Scene.html#addPostLayoutPulseListener(java.lang.Runnable)).

Giữ nguyên hai assertion maxX/maxY và sai số **1 px**. Bổ sung chẩn đoán kích
thước yêu cầu/Stage/Scene, output/render scale, form/host/footer/ScrollPane,
viewport, các cột, validation, Lưu/Hủy và layout/local/scene bounds.
Log ngắn lưu số đo cả lượt PASS trong Surefire.

Mỗi kích thước kiểm thêm lỗi bước giá, thông báo lỗi lưu dài xuống dòng,
trạng thái đang lưu, form hợp lệ, bounds footer/Hủy và không có scrollbar ngang.
Giữ kiểm tra hai cột/một cột, validation, cancel/Escape/navigation và cleanup.
Layout sau pulse đã vừa nên không đổi FXML/CSS, minWidth cửa sổ hoặc production.
Backend, Socket, JSON, concurrency, ví, OOP và Design Patterns giữ nguyên.

## Kiểm thử thực tế

- Baseline một lần ở scale 1 và 1.25: PASS; chạy lặp mới bộc lộ race nêu trên.
- Baseline 20 lần, scale 1: 20 tests, 0 failures, **3 errors**, 0 skipped.
- Bản sửa 20 lần cùng scale/JVM: **20/20 PASS**, 0 failures/errors/skipped;
  mỗi lượt kiểm cả ba kích thước và bốn trạng thái footer.
- Nhóm `ClientUiTest#seller*`: **4 lượt × 4 tests PASS**; scale 1, 1.25 (hai
  lượt), 1.5. Đã xem ảnh form một cột 640 và hai cột 960.
- `clean verify -Dauction.ui.tests=true`: **237/237 PASS**, 30 suites,
  0 failures/errors/skipped; BUILD SUCCESS lúc 15:57 (UTC+7), mất 6 phút 7 giây.
  Checkstyle **0 violations**; JaCoCo **4730/5741 lines = 82,39%**, qua gate 60%.
- Linux/Xvfb và GitHub Actions của bản sửa: **CHƯA CHẠY**. Máy không có WSL,
  Docker/Podman hay Linux runner; không kết luận Linux CI PASS.

Log, script lặp và bản sao Surefire ở thư mục git-ignored
`.maven-repository/seller-ci-layout-20261114/`. Thử nghiệm lặp dùng annotation
tạm thời, luôn khôi phục source byte-for-byte; bản giao giữ `@Test` và số test gốc.
Dữ liệu thật: **1979/1979 file khớp SHA-256** sau toàn bộ suite; không thêm,
xóa hoặc thay đổi file. Manifest và `final-audit.json` nằm cùng thư mục log.
Không commit/push/merge; workflow CI giữ nguyên.

Lệnh Windows đã chạy (cache/temp đặt trong workspace):

```powershell
mvn -o -B '-Dmaven.repo.local=D:/Bai-Tap-Lon-gon/.maven-repository' `
  '-Dauction.ui.tests=true' `
  '-DargLine=-Djavafx.cachedir=D:/Bai-Tap-Lon-gon/.maven-repository/seller-ci-layout-20261114/javafx-cache -Djava.io.tmpdir=D:/Bai-Tap-Lon-gon/.maven-repository/seller-ci-layout-20261114/test-tmp' `
  clean verify
```

Lượt tập trung dùng `test '-Dtest=ClientUiTest#seller*'`; các lượt scale thêm
`-Dglass.win.uiScale=1`, `1.25`, `1.5` trong `argLine`.
Lệnh cần chạy tiếp trên Linux/CI: `xvfb-run -a mvn -B clean verify -Dauction.ui.tests=true`.
