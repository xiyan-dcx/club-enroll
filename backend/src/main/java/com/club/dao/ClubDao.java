package com.club.dao;

import com.club.entity.Club;
import com.club.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//社团表 DAO
public class ClubDao {

    public List<Club> search(String keyword, String category, int offset, int size, boolean onlyActive) {
        StringBuilder sb = new StringBuilder(
                "SELECT id, name, category, intro, leader, "
                        + "       max_members, current_members, is_active, created_at "
                        + "FROM club WHERE 1=1 "
        );
        List<Object> params = new ArrayList<Object>();

        if (onlyActive) {
            sb.append(" AND is_active=1 ");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sb.append("AND name LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }
        if (category != null && !category.isEmpty()) {
            sb.append("AND category = ? ");
            params.add(category.trim());
        }
        sb.append("ORDER BY current_members DESC, id ASC LIMIT ? OFFSET ?");
        params.add(offset);
        params.add(size);

        List<Club> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 1; i <= params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }catch (SQLException e) {
            throw new RuntimeException("查询社团列表失败", e);
        }
        return list;
    }

    public int countSearch(String keyword, String category, boolean onlyActive) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM club WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (onlyActive) sb.append("AND is_active = 1 ");
        if (keyword != null && !keyword.isBlank()) {
            sb.append("AND name LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            sb.append("AND category = ? ");
            params.add(category.trim());
        }
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("统计社团数失败", e);
        }
    }

    public Club findById(Long id) {
        String sql = "SELECT id, name, category, intro, leader, "
                + "       max_members, current_members, is_active, created_at "
                + "FROM club WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("按 ID 查社团失败", e);
        }
    }

    public int insert(Club c) {
        String sql = "INSERT INTO club (name, category, intro, leader, max_members, "
                + "current_members, is_active) VALUES (?, ?, ?, ?, ?, 0, 1)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getCategory());
            ps.setString(3, c.getIntro());
            ps.setString(4, c.getLeader());
            ps.setInt(5, c.getMaxMembers());
            int n = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getLong(1));
            }
            return n;
        } catch (SQLException e) {
            throw new RuntimeException("新增社团失败", e);
        }
    }

    public int update(Club c) {
        String sql = "UPDATE club SET name = ?, category = ?, intro = ?, leader = ?, "
                + "max_members = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getCategory());
            ps.setString(3, c.getIntro());
            ps.setString(4, c.getLeader());
            ps.setInt(5, c.getMaxMembers());
            ps.setInt(6, c.getIsActive());
            ps.setLong(7, c.getId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("修改社团失败", e);
        }
    }

    public int deactivate(Long id) {
        String sql = "UPDATE club SET is_active = 0 WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("下架社团失败", e);
        }
    }

    /** 在外部事务里给 current_members + 1，配合审批通过。 */
    public int incrementCurrentMembers(Connection conn, Long id) throws SQLException {
        String sql = "UPDATE club SET current_members = current_members + 1 "
                + "WHERE id = ? AND current_members < max_members";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    private Club map(ResultSet rs) throws SQLException {
        Club c = new Club();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setCategory(rs.getString("category"));
        c.setIntro(rs.getString("intro"));
        c.setLeader(rs.getString("leader"));
        c.setMaxMembers(rs.getInt("max_members"));
        c.setCurrentMembers(rs.getInt("current_members"));
        c.setIsActive(rs.getInt("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        return c;
    }
}
