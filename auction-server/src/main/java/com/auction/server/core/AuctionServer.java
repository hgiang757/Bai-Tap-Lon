package com.auction.server.core;

/** Compatibility launcher: both entry points use the same JSON application and protocol. */
public final class AuctionServer {
    private AuctionServer() { }
    public static void main(String[] args) throws Exception {
        com.auction.server.network.Server.main(args);
    }
}
