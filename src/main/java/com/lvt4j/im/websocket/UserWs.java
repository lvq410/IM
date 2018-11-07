package com.lvt4j.im.websocket;

import javax.websocket.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.lvt4j.im.actor.UserRegActor;
import com.lvt4j.im.actor.UserWsActorSupervisor;
import com.lvt4j.im.actor.UserWsActorSupervisor.Connect;
import com.lvt4j.im.common.JsonResult;
import com.lvt4j.im.common.websocket.AbstractWebSocket;
import com.lvt4j.im.common.websocket.WebSocketMapping;
import com.lvt4j.im.msg.ChatMsg;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;

/**
 * 用户ws连接
 * @author LV
 */
@WebSocketMapping("/user/ws")
@Slf4j
public class UserWs extends AbstractWebSocket {

    String userId;
    Session session;
    
    ActorRef userWsActorRef;
    
    @Override
    public void postOpen() throws Exception {
        userId = param("userId");
        UserWsActorSupervisor.Ref.tell(Connect.of(userId, this), null);
        log.debug("用户[{}]WS连接建立于IP[{}],session[{}]", userId, IP);
    }
    
    public void connSuc(ActorRef userWsActorRef) {
        this.userWsActorRef = userWsActorRef;
        log.debug("用户[{}]WS连接注册成功", userId);
        send(JsonResult.success().dataPut("type", "connSuc"));
    }
    
    public void chatMsg(ChatMsg chatMsg) {
        log.debug("用户[{}]WS连接收到聊天:{}", userId, chatMsg);
        send(JsonResult.success().dataPut("type", "msg").dataPut("msg", chatMsg));
    }
    
    @Override
    public void onMessage(JSONObject msg) throws Exception {
        String to = msg.optString("to");
        String content = msg.optString("content");
        chatTo(to, content);
    }
    private void chatTo(String to, String content) {
        if(StringUtils.isEmpty(to)) return;
        if(StringUtils.isEmpty(content)) return;
        log.debug("用户[{}]WS连接发送聊天至[{}]:{}", userId, to, content);
        ChatMsg msg = ChatMsg.of(userId, to, content);
        UserRegActor.ShardingRef.tell(Pair.of(userId, msg), null);
        if(userId.equals(to)) return;
        UserRegActor.ShardingRef.tell(Pair.of(to, msg), null);
    }
    
    @Override
    public void preClose() throws Exception {
        log.debug("用户[{}]WS连接关闭", userId);
        if(userWsActorRef==null) return;
        userWsActorRef.tell(PoisonPill.getInstance(), null);
    }
    
}
