package com.club;

import com.club.util.DbUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;


public class Main {
    public static void main(String[] args) {
        Connection conn = null;
        try {
            // 1. 触发 HikariCP 懒加载，真正尝试建立连接
            System.out.println("[1] 调用 DbUtil.getConnection() ...");
            conn = DbUtil.getConnection();

            // 2. 拿到连接后输出基本信息
            System.out.println("[2] 连接对象: " + conn);
            System.out.println("[3] 连接是否关闭: " + conn.isClosed());

            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("[4] 数据库产品名: " + meta.getDatabaseProductName());
            System.out.println("[5] 数据库版本: " + meta.getDatabaseProductVersion());
            System.out.println("[6] JDBC 驱动名: " + meta.getDriverName());
            System.out.println("[7] 连接 URL: " + meta.getURL());
            System.out.println("[8] 当前用户名: " + meta.getUserName());

            System.out.println("\n========== 验证通过：DbUtil 可以正常获取连接！ ==========");

        } catch (IllegalStateException e) {
            System.err.println("========== 验证失败：db.properties 未找到 ==========");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("========== 验证失败：数据库连接出错 ==========");
            System.err.println("SQLState:  " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
            System.err.println("Message:   " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("========== 验证失败：未知异常 ==========");
            e.printStackTrace();
        } finally {
            // 3. 归还连接到连接池
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("\n[9] 连接已归还到连接池（close）。");
                } catch (SQLException e) {
                    System.err.println("关闭连接时出错: " + e.getMessage());
                }
            }
            // 4. 关闭整个连接池
            DbUtil.close();
            System.out.println("[10] 连接池已关闭。");
        }
    }
}
