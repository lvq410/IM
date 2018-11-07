package com.lvt4j.im.common.websocket;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import lombok.SneakyThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvt4j.im.Global;
import com.lvt4j.im.common.JsonResult;

/**
 *
 * @author LV
 */
public class JsonResultEncoder implements Encoder.Text<JsonResult> {

    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    @SneakyThrows
    public String encode(JsonResult rst) throws EncodeException {
        return Global.SpringCtx.getBean(ObjectMapper.class).writeValueAsString(rst);
    }
    
}
