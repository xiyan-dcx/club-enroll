package com.club.controller;
import org.springframework.web.bind.annotation.*;

import com.club.dao.EnrollmentDao;
import com.club.dao.NoticeDao;
import com.club.dao.UserDao;
import com.club.service.ClubService;
import com.club.util.ApiResponse;
import com.club.util.JsonUtil;



import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/** 管理员仪表盘:4 张统计卡片(社团 / 学生 / 待审 / 公告) */
@RestController
@RequestMapping("/api/stat")
public class StatController {

    @org.springframework.beans.factory.annotation.Autowired private ClubService clubService;
    private final UserDao userDao = new UserDao();
    private final EnrollmentDao enrollmentDao = new EnrollmentDao();
    private final NoticeDao noticeDao = new NoticeDao();

    
    @GetMapping({"","/**"})
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> data = new HashMap<>();
        // 4 个核心字段,字段名跟前端 Dashboard.vue 对齐
        data.put("clubCount",    clubService.countAll());
        data.put("studentCount", userDao.countByRole("student"));
        data.put("pendingCount", enrollmentDao.countPending());
        data.put("noticeCount",  noticeDao.countAll());
        // 保留扩展统计:今日新报 + 本月通过(老前端兼容)
        data.put("todayNew",      enrollmentDao.countTodayNew());
        data.put("monthlyPassed", enrollmentDao.countMonthlyPassed());

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(JsonUtil.toJson(ApiResponse.ok(data)));
        }
    }
}
