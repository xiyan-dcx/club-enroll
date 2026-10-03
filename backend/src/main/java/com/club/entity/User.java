package com.club.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体（对应 user 表，含学生和管理员两种角色）。
 * 实现 Serializable 让 Tomcat 可以把 Session 序列化到磁盘做热重启。
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    //角色常量，避免代码里到处是字符串
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ADMIN = "admin";

    private Long id;
    private String username;
    private String password;
    private String role;
    private String realName;
    private String phone;
    private LocalDateTime createdAt;

    public User() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String toString(){
        return "User{id=" + id + ", username=" + username + ", password=" + password + ", role=" + role + "'}";
    }
}
