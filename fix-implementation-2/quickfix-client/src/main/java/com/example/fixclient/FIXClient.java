package com.example.fixclient;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FIXClient {
    private ThreadedSocketInitiator initiator;
    private Application application;
    private MessageStoreFactory storeFactory;
    private LogFactory logFactory;
    private SessionSettings settings;
    private SessionID sessionID;
    private ScheduledExecutorService scheduler;
    
    public FIXClient(String configFile) throws Exception {
        // 初始化客户端配置
        initConfig(configFile);
        
        // 初始化应用程序
        this.application = new FIXClientApplication(this);
        
        // 启动客户端
        startClient();
    }
    
    private void initConfig(String configFile) throws ConfigError, IOException {
        this.settings = new SessionSettings(new FileInputStream(configFile));
        
        // 获取会话ID
        Iterator<SessionID> sessionIds = settings.sectionIterator();
        if (sessionIds.hasNext()) {
            this.sessionID = sessionIds.next();
        } else {
            throw new ConfigError("No session defined in config file");
        }
    }
    
    private void startClient() throws Exception {
        // 创建消息存储工厂
        storeFactory = new FileStoreFactory(settings);
        
        // 创建日志工厂
        logFactory = new FileLogFactory(settings);
        
        // 创建消息工厂
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        // 创建并启动客户端
        initiator = new ThreadedSocketInitiator(
                application,
                storeFactory,
                settings,
                logFactory,
                messageFactory
        );
        
        // 设置SSL上下文
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        
        String keyStorePath = settings.getString("KeyStorePath");
        char[] keyStorePassword = settings.getString("KeyStorePassword").toCharArray();
        
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath), keyStorePassword);
        
        kmf.init(keyStore, keyStorePassword);
        
        // 信任所有证书（生产环境中应该使用信任库）
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
        
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
        
        // 设置SSL套接字工厂
        initiator.setSSLSocketFactory(sslContext.getSocketFactory());
        
        // 启动客户端
        initiator.start();
        
        // 启动调度器
        scheduler = Executors.newScheduledThreadPool(1);
        
        System.out.println("FIX Client started successfully");
    }
    
    public void stopClient() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (initiator != null) {
            initiator.stop();
            System.out.println("FIX Client stopped");
        }
    }
    
    public SessionID getSessionID() {
        return sessionID;
    }
    
    public void sendNewOrder(String symbol, Side side, OrderQty orderQty, Price price) {
        try {
            if (!Session.lookupSession(sessionID).isLoggedOn()) {
                System.out.println("Cannot send order: session is not logged on");
                return;
            }
            
            NewOrderSingle order = new NewOrderSingle(
                    new ClOrdID(generateClOrdID()),
                    side,
                    new TransactTime(new Date()),
                    new OrdType(OrdType.LIMIT)
            );
            
            order.set(symbol);
            order.set(orderQty);
            order.set(price);
            
            Session.sendToTarget(order, sessionID);
            System.out.println("Order sent: " + order.getClOrdID().getValue());
        } catch (Exception e) {
            System.out.println("Error sending order: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void sendOrderCancel(String origClOrdID, String symbol, Side side, OrderQty orderQty) {
        try {
            if (!Session.lookupSession(sessionID).isLoggedOn()) {
                System.out.println("Cannot send cancel request: session is not logged on");
                return;
            }
            
            OrderCancelRequest cancelRequest = new OrderCancelRequest(
                    new ClOrdID(generateClOrdID()),
                    new OrigClOrdID(origClOrdID),
                    new Symbol(symbol),
                    side,
                    new TransactTime(new Date())
            );
            
            cancelRequest.set(orderQty);
            
            Session.sendToTarget(cancelRequest, sessionID);
            System.out.println("Cancel request sent: " + cancelRequest.getClOrdID().getValue());
        } catch (Exception e) {
            System.out.println("Error sending cancel request: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String generateClOrdID() {
        return "CLORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    public static void main(String[] args) {
        try {
            String configFile = "config/client.cfg";
            if (args.length > 0) {
                configFile = args[0];
            }
            
            FIXClient client = new FIXClient(configFile);
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(client::stopClient));
            
            // 发送测试订单
            client.scheduler.schedule(() -> {
                client.sendNewOrder(
                        new Symbol("AAPL"),
                        new Side(Side.BUY),
                        new OrderQty(100),
                        new Price(150.25)
                );
            }, 5, TimeUnit.SECONDS);
            
            // 发送测试取消请求
            client.scheduler.schedule(() -> {
                client.sendOrderCancel(
                        "TEST-CANCEL",
                        "AAPL",
                        new Side(Side.BUY),
                        new OrderQty(100)
                );
            }, 10, TimeUnit.SECONDS);
            
            // 保持主线程运行
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
    