package com.club.util;
import com.club.mapper.SqlMapper;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DuplicateKeyException;
import java.util.*;
import java.sql.SQLException;
public final class Sql {
 public interface Work<T>{T run(SqlMapper mapper)throws SQLException;}
 public static <T>T transaction(Work<T> work){
  try{return Beans.get(TransactionTemplate.class).execute(s->{try{return work.run(Beans.get(SqlMapper.class));}catch(SQLException e){throw new IllegalStateException(e);}});}
  catch(DuplicateKeyException e){throw new IllegalArgumentException("该学生身份或记录已存在，请刷新后重试");}
 }
 public static int update(SqlMapper c,String sql,Object...args){return c.update(sql,args);}
 public static long insert(SqlMapper c,String sql,Object...args){Map<String,Object> p=new HashMap<>();p.put("sql",sql);p.put("args",args);c.insert(p);return ((Number)p.get("id")).longValue();}
 public static List<Map<String,Object>> rows(SqlMapper c,String sql,Object...args){var result=c.rows(sql,args);for(var row:result)row.replaceAll((k,v)->v instanceof java.sql.Timestamp t?t.toLocalDateTime().toString():v instanceof java.time.LocalDateTime t?t.toString():v);return result;}
 public static Map<String,Object> one(SqlMapper c,String sql,Object...args){var list=rows(c,sql,args);return list.isEmpty()?null:list.get(0);}
 public static long number(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Boolean b?(b?1:0):((Number)v).longValue();}
 public static int count(String sql,Object...args){return transaction(c->(int)number(one(c,sql,args),"n"));}
 public static <T>T entity(Map<String,Object> r,Class<T> type){
  if(r==null)return null;Map<String,Object> camel=new LinkedHashMap<>();
  r.forEach((key,value)->{StringBuilder b=new StringBuilder();boolean upper=false;for(char ch:key.toCharArray()){if(ch=='_')upper=true;else{b.append(upper?Character.toUpperCase(ch):ch);upper=false;}}camel.put(b.toString(),value);});
  return Beans.get(com.fasterxml.jackson.databind.ObjectMapper.class).convertValue(camel,type);
 }
}