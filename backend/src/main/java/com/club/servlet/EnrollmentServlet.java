package com.club.servlet;

import com.club.entity.User;
import com.club.service.EnrollmentService;
import com.club.util.ApiResponse;
import com.club.util.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
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
@WebServlet(name = "EnrollmentServlet", urlPatterns = "/api/enrollment/*")
public class EnrollmentServlet extends HttpServlet {

    private final EnrollmentService service = new EnrollmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
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
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        Long clubId = ((Number) body.get("clubId")).longValue();
        String reason = (String) body.get("reason");
        Long id = service.apply(me.getId(), clubId, reason);
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
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        Long id = ((Number) body.get("id")).longValue();
        //Long id = Double.valueOf(String.valueOf(body.get("enrollmentId"))).longValue();
        String action = (String) body.get("action");
//        String action = (String) body.get("decision");
        String remark = (String) body.get("remark");
        System.out.println("EnrollmentServlet review...."+body);
        service.review(id, action, remark, me.getId());
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
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = resp.getWriter()) { pw.write(JsonUtil.toJson(body)); }
    }
}
