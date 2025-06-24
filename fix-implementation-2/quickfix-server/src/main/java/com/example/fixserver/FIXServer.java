package com.example.fixserver;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FIXServer {
    private SocketAcceptor acceptor;
    private Application application;
    private MessageStoreFactory storeFactory;
    private LogFactory logFactory;
    private SessionSettings settings;
    private Map<String, SessionID> sessionMap;
    private IPWhitelistManager ipWhitelistManager;
    private CertificateManager certificateManager;
    private SessionHistoryManager sessionHistoryManager;
    private SecurityLogManager securityLogManager;
    
    public FIXServer(String configFile) throws Exception {
        // 初始化服务器配置
        initConfig(configFile);
        
        // 初始化安全组件
        initSecurityComponents();
        
        // 初始化应用程序
        this.application = new FIXServerApplication(this);
        
        // 启动服务器
        startServer();
    }
    
    private void initConfig(String configFile) throws ConfigError, IOException {
        this.settings = new SessionSettings(new FileInputStream(configFile));
        this.sessionMap = new ConcurrentHashMap<>();
    }
    
    private void initSecurityComponents() {
        // 初始化IP白名单管理器
        this.ipWhitelistManager = new IPWhitelistManager();
        
        // 初始化证书管理器
        this.certificateManager = new CertificateManager();
        
        // 初始化会话历史管理器
        this.sessionHistoryManager = new SessionHistoryManager();
        
        // 初始化安全日志管理器
        this.securityLogManager = new SecurityLogManager();
    }
    
    private void startServer() throws Exception {
        // 创建消息存储工厂
        storeFactory = new FileStoreFactory(settings);
        
        // 创建日志工厂
        logFactory = new FileLogFactory(settings);
        
        // 创建消息工厂
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        // 创建并启动服务器
        acceptor = new SocketAcceptor(
                application,
                storeFactory,
                settings,
                logFactory,
                messageFactory
        );
        
        // 启动服务器
        acceptor.start();
        
        System.out.println("FIX Server started successfully");
    }
    
    public void stopServer() {
        if (acceptor != null) {
            acceptor.stop();
            System.out.println("FIX Server stopped");
        }
    }
    
    public void registerSession(String sessionId, SessionID fixSessionId) {
        sessionMap.put(sessionId, fixSessionId);
        securityLogManager.logSessionEstablished(sessionId, fixSessionId);
    }
    
    public void unregisterSession(String sessionId) {
        SessionID fixSessionId = sessionMap.remove(sessionId);
        if (fixSessionId != null) {
            securityLogManager.logSessionTerminated(sessionId, fixSessionId);
        }
    }
    
    public boolean isIPWhitelisted(String ipAddress) {
        return ipWhitelistManager.isIPWhitelisted(ipAddress);
    }
    
    public boolean validateCertificate(String certificate) {
        return certificateManager.validateCertificate(certificate);
    }
    
    public void logSecurityEvent(String eventType, String details) {
        securityLogManager.logSecurityEvent(eventType, details);
    }
    
    public void logSessionActivity(String sessionId, String messageType, String details) {
        sessionHistoryManager.logSessionActivity(sessionId, messageType, details);
    }
    
    public void sendMarketDataSnapshot(String sessionId, String symbol, double bidPrice, double askPrice) {
        SessionID fixSessionId = sessionMap.get(sessionId);
        if (fixSessionId != null && Session.lookupSession(fixSessionId).isLoggedOn()) {
            try {
                MarketDataSnapshotFullRefresh snapshot = new MarketDataSnapshotFullRefresh(
                        new MDReqID("MD-" + System.currentTimeMillis()),
                        new NoMDEntries(2)
                );
                
                // 添加买盘
                MarketDataSnapshotFullRefresh.NoMDEntries bidEntry = new MarketDataSnapshotFullRefresh.NoMDEntries();
                bidEntry.set(new MDEntryType(MDEntryType.BID));
                bidEntry.set(new MDEntryPx(bidPrice));
                bidEntry.set(new MDEntrySize(100));
                bidEntry.set(new Symbol(symbol));
                snapshot.addGroup(bidEntry);
                
                // 添加卖盘
                MarketDataSnapshotFullRefresh.NoMDEntries askEntry = new MarketDataSnapshotFullRefresh.NoMDEntries();
                askEntry.set(new MDEntryType(MDEntryType.OFFER));
                askEntry.set(new MDEntryPx(askPrice));
                askEntry.set(new MDEntrySize(100));
                askEntry.set(new Symbol(symbol));
                snapshot.addGroup(askEntry);
                
                Session.sendToTarget(snapshot, fixSessionId);
                logSessionActivity(sessionId, "MarketDataSnapshot", "Sent snapshot for " + symbol);
            } catch (Exception e) {
                System.out.println("Error sending market data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public void sendExecutionReport(String sessionId, String clOrdID, ExecType execType, OrdStatus ordStatus, 
                                   double price, int quantity) {
        SessionID fixSessionId = sessionMap.get(sessionId);
        if (fixSessionId != null && Session.lookupSession(fixSessionId).isLoggedOn()) {
            try {
                ExecutionReport report = new ExecutionReport(
                        new OrderID("ORD-" + System.currentTimeMillis()),
                        new ExecID("EXEC-" + System.currentTimeMillis()),
                        execType,
                        ordStatus,
                        new Side(Side.BUY),
                        new LeavesQty(0),
                        new CumQty(quantity),
                        new AvgPx(price)
                );
                
                report.set(new ClOrdID(clOrdID));
                
                Session.sendToTarget(report, fixSessionId);
                logSessionActivity(sessionId, "ExecutionReport", "Sent report for " + clOrdID);
            } catch (Exception e) {
                System.out.println("Error sending execution report: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            String configFile = "config/server.cfg";
            if (args.length > 0) {
                configFile = args[0];
            }
            
            FIXServer server = new FIXServer(configFile);
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));
            
            // 保持主线程运行
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
    