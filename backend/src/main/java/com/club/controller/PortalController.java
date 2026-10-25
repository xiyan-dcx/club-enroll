package com.club.controller;
import org.springframework.web.bind.annotation.*;

import com.club.entity.User;
import com.club.service.*;
import com.club.util.*;
import com.google.gson.JsonObject;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

/** 新模块统一 API。每个管理操作在服务端验证平台角色或社团归属。 */
@RestController
@RequestMapping("/api/portal")
public class PortalController {
    @org.springframework.beans.factory.annotation.Autowired private VisitService visits;
    @org.springframework.beans.factory.annotation.Autowired private EnrollmentService enrollmentService;
    private static void admin(User u) { if(!User.ROLE_ADMIN.equals(u.getRole())) throw new SecurityException("仅管理员可操作"); }
    private static void student(User u) { if(!User.ROLE_STUDENT.equals(u.getRole())) throw new SecurityException("仅学生账号可操作"); }
     @GetMapping({"","/**"})
    public void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,false); }
     @com.club.infra.RateLimit
     @PostMapping({"","/**"})
    public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,true); }
    private void handle(HttpServletRequest req,HttpServletResponse resp,boolean post) throws IOException {
        try {
            User u=(User)req.getSession(false).getAttribute("user");
            String path=Objects.toString((req.getRequestURI().substring(req.getContextPath().length()+"/api/portal".length())),"");
            Object result;
            if(!post) result=switch(path) {
                case "/activities" -> Beans.get(com.club.infra.HotCache.class).get("activities",()->visits.activities());
                case "/notifications" -> Sql.transaction(c->Sql.rows(c,"SELECT id,message,created_at FROM inbox_notification WHERE user_id=? ORDER BY id DESC LIMIT 100",u.getId()));
                case "/cache-metrics" -> {admin(u);yield Beans.get(com.club.infra.HotCache.class).metrics();}
                case "/operations" -> {admin(u);yield Sql.transaction(c->Map.of("outbox",Sql.rows(c,"SELECT status,COUNT(*) AS count FROM outbox_event GROUP BY status"),"attempts",Sql.rows(c,"SELECT state,COUNT(*) AS count FROM reservation_attempt GROUP BY state")));}
                case "/reservations" -> visits.mine(u.getId());
                case "/clubs" -> visits.clubs(u);
                case "/managers" -> { admin(u); yield Sql.transaction(c -> Sql.rows(c,"SELECT m.club_id,c.name,u.username,u.real_name FROM club_manager m JOIN club c ON c.id=m.club_id JOIN user u ON u.id=m.user_id ORDER BY m.club_id")); }
                case "/roster" -> visits.roster(u,RequestUtil.positiveId(req.getParameter("activityId"),"activityId"));
                case "/enrollments" -> Sql.transaction(c -> {
                    long clubId=RequestUtil.positiveId(req.getParameter("clubId"),"clubId");
                    VisitService.requireManager(c,u,clubId);
                    return Sql.rows(c,"SELECT e.id,e.status,e.reason,e.review_remark,e.apply_time,u.real_name,ei.student_no,ei.real_name AS submitted_name FROM enrollment e JOIN user u ON u.id=e.user_id LEFT JOIN enrollment_identity ei ON ei.enrollment_id=e.id WHERE e.club_id=? ORDER BY e.id DESC LIMIT 500",clubId);
                });
                default -> throw new IllegalArgumentException("未知接口");
            };
            else {
                JsonObject b=RequestUtil.body(req);
                result=switch(path) {
                    case "/publish" -> Map.of("id",visits.publish(u,id(b,"clubId"),str(b,"title"),str(b,"content"),str(b,"location"),Math.toIntExact(id(b,"capacity")),VisitService.time(str(b,"startsAt")),VisitService.time(str(b,"endsAt")),VisitService.time(str(b,"opensAt")),VisitService.time(str(b,"closesAt")),VisitService.time(str(b,"cancelUntil"))));
                    case "/reserve" -> { student(u); yield visits.reserve(u.getId(),id(b,"activityId"),str(b,"requestId"),str(b,"studentNo"),str(b,"realName")); }
                    case "/cancel" -> { student(u); visits.cancel(u.getId(),id(b,"id"),id(b,"version")); yield Map.of(); }
                    case "/cancel-activity" -> {visits.cancelActivity(u,id(b,"activityId"),str(b,"reason"));yield Map.of();}
                    case "/grant" -> { admin(u); visits.grant(u,id(b,"clubId"),str(b,"username"),"remove".equals(str(b,"action"))); yield Map.of(); }
                    case "/review-enrollment" -> {
                        long enrollmentId=id(b,"id");
                        enrollmentService.reviewAsManager(enrollmentId,str(b,"action"),str(b,"remark"),u);
                        yield Map.of();
                    }
                    default -> throw new IllegalArgumentException("未知接口");
                };
            }
            if(post){try{Beans.get(com.club.infra.HotCache.class).invalidate();}catch(Exception ignored){}}
            write(resp,200,ApiResponse.ok(result));
        } catch(SecurityException e) { write(resp,403,ApiResponse.fail(403,e.getMessage())); }
        catch(IllegalArgumentException | ArithmeticException e) { write(resp,400,ApiResponse.fail(400,e.getMessage())); }
    }
    private static String str(JsonObject b,String k) { return RequestUtil.string(b,k); }
    private static long id(JsonObject b,String k) { return RequestUtil.id(b,k); }
    private static void write(HttpServletResponse resp,int status,Object body) throws IOException {
        resp.setStatus(status); resp.setContentType("application/json;charset=UTF-8"); resp.getWriter().write(JsonUtil.toJson(body));
    }
}
