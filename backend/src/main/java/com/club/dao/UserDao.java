package com.club.dao;

import com.club.entity.User;
import com.club.util.DbUtil;

import java.sql.*;


//用户表DAO：登陆查询 + 注册插入 + 用户名查重
public class UserDao {
    public User findByUsername(String username) {
        String sql = "SELECT id, username, password, role, real_name, phone, created_at "
                + "FROM user WHERE username = ?";
        try(Connection conn = DbUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1,username);
            try(ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }catch (SQLException e) {
            throw new RuntimeException("按用户名查用户失败",e);
        }
    }

    public boolean existByUsername(String username) {
        String sql = "SELECT 1 FROM user WHERE username = ? LIMIT 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查重失败", e);
        }
    }

    public int insert(User u) {
        String sql = "INSERT INTO user (username, password, role, real_name, phone) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getRole());
            ps.setString(4, u.getRealName());
            ps.setString(5, u.getPhone());
            int n = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getLong(1));
            }
            return n;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new IllegalArgumentException("用户名已被占用");
            throw new RuntimeException("注册用户失败", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setRealName(rs.getString("real_name"));
        u.setPhone(rs.getString("phone"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }

    /** 仪表盘统计:按角色计数 */
    public int countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM user WHERE role = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("按角色统计用户失败", ex);
        }
    }
}
