package com.lvt4j.im.actor;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

import com.lvt4j.im.msg.ChatMsg;
import com.lvt4j.im.websocket.UserWs;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.Duration;

/**
 * 用户Ws连接共生Actor
 * ，负责与WS交互
 * ，负责与用户注册Actor注册
 * @author LV
 */
@Slf4j
public class UserWsActor extends AbstractActor {

    private String userId;
    private UserWs ws;
    
    public static Props props(String customerId, UserWs ws) {
        return Props.create(UserWsActor.class, customerId, ws);
    }
    
    public UserWsActor(String customerId, UserWs ws) {
        this.userId = customerId;
        this.ws = ws;
    }
    
    @Override
    public void preStart() throws Exception {
        super.preStart();
        log.debug("用户[{}]WsActor建立[{}]", userId, self());
        UserRegActor.ShardingRef.tell(Pair.of(userId, UserRegActor.Reg), self());
        getContext().setReceiveTimeout(Duration.create(1, TimeUnit.SECONDS));
    }
    
    Receive initializing = receiveBuilder()
        .match(String.class, UserRegActor.RegAck::equals, this::regAck)
        .match(ReceiveTimeout.class, this::registerTimeout)
        .build();
    /** 注册成功 */
    private void regAck(String msg) {
        getContext().watch(getSender());
        ws.connSuc(getSelf());
        getContext().setReceiveTimeout(Duration.Undefined());
        getContext().become(working);
        log.debug("用户[{}]WsActor注册成功", userId);
    }
    private void registerTimeout(ReceiveTimeout timeout) {
        UserRegActor.ShardingRef.tell(Pair.of(userId, UserRegActor.Reg), self());
    }
    
    @Override
    public Receive createReceive() {
        return initializing;
    }
    
    Receive working = receiveBuilder()
        .match(ChatMsg.class, this::receive)
        .build();

    private void receive(ChatMsg chatMsg) {
        log.debug("用户[{}]WsActor收到聊天:{}", userId, chatMsg);
        ws.chatMsg(chatMsg);
    }
    
}
