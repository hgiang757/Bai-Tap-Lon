package com.auction.server.network;

import java.io.IOException;

/** Singleton lifecycle manager used by the executable server; tests use isolated servers. */
public enum ServerRuntime {
    INSTANCE;
    private Server server;
    public synchronized Server start(String host,int port,BackendGateway gateway) throws IOException {
        if(server!=null)throw new IllegalStateException("Server already started");
        Server started=new Server(host,port,gateway);
        try { started.start();server=started;return started; }
        catch(RuntimeException e){started.close();throw e;}
    }
    public synchronized void stop() throws IOException {
        if(server!=null)try{server.close();}finally{server=null;}
    }
}
