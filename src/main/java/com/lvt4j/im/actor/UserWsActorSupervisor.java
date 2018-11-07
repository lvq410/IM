package com.lvt4j.im.actor;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.lvt4j.im.websocket.UserWs;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.cluster.sharding.ShardRegion;
import akka.cluster.sharding.ShardRegion.ShardState;
import akka.japi.pf.DeciderBuilder;
import akka.pattern.PatternsCS;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

/**
 * 用户Ws连接共生Actor的监督Actor
 * @author LV
 */
@Slf4j
public class UserWsActorSupervisor extends AbstractActor {

    public static final String Name = "UserWsActorSupervisor";
    public static ActorRef Ref;
    
    /** 同一个用户可能在同一个机器上有多个Ws连接 */
    private Map<String, List<ActorRef>> userWsActorMap = MapUtils.lazyMap(new HashMap<>(), k->new LinkedList<>());
    private Map<ActorRef, String> userWsActor2UserId = new HashMap<>();
    
    
    @Override
    public void preStart() throws Exception {
        super.preStart();
        DistributedPubSub.get(getContext().getSystem()).mediator()
            .tell(new DistributedPubSubMediator.Put(getSelf()), getSelf());
        log.debug("用户WsActor监督启动:{}", self());
    }
    
    @Override
    public void postStop() throws Exception {
        super.postStop();
        log.debug("用户WsActor监督关闭:{}", self());
    }
    
    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(3, FiniteDuration.create(10, TimeUnit.SECONDS), DeciderBuilder
            .matchAny(c->SupervisorStrategy.stop())
            .build());
    }
    
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Connect.class, this::connect)
            .match(Terminated.class, this::terminate)
            .match(String.class, "status"::equals, this::status)
            .build();
    }
    private void connect(Connect connect) {
        log.debug("用户连接监督创建连接:{}", connect.userId);
        ActorRef userWsActorRef = context().actorOf(UserWsActor.props(connect.userId, connect.ws));
        context().watch(userWsActorRef);
        userWsActorMap.get(connect.userId).add(userWsActorRef);
        userWsActor2UserId.put(userWsActorRef, connect.userId);
        sender().tell(ConnectAck.of(userWsActorRef), self());
    }
    private void terminate(Terminated terminated) {
        String userId = userWsActor2UserId.get(terminated.actor());
        if(StringUtils.isEmpty(userId)) return;
        List<ActorRef> userWsActorRefs = userWsActorMap.get(userId);
        userWsActorRefs.remove(terminated.actor());
        if(!userWsActorRefs.isEmpty()) return;
        userWsActorMap.remove(userId);
        log.debug("用户连接监督关闭连接:{}", userId);
    }
    private void status(String msg) throws Exception {
        Map<String, Integer> connNum = userWsActorMap.keySet().stream().collect(toMap(k->k, k->userWsActorMap.get(k).size()));
        ShardRegion.CurrentShardRegionState  shardState = (ShardRegion.CurrentShardRegionState) PatternsCS.ask(UserRegActor.ShardingRef, ShardRegion.getShardRegionStateInstance(), 1000)
            .toCompletableFuture().get();
        Set<String> userRegs = shardState.getShards().stream().map(ShardState::getEntityIds).flatMap(s->s.stream()).collect(toSet());
        getSender().tell(Pair.of(connNum, userRegs), getSelf());
    }

    @Data
    @AllArgsConstructor(staticName="of")
    public static class Connect implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String userId;
        public final UserWs ws;
    }
    @Data
    @AllArgsConstructor(staticName="of")
    public static class ConnectAck implements Serializable {
        private static final long serialVersionUID = 1L;
        public final ActorRef userWsActorRef;
    }
    
}
