package com.club.service;

import com.club.dao.UserDao;
import com.club.entity.User;

/**
 * 账户系统业务：注册+登录
 */
public class AuthService {
    private final UserDao userDao =  new UserDao();

    public User login(String username, String password){
        if (isBlank(username) || isBlank(password))
            return null;
        User u = userDao.findByUsername(username.trim());
        if (u == null)
            return null;
        if (!password.equals(u.getPassword()))
            return null;
        u.setPassword(null);
        return u;
    }

    public Long register(String username, String password, String role, String realName, String phone){
        if (isBlank(username) || isBlank(password))
            throw new IllegalArgumentException("用户名/密码不能为空");
        if (password.length() < 6)
            throw new IllegalArgumentException("密码长度必须 ≥ 6 位");
        if (!User.ROLE_STUDENT.equals(role) && !User.ROLE_ADMIN.equals(role))
            throw new IllegalArgumentException("角色非法，仅支持 student / admin");
        if (isBlank(realName))
            throw new IllegalArgumentException("真实姓名不能为空");
        if (phone == null || !phone.matches("^\\d{11}$"))
            throw new IllegalArgumentException("手机号必须是 11 位数字");
        if (userDao.existByUsername(username.trim()))
            throw new IllegalArgumentException("用户名已被占用");

        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(password);
        u.setRole(role);
        u.setRealName(realName.trim());
        u.setPhone(phone);
        userDao.insert(u);
        return u.getId();
    }

    private boolean isBlank(String str){
        return  str == null || str.trim().isEmpty();
    }
}
