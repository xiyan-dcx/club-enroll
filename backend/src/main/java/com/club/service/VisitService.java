package com.club.service;

import com.club.entity.User;
import com.club.util.Sql;
import com.club.util.Beans;
import com.club.infra.*;
import java.sql.SQLException;
import com.club.mapper.SqlMapper;
import java.time.*;
import java.util.*;

/** 首版采用数据库事务保证容量与预约一致，活动发布同时写公告。 */
@org.springframework.stereotype.Service
public class VisitService {
    public static void requireManager(SqlMapper c,User actor,long clubId) throws SQLException {
        if(User.ROLE_ADMIN.equals(actor.getRole())) return;
        if(Sql.one(c,"SELECT club_id FROM club_manager WHERE club_id=? AND user_id=? FOR UPDATE",clubId,actor.getId())==null)
            throw new SecurityException("只能管理本人负责的社团");
    }
    public List<Map<String,Object>> clubs(User actor) {
        return Sql.transaction(c -> User.ROLE_ADMIN.equals(actor.getRole())
            ? Sql.rows(c,"SELECT id,name FROM club ORDER BY id")
            : Sql.rows(c,"SELECT c.id,c.name FROM club c JOIN club_manager m ON m.club_id=c.id WHERE m.user_id=? ORDER BY c.id",actor.getId()));
    }
    public List<Map<String,Object>> activities() {
        return Sql.transaction(c -> Sql.rows(c,"SELECT a.*,c.name AS club_name,NOW() AS server_now FROM visit_activity a JOIN club c ON c.id=a.club_id WHERE a.status='PUBLISHED' ORDER BY a.starts_at DESC,a.id DESC LIMIT 300"));
    }
    public List<Map<String,Object>> mine(long userId) {
        return Sql.transaction(c -> Sql.rows(c,"SELECT r.id,r.activity_id,r.status,r.version,a.title,a.location,a.starts_at,a.cancel_until,a.status AS activity_status,NOW() AS server_now FROM visit_reservation r JOIN visit_activity a ON a.id=r.activity_id WHERE r.user_id=? ORDER BY r.id DESC LIMIT 300",userId));
    }
    public static LocalDateTime time(String value) {
        try { return LocalDateTime.parse(value); }
        catch(Exception e) { throw new IllegalArgumentException("请填写有效的日期时间"); }
    }
    public static void validateWindow(LocalDateTime start,LocalDateTime end,LocalDateTime open,LocalDateTime close,LocalDateTime cancel) {
        if(!start.isBefore(end) || !open.isBefore(close) || close.isAfter(start) || cancel.isAfter(start) || cancel.isBefore(close))
            throw new IllegalArgumentException("须满足：开放 < 预约截止 ≤ 取消截止 ≤ 活动开始 < 活动结束");
    }
    public long publish(User actor,long clubId,String title,String content,String location,int capacity,
                        LocalDateTime start,LocalDateTime end,LocalDateTime open,LocalDateTime close,LocalDateTime cancel) {
        if(title==null || title.isBlank() || title.trim().length()>64 || content==null || content.isBlank() || content.length()>10000 || location==null || location.isBlank() || location.trim().length()>200)
            throw new IllegalArgumentException("请填写标题（最多64字）、正文（最多10000字）、地点（最多200字）");
        if(capacity<1 || capacity>100000) throw new IllegalArgumentException("容量须为 1—100000 的整数");
        validateWindow(start,end,open,close,cancel);
        return Sql.transaction(c -> {
            requireManager(c,actor,clubId);
            if(Sql.one(c,"SELECT id FROM club WHERE id=? AND is_active=1",clubId)==null) throw new IllegalArgumentException("社团不存在或已下架");
            LocalDateTime now=now(c);
            if(!close.isAfter(now)) throw new IllegalArgumentException("预约截止时间必须晚于当前时间");
            long notice=Sql.insert(c,"INSERT INTO notice(club_id,title,content,publisher_id) VALUES(?,?,?,?)",clubId,title.trim(),content.trim(),actor.getId());
            long activityId=Sql.insert(c,"INSERT INTO visit_activity(club_id,notice_id,title,content,location,capacity,starts_at,ends_at,opens_at,closes_at,cancel_until,publisher_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                clubId,notice,title.trim(),content.trim(),location.trim(),capacity,start,end,open,close,cancel,actor.getId());
            EventPipeline.add(c,"activity:"+activityId,"ACTIVITY_PUBLISHED",null,activityId,"活动已发布");
            return activityId;
        });
    }
    private static LocalDateTime now(SqlMapper c) throws SQLException {
        return time(Sql.one(c,"SELECT NOW() AS t").get("t").toString());
    }
    private static LocalDateTime date(Map<String,Object> row,String key) { return time(row.get(key).toString()); }
    private static Map<String,Object> result(SqlMapper c,long id) throws SQLException {
        return Sql.one(c,"SELECT id,activity_id,status,version FROM visit_reservation WHERE id=?",id);
    }
    public Map<String,Object> reserve(long userId,long activityId,String requestId,String studentNo,String realName) {
        if(requestId==null || !requestId.matches("[A-Za-z0-9_-]{8,64}"))throw new IllegalArgumentException("预约请求标识无效");
        var submitted=StudentIdentityService.submitted(studentNo,realName);
        return Beans.get(ReservationGate.class).execute(userId,activityId,requestId,submitted.get("student_no").toString(),token->reserveDatabase(userId,activityId,requestId,token,submitted));
    }
    private Map<String,Object> reserveDatabase(long userId,long activityId,String requestId,String token,Map<String,Object> identity) {
        if(requestId==null || !requestId.matches("[A-Za-z0-9_-]{8,64}")) throw new IllegalArgumentException("预约请求标识无效");
        return Sql.transaction(c -> {
            var attempt=Sql.one(c,"SELECT state FROM reservation_attempt WHERE token=? FOR UPDATE",token);
            if(attempt==null || !"PENDING".equals(attempt.get("state")))throw new IllegalArgumentException("预约请求已失效，请重新提交");
            // 每个场次串行处理容量和取消；不同场次互不阻塞。
            Map<String,Object> activity=Sql.one(c,"SELECT * FROM visit_activity WHERE id=? FOR UPDATE",activityId);
            if(activity==null) throw new IllegalArgumentException("活动不存在");
            Map<String,Object> prior=Sql.one(c,"SELECT activity_id,reservation_id FROM visit_request WHERE user_id=? AND request_id=?",userId,requestId);
            if(prior!=null) {
                if(Sql.number(prior,"activity_id")!=activityId) throw new IllegalArgumentException("请求标识不可用于其他活动");
                return result(c,Sql.number(prior,"reservation_id"));
            }
            var own=Sql.one(c,"SELECT student_no FROM visit_reservation WHERE activity_id=? AND user_id=? AND status='CONFIRMED'",activityId,userId);
            if(own!=null && !identity.get("student_no").equals(own.get("student_no")))throw new IllegalArgumentException("同一账号每场只能预约一次");
            Map<String,Object> existing=Sql.one(c,"SELECT * FROM visit_reservation WHERE activity_id=? AND student_no=? FOR UPDATE",activityId,identity.get("student_no"));
            if(existing!=null && Sql.number(existing,"user_id")!=userId) throw new IllegalArgumentException("该身份已有其他账号的预约记录，请联系管理员处理");
            long id;
            if(existing!=null && "CONFIRMED".equals(existing.get("status"))) id=Sql.number(existing,"id");
            else {
                LocalDateTime now=now(c);
                if(!"PUBLISHED".equals(activity.get("status")) || now.isBefore(date(activity,"opens_at")) || !now.isBefore(date(activity,"closes_at")))
                    throw new IllegalArgumentException("当前不在预约开放时间内");
                if(Sql.update(c,"UPDATE visit_activity SET booked_count=booked_count+1 WHERE id=? AND booked_count<capacity",activityId)!=1)
                    throw new IllegalArgumentException("本场名额已满");
                if(existing==null) id=Sql.insert(c,"INSERT INTO visit_reservation(activity_id,user_id,student_no,real_name,status) VALUES(?,?,?,?,'CONFIRMED')",activityId,userId,identity.get("student_no"),identity.get("real_name"));
                else {
                    id=Sql.number(existing,"id");
                    Sql.update(c,"UPDATE visit_reservation SET status='CONFIRMED',real_name=?,version=version+1,created_at=NOW(),cancelled_at=NULL WHERE id=?",identity.get("real_name"),id);
                }
            }
            Sql.update(c,"INSERT INTO visit_request(user_id,request_id,activity_id,reservation_id) VALUES(?,?,?,?)",userId,requestId,activityId,id);
            Sql.update(c,"UPDATE reservation_attempt SET state='CONFIRMED' WHERE token=?",token);
            EventPipeline.add(c,"reserved:"+id+":"+Sql.number(result(c,id),"version"),"RESERVED",userId,activityId,"预约成功："+activity.get("title"));
            return result(c,id);
        });
    }
    public void cancel(long userId,long id,long version) {
        long activityToInvalidate=Sql.transaction(c->{var r=Sql.one(c,"SELECT activity_id FROM visit_reservation WHERE id=? AND user_id=?",id,userId);if(r==null)throw new IllegalArgumentException("预约不存在");return Sql.number(r,"activity_id");});
        Sql.transaction(c -> {
            Map<String,Object> reservation=Sql.one(c,"SELECT activity_id FROM visit_reservation WHERE id=? AND user_id=?",id,userId);
            if(reservation==null) throw new IllegalArgumentException("预约不存在");
            long activityId=Sql.number(reservation,"activity_id");
            Map<String,Object> activity=Sql.one(c,"SELECT * FROM visit_activity WHERE id=? FOR UPDATE",activityId);
            Map<String,Object> current=Sql.one(c,"SELECT * FROM visit_reservation WHERE id=? FOR UPDATE",id);
            if("CANCELLED".equals(current.get("status"))) return null;
            if(Sql.number(current,"version")!=version) throw new IllegalArgumentException("预约状态已更新，请刷新后操作");
            if(!now(c).isBefore(date(activity,"cancel_until"))) throw new IllegalArgumentException("已超过取消截止时间，请联系社团负责人");
            Sql.update(c,"UPDATE visit_reservation SET status='CANCELLED',version=version+1,cancelled_at=NOW() WHERE id=?",id);
            if(Sql.update(c,"UPDATE visit_activity SET booked_count=booked_count-1 WHERE id=? AND booked_count>0",activityId)!=1)
                throw new IllegalStateException("预约人数不一致");
            EventPipeline.add(c,"cancelled:"+id+":"+(version+1),"CANCELLED",userId,activityId,"预约已取消："+activity.get("title"));
            return null;
        });
        Beans.get(ReservationGate.class).invalidate(activityToInvalidate);
        try{Beans.get(HotCache.class).invalidate();}catch(Exception ignored){}
    }
    public List<Map<String,Object>> roster(User actor,long activityId) {
        return Sql.transaction(c -> {
            Map<String,Object> a=Sql.one(c,"SELECT club_id FROM visit_activity WHERE id=?",activityId);
            if(a==null) throw new IllegalArgumentException("活动不存在");
            requireManager(c,actor,Sql.number(a,"club_id"));
            return Sql.rows(c,"SELECT id,student_no,real_name,status,created_at FROM visit_reservation WHERE activity_id=? ORDER BY id LIMIT 1000",activityId);
        });
    }
    public void cancelActivity(User actor,long activityId,String reason) {
        if(reason==null || reason.trim().length()<2 || reason.length()>200)throw new IllegalArgumentException("请填写 2—200 字取消原因");
        Sql.transaction(c->{
            var a=Sql.one(c,"SELECT * FROM visit_activity WHERE id=? FOR UPDATE",activityId);
            if(a==null)throw new IllegalArgumentException("活动不存在");
            requireManager(c,actor,Sql.number(a,"club_id"));
            if("CANCELLED".equals(a.get("status")))return null;
            var members=Sql.rows(c,"SELECT id,user_id,version FROM visit_reservation WHERE activity_id=? AND status='CONFIRMED'",activityId);
            Sql.update(c,"UPDATE visit_activity SET status='CANCELLED',booked_count=0 WHERE id=?",activityId);
            Sql.update(c,"UPDATE visit_reservation SET status='CANCELLED',version=version+1,cancelled_at=NOW() WHERE activity_id=? AND status='CONFIRMED'",activityId);
            Sql.update(c,"UPDATE notice SET content=CONCAT(?,content) WHERE id=?","【活动已取消】"+reason.trim()+"\n",a.get("notice_id"));
            for(var r:members)EventPipeline.add(c,"activity-cancelled:"+activityId+":"+r.get("id"),"ACTIVITY_CANCELLED",Sql.number(r,"user_id"),activityId,"活动取消："+a.get("title")+"；原因："+reason.trim());
            EventPipeline.add(c,"activity-closed:"+activityId,"ACTIVITY_CHANGED",null,activityId,reason.trim());
            return null;
        });
        Beans.get(ReservationGate.class).invalidate(activityId);
    }
    public void grant(User admin,long clubId,String username,boolean remove) {
        if(!User.ROLE_ADMIN.equals(admin.getRole())) throw new SecurityException("仅管理员可授权负责人");
        Sql.transaction(c -> {
            Map<String,Object> user=Sql.one(c,"SELECT id FROM user WHERE username=? AND role='student'",username);
            if(user==null) throw new IllegalArgumentException("请填写已注册学生账号的用户名");
            if(Sql.one(c,"SELECT id FROM club WHERE id=?",clubId)==null) throw new IllegalArgumentException("社团不存在");
            var old=Sql.one(c,"SELECT user_id FROM club_manager WHERE club_id=? AND user_id=?",clubId,user.get("id"));
            if(remove) Sql.update(c,"DELETE FROM club_manager WHERE club_id=? AND user_id=?",clubId,user.get("id"));
            else Sql.update(c,"INSERT INTO club_manager(club_id,user_id) VALUES(?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id)",clubId,user.get("id"));
            if((remove && old!=null)||(!remove && old==null)){
                var club=Sql.one(c,"SELECT name FROM club WHERE id=?",clubId);
                EventPipeline.add(c,"manager:"+UUID.randomUUID(),"MANAGER_CHANGED",Sql.number(user,"id"),null,(remove?"已取消你对「":"你已被授权管理「")+club.get("name")+"」的管理权限");
            }
            return null;
        });
    }
}
