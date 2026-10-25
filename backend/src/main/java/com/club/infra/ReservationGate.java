package com.club.infra;
import com.club.util.*;
import com.club.service.StudentIdentityService;
import com.club.config.ApiErrors.Busy;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.*;
import java.util.function.Function;

/** Redis 原子预占 + MySQL 持久化请求终态。超时恢复与业务确认锁同一请求行。 */
@Component public class ReservationGate {
 private final StringRedisTemplate redis;
 @org.springframework.beans.factory.annotation.Value("${qingju.namespace:qingju}") private String namespace;
 private final DefaultRedisScript<Long> acquire=new DefaultRedisScript<>("if redis.call('HEXISTS',KEYS[2],ARGV[1])==1 then return 1 end; if redis.call('EXISTS',KEYS[1])==0 then redis.call('SET',KEYS[1],math.max(0,tonumber(ARGV[2])-redis.call('HLEN',KEYS[2]))) end; if tonumber(redis.call('GET',KEYS[1]))<=0 then return 0 end; redis.call('DECR',KEYS[1]); redis.call('HSET',KEYS[2],ARGV[1],1); return 1",Long.class);
 private final DefaultRedisScript<Long> release=new DefaultRedisScript<>("local n=redis.call('HDEL',KEYS[2],ARGV[1]); redis.call('DEL',KEYS[1]); return n",Long.class);
 public ReservationGate(StringRedisTemplate redis){this.redis=redis;}
 private List<String> keys(long activity){String tag="{visit:"+activity+"}";return List.of(namespace+":"+tag+":remaining",namespace+":"+tag+":holds");}
 public Map<String,Object> execute(long user,long activity,String requestId,String studentNo,Function<String,Map<String,Object>> work){
  // 幂等重试先读历史结果；已占位用户的新请求也不得因库存为零被误拒绝。
  Map<String,Object> prior=Sql.transaction(c->{
   var r=Sql.one(c,"SELECT v.id,v.activity_id,v.status,v.version FROM visit_request q JOIN visit_reservation v ON v.id=q.reservation_id WHERE q.user_id=? AND q.request_id=?",user,requestId);
   if(r!=null && Sql.number(r,"activity_id")!=activity)throw new IllegalArgumentException("请求标识不可用于其他活动");
   if(r==null)r=Sql.one(c,"SELECT id,activity_id,status,version FROM visit_reservation WHERE activity_id=? AND student_no=? AND user_id=? AND status='CONFIRMED'",activity,studentNo,user);
   return r;
  });
  if(prior!=null)return prior;
  String token=UUID.randomUUID().toString();
  long remaining=Sql.transaction(c->{
   var a=Sql.one(c,"SELECT capacity,booked_count FROM visit_activity WHERE id=?",activity);
   if(a==null)throw new IllegalArgumentException("活动不存在");
   Sql.update(c,"INSERT INTO reservation_attempt(token,user_id,activity_id,expires_at) VALUES(?,?,?,DATE_ADD(NOW(),INTERVAL 30 SECOND))",token,user,activity);
   return Sql.number(a,"capacity")-Sql.number(a,"booked_count");
  });
  try{
   Long result;
   try{result=redis.execute(acquire,keys(activity),token,""+remaining);}catch(Exception e){throw new Busy(503,"预约服务暂不可用，请稍后重试");}
   if(!Long.valueOf(1).equals(result))throw new IllegalArgumentException("本场名额已满或正在处理中，请稍后查询");
   return work.apply(token);
  }finally{
   // 业务事务若已提交则保留 CONFIRMED；未知/失败请求置为取消，迟到执行者不能再确认。
   Sql.transaction(c->Sql.update(c,"UPDATE reservation_attempt SET state='CANCELLED' WHERE token=? AND state='PENDING'",token));
   clean(activity,token);
  }
 }
 private void clean(long activity,String token){
  try{redis.execute(release,keys(activity),token);Sql.transaction(c->Sql.update(c,"UPDATE reservation_attempt SET state='CLEANED' WHERE token=? AND state<>'PENDING'",token));}
  catch(Exception ignored){/* 定时恢复会重试，不能删除持久化证据。 */}
 }
 @Scheduled(fixedDelay=5000,initialDelay=15000) public void recover(){
  var rows=Sql.transaction(c->Sql.rows(c,"SELECT token,activity_id FROM reservation_attempt WHERE state IN ('CONFIRMED','CANCELLED') OR (state='PENDING' AND expires_at<NOW()) LIMIT 100"));
  for(var r:rows){String token=r.get("token").toString();
   Sql.transaction(c->Sql.update(c,"UPDATE reservation_attempt SET state='CANCELLED' WHERE token=? AND state='PENDING' AND expires_at<NOW()",token));
   var state=Sql.transaction(c->Sql.one(c,"SELECT state FROM reservation_attempt WHERE token=?",token));
   if(!"PENDING".equals(state.get("state")))clean(Sql.number(r,"activity_id"),token);
  }
  // Redis AOF 恢复可能重新出现已清理的旧预占，反向核对数据库终态。
  try(var cursor=redis.scan(org.springframework.data.redis.core.ScanOptions.scanOptions().match(namespace+":{visit:*}:holds").count(100).build())){
   int checked=0;
   while(cursor.hasNext() && checked<100){String key=cursor.next();
    var tokens=redis.opsForHash().keys(key);
    long activity=Long.parseLong(key.substring(key.indexOf("{visit:")+7,key.indexOf('}')));
    for(Object raw:tokens){if(checked++>=100)break;String token=raw.toString();
     var state=Sql.transaction(c->Sql.one(c,"SELECT state FROM reservation_attempt WHERE token=?",token));
     if(state==null || !"PENDING".equals(state.get("state")))clean(activity,token);
    }
   }
  }catch(org.springframework.data.redis.RedisConnectionFailureException ignored){/* Redis 恢复后继续对账。 */}
 }
 public void invalidate(long activity){try{redis.delete(keys(activity).get(0));}catch(Exception ignored){}}
}
