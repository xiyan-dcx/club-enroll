package com.club.dao;
import com.club.entity.User;import com.club.mapper.UserMapper;import com.club.util.Beans;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
public class UserDao {
 private UserMapper mapper(){return Beans.get(UserMapper.class);}
 public User findByUsername(String name){return mapper().selectOne(new QueryWrapper<User>().eq("username",name));}
 public boolean existByUsername(String name){return mapper().selectCount(new QueryWrapper<User>().eq("username",name))>0;}
 public int insert(User user){return mapper().insert(user);}
 public int countByRole(String role){return mapper().selectCount(new QueryWrapper<User>().eq("role",role)).intValue();}
}