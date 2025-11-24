import quickfix.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 独立Initiator池：管理500个独立的SocketInitiator（每个对应1个Session）
 */
class InitiatorPool {
    // 存储所有独立Initiator（索引对应Session索引）
    private final List<SocketInitiator> initiatorList = new CopyOnWriteArrayList<>();
    // 存储每个Initiator对应的SessionID
    private final List<SessionID> sessionIDList = new CopyOnWriteArrayList<>();
    // 连接状态追踪（SessionID -> 连接状态）
    private final ConcurrentHashMap<String, ConnectionStatus> connStatusMap = new ConcurrentHashMap<>();
    // 连接成功计数器
    private final AtomicInteger connectedCount = new AtomicInteger(0);
    // 最大并发启动的Initiator数量（控制每秒启动速率）
    private final int connectionsPerSecond;
    // 总会话数
    private final int totalSessions;

    // 连接状态枚举
    enum ConnectionStatus {
        INIT, STARTING, CONNECTED, FAILED, ABORTED
    }

    public InitiatorPool(int totalSessions, int connectionsPerSecond) {
        this.totalSessions = totalSessions;
        this.connectionsPerSecond = connectionsPerSecond;
        // 初始化所有会话的连接状态
        for (int i = 0; i < totalSessions; i++) {
            String sessionKey = "BENCH-CLIENT-" + i;
            connStatusMap.put(sessionKey, ConnectionStatus.INIT);
        }
    }

    /**
     * 批量创建独立Initiator（每个对应1个Session）
     */
    public void createInitiators(FixBenchmarkApplication application, String configTemplatePath) throws Exception {
        for (int i = 0; i < totalSessions; i++) {
            // 为每个Session生成独立配置（仅包含当前Session）
            SessionSettings singleSessionSettings = createSingleSessionSettings(configTemplatePath, i);
            // 创建独立SocketInitiator（每个仅管理1个Session）
            SocketInitiator initiator = new SocketInitiator(
                    application,
                    new MemoryStoreFactory(),  // 内存存储，无文件IO
                    singleSessionSettings,
                    new NullLogFactory()       // 禁用日志，最大化性能
            );
            // 每个Initiator的线程池最小化（核心线程数=1）
            ((ScheduledThreadPoolExecutor) initiator.getExecutor()).setCorePoolSize(1);
            ((ScheduledThreadPoolExecutor) initiator.getExecutor()).setMaximumPoolSize(2);
            
            initiatorList.add(initiator);
            // 获取当前Initiator的唯一SessionID
            SessionID sessionID = singleSessionSettings.getSessions().iterator().next();
            sessionIDList.add(sessionID);
        }
        System.out.println("成功创建" + initiatorList.size() + "个独立Initiator（每个对应1个Session）");
    }

    /**
     * 并发启动所有Initiator（限流控制）
     */
    public void startWithRateLimit() throws InterruptedException {
        // 速率限制器：控制每秒启动的Initiator数量
        ScheduledExecutorService rateLimiter = Executors.newScheduledThreadPool(1);
        AtomicInteger startIndex = new AtomicInteger(0);

        // 每秒启动connectionsPerSecond个Initiator
        rateLimiter.scheduleAtFixedRate(() -> {
            int currentIndex = startIndex.getAndIncrement();
            if (currentIndex >= totalSessions) {
                rateLimiter.shutdown();
                return;
            }

            // 启动当前索引的Initiator
            try {
                SocketInitiator initiator = initiatorList.get(currentIndex);
                SessionID sessionID = sessionIDList.get(currentIndex);
                connStatusMap.put(sessionID.toString(), ConnectionStatus.STARTING);
                
                initiator.start();
                System.out.printf("启动Initiator[%d]，SessionID=%s%n", currentIndex, sessionID);
            } catch (Exception e) {
                SessionID sessionID = sessionIDList.get(currentIndex);
                connStatusMap.put(sessionID.toString(), ConnectionStatus.FAILED);
                System.err.printf("启动Initiator[%d]失败：%s%n", currentIndex, e.getMessage());
            }
        }, 0, 1000 / connectionsPerSecond, TimeUnit.MILLISECONDS);

        // 等待所有Initiator启动完成（或超时）
        rateLimiter.awaitTermination(5, TimeUnit.MINUTES);
        System.out.println("所有Initiator启动调度完成");
    }

    /**
     * 等待所有会话连接成功（或超时）
     */
    public void waitForAllConnected(long timeoutMinutes) throws InterruptedException {
        long timeoutMillis = timeoutMinutes * 60 * 1000;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMillis) {
            int connected = connectedCount.get();
            int failed = (int) connStatusMap.values().stream().filter(s -> s == ConnectionStatus.FAILED).count();
            int remaining = totalSessions - connected - failed;

            System.out.printf("连接进度：成功%d/%d，失败%d/%d，剩余%d/%d%n",
                    connected, totalSessions, failed, totalSessions, remaining, totalSessions);

            if (connected + failed >= totalSessions) {
                break; // 所有会话已连接或失败
            }

            TimeUnit.SECONDS.sleep(2); // 每2秒检查一次
        }
    }

    /**
     * 标记会话连接成功
     */
    public void markConnected(SessionID sessionID) {
        connStatusMap.put(sessionID.toString(), ConnectionStatus.CONNECTED);
        connectedCount.incrementAndGet();
    }

    /**
     * 标记会话连接失败
     */
    public void markFailed(SessionID sessionID) {
        connStatusMap.put(sessionID.toString(), ConnectionStatus.FAILED);
    }

    /**
     * 停止所有Initiator，释放资源
     */
    public void shutdownAll() {
        System.out.println("开始停止所有Initiator...");
        for (int i = 0; i < initiatorList.size(); i++) {
            try {
                SocketInitiator initiator = initiatorList.get(i);
                if (initiator.isRunning()) {
                    initiator.stop();
                    // 关闭Initiator的线程池，避免资源泄露
                    initiator.getExecutor().shutdown();
                    initiator.getExecutor().awaitTermination(10, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                System.err.printf("停止Initiator[%d]失败：%s%n", i, e.getMessage());
            }
        }
        System.out.println("所有Initiator已停止");
    }

    /**
     * 为单个Session生成独立配置（仅包含当前Session）
     */
    private SessionSettings createSingleSessionSettings(String configTemplatePath, int sessionIndex) throws ConfigError, FileNotFoundException {
        SessionSettings templateSettings = new SessionSettings(new FileInputStream(configTemplatePath));
        SessionID templateSessionID = templateSettings.getSessions().iterator().next();
        Properties templateProps = templateSettings.getSessionProperties(templateSessionID);

        // 创建新配置，仅包含当前Session
        SessionSettings singleSettings = new SessionSettings();
        // 复制全局配置
        templateSettings.getGlobalSettings().forEach((key, value) -> singleSettings.setGlobalSetting(key, value));

        // 生成当前Session的唯一配置
        String senderCompID = "BENCH-CLIENT-" + sessionIndex;
        SessionID newSessionID = new SessionID(
                templateSessionID.getBeginString(),
                senderCompID,
                templateSessionID.getTargetCompID()
        );
        Properties sessionProps = new Properties(templateProps);
        sessionProps.setProperty("SenderCompID", senderCompID);
        singleSettings.set(newSessionID, sessionProps);

        return singleSettings;
    }

    // Getter
    public int getConnectedCount() {
        return connectedCount.get();
    }
}