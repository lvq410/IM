package com.lvt4j.im.common.akka;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.ReceiveTimeout;
import akka.cluster.Cluster;
import lombok.Data;
import scala.concurrent.duration.Duration;

/**
 *
 * @author LV
 */
public class ClusterQueryActor extends AbstractActor {
    
    Cluster cluster = Cluster.get(getContext().getSystem());
    
    Set<Address> leftAddrs = new HashSet<>();
    Map<Address, Object> rsts = new HashMap<>();
    ActorRef asker;
    
    @Override
    public void preStart() throws Exception {
        super.preStart();
        getContext().setReceiveTimeout(Duration.create(2, TimeUnit.SECONDS));
    }
    
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ClusterQuery.class, this::query)
            .match(ReceiveTimeout.class, this::timeout)
            .matchAny(this::rst)
            .build();
    }
    private void query(ClusterQuery msg) {
        cluster.state().getMembers().forEach(m->{
            leftAddrs.add(m.address());
        });
        leftAddrs.forEach(a->getContext().actorSelection(a+msg.actorPath).tell(msg.msg, getSelf()));
        asker = getSender();
    }
    private void rst(Object msg) {
        Address addr = cluster.remotePathOf(getSender()).address();
        leftAddrs.remove(addr);
        rsts.put(addr, msg);
        if(!leftAddrs.isEmpty()) return;
        timeout(null);
    }
    private void timeout(ReceiveTimeout msg) {
        asker.tell(rsts, getSelf());
        getContext().stop(getSelf());
    }
    
    @Data
    public static class ClusterQuery implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String actorPath;
        public final Object msg;
    }
    
}
