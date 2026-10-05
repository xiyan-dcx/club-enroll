# 本机 Tomcat 部署

部署日期：2026-10-05。

- Tomcat：`D:\Daicx\实习简历\实习项目\.runtime\apache-tomcat-10.1.60`
- JDK：21.0.5，安装位置 `D:\`；Tomcat 的 `bin\setenv.bat` 已指定该 JDK。
- Maven：3.9.9。系统 JAVA_HOME 仍为 Java 11，构建时需在当前 PowerShell 会话指定 Java 21。
- 数据库：现有 MySQL 9.6.0 / club_enroll，连接验证通过。
- WAR：`backend\target\club-enroll.war`，已复制到 Tomcat 的 `webapps` 目录。
- 检查地址：http://localhost:8080/club-enroll/api/auth/me

未登录时返回 `{"code":401,"message":"未登录"}` 为正常业务响应。项目是后端 API，未配置首页，访问 `/club-enroll/` 可能返回 404。

## 启动与停止

在 PowerShell 中执行（已启动时无需再次启动）：

```powershell
& 'D:\Daicx\实习简历\实习项目\.runtime\apache-tomcat-10.1.60\bin\startup.bat'
```

停止：

```powershell
& 'D:\Daicx\实习简历\实习项目\.runtime\apache-tomcat-10.1.60\bin\shutdown.bat'
```

当前部署以后台进程运行，未注册 Windows 服务，重启电脑后需手动启动。首次启动日志在 Tomcat 的 `logs\console.err.log`。

## 重新打包和部署

在 backend 目录执行：

```powershell
$env:JAVA_HOME = 'D:\'
mvn package
if ($LASTEXITCODE -ne 0) { throw '打包失败，请勿部署旧 WAR' }
Copy-Item '.\target\club-enroll.war' 'D:\Daicx\实习简历\实习项目\.runtime\apache-tomcat-10.1.60\webapps\club-enroll.war' -Force
```

Tomcat 默认自动重新部署更新后的 WAR；重新部署会使原有登录会话失效。

此次删除了 UserDao.java 中误导入的 JDK 内部类静态导入，解决编译失败。项目没有自动化测试；已验证构建、Tomcat 部署、数据库连接及接口响应。未执行含 DROP DATABASE 的数据库初始化脚本。
