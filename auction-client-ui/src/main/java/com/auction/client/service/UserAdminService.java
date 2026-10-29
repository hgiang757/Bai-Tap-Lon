package com.auction.client.service;

import com.auction.client.model.AdminUser;
import com.auction.client.model.UserRole;
import java.util.List;

/** Hợp đồng quản trị người dùng. */
public interface UserAdminService {
    /** Lấy danh sách người dùng. */
    List<AdminUser> listUsers();
    /** Đổi trạng thái khóa. */
    void setLocked(String actor, String id, boolean locked);
    /** Đổi vai trò. */
    void setRole(String actor, String id, UserRole role);
    /** Xóa người dùng. */
    void deleteUser(String actor, String id);
}
