# Postman 并发测试与缓存对照

本说明对应当前版本：报名和预约直接提交学号、姓名，无独立身份审核。业务站点端口 18080；下面只对端口 18082 的隔离压测实例操作。MySQL、Redis、Kafka 需已启动。

## 1. 准备环境

在项目根目录打开 PowerShell：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-benchmark.ps1 -Mode baseline
```

首次自动创建独立测试库、100 场活动、40 个学生账号及会话。后续保留同一数据库，只重启 18082 并重新生成会话。该脚本不停止 18080，不清空业务数据。会话过期或切换模式后需重新导入生成的环境及数据文件。

打开 Postman 桌面版，Import 导入：

1. `scripts/postman/qingju.collection.json`：请求集合，可提交 Git。
2. `.local/postman/qingju.environment.json`：当前会话与压测地址，不提交 Git。

右上角选择「青聚本地压测」环境。初次可手动发送「01 缓存对照 / 活动列表」，应 HTTP 200、业务 code=0。

Postman 本地性能测试路径：集合或文件夹 → Run → Performance → App。不要选择 Cloud 执行，本地地址无法从云端访问。普通 Functional Runner 的 Iterations 是顺序循环，不代表并发人数。当前版本可能需要登录并进入 Cloud View 才显示 Performance，但实际负载仍选择 App 发出。

## 2. 缓存 A/B 对照：衡量优化效果

只选择「01 缓存对照」文件夹，避免将登录、准备、写入及检查请求混入压力循环。

1. 先进行 1 分钟预热，不计入正式结果；两种模式都预热。
2. 手动发送「03 / A 记录压测前计数」。
3. 对「01」Run → Performance → App，Load profile=Fixed，Virtual users=10，Duration=1 分钟，Start run。
4. 保存 Postman 结果中的请求数、Requests/sec、平均响应时间、P95、P99、HTTP 状态分布和断言失败数；有报告导出入口时导出，否则保存结果页截图。Error % 主要反映运行时错误，不能代替 HTTP/业务成功率。
5. 手动发送「03 / B 查看本轮回源统计」，在响应 Visualize 视图查看本轮缓存入口请求数、数据库回源次数及比例。原始累计值可在 Body 中查看，计数重启归零。
6. 用同样步骤跑 20、40 VU。每个档位做 3 次，保留所有结果并取中位数。没有足够资源时降低档位，记录实际设置。
7. 切换开启缓存：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-benchmark.ps1 -Mode optimized
```

重新导入 `.local/postman/qingju.environment.json` 替换环境值。重复预热、A、正式测试、B，保持 VU、时长、电脑和数据一致。测试期间不要操作压测站点、发布活动或运行另一组测试。更严谨时按 baseline/optimized/optimized/baseline 顺序交替测试，降低机器温度、JIT 等顺序影响。

| 模式 | VU | 时长 | HTTP 200 比例 | RPS | 平均 ms | P95 ms | P99 ms | 回源次数 | 回源比例 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| baseline | 10 | 60s | 实测 | 实测 | 实测 | 实测 | 实测 | 实测 | 实测 |
| optimized | 10 | 60s | 实测 | 实测 | 实测 | 实测 | 实测 | 实测 | 实测 |

P95 降幅 = (基线 P95 − 开启缓存 P95) / 基线 P95 × 100%。回源比例 = 本轮回源次数 / 本轮缓存入口请求数。此处回源计数是缓存 loader 调用次数，不是全系统 SQL 条数，也不是严格的缓存命中率（请求可能被回源并发限制拒绝）。比较时必须同时报告 503、超时与断言失败；不能把大量快速失败计为成功吞吐提升。

这个实验只隔离验证两级缓存效果，不能证明 Kafka、Lua 或整体系统提升了同样比例。小数据集、本机网络与客户端瓶颈可能使延迟改善不明显，按结果如实记录。

## 3. 名额竞争：验证正确性

1. 保持 optimized。先手动发送「03 / E 新建下一轮竞争场次」，自动保存新的 activityId，容量为 10。每轮都新建，不清空旧数据。
2. 对「02 名额竞争」文件夹 Run → Performance → App。
3. Iteration data 选择 **Datafiles**，上传 `.local/postman/users.json`，预览确认 40 行，包含不同 studentNo、sessionCookie。不要把所有 VU 配成一个登录账号，也不要把 adminCookie 当学生 cookie。
4. Fixed，40 VU，1 分钟。Datafiles 的 VU data mapping 选 Ordered；若使用 Datasets 则选 Fixed。VU 不超过行数。集合已禁用自动 Cookie Jar，显式使用每行会话，避免账号串用。
5. 运行后发送「03 / C 检查预约人数及学号唯一性」，看 Tests 两个断言和 Visualize 名单，预期 10 条有效预约、10 个不同学号。HTTP 200 次数可能远大于 10，因为同一用户重复请求会返回既有预约；不要把响应次数当新增预约数。
6. 请求可能返回 400（已满或处理中）、429（用户滑动窗口限流）。检查返回 message，只有预期的业务拒绝才合理；401/403、500、超时或其他 400 原因需要排查。Postman 的 Error % 不能代替这项分类。
7. 「03 / D」查看 Outbox 和预占请求状态，等待后台处理后应逐渐收敛到 SENT、CLEANED；它显示测试库累计状态，不只属于当前一轮。

循环压力中同一用户每 10 秒最多 60 次写请求，达到限制会出现 429，这是入口保护测试的一部分。本实验不能用来计算“成功新增预约 QPS”，因为名额满后主要测到幂等返回和业务拒绝。

### 无付费 Datafiles 功能时

当前 Postman 文档将性能测试中的 Datafiles/Datasets 标为付费能力。缓存对照不依赖该能力。名额竞争可先手动发送 E 新建空场次，再手动发送「04 / 发送一批并发预约」：脚本读取环境中的 40 个独立学生会话，通过 pm.sendRequest 同时发出一批预约，在 Visualize 中展示每人的 HTTP 状态、客户端耗时和单批 P95，Tests 预期 10 成功、30 业务拒绝。最后发送 C 验证数据库名单。每轮新建场次；重复发送同场次会测到幂等结果。这是单批并发正确性检查，不是 Performance Runner 的持续吞吐测试，不能将它的外层 GET 响应时间当成预约耗时。

## 4. 停止与排查

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-benchmark.ps1
```

日志：`.local/logs/benchmark.out`、`benchmark.err`。收到 401 时先运行 `node scripts/prepare-postman.mjs` 刷新会话，再重新导入环境和 users.json。切换模式也会使旧会话失效。测试库保留用于复查；`.local` 下会话、日志和数据库配置不上传 GitHub。

如果 Postman 不提供 Performance/App，先确认使用桌面版、登录和当前界面模式；仅有 Functional Runner 时不能把串行结果当并发结果。

官方参考：
- https://learning.postman.com/docs/tests-and-scripts/performance-testing/performance-test-configuration
- https://learning.postman.com/docs/tests-and-scripts/performance-testing/test-data/performance-test-data-files/
- https://learning.postman.com/docs/tests-and-scripts/performance-testing/test-data/performance-test-dataset
