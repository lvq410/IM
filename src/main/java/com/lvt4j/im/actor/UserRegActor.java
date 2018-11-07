package com.lvt4j.im.actor;

import java.util.HashSet;
import java.util.Set;

import com.lvt4j.im.msg.ChatMsg;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.cluster.sharding.ShardRegion.Passivate;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户注册Actor，持有所有本用户的WsActor引用，是集群分片的实体
 * @author LV
 */
@Slf4j
public class UserRegActor extends AbstractActor {

    public static final String ShardingType = "UserRegActor";
    
    /** 请求注册消息 */
    public static final String Reg = "Reg";
    /** 请求注册的回复消息 */
    public static final String RegAck = "RegAck";
    
    public static ActorRef ShardingRef;
    
    private String userId = self().path().name();
    private Set<ActorRef> userWsActorRefs = new HashSet<>();
    
    @Override
    public void preStart() throws Exception {
        super.preStart();
        log.debug("用户[{}]注册Actor启动:{}", userId, self());
    }
    
    @Override
    public void postStop() throws Exception {
        super.postStop();
        log.debug("用户[{}]注册Actor关闭:{}", userId, self());
    }
    
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(String.class, Reg::equals, this::reg)
            .match(ChatMsg.class, this::chatMsg)
            .match(Terminated.class, this::deregister)
            .build();
    }
    private void reg(String msg) {
        log.debug("用户[{}]注册Actor收到请求注册:{}", userId, sender());
        userWsActorRefs.add(sender());
        context().watch(sender());
        sender().tell(RegAck, self());
    }
    private void chatMsg(ChatMsg chatMsg) {
        log.debug("用户[{}]注册Actor收到聊天消息:{}", userId, chatMsg);
        userWsActorRefs.forEach(c->c.tell(chatMsg, self()));
    }
    private void deregister(Terminated terminated) {
        userWsActorRefs.remove(terminated.actor());
        log.debug("用户[{}]注册Actor取消注册:{}", userId, sender());
        if(!userWsActorRefs.isEmpty()) return;
        log.debug("用户[{}]注册Actor注册数量为空，开始关停", userId);
        context().parent().tell(new Passivate(PoisonPill.getInstance()), self());
    }
    
    
}
