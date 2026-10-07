package com.club.util;

import java.io.Console;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 仅通过服务器终端运行，未暴露 HTTP 接口。执行前备份数据库并停止应用写入。 */
public final class AccountMaintenance {
    public static void main(String[] args) throws Exception {
        try {
            if (args.length == 1 && "migrate".equals(args[0])) {
                migrate();
            } else if (args.length == 1 && "create-admin".equals(args[0])) {
                createAdmin();
            } else {
                throw new IllegalArgumentException("用法: AccountMaintenance migrate | create-admin");
            }
        } finally {
            DbUtil.close();
        }
    }

    private static void migrate() throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Long> ids = new ArrayList<>();
                List<String> hashes = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT id, password FROM user FOR UPDATE");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String old = rs.getString("password");
                        if (PasswordUtil.isHash(old)) continue;
                        if (old != null && old.startsWith("$2")) {
                            throw new IllegalArgumentException("发现不支持的 BCrypt 格式，迁移已取消");
                        }
                        ids.add(rs.getLong("id"));
                        hashes.add(PasswordUtil.hash(old));
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET password = ? WHERE id = ?")) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setString(1, hashes.get(i));
                        ps.setLong(2, ids.get(i));
                        if (ps.executeUpdate() != 1) throw new SQLException("账号迁移失败");
                    }
                }
                conn.commit();
                System.out.println("已迁移 " + ids.size() + " 个账号；已有 BCrypt 账号保持不变。");
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private static void createAdmin() throws SQLException {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("请在交互式终端运行，密码不能通过命令参数传递");
        String username = console.readLine("管理员用户名: ");
        String name = console.readLine("姓名: ");
        String phone = console.readLine("手机号: ");
        char[] password = console.readPassword("密码: ");
        char[] confirm = console.readPassword("确认密码: ");
        try {
            if (username == null || username.isBlank() || username.trim().length() > 32
                    || name == null || name.isBlank() || name.trim().length() > 32
                    || phone == null || !phone.matches("\\d{11}")
                    || password == null || password.length < 6 || !Arrays.equals(password, confirm)) {
                throw new IllegalArgumentException("账号资料不合法或两次密码不一致");
            }
            String hash = PasswordUtil.hash(new String(password));
            try (Connection conn = DbUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO user(username,password,role,real_name,phone) VALUES (?,?,'admin',?,?)")) {
                ps.setString(1, username.trim());
                ps.setString(2, hash);
                ps.setString(3, name.trim());
                ps.setString(4, phone);
                ps.executeUpdate();
            }
            System.out.println("管理员账号已创建。");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirm != null) Arrays.fill(confirm, '\0');
        }
    }
}
