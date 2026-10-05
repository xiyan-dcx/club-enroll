package com.club.filter;

import com.club.entity.User;
import com.club.util.ApiResponse;
import com.club.util.JsonUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * 登录拦截 + 角色校验。
 *
 * 拦截规则：
 *   - 拦所有 /api/* 请求
 *   - 白名单（放过）：/api/auth/login、/api/auth/register
 *   - 没登录 → 401
 *   - admin 专属接口（POST/PUT/DELETE 社团、待审、审批、公告发布、统计） → role=admin
 *   - student 专属接口（报名、我的报名） → role=student
 */
@WebFilter(filterName = "LoginCheckFilter", urlPatterns = "/api/*")
public class LoginCheckFilter implements Filter {

    private static final Set<String> WHITELIST = Set.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        //1.去掉 context path（/club）后的业务URI
        String uri  = req.getRequestURI().substring(req.getContextPath().length());

        //2.白名单直接放行
        if (WHITELIST.contains(uri)) {
            chain.doFilter(request, response);
            return;
        }

        //3.校验登录态
        HttpSession  session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            write(resp, 401, ApiResponse.fail(401,"未登录"));
            return;
        }

        //4.admin接口校验
        if (isAdminOnly(uri,req.getMethod())) {
            if (!User.ROLE_ADMIN.equals(user.getRole())) {
                write(resp, 403, ApiResponse.fail(403, "无管理员权限"));
                return;
            }
        }

        //5.student接口校验
        if (isStudentOnly(uri,req.getMethod())) {
            if (!User.ROLE_STUDENT.equals(user.getRole())) {
                write(resp, 403, ApiResponse.fail(403,"学生专属接口"));
                return;
            }
        }

        chain.doFilter(request, response);


    }

    private boolean isAdminOnly(String uri,String method) {
        // POST/PUT/DELETE 到 /api/club 是管理员维护；GET 列表/详情学生可看
        if (uri.startsWith("/api/club") && !uri.equals("/api/auth/login") && !uri.equals("/api/club/list") && !uri.equals("/api/club/detail")) {
            return !"GET".equalsIgnoreCase(method);
        }
        // 待审 + 审批
        if (uri.equals("/api/enrollment/pending")) return true;
        if (uri.equals("/api/enrollment/review"))  return true;
        // 发布公告（POST /api/notice，注意没有 pathInfo）
        if (uri.equals("/api/notice") && !"GET".equalsIgnoreCase(method)) return true;
        // 仪表盘
        if (uri.equals("/api/stat/dashboard")) return true;
        return false;
    }

    private boolean isStudentOnly(String uri, String method) {
        if (uri.equals("/api/enrollment/apply")) return true;
        if (uri.equals("/api/enrollment/mine"))  return true;
        return false;
    }

    private void write(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(JsonUtil.toJson(body));
        }
    }
}
