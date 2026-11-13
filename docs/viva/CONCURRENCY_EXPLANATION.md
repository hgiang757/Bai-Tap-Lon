# Giải thích đa luồng và tính nhất quán

Điểm tra code: [CODE_MAP.md](CODE_MAP.md). Đọc `Server`, `AuctionService.execute/commit`,
`AuctionRepository.save`, `JsonTransport` và `ViewTasks` theo thứ tự.

## Các thread thật sự tồn tại

| Thread / executor | Công việc |
|---|---|
| Server `auction-accept` (platform thread) | accept socket, giới hạn 200 client |
| Server virtual-thread-per-task executor | Hai task readLoop/writeLoop cho mỗi client |
| Server single-thread scheduler | tick mỗi 250 ms, mở/đóng/thanh toán phiên qua cùng service |
| Client `auction-reader` (virtual thread) | Đọc frame, hoàn tất future hoặc chuyển event sang dispatch |
| Client `auction-events` (single daemon thread) | Gọi adapter.onEvent/listeners theo thứ tự nhận |
| Client `auction-heartbeat` | PING mỗi 5 giây, phát hiện mất mạng và đăng ký feed lại |
| ViewTasks `client-service` | Pool 4 thread, queue 64, thực hiện gọi service nền |
| ViewTasks `client-image` | Pool 2 thread, queue 32 riêng cho đọc ảnh |
| JavaFX Application Thread | Xử lý thao tác và cập nhật scene graph; Timeline UI |

Virtual thread là thread nhẹ do JVM lập lịch; không đồng nghĩa mọi nghiệp vụ được
thực thi song song. Nhiều reader có thể chờ trên monitor của cùng AuctionService.

## Monitor và file lock bảo vệ điều gì?

`execute()` là synchronized instance method: khóa **this của service**, không phải
khóa từng client hoặc từng Auction record. Một Server có một service chung. Khóa
bao trùm refresh phiên, xác thực, tra commandId, kiểm tra ví/giá, sửa state, ghi
receipt, save và enqueue event. `tick()`, `authenticatedUserId()`, `close()` cũng
đồng bộ trên service; monitor Java là reentrant nên tick gọi execute không tự deadlock.

AuctionRepository có synchronized load/save/close và FileLock trên `server.lock`.
Monitor repository bảo vệ thao tác trong đối tượng; file lock ngăn server khác
ghi chung thư mục. File lock không thay thế khóa kiểm tra ví/giá. Không có
ReentrantLock trong transaction bản này; không cần nói là dùng cả hai loại khóa.

## Tình huống A/B cùng đặt 1.200.000 VNĐ

Giả thiết: giá hiện tại 1.000.000, minBidStep 100.000, phiên RUNNING còn giờ, hai
người khác seller/admin, đủ tiền, commandId khác nhau, không có auto-bid đang bật,
filesystem lưu được. Nếu bước giá lớn hơn 200.000 thì cả hai đều có thể bị từ chối.

1. A và B gửi qua hai socket, hai readLoop gọi cùng `execute()`.
2. Giả sử A lấy monitor trước: mức tối thiểu 1.100.000, A hợp lệ. State ghi A dẫn
   đầu 1.200.000, history thêm một bid, hold của A được tính từ state, receipt A lưu.
3. Repository lưu snapshot; sau thành công mới publish. A nhả monitor.
4. B mới được vào, đọc **giá mới** 1.200.000; mức tối thiểu bây giờ là 1.300.000.
   1.200.000 của B bị `BID_TOO_LOW`; không thêm bid/hold cho B. Receipt REJECTED của
   B vẫn được lưu để retry cùng mã trả đúng kết quả.
5. Chỉ một leader trong trường `highestBidderId`. Ai lấy monitor trước không được
   dự đoán theo lúc nhấn nút hay theo timestamp trên máy client. Monitor không hứa
   fairness hay thứ tự đến tuyệt đối.

Test thực tế: `AuctionServiceTest.equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice`
dùng latch cùng phát lệnh, kiểm tra một success, một BID_TOO_LOW, đúng một history
và tổng khả dụng 398.800.000 từ hai ví fixture 100 triệu + 300 triệu. Test socket
`ServerIntegrationTest.simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer`
kiểm tra tương tự qua hai kết nối thật.

Nếu A lỗi lưu, state/hold/receipt được rollback và không publish; B có thể thắng.
Nếu auto-bid bật, giá cuối có thể cao hơn 1.200.000 và leader khác A do auto-bid chạy
ngay trong transaction. “Một bid được chấp nhận” không đồng nghĩa thắng cuối phiên.

## Race condition, lost update và hai người thắng

Race condition xuất hiện nếu hai thread cùng đọc giá cũ rồi cùng ghi leader hoặc
nếu ví được kiểm tra riêng trước khi hold. Lost update là một lần ghi đè kết quả
của lần khác. Khóa service buộc thao tác **đọc → kiểm tra → cập nhật → lưu** thành
một vùng tới hạn; không chỉ synchronized riêng setter.

Khóa chung còn bảo vệ một ví tham gia nhiều phiên. Khóa từng phiên riêng sẽ chưa
đủ: hai phiên có thể cùng thấy đủ tiền và cùng giữ vượt số dư. Test
`concurrentAuctionsCannotOverspendOneWallet()` bao phủ tình huống này.

`refreshAuctions()` và `settle()` chạy trong cùng monitor. `settled` và trạng thái
PAID ngăn chuyển tiền hai lần. Có người thắng: FINISHED được đặt nội bộ rồi PAID
trong cùng transaction; client có thể chỉ thấy PAID. Không có bid: FINISHED,
settled=true, không chuyển tiền. Cancel giải phóng hold, dừng rules, không settle lại.

## Atomicity và lỗi lưu

Service giữ `before = state.deepCopy()`. Nếu nghiệp vụ bị từ chối, khôi phục bản
sau refresh để vẫn giữ việc đóng phiên đúng giờ, rồi lưu receipt từ chối. Nếu ghi
thất bại: khôi phục toàn bộ before, xóa events, trả STORAGE_ERROR. Phạm vi rollback
là JSON state (ví, phiên, history, rules, commands), không phải mọi hiệu ứng ngoài
state như session trong RAM. Không gọi đây là cơ sở dữ liệu ACID phân tán.

Repository ghi `state.json.tmp`, write đủ buffer, `force(true)`, rồi
`Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`. Không hỗ trợ atomic move thì từ chối,
không fallback ghi đè thường. File hỏng không được tự reset và tmp không tự thay
snapshot chính khi restart. Không tuyên bố mô phỏng mất điện thật hoặc bảo đảm mọi
filesystem/network drive từ các test lỗi ghi trong thư mục tạm.

Nếu server đã commit nhưng ACK bị mất, client chưa biết kết quả. `requestId` chỉ
ghép response; retry cần **commandId cũ**, receipt được lưu cùng snapshot nên không
trừ tiền/thêm bid lần hai, kể cả restart. Không gọi đây là đảm bảo network
exactly-once: đó là xử lý lặp an toàn cho cùng command key/payload.

## Push, socket chậm và FX thread

`Server.publish()` chạy sau save, chỉ enqueue vào ArrayBlockingQueue 256; writeLoop
gửi frame. Offer không chờ queue trống, queue đầy thì đóng client. Service vẫn giữ
khóa trong lúc enqueue/publish nên callback publisher cần nhanh và không chờ phản
hồi client; publisher tùy ý do bên ngoài inject mà block vẫn có thể làm nghẽn service.

Client reader không chạy listener trực tiếp: chuyển sang `auction-events`, nhờ vậy
listener có thể gọi read snapshot mà reader vẫn xử lý response. Nếu callback bị
RuntimeException, bản sửa bắt riêng để observer khác vẫn nhận; callback chậm vẫn
làm chậm dispatch, chưa có cơ chế cô lập thời gian chạy mỗi observer.

JavaFX scene graph phải cập nhật trên FX Application Thread. `ViewTasks.dispatch()`
dùng Platform.runLater và kiểm tra scope đã đóng. Kết quả đọc cũ được loại bằng
version/ticket; dispose gỡ listener, dừng countdown. UI không được tự kết luận
server chưa bid chỉ vì timeout hoặc đọc lại lịch sử chưa thấy lệnh.

## Deadlock và giới hạn throughput

Deadlock là các thread chờ vòng tròn tài nguyên mà nhau giữ. Đường transaction
hiện đi service monitor → repository monitor; repository không gọi ngược service.
Publisher production enqueue, không chờ response từ client. Cách này giảm nguy cơ
chờ vòng tròn, không phải chứng minh toàn ứng dụng không thể deadlock. Không gọi
blocking service read trên FX thread; không để callback giữ khóa tùy ý rồi chờ UI.

Khóa toàn cục và snapshot toàn bộ khiến các phiên phải chờ nhau, PBKDF2/ghi đĩa có
thể tăng độ trễ. Đây là đánh đổi rõ ràng cho quy mô bài tập, chưa kiểm thử tải lớn
hoặc nhiều server ghi phân tán. Không thay cơ chế concurrency đã có test chỉ vì
muốn thêm Lock hoặc tăng số pattern.

## Hai lỗi callback đã sửa trong Client

`AuctionRoomController.refreshAuction()` chụp `bidKey` vào biến `key` trước khi
submit: worker cũ chỉ reconciled đúng người dùng/phiên nó đã bắt đầu. Đóng scope
chặn UI callback cũ; việc chụp key còn ngăn chính worker chưa bị hủy sửa tracker
của phòng mới. Test `oldRecoveryCannotReconcileAnotherRoomUnknownCommand` chặn
đối soát bằng latch rồi chuyển phòng, giữ UNKNOWN của phòng mới.

`WalletController.reload()` dùng khóa ViewTasks `wallet-read`; nạp/rút dùng
`wallet-write`. Push WALLET_CHANGED đến trước ACK có thể bắt đầu đọc ví mới mà
không thay thế callback đóng dialog của lệnh ghi. Giao dịch không bị hủy/gửi lại;
ServerUiIntegrationTest kiểm tra form nạp tiền và snapshot sau restart.

`ServerServices.response()` bỏ bước đăng ký feed bằng token cũ cho LOGIN/REGISTER.
Sau restart, thao tác xác thực có thể tạo phiên mới ngay; các request cần quyền
vẫn bị kiểm tra token. `FullStackTest.loginAfterRestartDoesNotRequireAnExpiredSessionReadFirst`
kiểm chứng không phải gửi một request lỗi trước rồi mới đăng nhập được.
