import quickfix.*;
import quickfix.field.TestReqID;
import quickfix.fix44.Heartbeat;
import quickfix.fix44.LogonReject;
import quickfix.fix44.TestRequest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * FIX系统TestRequest消息压测工具（基于QuickFIX/J）
 * 功能：多会话并发发送、完整指标统计（延时、错误率、重连率等）、异常处理
 */
public class FixBenchmarkTool {

    // ==================== 数据模型与枚举 ====================
    enum MessageStatus {
        SENT, RECEIVED, TIMEOUT, SEND_FAILED
    }

    enum ErrorReason {
        SEND_EXCEPTION("发送时抛出异常"),
        SESSION_NOT_LOGGED_IN("会话未登录"),
        TIMEOUT("超时未收到回复"),
        ENGINE_REJECT("引擎拒签（如序列号错误）"),
        LOGON_REJECT("登录被拒"),
        UNKNOWN("未知错误");

        private final String desc;
        ErrorReason(String desc) { this.desc = desc; }
        public String getDesc() { return desc; }
    }

    static class TestRequestTrack {
        private final String testReqID;
        private final Instant sendTime;
        private Instant receiveTime;
        private MessageStatus status;
        private ErrorReason errorReason;

        public TestRequestTrack(String testReqID, Instant sendTime) {
            this.testReqID = testReqID;
            this.sendTime = sendTime;
            this.status = MessageStatus.SENT;
        }

        public String getTestReqID() { return testReqID; }
        public Instant getSendTime() { return sendTime; }
        public Instant getReceiveTime() { return receiveTime; }
        public void setReceiveTime(Instant receiveTime) { this.receiveTime = receiveTime; }
        public MessageStatus getStatus() { return status; }
        public void setStatus(MessageStatus status) { this.status = status; }
        public ErrorReason getErrorReason() { return errorReason; }
        public void setErrorReason(ErrorReason errorReason) { this.errorReason = errorReason; }
    }

    static class SessionReconnectStats {
        private boolean firstLoginSuccess = false;
        private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
        private final AtomicInteger reconnectSuccess = new AtomicInteger(0);
        private final String sessionID;

        public SessionReconnectStats(String sessionID) { this.sessionID = sessionID; }

        public void markFirstLoginSuccess() { this.firstLoginSuccess = true; }
        public void incrementReconnectAttempt() { reconnectAttempts.incrementAndGet(); }
        public void incrementReconnectSuccess() { reconnectSuccess.incrementAndGet(); }
        public double getReconnectRate() {
            int attempts = reconnectAttempts.get();
            return attempts == 0 ? 0 : (double) reconnectSuccess.get() / attempts * 100;
        }
        public int getReconnectAttempts() { return reconnectAttempts.get(); }
        public int getReconnectSuccess() { return reconnectSuccess.get(); }
        public boolean isFirstLoginSuccess() { return firstLoginSuccess; }
        public String getSessionID() { return sessionID; }
    }

    // ==================== 指标收集器 ====================
    static class MetricsCollector {
        private final ConcurrentHashMap<String, ConcurrentHashMap<String, TestRequestTrack>> sessionTracks = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, SessionReconnectStats> sessionReconnectStats = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<ErrorReason, AtomicInteger> errorReasonCounts = new ConcurrentHashMap<>();
        private final Set<String> abnormalSessions = ConcurrentHashMap.newKeySet();

        public MetricsCollector() {
            Arrays.stream(ErrorReason.values()).forEach(reason ->
                    errorReasonCounts.putIfAbsent(reason, new AtomicInteger(0)));
        }

        public void initSession(String sessionID) {
            sessionTracks.putIfAbsent(sessionID, new ConcurrentHashMap<>());
            sessionReconnectStats.putIfAbsent(sessionID, new SessionReconnectStats(sessionID));
        }

        public void markFirstLoginSuccess(String sessionID) {
            sessionReconnectStats.get(sessionID).markFirstLoginSuccess();
        }

        public void markReconnectAttempt(String sessionID) {
            sessionReconnectStats.get(sessionID).incrementReconnectAttempt();
        }

        public void markReconnectSuccess(String sessionID) {
            sessionReconnectStats.get(sessionID).incrementReconnectSuccess();
        }

        public void trackSent(String sessionID, TestRequestTrack track) {
            sessionTracks.get(sessionID).put(track.getTestReqID(), track);
        }

        public void trackReceived(String sessionID, String testReqID, Instant receiveTime) {
            TestRequestTrack track = sessionTracks.get(sessionID).get(testReqID);
            if (track != null) {
                track.setReceiveTime(receiveTime);
                track.setStatus(MessageStatus.RECEIVED);
            }
        }

        public void trackSendFailed(String sessionID, String testReqID, ErrorReason reason) {
            TestRequestTrack track = sessionTracks.get(sessionID).get(testReqID);
            if (track != null) {
                track.setStatus(MessageStatus.SEND_FAILED);
                track.setErrorReason(reason);
            }
            errorReasonCounts.get(reason).incrementAndGet();
        }

        public void checkTimeouts(long timeoutMillis) {
            Instant now = Instant.now();
            sessionTracks.values().forEach(trackMap -> {
                trackMap.values().forEach(track -> {
                    if (track.getStatus() == MessageStatus.SENT) {
                        long duration = java.time.Duration.between(track.getSendTime(), now).toMillis();
                        if (duration > timeoutMillis) {
                            track.setStatus(MessageStatus.TIMEOUT);
                            track.setErrorReason(ErrorReason.TIMEOUT);
                            errorReasonCounts.get(ErrorReason.TIMEOUT).incrementAndGet();
                        }
                    }
                });
            });
        }

        public void markAbnormalSession(String sessionID) {
            abnormalSessions.add(sessionID);
        }

        public String generateSummaryReport() {
            StringBuilder report = new StringBuilder("\n===== 全局汇总报告 =====");
            int totalSessions = sessionReconnectStats.size();
            int abnormalCount = abnormalSessions.size();
            int validSessions = totalSessions - abnormalCount;

            // 会话建立与重连统计
            int firstLoginSuccess = (int) sessionReconnectStats.values().stream()
                    .filter(stat -> !abnormalSessions.contains(stat.getSessionID()))
                    .filter(SessionReconnectStats::isFirstLoginSuccess)
                    .count();
            double firstLoginRate = validSessions == 0 ? 0 : (double) firstLoginSuccess / validSessions * 100;

            int totalReconnectAttempts = sessionReconnectStats.values().stream()
                    .filter(stat -> !abnormalSessions.contains(stat.getSessionID()))
                    .mapToInt(SessionReconnectStats::getReconnectAttempts)
                    .sum();
            int totalReconnectSuccess = sessionReconnectStats.values().stream()
                    .filter(stat -> !abnormalSessions.contains(stat.getSessionID()))
                    .mapToInt(SessionReconnectStats::getReconnectSuccess)
                    .sum();
            double globalReconnectRate = totalReconnectAttempts == 0 ? 0 :
                    (double) totalReconnectSuccess / totalReconnectAttempts * 100;

            report.append("\n会话总数: ").append(totalSessions)
                    .append("，异常会话数: ").append(abnormalCount)
                    .append("\n首次连接成功率: ").append(String.format("%.2f", firstLoginRate)).append("%")
                    .append("\n总重连尝试次数: ").append(totalReconnectAttempts)
                    .append("，总重连成功次数: ").append(totalReconnectSuccess)
                    .append("\n全局重连率: ").append(String.format("%.2f", globalReconnectRate)).append("%");

            // 消息错误统计
            int totalSent = sessionTracks.values().stream()
                    .filter(map -> !abnormalSessions.contains(getSessionIdFromMap(map)))
                    .mapToInt(ConcurrentHashMap::size)
                    .sum();
            int totalFailed = errorReasonCounts.values().stream().mapToInt(AtomicInteger::get).sum();
            double errorRate = totalSent == 0 ? 0 : (double) totalFailed / totalSent * 100;

            report.append("\n\n消息总发送数: ").append(totalSent)
                    .append("，总失败数: ").append(totalFailed)
                    .append("，总错误率: ").append(String.format("%.4f", errorRate)).append("%")
                    .append("\n错误原因分布:");
            errorReasonCounts.forEach((reason, count) -> {
                if (count.get() > 0) {
                    report.append("\n  - ").append(reason.getDesc()).append(": ").append(count.get()).append("次");
                }
            });

            // 延时统计
            List<Long> latencies = new ArrayList<>();
            sessionTracks.forEach((sessionID, trackMap) -> {
                if (abnormalSessions.contains(sessionID)) return;
                trackMap.values().forEach(track -> {
                    if (track.getStatus() == MessageStatus.RECEIVED) {
                        long latency = java.time.Duration.between(track.getSendTime(), track.getReceiveTime()).toMillis();
                        latencies.add(latency);
                    }
                });
            });
            if (!latencies.isEmpty()) {
                latencies.sort(Long::compare);
                long avg = (long) latencies.stream().mapToLong(l -> l).average().orElse(0);
                long p50 = latencies.get(latencies.size() / 2);
                long p90 = latencies.get((int) (latencies.size() * 0.9));
                long p99 = latencies.get((int) (latencies.size() * 0.99));
                report.append("\n\n消息延时统计:")
                        .append("\n  平均延时: ").append(avg).append("ms")
                        .append("\n  P50延时: ").append(p50).append("ms")
                        .append("\n  P90延时: ").append(p90).append("ms")
                        .append("\n  P99延时: ").append(p99).append("ms");
            }

            return report.toString();
        }

        private String getSessionIdFromMap(ConcurrentHashMap<String, TestRequestTrack> map) {
            return map.isEmpty() ? "" : map.keySet().iterator().next().split("-")[1];
        }

        public String generatePerSessionReport() {
            StringBuilder report = new StringBuilder("\n===== 单会话详细报告 =====");
            sessionReconnectStats.forEach((sessionID, stats) -> {
                if (abnormalSessions.contains(sessionID)) {
                    report.append("\n\n会话ID: ").append(sessionID).append("（异常会话，未参与压测）");
                    return;
                }

                report.append("\n\n会话ID: ").append(sessionID)
                        .append("\n  首次连接: ").append(stats.isFirstLoginSuccess() ? "成功" : "失败")
                        .append("\n  重连尝试: ").append(stats.getReconnectAttempts())
                        .append("次，成功: ").append(stats.getReconnectSuccess())
                        .append("次，重连率: ").append(String.format("%.2f", stats.getReconnectRate())).append("%");

                ConcurrentHashMap<String, TestRequestTrack> trackMap = sessionTracks.get(sessionID);
                if (trackMap == null) return;

                int sent = trackMap.size();
                int received = 0;
                Map<ErrorReason, Integer> sessionErrors = new HashMap<>();
                for (TestRequestTrack track : trackMap.values()) {
                    if (track.getStatus() == MessageStatus.RECEIVED) {
                        received++;
                    } else if (track.getErrorReason() != null) {
                        sessionErrors.put(track.getErrorReason(),
                                sessionErrors.getOrDefault(track.getErrorReason(), 0) + 1);
                    }
                }

                report.append("\n  消息发送总数: ").append(sent)
                        .append("，成功接收: ").append(received)
                        .append("，失败数: ").append(sessionErrors.values().stream().mapToInt(i -> i).sum());
                if (!sessionErrors.isEmpty()) {
                    report.append("\n  错误原因:");
                    sessionErrors.forEach((reason, count) ->
                            report.append("\n    - ").append(reason.getDesc()).append(": ").append(count).append("次")
                    );
                }
            });
            return report.toString();
        }
    }

    // ==================== 多连接客户端应用 ====================
    static class FixBenchmarkApplication implements Application {
        private final MetricsCollector metrics;
        private final int tpsPerSession;
        private final long durationMinutes;
        private final long timeoutMillis;
        private final ScheduledExecutorService scheduler;
        private final CountDownLatch allSessionsStarted;
        private final ConcurrentHashMap<String, Boolean> isFirstLogin = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Integer> retryCount = new ConcurrentHashMap<>();
        private final int maxRetry;
        private final ConcurrentHashMap<String, ScheduledFuture<?>> sendFutures = new ConcurrentHashMap<>();

        public FixBenchmarkApplication(MetricsCollector metrics, int tpsPerSession, long durationMinutes,
                                      long timeoutMillis, int sessionCount, int maxRetry) {
            this.metrics = metrics;
            this.tpsPerSession = tpsPerSession;
            this.durationMinutes = durationMinutes;
            this.timeoutMillis = timeoutMillis;
            this.maxRetry = maxRetry;
            this.allSessionsStarted = new CountDownLatch(sessionCount);
            this.scheduler = new ScheduledThreadPoolExecutor(
                    sessionCount,
                    r -> {
                        Thread t = new Thread(r, "session-worker-" + UUID.randomUUID().toString().substring(0, 4));
                        t.setDaemon(true);
                        return t;
                    }
            );

            scheduler.scheduleAtFixedRate(() -> metrics.checkTimeouts(timeoutMillis), 0, 1, TimeUnit.SECONDS);
            startLoginMonitor();
        }

        private void startLoginMonitor() {
            scheduler.scheduleAtFixedRate(() -> {
                for (SessionID sessionID : Session.getAllSessions()) {
                    String sessionKey = sessionID.toString();
                    if (metrics.abnormalSessions.contains(sessionKey)) continue;

                    if (!Session.lookupSession(sessionID).isLoggedOn()) {
                        int retries = retryCount.getOrDefault(sessionKey, 0);
                        if (retries < maxRetry) {
                            try {
                                Session.lookupSession(sessionID).logon();
                                retryCount.put(sessionKey, retries + 1);
                                System.out.printf("会话[%s]第%d次重试登录%n", sessionKey, retries + 1);
                            } catch (Exception e) {
                                System.err.printf("会话[%s]重试登录失败: %s%n", sessionKey, e.getMessage());
                            }
                        } else {
                            metrics.markAbnormalSession(sessionKey);
                            allSessionsStarted.countDown(); // 异常会话也减少计数器，避免阻塞
                            System.err.printf("会话[%s]超过最大重试次数(%d次)，标记为异常%n", sessionKey, maxRetry);
                        }
                    }
                }
            }, 10, 5, TimeUnit.SECONDS);
        }

        @Override
        public void onCreate(SessionID sessionID) {
            String sessionKey = sessionID.toString();
            metrics.initSession(sessionKey);
            isFirstLogin.put(sessionKey, true);
            retryCount.putIfAbsent(sessionKey, 0);
        }

        @Override
        public void onLogon(SessionID sessionID) {
            String sessionKey = sessionID.toString();
            if (metrics.abnormalSessions.contains(sessionKey)) return;

            boolean firstLogin = isFirstLogin.getOrDefault(sessionKey, true);
            if (firstLogin) {
                metrics.markFirstLoginSuccess(sessionKey);
                isFirstLogin.put(sessionKey, false);
                System.out.printf("会话[%s]首次登录成功%n", sessionKey);
            } else {
                metrics.markReconnectSuccess(sessionKey);
                System.out.printf("会话[%s]重连成功%n", sessionKey);
            }

            startSendTask(sessionID);
            allSessionsStarted.countDown();
        }

        @Override
        public void onLogout(SessionID sessionID) {
            String sessionKey = sessionID.toString();
            if (metrics.sessionReconnectStats.get(sessionKey).isFirstLoginSuccess()
                    && !metrics.abnormalSessions.contains(sessionKey)) {
                metrics.markReconnectAttempt(sessionKey);
                System.out.printf("会话[%s]断开连接，触发重连（累计尝试：%d次）%n",
                        sessionKey, metrics.sessionReconnectStats.get(sessionKey).getReconnectAttempts() + 1);
            }
        }

        @Override
        public void toAdmin(Message message, SessionID sessionID) {}

        @Override
        public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
            if (message instanceof LogonReject) {
                LogonReject reject = (LogonReject) message;
                String reason = reject.isSetField(quickfix.field.Text.FIELD) ?
                        reject.getText().getValue() : "未知原因";
                String sessionKey = sessionID.toString();
                System.err.printf("会话[%s]登录被拒: %s%n", sessionKey, reason);
                metrics.trackSendFailed(sessionKey, "LOGON-REJECT", ErrorReason.LOGON_REJECT);
            } else if (message instanceof Reject) {
                Reject reject = (Reject) message;
                String testReqID = reject.isSetField(TestReqID.FIELD) ?
                        reject.getTestReqID().getValue() : "UNKNOWN";
                metrics.trackSendFailed(sessionID.toString(), testReqID, ErrorReason.ENGINE_REJECT);
            }
        }

        @Override
        public void toApp(Message message, SessionID sessionID) throws DoNotSend {}

        @Override
        public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
            if (message instanceof Heartbeat && message.isSetField(TestReqID.FIELD)) {
                Heartbeat heartbeat = (Heartbeat) message;
                String testReqID = heartbeat.getTestReqID().getValue();
                metrics.trackReceived(sessionID.toString(), testReqID, Instant.now());
            }
        }

        private void startSendTask(SessionID sessionID) {
            String sessionKey = sessionID.toString();
            AtomicLong seq = new AtomicLong(0);
            long intervalMs = tpsPerSession > 0 ? 1000 / tpsPerSession : 0;

            // 取消之前的发送任务（重连后重启）
            if (sendFutures.containsKey(sessionKey)) {
                sendFutures.get(sessionKey).cancel(true);
            }

            Runnable sendTask = () -> {
                try {
                    Session session = Session.lookupSession(sessionID);
                    if (session == null || !session.isLoggedOn()) {
                        String testReqID = "TEST-" + sessionKey + "-" + seq.getAndIncrement();
                        metrics.trackSendFailed(sessionKey, testReqID, ErrorReason.SESSION_NOT_LOGGED_IN);
                        return;
                    }

                    String testReqID = "TEST-" + sessionKey + "-" +
                            UUID.randomUUID().toString().substring(0, 8) + "-" + seq.getAndIncrement();
                    TestRequest request = new TestRequest();
                    request.set(new TestReqID(testReqID));
                    metrics.trackSent(sessionKey, new TestRequestTrack(testReqID, Instant.now()));

                    boolean sent = Session.sendToTarget(request, sessionID);
                    if (!sent) {
                        metrics.trackSendFailed(sessionKey, testReqID, ErrorReason.SEND_EXCEPTION);
                    }
                } catch (Exception e) {
                    String testReqID = "TEST-" + sessionKey + "-" + seq.getAndIncrement();
                    metrics.trackSendFailed(sessionKey, testReqID, ErrorReason.SEND_EXCEPTION);
                    System.err.printf("会话[%s]发送异常: %s%n", sessionKey, e.getMessage());
                }
            };

            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(sendTask, 0, intervalMs, TimeUnit.MILLISECONDS);
            sendFutures.put(sessionKey, future);

            // 测试结束后取消任务
            scheduler.schedule(() -> {
                future.cancel(true);
                System.out.printf("会话[%s]发送任务结束%n", sessionKey);
            }, durationMinutes, TimeUnit.MINUTES);
        }

        public void waitForSessionsStart() throws InterruptedException {
            if (!allSessionsStarted.await(5, TimeUnit.MINUTES)) {
                System.err.println("部分会话未在规定时间内登录，继续执行剩余会话压测");
            }
        }

        public void shutdown() {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    // ==================== 会话配置工具 ====================
    static class SessionConfigBuilder {
        public static SessionSettings createMultiSessionSettings(String configTemplatePath, int sessionCount)
                throws ConfigError, FileNotFoundException {
            SessionSettings settings = new SessionSettings(new FileInputStream(configTemplatePath));
            SessionID templateSessionID = settings.getSessions().iterator().next();
            Properties templateProps = settings.getSessionProperties(templateSessionID);
            settings.removeSession(templateSessionID);

            for (int i = 0; i < sessionCount; i++) {
                String senderCompID = "BENCH-CLIENT-" + i;
                SessionID newSessionID = new SessionID(
                        templateSessionID.getBeginString(),
                        senderCompID,
                        templateSessionID.getTargetCompID()
                );
                Properties sessionProps = new Properties(templateProps);
                sessionProps.setProperty("SenderCompID", senderCompID);
                settings.set(newSessionID, sessionProps);
            }
            return settings;
        }
    }

    // ==================== 测试启动器 ====================
    public static void main(String[] args) throws Exception {
        // 压测参数（可通过命令行或配置文件传入）
        int sessionCount = 5;          // 并发会话数
        int tpsPerSession = 10;        // 单会话TPS
        long durationMinutes = 2;      // 测试持续时间（分钟）
        long timeoutMillis = 5000;     // 消息超时时间（毫秒）
        int maxLoginRetry = 3;         // 登录最大重试次数
        String configPath = "fix-client-template.cfg"; // 配置文件路径

        // 初始化组件
        MetricsCollector metrics = new MetricsCollector();
        SessionSettings settings = SessionConfigBuilder.createMultiSessionSettings(configPath, sessionCount);
        FixBenchmarkApplication application = new FixBenchmarkApplication(
                metrics, tpsPerSession, durationMinutes, timeoutMillis, sessionCount, maxLoginRetry
        );

        // 启动压测
        Initiator initiator = new SocketInitiator(
                application,
                new MemoryStoreFactory(),  // 内存存储（避免文件IO影响）
                settings,
                new NullLogFactory()       // 禁用日志（高TPS场景推荐）
                // new ScreenLogFactory(settings) // 调试时启用日志
        );
        initiator.start();
        System.out.printf("压测启动：会话数=%d，单会话TPS=%d，持续时间=%d分钟%n",
                sessionCount, tpsPerSession, durationMinutes);

        // 等待会话登录
        application.waitForSessionsStart();
        System.out.println("所有有效会话已登录，开始压测...");

        // 等待测试完成
        TimeUnit.MINUTES.sleep(durationMinutes);

        // 停止并输出报告
        initiator.stop();
        application.shutdown();
        System.out.println(metrics.generateSummaryReport());
        System.out.println(metrics.generatePerSessionReport());
    }
}