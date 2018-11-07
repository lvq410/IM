package com.lvt4j.im.common.websocket;

import java.io.IOException;
import java.util.List;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.collections4.CollectionUtils;

import com.lvt4j.im.Global;
import com.lvt4j.im.common.JsonResult;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;

/**
 * 抽象WebSocket<br>
 * WebSocket业务类必须继承本类，并用{@link WebSocketMapping @WebSocketMapping}标注WebSocket的uri<br>
 * 每一个WebSocket都会是一个动态生成的spring的bean，因此可以用@Autowire的方式注入springbean<br>
 * 销毁可以重写preClose()，也可以定义@PreDestory，但注意不要重复
 * @author LV
 */
@Slf4j
public abstract class AbstractWebSocket {

    protected String uri;
    
    protected Session session;
    @Getter
    protected String IP;
    
    @OnOpen
    public final void onOpen(Session session) throws Exception {
        this.session = session;
        this.uri = session.getRequestURI().toString();
        this.IP = (String) session.getUserProperties().get("IP");
        postOpen();
    }
    /** 重写本方法处理websocket连接建立后的初始化 */
    public void postOpen() throws Exception {};
    /** 本方法用于提取请求参数 */
    protected final String param(String key) {
        List<String> values = session.getRequestParameterMap().get(key);
        if(CollectionUtils.isEmpty(values)) return null;
        return values.get(0);
    }
    
    @OnMessage
    public final void _onMessage(JSONObject msg) throws Exception {
        onMessage(msg);
    }
    /** 重写本方法处理收消息 */
    public void onMessage(JSONObject msg) throws Exception {}
    
    /** 本方法用于发消息 */
    protected final void send(JsonResult msg) {
        if(!session.isOpen()) return;
        try{
            session.getBasicRemote().sendObject(msg);
        }catch(Exception e){
            log.error("WebSocket[{}]发信失败", uri, e);
        }
    }
    
    /** 关闭连接 */
    public final void close() {
        if(!session.isOpen()) return;
        try{
            session.close();
        }catch(Exception e){
            log.error("WebSocket[{}]关闭失败", uri, e);
        }
    }
    
    @OnError
    public final void onError(Session session, Throwable e) throws IOException {
        if(e!=null && !(e instanceof IOException))
            log.error("WebSocket[{}]异常", uri, e);
        session.close();
    }
    
    @OnClose
    public final void onClose() throws Exception {
        log.trace("WebSocket[{}]关闭", uri);
        preClose();
        Global.SpringCtx.getAutowireCapableBeanFactory().destroyBean(this);
    }
    public void preClose() throws Exception {}
    
}
