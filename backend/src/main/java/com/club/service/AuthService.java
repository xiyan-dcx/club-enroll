package com.club.service;

import com.club.dao.UserDao;
import com.club.entity.User;
import com.club.util.PasswordUtil;

/**
 * 账户系统业务：注册+登录
 */
public class AuthService {
    private final UserDao userDao;

    public AuthService() { this(new UserDao()); }

    AuthService(UserDao userDao) { this.userDao = userDao; }

    public User login(String username, String password){
        if (isBlank(username) || isBlank(password))
            return null;
        User u = userDao.findByUsername(username.trim());
        if (u == null)
            return null;
        if (!PasswordUtil.matches(password, u.getPassword()))
            return null;
        u.setPassword(null);
        return u;
    }

    public Long register(String username, String password, String role, String realName, String phone){
        if (isBlank(username) || isBlank(password))
            throw new IllegalArgumentException("用户名/密码不能为空");
        if (username.trim().length() < 3 || username.trim().length() > 32)
            throw new IllegalArgumentException("用户名长度必须为 3-32 位");
        if (password.length() < 6)
            throw new IllegalArgumentException("密码长度必须 ≥ 6 位");
        if (role != null && !User.ROLE_STUDENT.equals(role))
            throw new IllegalArgumentException("公开注册仅支持学生账号");
        if (isBlank(realName))
            throw new IllegalArgumentException("真实姓名不能为空");
        if (realName.trim().length() > 32) throw new IllegalArgumentException("姓名不能超 32 字");
        if (phone == null || !phone.matches("^\\d{11}$"))
            throw new IllegalArgumentException("手机号必须是 11 位数字");
        if (userDao.existByUsername(username.trim()))
            throw new IllegalArgumentException("用户名已被占用");

        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(PasswordUtil.hash(password));
        u.setRole(User.ROLE_STUDENT);
        u.setRealName(realName.trim());
        u.setPhone(phone);
        userDao.insert(u);
        return u.getId();
    }

    private boolean isBlank(String str){
        return  str == null || str.trim().isEmpty();
    }
}
