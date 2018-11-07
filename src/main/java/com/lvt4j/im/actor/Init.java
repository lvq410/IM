package com.lvt4j.im.actor;

import com.lvt4j.im.common.akka.AkkaService.AkkaClusterInit;
import com.lvt4j.im.common.akka.ShardPairMsgExtractor;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;

/**
 * 
 * @author LV
 */
public class Init implements AkkaClusterInit {

    @Override
    public void init(ActorSystem sys, Cluster cluster) {
        UserWsActorSupervisor.Ref = sys.actorOf(Props.create(UserWsActorSupervisor.class), UserWsActorSupervisor.Name);
        ClusterShardingSettings userRegShardingSettings = ClusterShardingSettings.create(sys);
        Props customerRegisterShardingEntityProps = Props.create(UserRegActor.class);
        UserRegActor.ShardingRef = ClusterSharding.get(sys).start(UserRegActor.ShardingType,
            customerRegisterShardingEntityProps, userRegShardingSettings,
            ShardPairMsgExtractor.Eight);
        
    }

}
