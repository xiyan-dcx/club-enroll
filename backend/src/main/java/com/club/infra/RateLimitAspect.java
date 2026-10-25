package com.club.infra;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.*;
import com.club.entity.User;
import com.club.config.ApiErrors.Busy;
import java.util.*;
@Aspect @Component
public class RateLimitAspect {
 private final StringRedisTemplate redis;
 @Value("${qingju.namespace:qingju}") private String namespace;
 @Value("${qingju.rate-limit:60}") private int limit;
 private final DefaultRedisScript<Long> script=new DefaultRedisScript<>("local t=redis.call('TIME'); local n=t[1]*1000+math.floor(t[2]/1000); redis.call('ZREMRANGEBYSCORE',KEYS[1],0,n-10000); if redis.call('ZCARD',KEYS[1])>=tonumber(ARGV[1]) then return 0 end; redis.call('ZADD',KEYS[1],n,ARGV[2]); redis.call('PEXPIRE',KEYS[1],11000); return 1",Long.class);
 public RateLimitAspect(StringRedisTemplate redis){this.redis=redis;}
 @Around("@annotation(com.club.infra.RateLimit)") public Object check(ProceedingJoinPoint point)throws Throwable{
  var req=((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
  User u=(User)req.getSession(false).getAttribute("user");
  Long accepted;
  try{accepted=redis.execute(script,List.of(namespace+":rate:"+u.getId()+":"+req.getRequestURI()),""+limit,UUID.randomUUID().toString());}
  catch(Exception e){throw new Busy(503,"流量保护服务暂不可用，请稍后重试");}
  if(!Long.valueOf(1).equals(accepted))throw new Busy(429,"操作过于频繁，请稍后重试");
  return point.proceed();
 }
}
