package com.club.servlet;

import com.club.entity.Club;
import com.club.entity.User;
import com.club.service.ClubService;
import com.club.util.ApiResponse;
import com.club.util.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 社团接口：
 *   GET    /api/club/list       学生 + 管理员
 *   GET    /api/club/detail     学生 + 管理员
 *   POST   /api/club            管理员新增
 *   PUT    /api/club            管理员修改
 *   DELETE /api/club?id=        管理员下架
 */
@WebServlet(name = "ClubServlet", urlPatterns = "/api/club/*")
public class ClubServlet extends HttpServlet {

    private final ClubService clubService = new ClubService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            switch (path == null ? "" : path) {
                case "/list" -> list(req, resp);
                case "/detail" -> detail(req, resp);
                default -> write(resp, ApiResponse.fail(404, "未知路径"));
            }
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Club c = JsonUtil.fromJson(req.getReader(), Club.class);
            Long id = clubService.create(c);
            write(resp, ApiResponse.ok(Map.of("id", id)));
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Club c = JsonUtil.fromJson(req.getReader(), Club.class);
            clubService.update(c);
            write(resp, ApiResponse.ok());
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            clubService.deactivate(id);
            write(resp, ApiResponse.ok());
        } catch (NumberFormatException e) {
            write(resp, ApiResponse.fail(400, "id 非法"));
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        String category = req.getParameter("category");
        int page = parseIntDefault(req.getParameter("page"), 1);
        int size = parseIntDefault(req.getParameter("size"), 10);
        // 管理员能看到下架的，学生只能看在招的
        boolean onlyActive = currentRoleIsStudent(req);
        write(resp, ApiResponse.ok(
                clubService.search(keyword, category, page, size, onlyActive)));
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = Long.parseLong(req.getParameter("id"));
        write(resp, ApiResponse.ok(clubService.detail(id)));
    }

    private boolean currentRoleIsStudent(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return true;
        Object u = s.getAttribute("user");
        return u instanceof User uu && User.ROLE_STUDENT.equals(uu.getRole());
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
