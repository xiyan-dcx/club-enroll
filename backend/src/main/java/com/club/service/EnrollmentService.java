package com.club.service;

import com.club.dao.ClubDao;
import com.club.dao.EnrollmentDao;
import com.club.entity.Club;
import com.club.entity.Enrollment;
import com.club.util.DbUtil;
import com.club.util.RequestUtil;
import com.club.util.Sql;
import com.club.entity.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报名业务：含审批业务
 * 审批通过时要同时改两张表（enrollment.status=2 + club.current_members+1），必须事务。
 */
@org.springframework.stereotype.Service
public class EnrollmentService {

    private final EnrollmentDao enrollmentDao = new EnrollmentDao();
    private final ClubDao clubDao = new ClubDao();

    public Long apply(Long userId, Long clubId, String reason, String studentNo, String realName){
        if (reason == null || reason.trim().length() < 10)
            throw new IllegalArgumentException("报名理由至少 10 字");
        if (reason.length() > 200)
            throw new IllegalArgumentException("报名理由不能超 200 字");

        return Sql.transaction(conn -> {
            Map<String,Object> identity=StudentIdentityService.submitted(studentNo,realName);
            Map<String,Object> club=Sql.one(conn,"SELECT is_active,current_members,max_members FROM club WHERE id=? FOR UPDATE",clubId);
            if(club==null || Sql.number(club,"is_active")!=1) throw new IllegalArgumentException("社团不存在或已下架");
            if(Sql.number(club,"current_members")>=Sql.number(club,"max_members")) throw new IllegalArgumentException("社团已招满");
            if(Sql.one(conn,"SELECT id FROM enrollment WHERE user_id=? AND club_id=?",userId,clubId)!=null)
                throw new IllegalArgumentException("你已报过这个社团，请等待审批结果");
            long id=Sql.insert(conn,"INSERT INTO enrollment(user_id,club_id,reason,status) VALUES(?,?,?,1)",userId,clubId,reason.trim());
            Sql.update(conn,"INSERT INTO enrollment_identity(enrollment_id,club_id,student_no,real_name) VALUES(?,?,?,?)",id,clubId,identity.get("student_no"),identity.get("real_name"));
            return id;
        });
    }

    public List<Enrollment> mine(Long userId) {
        return enrollmentDao.findMineWithClub(userId);
    }

    public Map<String, Object> pending(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        int offset = RequestUtil.offset(page, size);
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
        reviewInternal(enrollmentId,action,remark,reviewerId,null);
    }

    public void reviewAsManager(Long enrollmentId,String action,String remark,User actor) {
        reviewInternal(enrollmentId,action,remark,actor.getId(),actor);
    }

    private void reviewInternal(Long enrollmentId, String action, String remark, Long reviewerId,User actor) {
        if (remark == null || remark.trim().length() < 5)
            throw new IllegalArgumentException("审批备注至少 5 字");
        if (remark.trim().length() > 200) throw new IllegalArgumentException("审批备注不能超 200 字");
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
            if (club == null || club.getIsActive() != 1)
                throw new IllegalArgumentException("社团不存在或已下架，无法通过");
            if (club.getCurrentMembers() >= club.getMaxMembers())
                throw new IllegalArgumentException("社团已招满，无法通过");
        }

        Sql.transaction(conn -> {
            if(actor!=null) VisitService.requireManager(conn,actor,e.getClubId());
            if(enrollmentDao.reviewWithConn(conn,enrollmentId,newStatus,reviewerId,remark.trim())!=1) throw new IllegalArgumentException("该记录已被处理");
            if(newStatus==Enrollment.STATUS_PASSED && clubDao.incrementCurrentMembers(conn,e.getClubId())!=1) throw new IllegalArgumentException("社团已下架或人数已满");
            return null;
        });
    }
}
