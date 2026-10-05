package com.club.servlet;

import com.club.entity.User;
import com.club.service.AuthService;
import com.club.util.ApiResponse;
import com.club.util.JsonUtil;
import jakarta.servlet.ServletException;
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
    
    private final AuthService authService = new  AuthService();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        switch (path == null ? "" : path) {
            case "/me" -> me(request, response);
            default -> write(response, ApiResponse.fail(404, "未知路径：" + path));
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        switch (path == null ? "" : path) {
            case "/register" -> register(request,response);
            case "/login" -> login(request,response);
            case "login" -> logout(request,response);
            default -> write(response,ApiResponse.fail(404,"未知路径：" + path));
        }
    }
    
    @SuppressWarnings("unchecked")
    private void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String,String> body = JsonUtil.fromJson(request.getReader(),Map.class);
            Long id = authService.register(
                    body.get("username"),body.get("password"), body.get("role"),
                    body.get("realName"),body.get("phone")
            );
            write(response,ApiResponse.ok(Map.of("id",id)));
        }catch (IllegalArgumentException e) {
            write(response,ApiResponse.fail(400,e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String,String> body = JsonUtil.fromJson(request.getReader(),Map.class);
        User user = authService.login(body.get("username"), body.get("password"));
        if(user == null){
            write(response, ApiResponse.fail(401,"用户名或密码错误"));
            return;
        }
        //登陆后把user写入Session
        HttpSession session = request.getSession(true);
        session.setAttribute("user",user);
        write(response,ApiResponse.ok(user));
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if(session != null){
            session.invalidate();
        }
        write(response,ApiResponse.ok());
    }

    private void me(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object u = session == null ? null : session.getAttribute("user");
        if(u == null){
            write(response, ApiResponse.fail(401,"未登录"));
            return;
        }
        write(response, ApiResponse.ok(u));
    }

    private void write(HttpServletResponse response, Object body) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        try(PrintWriter pw = response.getWriter()){
            pw.write(JsonUtil.toJson(body));
        }
    }
}
