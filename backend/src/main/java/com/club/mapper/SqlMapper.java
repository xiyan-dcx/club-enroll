package com.club.mapper;
import org.apache.ibatis.annotations.*;
import java.util.*;
/** 仅执行服务端固定 SQL，所有值绑定参数；不接受客户端 SQL。 */
public interface SqlMapper {
 @SelectProvider(type=Provider.class,method="sql") List<Map<String,Object>> rows(@Param("sql")String sql,@Param("args")Object[] args);
 @UpdateProvider(type=Provider.class,method="sql") int update(@Param("sql")String sql,@Param("args")Object[] args);
 @InsertProvider(type=Provider.class,method="sql") @Options(useGeneratedKeys=true,keyProperty="id") int insert(Map<String,Object> command);
 class Provider {
  public static String sql(Map<String,Object> params) {
   StringBuilder out=new StringBuilder();int index=0;
   for(char ch:((String)params.get("sql")).toCharArray()) {if(ch=='?')out.append("#{args[").append(index++).append("]}");else out.append(ch);}return out.toString();
  }
 }
}
