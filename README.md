# 校园社团报名通


- 后端：Tomcat 10 + Servlet 5 + JDBC + HikariCP 5 + Gson 2.10（JDK 21）
- 前端：Vue 3 + Vite 5 + Element Plus 2 + Vue Router 4 + axios 1.6（Node 20）
- 数据库：MySQL 8（utf8mb4）

跑起来：

\`\`\`bash
# 后端
cd backend && mvn clean package
# 把 target/club-enroll.war 部署到 Tomcat 10
# 前端
cd frontend && npm install && npm run dev
# 浏览器开 http://localhost:5173
\`\`\`
