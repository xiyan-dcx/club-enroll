package com.club.dao;

import com.club.entity.Notice;
import com.club.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//公告表 DAO
public class NoticeDao {
    public int insert(Notice n) {
        String sql = "INSERT INTO notice (club_id, title, content, publisher_id) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, n.getClubId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getContent());
            ps.setLong(4, n.getPublisherId());
            int x = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) n.setId(keys.getLong(1));
            }
            return x;
        } catch (SQLException e) {
            throw new RuntimeException("新增公告失败", e);
        }
    }

    public List<Notice> listAllWithClub() {
        String sql = "SELECT n.id, n.club_id, n.title, n.content, n.publisher_id, n.created_at, "
                + "       c.name AS club_name "
                + "FROM notice n "
                + "JOIN club c ON c.id = n.club_id "
                + "ORDER BY n.created_at DESC";
        List<Notice> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs, true));
        } catch (SQLException e) {
            throw new RuntimeException("查公告列表失败", e);
        }
        return list;
    }

    public List<Notice> listByClubId(Long clubId, int limit) {
        String sql = "SELECT n.id, n.club_id, n.title, n.content, n.publisher_id, n.created_at, "
                + "       c.name AS club_name "
                + "FROM notice n "
                + "JOIN club c ON c.id = n.club_id "
                + "WHERE n.club_id = ? "
                + "ORDER BY n.created_at DESC "
                + "LIMIT ?";
        List<Notice> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clubId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs, true));
            }
        } catch (SQLException e) {
            throw new RuntimeException("按社团查公告失败", e);
        }
        return list;
    }

    private Notice map(ResultSet rs, boolean withJoin) throws SQLException {
        Notice n = new Notice();
        n.setId(rs.getLong("id"));
        n.setClubId(rs.getLong("club_id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setPublisherId(rs.getLong("publisher_id"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        if (withJoin) n.setClubName(rs.getString("club_name"));
        return n;
    }

    /** 仪表盘统计:全平台公告总数 */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM notice";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new RuntimeException("统计公告总数失败", ex);
        }
    }
}
