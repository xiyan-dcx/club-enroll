package com.club.controller;
import org.springframework.web.bind.annotation.*;

import com.club.entity.User;
import com.club.service.EnrollmentService;
import com.club.util.ApiResponse;
import com.club.util.JsonUtil;
import com.club.util.RequestUtil;
import com.google.gson.JsonObject;



import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 报名接口：
 *   POST /api/enrollment/apply    学生提交
 *   GET  /api/enrollment/mine     学生看自己的
 *   GET  /api/enrollment/pending  管理员看待审
 *   POST /api/enrollment/review   管理员审批
 */
@RestController
@RequestMapping("/api/enrollment")
public class EnrollmentController {

    @org.springframework.beans.factory.annotation.Autowired private EnrollmentService service;

    
    @GetMapping({"","/**"})
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = (req.getRequestURI().substring(req.getContextPath().length()+"/api/enrollment".length()));
        try {
            switch (path == null ? "" : path) {
                case "/mine" -> mine(req, resp);
                case "/pending" -> pending(req, resp);
                default -> write(resp, ApiResponse.fail(404, "未知路径"));
            }
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    
    @com.club.infra.RateLimit
    @PostMapping({"","/**"})
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = (req.getRequestURI().substring(req.getContextPath().length()+"/api/enrollment".length()));
        try {
            switch (path == null ? "" : path) {
                case "/apply" -> apply(req, resp);
                case "/review" -> review(req, resp);
                default -> write(resp, ApiResponse.fail(404, "未知路径"));
            }
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void apply(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User me = currentUser(req);
        JsonObject body = RequestUtil.body(req);
        Long clubId = RequestUtil.id(body, "clubId");
        String reason = RequestUtil.string(body, "reason");
        Long id = service.apply(me.getId(), clubId, reason, RequestUtil.string(body, "studentNo"), RequestUtil.string(body, "realName"));
        write(resp, ApiResponse.ok(Map.of("id", id)));
    }

    private void mine(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User me = currentUser(req);
        write(resp, ApiResponse.ok(service.mine(me.getId())));
    }

    private void pending(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = parseIntDefault(req.getParameter("page"), 1);
        int size = parseIntDefault(req.getParameter("size"), 10);
        write(resp, ApiResponse.ok(service.pending(page, size)));
    }

    @SuppressWarnings("unchecked")
    private void review(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User me = currentUser(req);
        JsonObject body = RequestUtil.body(req);
        Long id = RequestUtil.id(body, "id");
        String action = RequestUtil.string(body, "action");
        String remark = RequestUtil.string(body, "remark");
        service.review(id, action, remark, me.getId());
        try{com.club.util.Beans.get(com.club.infra.HotCache.class).invalidate();}catch(Exception ignored){}
        write(resp, ApiResponse.ok());
    }

    private User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute("user");
    }

    private int parseIntDefault(String v, int def) {
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    private void write(HttpServletResponse resp, Object body) throws IOException {
        resp.setStatus(((ApiResponse<?>) body).getCode() == 0 ? 200 : ((ApiResponse<?>) body).getCode());
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = resp.getWriter()) { pw.write(JsonUtil.toJson(body)); }
    }
}
