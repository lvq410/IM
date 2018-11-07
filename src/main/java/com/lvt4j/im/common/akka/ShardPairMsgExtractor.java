package com.lvt4j.im.common.akka;

import org.apache.commons.lang3.tuple.Pair;

import akka.cluster.sharding.ShardRegion;
import akka.cluster.sharding.ShardRegion.MessageExtractor;
import lombok.AllArgsConstructor;

/**
 * 分片消息类型为Pair的消息的分片ID与实体ID提取器<br>
 * Pair对象的left即为实体ID，right为真正要传递的消息<br>
 * 分片用hash(实体ID)计算
 * @author LV
 */
@SuppressWarnings("rawtypes")
@AllArgsConstructor
public class ShardPairMsgExtractor implements MessageExtractor {
    
    /** 实体ID即是分片ID */
    public static final ShardPairMsgExtractor Origin = new ShardPairMsgExtractor(null);
    /** 总计8个分片 */
    public static final ShardPairMsgExtractor Eight = new ShardPairMsgExtractor(8);
    
    Integer shardNum;
    
    @Override
    public String shardId(Object obj) {
        if(shardNum==null) return entityId(obj);
        return String.valueOf(Math.abs(String.valueOf(entityId(obj)).hashCode())%shardNum);
    }
    
    @Override
    public Object entityMessage(Object obj) {
        if(!(obj instanceof Pair)) return obj;
        return ((Pair)obj).getValue();
    }
    
    @Override
    public String entityId(Object obj) {
        if(ShardRegion.StartEntity.class==obj.getClass())
            return ((ShardRegion.StartEntity)obj).entityId();
        if(!(obj instanceof Pair))return null;
        return (String) ((Pair)obj).getKey();
    }
}
