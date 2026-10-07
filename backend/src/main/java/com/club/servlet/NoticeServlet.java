package com.club.servlet;

import com.club.entity.User;
import com.club.service.NoticeService;
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
 * 公告接口：
 *   GET  /api/notice/list?clubId=  已登录均可
 *   POST /api/notice               管理员发布（路径无后缀，pathInfo 为 null）
 */
@WebServlet(name = "NoticeServlet", urlPatterns = {"/api/notice", "/api/notice/*"})
public class NoticeServlet extends HttpServlet {

    private final NoticeService service = new NoticeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (!"/list".equals(path)) {
            write(resp, ApiResponse.fail(404, "未知路径"));
            return;
        }
        String cid = req.getParameter("clubId");
        Long clubId = (cid == null || cid.isBlank()) ? null : RequestUtil.positiveId(cid, "clubId");
        write(resp, ApiResponse.ok(service.list(clubId)));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getPathInfo() != null && !"/".equals(req.getPathInfo())) {
                write(resp, ApiResponse.fail(404, "未知路径")); return;
            }
            User me = currentUser(req);
            JsonObject body = RequestUtil.body(req);
            Long clubId = RequestUtil.id(body, "clubId");
            String title = RequestUtil.string(body, "title");
            String content = RequestUtil.string(body, "content");
            Long id = service.publish(clubId, title, content, me.getId());
            write(resp, ApiResponse.ok(Map.of("id", id)));
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    private User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute("user");
    }

    private void write(HttpServletResponse resp, Object body) throws IOException {
        resp.setStatus(((ApiResponse<?>) body).getCode() == 0 ? 200 : ((ApiResponse<?>) body).getCode());
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = resp.getWriter()) { pw.write(JsonUtil.toJson(body)); }
    }
}
