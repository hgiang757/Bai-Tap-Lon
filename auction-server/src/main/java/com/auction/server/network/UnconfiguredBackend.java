package com.auction.server.network;

import com.google.gson.JsonObject;

/** Network health works without business services; no simulated business success. */
final class UnconfiguredBackend implements BackendGateway {
    @Override public JsonObject execute(String action,String token,String commandId,JsonObject payload) {
        if("PING".equals(action))return Protocol.obj("success",true,"result",Protocol.obj("status","OK","backendConfigured",false));
        return Protocol.obj("success",false,"errorCode","BACKEND_NOT_CONFIGURED",
                "message","Member 2 must register a BackendGateway provider.");
    }
    @Override public String authenticatedUserId(String token) {
        throw new NetworkException("BACKEND_NOT_CONFIGURED","No business gateway configured.");
    }
}
