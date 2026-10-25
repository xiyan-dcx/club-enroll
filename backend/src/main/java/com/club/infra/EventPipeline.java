package com.club.infra;
import com.club.mapper.SqlMapper;
import com.club.util.Sql;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Bean;
import org.apache.kafka.clients.admin.NewTopic;
import java.util.*;
import java.util.concurrent.TimeUnit;
@Component public class EventPipeline {
 private final KafkaTemplate<String,String> kafka;private final ObjectMapper json;private final HotCache cache;
 @org.springframework.beans.factory.annotation.Value("${qingju.namespace:qingju}") private String namespace;
 public EventPipeline(KafkaTemplate<String,String> k,ObjectMapper j,HotCache c){kafka=k;json=j;cache=c;}
 @Bean public NewTopic eventsTopic(){return new NewTopic(namespace+"-events",3,(short)1);}
 public static void add(SqlMapper c,String key,String type,Long user,Long activity,String message){
  Sql.update(c,"INSERT IGNORE INTO outbox_event(event_key,event_type,user_id,activity_id,payload) VALUES(?,?,?,?,?)",key,type,user,activity,message);
 }
 @Scheduled(fixedDelay=2000,initialDelay=10000) public void publish(){
  var events=Sql.transaction(c->Sql.rows(c,"SELECT * FROM outbox_event WHERE status='PENDING' AND next_attempt<=NOW() ORDER BY id LIMIT 30"));
  for(var e:events){long id=Sql.number(e,"id");try{
   kafka.send(namespace+"-events",Objects.toString(e.get("activity_id"),"global"),json.writeValueAsString(e)).get(5,TimeUnit.SECONDS);
   Sql.transaction(c->Sql.update(c,"UPDATE outbox_event SET status='SENT',sent_at=NOW(),last_error=NULL WHERE id=?",id));
  }catch(Exception ex){Sql.transaction(c->Sql.update(c,"UPDATE outbox_event SET attempts=attempts+1,next_attempt=DATE_ADD(NOW(),INTERVAL LEAST(300,POW(2,LEAST(attempts,8))) SECOND),last_error=? WHERE id=?",ex.getClass().getSimpleName(),id));break;}}
 }
 @KafkaListener(topics="${qingju.namespace:qingju}-events",groupId="${qingju.namespace:qingju}-business-v1") public void consume(String value)throws Exception{
  Map<String,Object> event=json.readValue(value,new com.fasterxml.jackson.core.type.TypeReference<>(){});long id=Sql.number(event,"id");
  // 缓存版本广播在数据库事务前；重投只产生多次失效，不会丢失失效事件。
  cache.invalidate();
  Sql.transaction(c->{
   if(Sql.update(c,"INSERT IGNORE INTO consumed_event(event_id) VALUES(?)",id)==0)return null;
   if(event.get("user_id")!=null)Sql.update(c,"INSERT IGNORE INTO inbox_notification(event_id,user_id,message) VALUES(?,?,?)",id,event.get("user_id"),event.get("payload"));
   return null;
  });
 }
}
