package com.club.dao;
import com.club.entity.Enrollment;import com.club.mapper.SqlMapper;import com.club.util.Sql;import java.util.*;
public class EnrollmentDao {
 public Enrollment findById(Long id){return Sql.transaction(c->Sql.entity(Sql.one(c,"SELECT * FROM enrollment WHERE id=?",id),Enrollment.class));}
 private List<Enrollment> list(String clause,Object...args){return Sql.transaction(c->Sql.rows(c,"SELECT e.*,COALESCE(ei.real_name,u.real_name) AS student_name,ei.student_no,c.name AS club_name,c.category FROM enrollment e JOIN user u ON u.id=e.user_id JOIN club c ON c.id=e.club_id LEFT JOIN enrollment_identity ei ON ei.enrollment_id=e.id "+clause,args).stream().map(r->Sql.entity(r,Enrollment.class)).toList());}
 public List<Enrollment> findMineWithClub(Long id){return list("WHERE e.user_id=? ORDER BY e.apply_time DESC",id);}
 public List<Enrollment> findPendingWithJoin(int offset,int size){return list("WHERE e.status=1 ORDER BY e.apply_time LIMIT ? OFFSET ?",size,offset);}
 public int countPending(){return Sql.count("SELECT COUNT(*) n FROM enrollment WHERE status=1");}
 public int countTodayNew(){return Sql.count("SELECT COUNT(*) n FROM enrollment WHERE apply_time>=CURDATE() AND apply_time<CURDATE()+INTERVAL 1 DAY");}
 public int countMonthlyPassed(){return Sql.count("SELECT COUNT(*) n FROM enrollment WHERE status=2 AND review_time>=DATE_FORMAT(NOW(),'%Y-%m-01')");}
 public int reviewWithConn(SqlMapper c,Long id,int status,Long reviewer,String remark){return Sql.update(c,"UPDATE enrollment SET status=?,review_time=NOW(),reviewer_id=?,review_remark=? WHERE id=? AND status=1",status,reviewer,remark,id);}
}