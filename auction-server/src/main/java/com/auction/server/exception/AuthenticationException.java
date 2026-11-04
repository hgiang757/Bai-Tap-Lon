package com.auction.server.exception;

/**
 * Ném ra khi đăng nhập hoặc đăng ký thất bại (sai tên đăng nhập hoặc mật khẩu, tài khoản không tồn tại, không đủ quyền...).
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
