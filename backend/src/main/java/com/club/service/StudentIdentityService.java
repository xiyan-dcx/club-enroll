package com.club.service;
import java.util.Map;
/** 提交信息格式校验；真实性由负责人在业务名单中核对。 */
public final class StudentIdentityService {
 public static Map<String,Object> submitted(String studentNo,String name){
  if(studentNo==null || !studentNo.trim().matches("[A-Za-z0-9]{1,32}"))throw new IllegalArgumentException("学号须为 1—32 位字母或数字");
  if(name==null || name.trim().isEmpty() || name.trim().length()>32)throw new IllegalArgumentException("姓名须为 1—32 字");
  return Map.of("student_no",studentNo.trim(),"real_name",name.trim());
 }
}
