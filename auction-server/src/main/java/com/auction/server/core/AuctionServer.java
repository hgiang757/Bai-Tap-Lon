package com.auction.server.core;
/** Both launchers use the same transport and externally supplied business gateway. */
public final class AuctionServer {
    private AuctionServer() { }
    public static void main(String[] args) throws Exception { com.auction.server.network.Server.main(args); }
}
