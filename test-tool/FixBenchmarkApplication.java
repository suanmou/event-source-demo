static class FixBenchmarkApplication implements Application {
    private final MetricsCollector metrics;
    private final int tpsPerSession;
    private final long durationMinutes;
    private final long timeoutMillis;
    private final int maxLoginRetry;
    private final InitiatorPool initiatorPool; // 关联Initiator池
    private final ConcurrentHashMap<String, ScheduledFuture<?>> sendFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService globalScheduler = Executors.newScheduledThreadPool(5);

    public FixBenchmarkApplication(MetricsCollector metrics, int tpsPerSession, long durationMinutes,
                                  long timeoutMillis, int maxLoginRetry, InitiatorPool initiatorPool) {
        this.metrics = metrics;
        this.tpsPerSession = tpsPerSession;
        this.durationMinutes = durationMinutes;
        this.timeoutMillis = timeoutMillis;
        this.maxLoginRetry = maxLoginRetry;
        this.initiatorPool = initiatorPool;

        // 启动全局超时检查（1秒一次）
        globalScheduler.scheduleAtFixedRate(() -> metrics.checkTimeouts(timeoutMillis), 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onCreate(SessionID sessionID) {
        String sessionKey = sessionID.toString();
        metrics.initSession(sessionKey);
        System.out.printf("Session[%s]创建%n", sessionKey);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        String sessionKey = sessionID.toString();
        metrics.markFirstLoginSuccess(sessionKey);
        initiatorPool.markConnected(sessionID); // 通知Initiator池：连接成功
        System.out.printf("Session[%s]登录成功%n", sessionKey);

        // 启动消息发送任务
        startSendTask(sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        String sessionKey = sessionID.toString();
        System.out.printf("Session[%s]断开连接%n", sessionKey);
        // 重连逻辑（独立Initiator会自动重试，无需额外处理）
        if (metrics.sessionReconnectStats.get(sessionKey).isFirstLoginSuccess()) {
            metrics.markReconnectAttempt(sessionKey);
        }
    }

    // 发送消息任务（与之前逻辑一致，略作简化）
    private void startSendTask(SessionID sessionID) {
        String sessionKey = sessionID.toString();
        AtomicLong seq = new AtomicLong(0);
        long intervalMs = tpsPerSession > 0 ? 1000 / tpsPerSession : 0;

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
            }
        };

        ScheduledFuture<?> future = globalScheduler.scheduleAtFixedRate(sendTask, 0, intervalMs, TimeUnit.MILLISECONDS);
        sendFutures.put(sessionKey, future);

        // 测试结束后取消任务
        globalScheduler.schedule(() -> {
            future.cancel(true);
            System.out.printf("Session[%s]发送任务结束%n", sessionKey);
        }, durationMinutes, TimeUnit.MINUTES);
    }

    // 处理登录失败（通知Initiator池）
    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        if (message instanceof LogonReject) {
            LogonReject reject = (LogonReject) message;
            String reason = reject.isSetField(quickfix.field.Text.FIELD) ? reject.getText().getValue() : "未知原因";
            System.err.printf("Session[%s]登录被拒: %s%n", sessionID, reason);
            metrics.trackSendFailed(sessionID.toString(), "LOGON-REJECT", ErrorReason.LOGON_REJECT);
            initiatorPool.markFailed(sessionID); // 标记为失败
        } else if (message instanceof Reject) {
            Reject reject = (Reject) message;
            String testReqID = reject.isSetField(TestReqID.FIELD) ? reject.getTestReqID().getValue() : "UNKNOWN";
            metrics.trackSendFailed(sessionID.toString(), testReqID, ErrorReason.ENGINE_REJECT);
        }
    }

    // 其他接口实现（与之前一致，略）
    @Override public void toAdmin(Message message, SessionID sessionID) {}
    @Override public void toApp(Message message, SessionID sessionID) throws DoNotSend {}
    @Override public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        if (message instanceof Heartbeat && message.isSetField(TestReqID.FIELD)) {
            Heartbeat heartbeat = (Heartbeat) message;
            String testReqID = heartbeat.getTestReqID().getValue();
            metrics.trackReceived(sessionID.toString(), testReqID, Instant.now());
        }
    }

    public void shutdown() {
        globalScheduler.shutdown();
        try {
            if (!globalScheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                globalScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            globalScheduler.shutdownNow();
        }
    }
}