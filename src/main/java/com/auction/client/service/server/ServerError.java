package com.auction.client.service.server;

/** A definitive backend rejection, distinct from a transport failure with unknown outcome. */
public final class ServerError extends IllegalStateException {
  private final String code;

  /** Stores a stable server rejection code and its display message. */
  public ServerError(String code, String detail) {
    this(code, detail, null);
  }

  /** Retains the local cause when an error code is produced by the client. */
  public ServerError(String code, String detail, Throwable cause) {
    super(message(code, detail), cause);
    this.code = code;
  }

  /** Returns the stable error code used by the client or protocol. */
  public String code() {
    return code;
  }

  private static String message(String code, String detail) {
    return switch (code) {
      case "UNAUTHENTICATED" ->
          "Phiên đăng nhập đã hết hạn hoặc tài khoản bị khóa. Vui lòng đăng nhập lại.";
      case "AUTHENTICATION_FAILED" -> "Sai thông tin đăng nhập hoặc tài khoản bị khóa.";
      case "FORBIDDEN" -> "Tài khoản không có quyền thực hiện thao tác này.";
      case "INSUFFICIENT_FUNDS" -> "Số tiền có thể sử dụng không đủ.";
      case "WALLET_LIMIT_EXCEEDED" ->
          "Tổng số dư sau giao dịch vượt quá hạn mức tối đa 9.000.000.000.000 ₫.";
      case "BID_TOO_LOW" -> "Giá đặt phải từ giá hiện tại cộng bước giá tối thiểu trở lên.";
      case "AUCTION_NOT_STARTED" -> "Phiên đấu giá chưa bắt đầu.";
      case "AUCTION_CLOSED" -> "Phiên đã kết thúc hoặc bị hủy.";
      case "NOT_FOUND" -> "Không tìm thấy dữ liệu; hãy tải lại.";
      case "STORAGE_ERROR" -> "Máy chủ chưa lưu được thao tác. Vui lòng thử lại sau.";
      case "SERVER_ERROR" -> "Máy chủ gặp lỗi khi xử lý thao tác.";
      case "INVALID_REQUEST", "PROTOCOL_ERROR" ->
          "Yêu cầu không hợp lệ. Hãy kiểm tra phiên bản ứng dụng.";
      case "VALIDATION_ERROR" -> validationMessage(detail);
      case "CONFLICT" ->
          "Dữ liệu bị trùng, đã thay đổi hoặc thao tác đang bị hạn chế. Hãy tải lại để"
              + " kiểm tra.";
      case "AUTO_BID_LIMIT" -> "Có quá nhiều lượt đấu tự động. Hãy tăng bước giá rồi thử lại.";
      case "SERVER_BUSY" -> "Máy chủ đang bận. Vui lòng thử lại sau.";
      case "AI_BUSY" -> "Trợ lý đang xử lý yêu cầu. Vui lòng thử lại sau.";
      case "AI_TIMEOUT" -> "Trợ lý phản hồi quá thời gian. Vui lòng thử lại.";
      case "AI_NETWORK_ERROR" -> "Máy chủ đấu giá chưa kết nối được với dịch vụ AI.";
      case "AI_DNS_ERROR" -> "Máy chủ đấu giá không phân giải được địa chỉ dịch vụ AI.";
      case "AI_TLS_ERROR" -> "Kết nối TLS tới dịch vụ AI thất bại. Hãy nhờ quản trị viên kiểm tra.";
      case "AI_PROXY_ERROR" -> "Proxy yêu cầu xác thực để kết nối tới dịch vụ AI.";
      case "AI_PROVIDER_ERROR" -> "Dịch vụ AI chưa trả lời được. Vui lòng thử lại sau.";
      case "AI_RESPONSE_INVALID" -> "Phản hồi của trợ lý không đúng định dạng. Hãy thử lại.";
      case "AI_UNAVAILABLE" ->
          "Máy chủ chưa có khóa Gemini. Bạn vẫn có thể xem Thông tin phiên và Giá thị trường.";
      case "AI_KEY_FORMAT" ->
          geminiKeyMessage(detail);
      case "AI_AUTH_FAILED" -> "Gemini từ chối xác thực. Hãy nhờ quản trị viên kiểm tra khóa.";
      case "AI_FORBIDDEN" -> "Dự án Google hoặc khóa hiện không có quyền gọi Gemini.";
      case "AI_REQUEST_REJECTED" ->
          "Gemini từ chối yêu cầu. Hãy nhờ quản trị viên kiểm tra cấu hình.";
      case "AI_PROVIDER_CONFIGURATION" -> "Máy chủ đang chọn một nhà cung cấp AI chưa hỗ trợ.";
      case "AI_OLLAMA_NOT_CONFIGURED" -> "Máy chủ chưa cấu hình mô hình Ollama cục bộ.";
      case "AI_MODEL_UNAVAILABLE" -> "Không tìm thấy mô hình AI đã cấu hình trên máy chủ.";
      case "AI_PROVIDER_BUSY" -> "Dịch vụ AI đang quá tải. Vui lòng thử lại sau.";
      case "AI_CONTENT_BLOCKED" -> "Gemini không thể xử lý nội dung này. Hãy diễn đạt lại câu hỏi.";
      case "AI_RATE_LIMIT" -> "Dịch vụ AI đã đạt hạn mức hoặc đang giới hạn tốc độ yêu cầu.";
      case "AI_USER_LIMIT" -> "Bạn đã hết lượt hỏi AI trong giờ này.";
      case "AI_DAILY_LIMIT" -> "Máy chủ đã hết lượt hỏi AI hôm nay.";
      default -> "Không thể thực hiện thao tác. Kiểm tra kết nối và phiên bản ứng dụng.";
    };
  }

  private static String geminiKeyMessage(String detail) {
    String codePointPrefix = "Gemini preflight: forbidden whitespace/control/format ";
    if (detail.startsWith(codePointPrefix)) {
      String codePoint = detail.substring(codePointPrefix.length());
      if (codePoint.matches("U\\+[0-9A-F]{4,6}")) {
        return "Java đã chặn ký tự trắng, điều khiển hoặc ký tự ẩn " + codePoint
            + " trong khóa trước khi gọi Gemini.";
      }
    }
    return switch (detail) {
      case "Gemini preflight: only masking characters were received" ->
          "Máy chủ nhận toàn dấu che khóa. Dấu sao ở lời nhắc chỉ che phần nhập.";
      default -> "Khóa Gemini máy chủ nhận chứa ký tự ẩn hoặc ký tự HTTP không hỗ trợ.";
    };
  }

  private static String validationMessage(String detail) {
    return switch (detail) {
      case "Invalid name" -> "Tên sản phẩm phải có từ 1 đến 200 ký tự.";
      case "Invalid product" -> "Thiếu dữ liệu sản phẩm.";
      case "Invalid question" -> "Câu hỏi phải có từ 1 đến 1.000 ký tự.";
      case "Invalid description" -> "Mô tả sản phẩm phải có từ 1 đến 10.000 ký tự.";
      case "Invalid category", "Unknown category" -> "Chọn loại sản phẩm trong danh sách.";
      case "End must be after start" -> "Kết thúc phải sau thời gian bắt đầu.";
      case "Start time has passed" ->
          "Thời gian bắt đầu đã qua. Chọn lại hoặc dùng Bắt đầu ngay.";
      case "Auction duration exceeds 365 days" -> "Thời lượng đấu giá tối đa 365 ngày.";
      case "Invalid startNow" -> "Tùy chọn Bắt đầu ngay không hợp lệ.";
      case "Invalid start/end time (maximum duration 365 days)" ->
          "Thời gian bắt đầu đã qua, kết thúc không sau bắt đầu hoặc thời lượng vượt 365 ngày.";
      case "Invalid startTime" -> "Ngày hoặc giờ bắt đầu không hợp lệ.";
      case "Invalid endTime" -> "Ngày hoặc giờ kết thúc không hợp lệ.";
      case "Use ISO local time in Asia/Ho_Chi_Minh" ->
          "Thời gian phiên không hợp lệ. Hãy chọn lại ngày và giờ bắt đầu, kết thúc.";
      case "Expected integer: startingPrice", "Invalid money: startingPrice" ->
          "Giá khởi điểm phải là số nguyên dương, tối đa 9.000 tỷ đồng.";
      case "Expected integer: minBidStep", "Invalid money: minBidStep" ->
          "Bước giá phải là số nguyên dương, tối đa 9.000 tỷ đồng.";
      case "Expected integer: amount", "Invalid money: amount" ->
          "Số tiền giao dịch không hợp lệ (tối đa 9.000.000.000.000 ₫).";
      case "Minimum deposit is 10000 VND" -> "Số tiền nạp tối thiểu là 10.000 ₫.";
      case "Wallet limit exceeded" ->
          "Tổng số dư sau giao dịch vượt quá hạn mức tối đa 9.000.000.000.000 ₫.";
      case "Image must be smaller than 5 MB" -> "Ảnh sản phẩm phải nhỏ hơn 5 MB.";
      case "Invalid image encoding", "Unsupported image", "Invalid image" ->
          "Không đọc được ảnh sản phẩm. Hãy chọn ảnh PNG/JPG/GIF/BMP hợp lệ.";
      case "Image dimensions too large" -> "Ảnh sản phẩm quá lớn, tối đa 40 triệu điểm ảnh.";
      default -> "Thông tin chưa hợp lệ. Kiểm tra giá, bước giá, thời gian và ảnh đã nhập.";
    };
  }
}
