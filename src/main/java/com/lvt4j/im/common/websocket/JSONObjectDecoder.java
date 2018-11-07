package com.lvt4j.im.common.websocket;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import net.sf.json.JSONObject;

/**
 * @author LV
 */
public class JSONObjectDecoder implements Decoder.Text<JSONObject> {

    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public JSONObject decode(String s) throws DecodeException {
        return JSONObject.fromObject(s);
    }

    @Override
    public boolean willDecode(String s) {
        return s.startsWith("{");
    }

}
