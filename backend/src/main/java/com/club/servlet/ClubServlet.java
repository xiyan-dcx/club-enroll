package com.club.servlet;

import com.club.entity.Club;
import com.club.entity.User;
import com.club.service.ClubService;
import com.club.util.ApiResponse;
import com.club.util.JsonUtil;
import com.club.util.RequestUtil;
import com.google.gson.JsonObject;

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
            if (req.getPathInfo() != null && !"/".equals(req.getPathInfo())) {
                write(resp, ApiResponse.fail(404, "未知路径")); return;
            }
            Club c = readClub(req, false);
            Long id = clubService.create(c);
            write(resp, ApiResponse.ok(Map.of("id", id)));
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getPathInfo() != null && !"/".equals(req.getPathInfo())) {
                write(resp, ApiResponse.fail(404, "未知路径")); return;
            }
            Club c = readClub(req, true);
            clubService.update(c);
            write(resp, ApiResponse.ok());
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getPathInfo() != null && !"/".equals(req.getPathInfo())) {
                write(resp, ApiResponse.fail(404, "未知路径")); return;
            }
            long id = RequestUtil.positiveId(req.getParameter("id"), "id");
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

    private Club readClub(HttpServletRequest req, boolean updating) throws IOException {
        JsonObject body = RequestUtil.body(req);
        Club c = new Club();
        if (updating) c.setId(RequestUtil.id(body, "id"));
        c.setName(RequestUtil.string(body, "name"));
        c.setCategory(RequestUtil.string(body, "category"));
        c.setLeader(RequestUtil.string(body, "leader"));
        c.setIntro(RequestUtil.string(body, "intro"));
        long max = RequestUtil.id(body, "maxMembers");
        if (max > Integer.MAX_VALUE) throw new IllegalArgumentException("人数上限超出范围");
        c.setMaxMembers((int) max);
        if (body.has("isActive") && !body.get("isActive").isJsonNull()) {
            String status = body.get("isActive").toString();
            if (!"0".equals(status) && !"1".equals(status)) throw new IllegalArgumentException("状态仅支持 0 或 1");
            c.setIsActive(Integer.parseInt(status));
        }
        return c;
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = RequestUtil.positiveId(req.getParameter("id"), "id");
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
        resp.setStatus(((ApiResponse<?>) body).getCode() == 0 ? 200 : ((ApiResponse<?>) body).getCode());
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = resp.getWriter()) { pw.write(JsonUtil.toJson(body)); }
    }
}
