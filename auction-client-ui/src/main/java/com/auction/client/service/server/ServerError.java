package com.auction.client.service.server;

/** A definitive backend rejection, distinct from a transport failure with unknown outcome. */
public final class ServerError extends IllegalStateException {
    private final String code;
    public ServerError(String code, String detail) { super(message(code, detail)); this.code = code; }
    public String code() { return code; }
    private static String message(String code, String detail) {
        return switch (code) {
            case "UNAUTHENTICATED" -> "Phiên đăng nhập đã hết hạn hoặc tài khoản bị khóa. Vui lòng đăng nhập lại.";
            case "AUTHENTICATION_FAILED" -> "Sai thông tin đăng nhập hoặc tài khoản bị khóa.";
            case "FORBIDDEN" -> "Tài khoản không có quyền thực hiện thao tác này.";
            case "INSUFFICIENT_FUNDS" -> "Số tiền có thể sử dụng không đủ.";
            case "BID_TOO_LOW" -> "Giá đặt phải từ giá hiện tại cộng bước giá tối thiểu trở lên.";
            case "AUCTION_NOT_STARTED" -> "Phiên đấu giá chưa bắt đầu.";
            case "AUCTION_CLOSED" -> "Phiên đã kết thúc hoặc bị hủy.";
            case "NOT_FOUND" -> "Không tìm thấy dữ liệu; hãy tải lại.";
            case "STORAGE_ERROR" -> "Máy chủ chưa lưu được thao tác. Vui lòng thử lại sau.";
            case "VALIDATION_ERROR" -> "Thông tin chưa hợp lệ. Kiểm tra giá, bước giá, thời gian và ảnh đã nhập.";
            case "CONFLICT" -> "Dữ liệu bị trùng, đã thay đổi hoặc thao tác đang bị hạn chế. Hãy tải lại để kiểm tra.";
            case "AUTO_BID_LIMIT" -> "Có quá nhiều lượt đấu tự động. Hãy tăng bước giá rồi thử lại.";
            case "BACKEND_NOT_CONFIGURED" -> "Thành viên backend chưa cung cấp lớp nối nghiệp vụ. Hiện có thể chạy UI bằng mock.";
            case "SERVER_BUSY" -> "Máy chủ đang bận. Vui lòng thử lại sau.";
            default -> "Không thể thực hiện thao tác. Kiểm tra kết nối và phiên bản ứng dụng.";
        };
    }
}
