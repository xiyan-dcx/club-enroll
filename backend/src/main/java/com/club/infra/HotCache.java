package com.club.infra;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;
import com.club.config.ApiErrors.Busy;
@Component public class HotCache {
 private final StringRedisTemplate redis;private final ObjectMapper json;
 @org.springframework.beans.factory.annotation.Value("${qingju.cache-enabled:true}") private boolean enabled;
 private final java.util.concurrent.atomic.LongAdder requests=new java.util.concurrent.atomic.LongAdder(),databaseLoads=new java.util.concurrent.atomic.LongAdder();
 public java.util.Map<String,Object> metrics(){return java.util.Map.of("enabled",enabled,"requests",requests.sum(),"databaseLoads",databaseLoads.sum());}
 @org.springframework.beans.factory.annotation.Value("${qingju.namespace:qingju}") private String namespace;
 private final Cache<String,Object> local=Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofSeconds(3)).build();
 private final Semaphore loads=new Semaphore(5);
 public HotCache(StringRedisTemplate r,ObjectMapper j){redis=r;json=j;}
 public Object get(String key,Supplier<Object> loader){
  requests.increment();
  if(!enabled)return load(loader);
  String version;
  try{version=redis.opsForValue().get(namespace+":cache-version");}catch(Exception e){return load(loader);}
  String full=namespace+":hot:"+version+":"+key;
  return local.get(full,k->{
   try{String cached=redis.opsForValue().get(k);if(cached!=null)return json.readValue(cached,Object.class);}catch(Exception ignored){}
   Object value=load(loader);
   try{redis.opsForValue().set(k,json.writeValueAsString(value),Duration.ofSeconds(ThreadLocalRandom.current().nextInt(20,41)));}catch(Exception ignored){}
   return value;
  });
 }
 private Object load(Supplier<Object> loader){if(!loads.tryAcquire())throw new Busy(503,"查询繁忙，请稍后重试");try{databaseLoads.increment();return loader.get();}finally{loads.release();}}
 public void invalidate(){redis.opsForValue().increment(namespace+":cache-version");local.invalidateAll();}
}
