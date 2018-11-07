package com.lvt4j.im.common.websocket;

import static com.lvt4j.im.SpringCoreConfig.scanAssignableClss;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tomcat.websocket.server.WsHandshakeRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import com.lvt4j.im.Global;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket请求配置，主要用于获取登陆ID、客户端IP
 * @author LV
 */
@Slf4j
@Service
public class WebSocketConfig extends Configurator implements ServletContextAware {

    private static final Field wsRequestField = field(WsHandshakeRequest.class, "request");
    static{
        wsRequestField.setAccessible(true);
    }
    
    @Override
    @SneakyThrows
    public void setServletContext(ServletContext servletContext) {
        ServerContainer serverContainer = (ServerContainer) servletContext
            .getAttribute("javax.websocket.server.ServerContainer");
        List<Class<? extends Decoder>> decoders = Arrays.asList(JSONObjectDecoder.class);
        List<Class<? extends Encoder>> encoders = Arrays.asList(JsonResultEncoder.class);
        for(Class<? extends AbstractWebSocket> wsCls : scanAssignableClss(AbstractWebSocket.class)){
            WebSocketMapping mapping = wsCls.getAnnotation(WebSocketMapping.class);
            Validate.notNull(mapping, "WebSocket类[%s]缺少@WebSocketMapping注解", wsCls);
            ServerEndpointConfig config = ServerEndpointConfig.Builder.create(wsCls, mapping.value())
                    .decoders(decoders).encoders(encoders).configurator(this).build();
            serverContainer.addEndpoint(config);
            log.debug("WebSocket注册 {} at {}", mapping.value(), wsCls);
        }
    }
    
    @Override
    @SneakyThrows
    public void modifyHandshake(ServerEndpointConfig sec,
            HandshakeRequest request, HandshakeResponse response) {
        super.modifyHandshake(sec, request, response);
        HttpServletRequest req = (HttpServletRequest) wsRequestField.get(request);
        sec.getUserProperties().put("IP", parseIPFromReq(req));
    }
    private String parseIPFromReq(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
                if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
            }
        }
        return ip;
    }
    
    @Override
    public <T> T getEndpointInstance(Class<T> clazz)
            throws InstantiationException {
        return  Global.SpringCtx.getAutowireCapableBeanFactory().createBean(clazz);
    }
    
    private static final Field field(Class<?> cls, String fieldName) {
        Field field = null;
        while (cls != null) {
            try {
                field = cls.getDeclaredField(fieldName);
                return field;
            } catch (NoSuchFieldException e) {}
            cls = cls==Object.class ? null : cls.getSuperclass();
        }
        return null;
    }
    
}
