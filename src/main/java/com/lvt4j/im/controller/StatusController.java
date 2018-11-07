package com.lvt4j.im.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lvt4j.im.actor.UserWsActorSupervisor;
import com.lvt4j.im.common.akka.AkkaService;

import akka.actor.Address;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.Member;

/**
 *
 * @author LV
 */
@RestController
public class StatusController {

    @Autowired
    AkkaService akkaService;
    
    @RequestMapping("status.json")
    public Map<String, ?> status() throws Exception {
        Map<String, Map<String, Object>> clusterStatus = MapUtils.lazyMap(new HashMap<>(), k->new HashMap<>());
        CurrentClusterState state = akkaService.getCluster().state();
        Address leader = state.getLeader();
        Set<Member> unreachables = state.getUnreachable();
        state.getMembers().forEach(m->{
            String status = m.status().toString();
            if(unreachables.contains(m)) status = "Unreachable";
            if(m.address().equals(leader)) status = "Leader";
            String node = AkkaService.addr2HostPort(m.address());
            clusterStatus.get(node).put("status", status);
        });
        
        Map<Address, Pair<Map<String, Integer>, Set<String>>> statusRawMap = akkaService.clusterQuery(UserWsActorSupervisor.Name, "status");
        statusRawMap.forEach((a, p)->{
            String node = AkkaService.addr2HostPort(a);
            clusterStatus.get(node).put("userWss", p.getKey());
            clusterStatus.get(node).put("userRegs", p.getValue());
        });
        return clusterStatus;
    }
    
}
