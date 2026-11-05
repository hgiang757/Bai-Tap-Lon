package com.auction.client.network;

import com.auction.shared.network.NetworkResponse;

public interface ClientResponseDispatcher {
    void dispatch(NetworkResponse response);
}
