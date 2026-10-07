package com.club.servlet;

import com.club.entity.User;
import com.club.service.AuthService;
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
 * 账户系统：注册 / 登录 / 登出 / 当前用户信息。
 * 路径分发：
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   POST /api/auth/logout
 *   GET  /api/auth/me
 */
@WebServlet(name = "AuthServlet", urlPatterns = "/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        switch (path == null ? "" : path) {
            case "/me" -> me(req, resp);
            default -> write(resp, ApiResponse.fail(404, "未知路径：" + path));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        switch (path == null ? "" : path) {
            case "/register" -> register(req, resp);
            case "/login"    -> login(req, resp);
            case "/logout"   -> logout(req, resp);
            default -> write(resp, ApiResponse.fail(404, "未知路径：" + path));
        }
    }

    @SuppressWarnings("unchecked")
    private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonObject body = RequestUtil.body(req);
            Long id = authService.register(
                    RequestUtil.string(body, "username"), RequestUtil.string(body, "password"),
                    RequestUtil.string(body, "role"), RequestUtil.string(body, "realName"),
                    RequestUtil.string(body, "phone"));
            write(resp, ApiResponse.ok(Map.of("id", id)));
        } catch (IllegalArgumentException e) {
            write(resp, ApiResponse.fail(400, e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = RequestUtil.body(req);
        User u = authService.login(RequestUtil.string(body, "username"), RequestUtil.string(body, "password"));
        if (u == null) {
            write(resp, ApiResponse.fail(401, "用户名或密码错误"));
            return;
        }
        // 登录后把 user 写入 Session
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        session.setAttribute("user", u);
        write(resp, ApiResponse.ok(u));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        write(resp, ApiResponse.ok());
    }

    private void me(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Object u = session == null ? null : session.getAttribute("user");
        if (u == null) {
            write(resp, ApiResponse.fail(401, "未登录"));
            return;
        }
        write(resp, ApiResponse.ok(u));
    }

    private void write(HttpServletResponse resp, Object body) throws IOException {
        resp.setStatus(((ApiResponse<?>) body).getCode() == 0 ? 200 : ((ApiResponse<?>) body).getCode());
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(JsonUtil.toJson(body));
        }
    }
}
