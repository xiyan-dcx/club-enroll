package com.club.dao;

import com.club.entity.Enrollment;
import com.club.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//报名表 DAO
public class EnrollmentDao {
    /** 同一学生对同一社团是否已报过 */
    public boolean existsByUserAndClub(Long userId, Long clubId) {
        String sql = "SELECT 1 FROM enrollment WHERE user_id = ? AND club_id = ? LIMIT 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查重失败", e);
        }
    }

    public int insert(Enrollment e) {
        String sql = "INSERT INTO enrollment (user_id, club_id, reason, status) "
                + "VALUES (?, ?, ?, 1)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, e.getUserId());
            ps.setLong(2, e.getClubId());
            ps.setString(3, e.getReason());
            int n = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getLong(1));
            }
            return n;
        } catch (SQLException ex) {
            throw new RuntimeException("新增报名失败", ex);
        }
    }

    public Enrollment findById(Long id) {
        String sql = "SELECT id, user_id, club_id, reason, status, apply_time, "
                + "review_time, reviewer_id, review_remark "
                + "FROM enrollment WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("按 ID 查报名失败", ex);
        }
    }

    /** 学生侧：我的报名记录（含社团名 + 分类，JOIN 查询） */
    public List<Enrollment> findMineWithClub(Long userId) {
        String sql = "SELECT e.id, e.user_id, e.club_id, e.reason, e.status, "
                + "       e.apply_time, e.review_time, e.reviewer_id, e.review_remark, "
                + "       c.name AS club_name, c.category "
                + "FROM enrollment e "
                + "JOIN club c ON c.id = e.club_id "
                + "WHERE e.user_id = ? "
                + "ORDER BY e.apply_time DESC";
        List<Enrollment> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithJoin(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("查我的报名失败", ex);
        }
        return list;
    }

    /** 管理员侧：待审列表 + 学生名 + 社团名（分页） */
    public List<Enrollment> findPendingWithJoin(int offset, int size) {
        String sql = "SELECT e.id, e.user_id, e.club_id, e.reason, e.status, "
                + "       e.apply_time, e.review_time, e.reviewer_id, e.review_remark, "
                + "       u.real_name AS student_name, "
                + "       c.name AS club_name, c.category "
                + "FROM enrollment e "
                + "JOIN user u ON u.id = e.user_id "
                + "JOIN club c ON c.id = e.club_id "
                + "WHERE e.status = 1 "
                + "ORDER BY e.apply_time ASC "
                + "LIMIT ? OFFSET ?";
        List<Enrollment> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithJoin(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("查待审失败", ex);
        }
        return list;
    }

    public int countPending() {
        String sql = "SELECT COUNT(*) FROM enrollment WHERE status = 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new RuntimeException("统计待审数失败", ex);
        }
    }

    /** 统计：今日新报名数 */
    public int countTodayNew() {
        String sql = "SELECT COUNT(*) FROM enrollment WHERE DATE(apply_time) = CURDATE()";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new RuntimeException("统计今日报名数失败", ex);
        }
    }

    /** 统计：当月通过数 */
    public int countMonthlyPassed() {
        String sql = "SELECT COUNT(*) FROM enrollment "
                + "WHERE status = 2 "
                + "  AND review_time >= DATE_FORMAT(NOW(), '%Y-%m-01')";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new RuntimeException("统计当月新会员失败", ex);
        }
    }

    /** 在外部事务里更新审批结果（pass / reject 共用） */
    public int reviewWithConn(Connection conn, Long id, int newStatus,
                              Long reviewerId, String remark) throws SQLException {
        String sql = "UPDATE enrollment "
                + "SET status = ?, review_time = ?, reviewer_id = ?, review_remark = ? "
                + "WHERE id = ? AND status = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStatus);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, reviewerId);
            ps.setString(4, remark);
            ps.setLong(5, id);
            return ps.executeUpdate();
        }
    }

    //把查询到的ResultSet结果集转换成Enrollment对象
    private Enrollment map(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setId(rs.getLong("id"));
        e.setUserId(rs.getLong("user_id"));
        e.setClubId(rs.getLong("club_id"));
        e.setReason(rs.getString("reason"));
        e.setStatus(rs.getInt("status"));
        Timestamp t1 = rs.getTimestamp("apply_time");
        if (t1 != null) e.setApplyTime(t1.toLocalDateTime());
        Timestamp t2 = rs.getTimestamp("review_time");
        if (t2 != null) e.setReviewTime(t2.toLocalDateTime());
        long rid = rs.getLong("reviewer_id");
        if (!rs.wasNull()) e.setReviewerId(rid);
        e.setReviewRemark(rs.getString("review_remark"));
        return e;
    }

    private Enrollment mapWithJoin(ResultSet rs) throws SQLException {
        Enrollment e = map(rs);
        try { e.setStudentName(rs.getString("student_name")); } catch (SQLException ignore) {}
        try { e.setClubName(rs.getString("club_name")); } catch (SQLException ignore) {}
        try { e.setCategory(rs.getString("category")); } catch (SQLException ignore) {}
        return e;
    }
}
