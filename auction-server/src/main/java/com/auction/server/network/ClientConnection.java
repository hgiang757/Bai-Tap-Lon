package com.auction.server.network;

import com.auction.shared.model.User;
import com.auction.shared.network.NetworkResponse;

public interface ClientConnection {
    User getCurrentUser();
    void setCurrentUser(User user);
    void refreshCurrentUser(String username);
    void send(NetworkResponse response);
}
