package com.auction.server.network;

import com.google.gson.*;

/** Framing-independent JSON envelope utilities; no domain entities. */
public final class Protocol {
    private Protocol() { }
    public static String text(JsonObject object, String field) {
        return !object.has(field) || object.get(field).isJsonNull() ? "" : object.get(field).getAsString();
    }
    public static long number(JsonObject object, String field) {
        try { return object.get(field).getAsBigDecimal().longValueExact(); }
        catch (RuntimeException e) { throw new NetworkException("PROTOCOL_ERROR", "Expected integer " + field); }
    }
    public static JsonObject obj(Object... pairs) {
        JsonObject object=new JsonObject();
        for(int i=0;i<pairs.length;i+=2){
            String key=pairs[i].toString();Object value=pairs[i+1];
            if(value==null)object.add(key,JsonNull.INSTANCE);
            else if(value instanceof JsonElement j)object.add(key,j);
            else if(value instanceof Number n)object.addProperty(key,n);
            else if(value instanceof Boolean b)object.addProperty(key,b);
            else object.addProperty(key,value.toString());
        }
        return object;
    }
}
