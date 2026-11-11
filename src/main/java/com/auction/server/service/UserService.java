package com.auction.server.service;

import com.auction.server.dao.AuctionRepository;
import com.auction.server.dao.UserRepository;
import com.auction.server.exception.ApiException;
import com.auction.server.model.*;
import java.util.List;
import java.util.UUID;

public class UserService {
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    public UserService(UserRepository userRepository, AuctionRepository auctionRepository) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
    }

    public User register(String username, String rawPassword, String email, UserRole role, double initialBalance) {
        if (username == null || username.isBlank()) {
            throw new ApiException(400, "INVALID_INPUT", "Username không được để trống");
        }
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new ApiException(400, "INVALID_INPUT", "Mật khẩu phải từ 4 ký tự trở lên");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ApiException(409, "DUPLICATE_USER", "Username đã tồn tại");
        }

        String id = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String passwordHash = Passwords.hash(rawPassword);
        UserRole assignedRole = (role != null) ? role : UserRole.BIDDER;

        User newUser = switch (assignedRole) {
            case ADMIN -> new Admin(id, username.trim(), passwordHash, email);
            case SELLER -> new Seller(id, username.trim(), passwordHash, email, initialBalance);
            case BIDDER -> new Bidder(id, username.trim(), passwordHash, email, initialBalance);
        };

        return userRepository.save(newUser);
    }

    public User login(String username, String rawPassword) {
        // TODO (TV4): Thay ApiException bằng AuthenticationException
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(401, "AUTH_FAILED", "Sai tên đăng nhập hoặc mật khẩu"));

        if (!user.isActive()) {
            throw new ApiException(403, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa bởi Admin");
        }
        if (!Passwords.verify(rawPassword, user.getPasswordHash())) {
            throw new ApiException(401, "AUTH_FAILED", "Sai tên đăng nhập hoặc mật khẩu");
        }
        return user;
    }

    public synchronized User deposit(String userId, double amount) {
        User user = getUserOrThrow(userId);
        user.deposit(amount);
        userRepository.save(user);
        auctionRepository.recordTransaction(
                new Transaction(userId, null, amount, "DEPOSIT", "Nạp tiền vào ví"));
        return user;
    }

    public synchronized User withdraw(String userId, double amount) {
        User user = getUserOrThrow(userId);
        if (user.getBalance() < amount) {
            throw new ApiException(400, "INSUFFICIENT_FUNDS", "Số dư không đủ để rút");
        }
        user.deduct(amount);
        userRepository.save(user);
        auctionRepository.recordTransaction(
                new Transaction(userId, null, amount, "WITHDRAW", "Rút tiền từ ví"));
        return user;
    }

    public User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "USER_NOT_FOUND", "Không tìm thấy người dùng: " + userId));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
