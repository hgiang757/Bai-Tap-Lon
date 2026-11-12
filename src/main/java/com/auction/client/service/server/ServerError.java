package com.auction.client.service.server;

/** A definitive backend rejection, distinct from a transport failure with unknown outcome. */
public final class ServerError extends IllegalStateException {
    private final String code;

    public ServerError(String code, String detail) {
        super(message(code, detail));
        this.code = code;
    }

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
            case "BID_TOO_LOW" -> "Giá đặt phải từ giá hiện tại cộng bước giá tối thiểu trở lên.";
            case "AUCTION_NOT_STARTED" -> "Phiên đấu giá chưa bắt đầu.";
            case "AUCTION_CLOSED" -> "Phiên đã kết thúc hoặc bị hủy.";
            case "NOT_FOUND" -> "Không tìm thấy dữ liệu; hãy tải lại.";
            case "STORAGE_ERROR" -> "Máy chủ chưa lưu được thao tác. Vui lòng thử lại sau.";
            case "VALIDATION_ERROR" -> validationMessage(detail);
            case "CONFLICT" ->
                    "Dữ liệu bị trùng, đã thay đổi hoặc thao tác đang bị hạn chế. Hãy tải lại để"
                        + " kiểm tra.";
            case "AUTO_BID_LIMIT" ->
                    "Có quá nhiều lượt đấu tự động. Hãy tăng bước giá rồi thử lại.";
            case "SERVER_BUSY" -> "Máy chủ đang bận. Vui lòng thử lại sau.";
            default -> "Không thể thực hiện thao tác. Kiểm tra kết nối và phiên bản ứng dụng.";
        };
    }

    private static String validationMessage(String detail) {
        return switch (detail) {
            case "Invalid name" -> "Tên sản phẩm phải có từ 1 đến 200 ký tự.";
            case "Invalid description" -> "Mô tả sản phẩm phải có từ 1 đến 10.000 ký tự.";
            case "Invalid start/end time (maximum duration 365 days)" ->
                    "Thời gian bắt đầu đã qua, kết thúc không sau bắt đầu hoặc thời lượng vượt 365 ngày.";
            case "Use ISO local time in Asia/Ho_Chi_Minh", "Invalid startTime", "Invalid endTime" ->
                    "Thời gian phiên không hợp lệ. Hãy chọn lại ngày và giờ bắt đầu, kết thúc.";
            case "Image must be smaller than 5 MB" -> "Ảnh sản phẩm phải nhỏ hơn 5 MB.";
            case "Invalid image encoding", "Unsupported image", "Invalid image" ->
                    "Không đọc được ảnh sản phẩm. Hãy chọn ảnh PNG/JPG/GIF/BMP hợp lệ.";
            case "Image dimensions too large" -> "Ảnh sản phẩm quá lớn, tối đa 40 triệu điểm ảnh.";
            default -> "Thông tin chưa hợp lệ. Kiểm tra giá, bước giá, thời gian và ảnh đã nhập.";
        };
    }
}
