package com.lvt4j.im.common.akka;

import static com.lvt4j.im.Consts.AppName;
import static com.lvt4j.im.SpringCoreConfig.scanAssignables;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import com.google.common.net.HostAndPort;
import com.lvt4j.im.Config;
import com.lvt4j.im.common.akka.ClusterQueryActor.ClusterQuery;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.ddata.DistributedData;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.pattern.PatternsCS;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author LV
 */
@Slf4j
@Service
public class AkkaService {

    private static final String Protocol = "akka.tcp";
    
    ActorSystem actorSystem;
    Cluster cluster;
    
    ActorRef pubsubMediator;
    ActorRef replicator;
    
    @SneakyThrows
    @PostConstruct
    public void startCluster() {
        List<HostAndPort> seeds = Stream.of(Config.Nodes()).map(HostAndPort::fromString).collect(toList());
        log.info("开始加入集群,种子节点:{}", seeds);
        Map<String, Object> configVals = new HashMap<>();
        configVals.put("akka.loggers", asList("akka.event.slf4j.Slf4jLogger"));
        configVals.put("akka.loglevel", "INFO");
        configVals.put("akka.coordinated-shutdown.exit-jvm", "on");
        configVals.put("akka.actor.provider", "cluster");
        configVals.put("akka.cluster.seed-node-timeout", "20s");
        configVals.put("akka.cluster.retry-unsuccessful-join-after", "10s");
        configVals.put("akka.cluster.shutdown-after-unsuccessful-join-seed-nodes", "30s");
        configVals.put("akka.remote.netty.tcp.hostname", Config.host);
        configVals.put("akka.remote.netty.tcp.port", Config.port);
        configVals.put("akka.cluster.min-nr-of-members", Config.Quorum());
        if(isNotEmpty(Config.bindHost)) configVals.put("akka.remote.netty.tcp.bind-hostname", Config.bindHost);
        if(Config.bindPort!=null) configVals.put("akka.remote.netty.tcp.bind-port", Config.bindPort) ;
        
        CountDownLatch clusterUpWaitLatch = new CountDownLatch(1);
        actorSystem = ActorSystem.create(AppName, ConfigFactory.parseMap(configVals));
        
        cluster = Cluster.get(actorSystem);
        cluster.registerOnMemberUp(()->{
            clusterUpWaitLatch.countDown();
            initOnUp();
        });
        actorSystem.registerOnTermination(clusterUpWaitLatch::countDown);
        List<Address> addrs = seeds.stream().map(hp->Address.apply(Protocol, AppName, hp.getHostText() , hp.getPort())).collect(toList());
        cluster.joinSeedNodes(addrs);
        clusterUpWaitLatch.await();
        if(cluster.isTerminated()) {
            log.error("集群加入失败！");
            throw new Error("集群加入失败！");
        }
        log.info("集群加入成功");
    }
    private void initOnUp() {
        pubsubMediator = DistributedPubSub.get(actorSystem).mediator();
        replicator = DistributedData.get(actorSystem).replicator();
        scanAssignables(AkkaClusterInit.class).forEach(i->i.init(actorSystem, cluster));
    }
    
    @PreDestroy
    public void destory() {
        if(cluster.isTerminated()) return;
        gracefullyLeave();
    }
    @SneakyThrows
    private void gracefullyLeave() {
        log.info("开始离开Actor集群");
        CountDownLatch clusterLeaveLatch = new CountDownLatch(1);
        cluster.leave(cluster.selfAddress());
        actorSystem.registerOnTermination(clusterLeaveLatch::countDown);
        clusterLeaveLatch.await();
        log.info("已离开Actor集群");
    }
    @SneakyThrows
    private void forceDown() {
        log.info("强制关闭本节点");
        CountDownLatch clusterDownLatch = new CountDownLatch(1);
        cluster.down(cluster.selfAddress());
        actorSystem.registerOnTermination(clusterDownLatch::countDown);
        clusterDownLatch.await();
        log.info("已强制关闭本节点");
    }
    
    public ActorSystem getActorSystem() {
        return actorSystem;
    }
    public Cluster getCluster() {
        return cluster;
    }
    
    public ActorRef getReplicator() {
        return replicator;
    }
    
    public Set<Address> getUpMemberAddrs() {
        return getUpMembers().stream().map(Member::address).collect(toSet());
    }
    public Set<Member> getUpMembers() {
        Set<Member> members = new HashSet<>();
        cluster.state().getMembers().forEach(m->{
            if(!MemberStatus.up().equals(m.status())) return;
            members.add(m);
        });
        members.removeAll(cluster.state().getUnreachable());
        return members;
    }
    
    public void sub(ActorRef actorRef) {
        pubsubMediator.tell(new DistributedPubSubMediator.Put(actorRef), actorRef);
    }
    
    public void pubAll(DistributedPubSubMediator.SendToAll send, ActorRef actorRef) {
        pubsubMediator.tell(send, actorRef);
    }
    
    /**
     * 对集群中每个路径为'/user/'+path的Actor发送msg请求并等待数据返回<br>
     * 等所有节点数据返回后(或超时10s)组合成一个key为Address的Map返回
     * @param path
     * @param msg
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public <T> Map<Address, T> clusterQuery(String path, Object msg) throws Exception {
        ActorRef queryActor = actorSystem.actorOf(Props.create(ClusterQueryActor.class));
        return (Map<Address, T>) PatternsCS.ask(queryActor, new ClusterQuery("/user/"+path, msg), 10000).toCompletableFuture().get();
    }
    
    /** 下线集群成员 */
    public void memberDown(String node) {
        cluster.down(addressOf(node));
    }
    
    public static Address addressOf(String node) {
        HostAndPort hostAndPort = HostAndPort.fromString(node);
        return Address.apply(Protocol, AppName, hostAndPort.getHostText(), hostAndPort.getPort());
    }
    public static String addr2HostPort(Address addr) {
        return addr.host().get()+":"+addr.port().get();
    }
    
    /** akka集群成功加入集群后初始化动作，用该接口标记的类，在akka集群加入成功后都会执行 */
    public interface AkkaClusterInit {
        public void init(ActorSystem sys, Cluster cluster);
    }
    
}
