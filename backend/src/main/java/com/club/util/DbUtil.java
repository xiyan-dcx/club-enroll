package com.club.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;


/**
 * 数据库连接工具类：使用 HikariCP 起连接池，从 classpath 下 db.properties 读取配置。
 * 全局唯一获取 Connection 的入口。
 */
public class DbUtil {

    private static HikariDataSource DATA_SOURCE = new HikariDataSource();

    static {
        Properties props = new Properties();
        try(InputStream in =DbUtil.class.getClassLoader().getResourceAsStream("db.properties")){
            if(in == null){
                throw new IllegalArgumentException("未找到 db.properties，请放在 src/main/resources");
            }
            props.load(in);
        }catch (IOException e){
            throw new RuntimeException("读取 db.properties 失败", e);
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getProperty("jdbc.url"));
        cfg.setUsername(props.getProperty("jdbc.username"));
        cfg.setPassword(props.getProperty("jdbc.password"));
        cfg.setDriverClassName(props.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"));

        cfg.setMaximumPoolSize(parseInt(props.getProperty("hikari.maximumPoolSize"),10));
        cfg.setMinimumIdle(parseInt(props.getProperty("hikari.minimumIdle"),2));
        cfg.setConnectionTimeout(parseLong(props.getProperty("hikari.connectionTimeout"), 3_000));
        cfg.setIdleTimeout(parseLong(props.getProperty("hikari.idleTimeout"), 600_000));
        cfg.setMaxLifetime(parseLong(props.getProperty("hikari.maxLifetime"), 1_800_000));
        cfg.setPoolName(props.getProperty("hikari.poolName","ClubSystemHikariPool"));

        // 关键:不让 Hikari 在启动时强制建连接。
        //   默认值 1 ms:构造时立即建连接,连不上直接抛 PoolInitializationException
        //   静态块抛运行时异常会被 JVM 包成 ExceptionInInitializerError(Error 类型),
        //   listener 抓 Exception 抓不住 → 整个 webapp 启动失败。
        //   设为 -1:连接懒加载,首次调用 getConnection() 才尝试,即使 DB 还没建表 webapp 也能起来。
        cfg.setInitializationFailTimeout(-1);

        DATA_SOURCE = new HikariDataSource(cfg);
    }

    /** 从连接池取一个连接；调用方负责 close（HikariCP 会归还到池）。 */
    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    /** 应用关闭时调用，归还所有资源。 */
    public static void close() {
        if(DATA_SOURCE != null && !DATA_SOURCE.isClosed()){
            DATA_SOURCE.close();
        }
    }

    private static int parseInt(String v, int defaultVal) {  //parseInt("18",0)
        try { return v == null ? defaultVal : Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private static long parseLong(String v, long defaultVal) {
        try { return v == null ? defaultVal : Long.parseLong(v.trim()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private DbUtil() {}
}
