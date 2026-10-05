package com.club.listener;

import com.club.util.DbUtil;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

/**
 * Web 应用启动 / 销毁钩子：
 *   启动：确认 HikariCP 连接池就绪、打印 DB 信息
 *   销毁：归还连接池 + 关 MySQL 清理线程 + 注销 JDBC Driver,
 *        防止 Tomcat 热部署累积 ClassLoader 泄漏
 */
@WebListener
public class StartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("============================================");
        System.out.println("  社团报名通 · 后端启动中");
        System.out.println("  Context Path = " + sce.getServletContext().getContextPath());
        // 关键:catch Throwable 而不是 Exception。
        //   DbUtil 静态块连数据库失败时会抛 ExceptionInInitializerError(Error 类型),
        //   只 catch Exception 抓不住,异常上抛 listener 启动失败 webapp 直接挂。
        try (Connection conn = DbUtil.getConnection()) {
            System.out.println("  HikariCP 连接池就绪,DB = " + conn.getCatalog());
        } catch (Throwable t) {
            System.err.println("  [警告] 数据库连接失败,webapp 继续启动以方便排查");
            System.err.println("  原因:" + t.getClass().getSimpleName() + " - " + t.getMessage());
            System.err.println("  排查步骤:");
            System.err.println("    1) 确认本地 MySQL 已启动 (默认 3306 端口)");
            System.err.println("    2) 在 MySQL 中执行 sql/init.sql 建库 + 建表 + 灌测试数据");
            System.err.println("    3) 改 src/main/resources/db.properties 里的 jdbc.password");
            t.printStackTrace();
        }
        System.out.println("============================================");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("社团报名通 · 后端关闭,归还 HikariCP 连接池");
        DbUtil.close();

        // 1. 显式关闭 MySQL Connector/J 的后台清理线程
        //    否则 Tomcat 卸载 webapp 后这个守护线程仍持有 webapp ClassLoader
        //    引发"非法访问:此Web应用程序实例已停止"警告 + Metaspace 泄漏
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable t) {
            System.err.println("关闭 MySQL 清理线程失败:" + t.getMessage());
        }

        // 2. 注销当前 webapp ClassLoader 加载的 JDBC Driver
        //    DriverManager 是 boot ClassLoader 持有的全局表,
        //    webapp 加载的 Driver 不注销会导致整个 webapp ClassLoader 无法 GC
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                try {
                    DriverManager.deregisterDriver(driver);
                    System.out.println("已注销 JDBC Driver: " + driver.getClass().getName());
                } catch (Exception ignored) {
                }
            }
        }
    }
}
