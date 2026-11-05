# Giao thức backend v1

Socket TCP. Mỗi frame: 4 byte signed int big-endian độ dài N, tiếp theo N byte JSON UTF-8. 0 < N <= 8 MiB. Không dùng readUTF/writeUTF hoặc Java Object streams. Client có một reader duy nhất phân biệt `type=response/event`; việc ghi phải tuần tự hóa. Server giới hạn 200 kết nối, hàng đợi 256 frame/client; client quá chậm bị ngắt để không chặn giao dịch. Idle 60 giây: gửi PING định kỳ (ví dụ 20 giây).

```json
{"protocolVersion":1,"requestId":"req-1","action":"LOGIN","token":"","payload":{"username":"bidder_hieu","password":"Bidder12345"}}
```

```json
{"protocolVersion":1,"type":"response","requestId":"req-1","success":true,"result":{"token":"...","expiresAt":"...Z","user":{"id":"...","username":"bidder_hieu","role":"BIDDER","balance":100000000}},"version":12,"serverTime":"2026-11-05T07:00:00"}
```

Lỗi trả `success=false,errorCode,message` thay result. Mã lỗi gồm UNAUTHENTICATED, AUTHENTICATION_FAILED, FORBIDDEN, VALIDATION_ERROR, NOT_FOUND, CONFLICT, BID_TOO_LOW, INSUFFICIENT_FUNDS, AUCTION_NOT_STARTED, AUCTION_CLOSED, AUTO_BID_LIMIT, STORAGE_ERROR, UNKNOWN_ACTION, PROTOCOL_ERROR, INVALID_REQUEST, SERVER_BUSY. Một số lỗi transport không có response; client phải giữ kết quả lệnh là chưa biết.

Tất cả lệnh ghi dưới đây cần `commandId` 8–100 ký tự `[a-zA-Z0-9_-]` ở envelope, đề xuất UUID. Khóa được lưu bền vững theo user; cùng khóa và cùng action/payload trả đúng response cũ, không thực thi lại. Đổi nội dung nhưng giữ khóa → CONFLICT. Payload phải giữ cùng thứ tự trường JSON khi replay vì fingerprint v1 dùng chuỗi JSON. RequestId là mỗi lần trao đổi và được phép đổi; commandId là cùng một thao tác nghiệp vụ và phải giữ nguyên khi gửi lại.

Session được kiểm tra trước replay; user khóa không được dùng replay để thao tác. NOT_FOUND khi tra command chỉ có nghĩa chưa có kết quả đã commit tại lúc đọc; không phải bằng chứng request trên kết nối khác sẽ không tới sau đó. Client có thể gửi lại với cùng commandId để đối soát an toàn, không tạo mã mới.

| Action | Payload | Result |
|---|---|---|
| PING | `{}` | status OK và serverTime |
| REGISTER (ghi) | username,password,fullName,email,role=BIDDER/SELLER | profile, số dư 0; cần LOGIN sau đó |
| LOGIN | username (hoặc email),password | token,expiresAt,user |
| LOGOUT | `{}` + token | loggedOut |
| GET_ME | `{}` + token | profile hiện hành |
| LIST_AUCTIONS | status?,category?,search? | array snapshot không chứa ảnh byte/lịch sử |
| GET_AUCTION | auctionId | snapshot |
| GET_BID_HISTORY | auctionId | array bid theo thứ tự chấp nhận |
| LIST_MY_PRODUCTS | `{}` + token Seller | array snapshot của Seller |
| CREATE_PRODUCT_AUCTION (ghi) | name,description,category,startingPrice,minBidStep,startTime,endTime,imageBase64? | snapshot |
| UPDATE_PRODUCT_AUCTION (ghi) | auctionId và toàn bộ trường CREATE | snapshot; chỉ OPEN chưa bid |
| DELETE_PRODUCT_AUCTION (ghi) | auctionId | snapshot đánh dấu xóa |
| GET_IMAGE | auctionId | imageBase64,imageType |
| PLACE_BID (ghi) | auctionId,amount | auction snapshot và bids lịch sử |
| SET_MY_AUTO_BID (ghi) | auctionId,maxBid,increment | rule |
| CANCEL_MY_AUTO_BID (ghi) | auctionId | rule hoặc null |
| GET_MY_AUTO_BID | auctionId | rule hoặc null |
| GET_MY_WALLET_SNAPSHOT | `{}` | balance,held,available,version |
| LIST_MY_TRANSACTIONS | `{}` | array mới nhất trước |
| DEPOSIT_SIMULATED (ghi) | amount >= 10000 | wallet snapshot |
| WITHDRAW_SIMULATED (ghi) | amount | wallet snapshot |
| GET_COMMAND_STATUS | commandId (trong payload) | status SUCCEEDED/REJECTED + response; hoặc NOT_FOUND |
| LIST_USERS | `{}` + Admin token | array profile không hash |
| ADMIN_LIST_AUCTIONS | bộ lọc như LIST_AUCTIONS | array snapshot |
| LOCK_USER / UNLOCK_USER / DELETE_USER (ghi) | userId | profile |
| CHANGE_USER_ROLE (ghi) | userId,role | profile |
| CANCEL_AUCTION (ghi) | auctionId,reason | snapshot |
| DELETE_AUCTION (ghi) | auctionId | snapshot đánh dấu xóa, chỉ CANCELED |
| SUBSCRIBE_AUCTION | auctionId + token | snapshot sau khi subscription có hiệu lực |
| UNSUBSCRIBE_AUCTION | auctionId | `{}` |

Danh mục: ELECTRONICS, ART, VEHICLE, JEWELRY, REAL_ESTATE, OTHER. Tiền dùng JSON integer (VND). Thời gian phiên dùng ISO LocalDateTime ở Asia/Ho_Chi_Minh. Session expiresAt dùng Instant UTC. Trạng thái OPEN/RUNNING/FINISHED/PAID/CANCELED. ID được server cấp; không sử dụng ID do client tự tạo làm chủ sở hữu hoặc quyết định quyền.

Snapshot có id,itemId,name,description,category,sellerId,sellerUsername,startingPrice,currentPrice,minBidStep,minNextBid,startTime,endTime,status,highestBidderId,highestBidderUsername,totalBids,hasImage,version,settled,deleted; khi thích hợp có paidAt/cancellationReason. Giá trị highestBidder trống khi chưa có bid. Model JavaFX hiện dùng Item lồng trong Auction; adapter cần ánh xạ từ snapshot phẳng này.

Bid gồm id,auctionId,bidderUsername,amount,timestamp,auto. Rule gồm id,auctionId,userId,username,maxBid,increment,createdAt,order,active,limitReached,autoCount. Giao dịch gồm id,userId,username,type,amount,balanceAfter,timestamp,status,note,auctionId; type DEPOSIT/WITHDRAW/BID_HOLD/BID_RELEASE/PAYMENT/SALE. HOLD/RELEASE chỉ thay khả dụng, không làm đổi tổng số dư.

## Push và đối soát

```json
{"type":"event","event":"BID_PLACED","eventId":"...","version":20,"serverTime":"2026-11-05T07:00:01","auctionId":"...","userId":"","auction":{"id":"...","currentPrice":1200000,"version":20}}
```

Event công khai cho subscriber: AUCTION_CREATED, AUCTION_UPDATED, AUCTION_DELETED, AUCTION_STARTED, BID_PLACED, AUCTION_EXTENDED, AUCTION_FINISHED, AUCTION_CANCELED. Event riêng cho tài khoản đăng nhập: WALLET_CHANGED, USER_CHANGED, AUTO_BID_CHANGED. Các giá trị cấu hình maxBid của người khác không nằm trong event công khai.

Client JavaFX dùng SUBSCRIBE_FEED cho thay đổi danh sách và các phiên; xem phần bổ sung tích hợp bên dưới. SUBSCRIBE_AUCTION đăng ký trước rồi đọc snapshot; push có thể tới trước response. Client đệm event trong lúc subscribe, áp dụng snapshot rồi chỉ giữ event có version mới hơn. Version là toàn hệ thống nên khoảng nhảy không tự chứng minh mất event của một phiên. Dùng version snapshot của phiên để bỏ event cũ/trùng; reconnect thì subscribe và tải snapshot/lịch sử lại.

Lệnh đã commit nhưng mất response: giữ commandId, kết nối/đăng nhập lại, GET_COMMAND_STATUS hoặc replay cùng payload/commandId; không suy đoán từ lịch sử rằng lệnh chưa chạy. Sau kết quả cuối, tải snapshot đủ mới rồi mở khóa UI. Khi LOGOUT/đổi token, subscription cũ bị xóa. Không tự chuyển mock khi server lỗi.

## Adapter JavaFX hiện hành

Giữ các interface service hiện có. Tạo transport JSON và các adapter dùng chung token. LOGIN lưu profile, REGISTER thành công gọi LOGIN để giữ hành vi tự đăng nhập của UI. Adapter ProductService ánh xạ ItemCategory sang mã enum; chuyển ảnh local thành base64. Không chuyển actor/username của interface thành quyền server. Ví đọc một snapshot thay vì bốn request không cùng thời điểm. Bid/auto-bid cần xử lý errorCode kết thúc rõ ràng và kết quả chưa xác định riêng; token cục bộ trong BidSubmissionTracker hiện không phải commandId của server.


## Bổ sung sau tích hợp JavaFX (05/11/2026)

- `SUBSCRIBE_FEED` (token bắt buộc) nhận mọi sự kiện phiên công khai, cùng sự kiện riêng của chính user. Client gọi lại sau reconnect để Home/Seller/Admin cập nhật không polling giá.
- `GET_ROOM_SNAPSHOT` (token bắt buộc, payload auctionId) trả `{auction,bids}` trong cùng giao dịch đọc; tránh lịch sử và giá thuộc hai thời điểm khác nhau.
- `GET_MY_WALLET_SNAPSHOT` thêm `transactions` mới nhất trước, cùng số dư/held/available trong một lần đọc.
- `BID_PLACED` thêm `bid` của chính lượt giá đó để adapter gọi listener đúng ID/cờ auto.
- `SALE` được ánh xạ thành Thu tiền bán sản phẩm trong UI; BID_HOLD/BID_RELEASE được phân biệt với thu/chi làm thay đổi tổng số dư.
- `service/server/ServerServices` dùng lại các interface hiện có, tự đăng nhập sau đăng ký, cache ảnh đã tải, lưu pending command trước khi gửi và replay cùng commandId khi cần. Token và mật khẩu không được lưu ở file pending. Heartbeat 5 giây giữ kết nối và phục hồi feed, không polling giá.
