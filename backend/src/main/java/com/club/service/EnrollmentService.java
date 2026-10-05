package com.club.service;

import com.club.dao.ClubDao;
import com.club.dao.EnrollmentDao;
import com.club.entity.Club;
import com.club.entity.Enrollment;
import com.club.util.DbUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报名业务：含审批业务
 * 审批通过时要同时改两张表（enrollment.status=2 + club.current_members+1），必须事务。
 */
public class EnrollmentService {

    private final EnrollmentDao enrollmentDao = new EnrollmentDao();
    private final ClubDao clubDao = new ClubDao();

    public Long apply(Long userId, Long clubId, String reason){
        if (reason == null || reason.trim().length() < 10)
            throw new IllegalArgumentException("报名理由至少 10 字");
        if (reason.length() > 200)
            throw new IllegalArgumentException("报名理由不能超 200 字");

        Club club = clubDao.findById(clubId);
        if (club == null) throw new IllegalArgumentException("社团不存在");
        if (club.getIsActive() == 0)
            throw new IllegalArgumentException("社团已下架，无法报名");
        if (club.getCurrentMembers() >= club.getMaxMembers())
            throw new IllegalArgumentException("社团已招满");

        if (enrollmentDao.existsByUserAndClub(userId, clubId))
            throw new IllegalArgumentException("你已报过这个社团，请等待审批结果");

        Enrollment e = new Enrollment();
        e.setUserId(userId);
        e.setClubId(clubId);
        e.setReason(reason.trim());
        e.setStatus(Enrollment.STATUS_PENDING);
        enrollmentDao.insert(e);
        return e.getId();
    }

    public List<Enrollment> mine(Long userId) {
        return enrollmentDao.findMineWithClub(userId);
    }

    public Map<String, Object> pending(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        int offset = (page - 1) * size;
        List<Enrollment> records = enrollmentDao.findPendingWithJoin(offset, size);
        int total = enrollmentDao.countPending();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     *审批：action = "pass" / "reject"
     * 通过：enrollment.status=2 + club.current_members+1（事务）
     * 拒绝：enrollment.status=3（单表 UPDATE，事务也用，统一风格）
     */
    public void review(Long enrollmentId, String action, String remark, Long reviewerId) {
        if (remark == null || remark.trim().length() < 5)
            throw new IllegalArgumentException("审批备注至少 5 字");
        if (!"pass".equals(action) && !"reject".equals(action))
            throw new IllegalArgumentException("action 仅支持 pass / reject");

        Enrollment e = enrollmentDao.findById(enrollmentId);
        if (e == null) throw new IllegalArgumentException("报名记录不存在");
        if (e.getStatus() != Enrollment.STATUS_PENDING)
            throw new IllegalArgumentException("该记录已审批，不可重复操作");

        int newStatus = "pass".equals(action) ? Enrollment.STATUS_PASSED : Enrollment.STATUS_REJECTED;

        //通过时校验容量
        if (newStatus == Enrollment.STATUS_PASSED) {
            Club club = clubDao.findById(e.getClubId());
            if (club.getCurrentMembers() >= club.getMaxMembers())
                throw new IllegalArgumentException("社团已招满，无法通过");
        }

        //====== 事务开始 ======
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            int n1 = enrollmentDao.reviewWithConn(conn, enrollmentId, newStatus, reviewerId,remark.trim());
            if (n1 != 1) {
                throw new IllegalArgumentException("审批失败，可能已被其他管理员处理");
            }

            if (newStatus == Enrollment.STATUS_PASSED) {
                int n2 = clubDao.incrementCurrentMembers(conn, e.getClubId());
                if (n2 != 1) {
                    // 防超员（并发审批同时通过两条，可能挤爆容量）
                    throw new IllegalArgumentException("社团人数已达上限");
                }
            }

            conn.commit();
        } catch (Exception ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {}
            }
            if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException)
                throw (RuntimeException) ex;
            throw new RuntimeException("审批失败，已回滚：" + ex.getMessage(), ex);
        }finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                }catch (SQLException ignore) {}
                try {
                    conn.close();
                }catch (SQLException ignore) {}
            }
        }
    }
}
