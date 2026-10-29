# Hệ thống đấu giá trực tuyến

Hệ thống hỗ trợ người dùng tham gia đấu giá, người bán quản lý sản phẩm và quản trị viên quản lý người dùng và phiên đấu giá. Giao diện JavaFX hiện chạy với dữ liệu và dịch vụ giả lập (mock).

## Cấu trúc thư mục

- `auction-client-ui/`: ứng dụng JavaFX, dịch vụ mock, tài liệu và kiểm thử client.
- `auction-server/`: module hệ thống và mạng đang phát triển.

## Chạy client với mock

Cần JDK 25 và Maven. Từ thư mục gốc của repo:

```bash
cd auction-client-ui
mvn javafx:run
```

Tài khoản thử được liệt kê trong [docs/DEV_ACCOUNTS.md](auction-client-ui/docs/DEV_ACCOUNTS.md).

## Kiểm thử

```bash
cd auction-client-ui
mvn -B test
python scripts/check-encoding.py
```

## Phân công

- Thành viên 1: Hệ thống & Mạng
- Thành viên 2: Dữ liệu & Logic nghiệp vụ
- Thành viên 3: Lập trình giao diện (client JavaFX, đã hoàn thành phần UI chạy bằng mock)
- Thành viên 4: Nâng cao & Đảm bảo chất lượng
