package com.example.fixserver;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FIXServer {
    // 会话管理
    private final Map<String, SessionConfig> sessionConfigs = new ConcurrentHashMap<>();
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    
    // 证书管理
    private KeyStore trustStore;
    private KeyStore keyStore;
    private char[] keyStorePassword;
    
    // 服务器组件
    private ThreadedSocketAcceptor acceptor;
    private Application application;
    private MessageStoreFactory storeFactory;
    private LogFactory logFactory;
    private SessionSettings settings;
    
    public FIXServer(String configFile) throws Exception {
        // 初始化服务器配置
        initConfig(configFile);
        
        // 初始化证书管理
        initCertificateManagement();
        
        // 初始化应用程序
        this.application = new FIXServerApplication(this);
        
        // 启动服务器
        startServer();
    }
    
    private void initConfig(String configFile) throws ConfigError, IOException {
        this.settings = new SessionSettings(new FileInputStream(configFile));
        
        // 从配置中加载会话配置
        Iterator<SessionID> sessionIds = settings.sectionIterator();
        while (sessionIds.hasNext()) {
            SessionID sessionID = sessionIds.next();
            if (sessionID.getBeginString().startsWith("FIX.")) {
                SessionConfig config = new SessionConfig();
                config.setSessionID(sessionID);
                
                // 加载IP白名单
                if (settings.isSetting(sessionID, "AllowedIPs")) {
                    String[] allowedIPs = settings.getString(sessionID, "AllowedIPs").split(",");
                    for (String ip : allowedIPs) {
                        config.addAllowedIP(ip.trim());
                    }
                }
                
                // 加载证书信息
                if (settings.isSetting(sessionID, "AllowedCertificates")) {
                    String[] certs = settings.getString(sessionID, "AllowedCertificates").split(",");
                    for (String cert : certs) {
                        config.addAllowedCertificate(cert.trim());
                    }
                }
                
                sessionConfigs.put(sessionID.toString(), config);
            }
        }
    }
    
    private void initCertificateManagement() throws Exception {
        // 加载密钥库和信任库
        String keyStorePath = settings.getString("KeyStorePath");
        String trustStorePath = settings.getString("TrustStorePath");
        keyStorePassword = settings.getString("KeyStorePassword").toCharArray();
        
        keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath), keyStorePassword);
        
        trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(trustStorePath), keyStorePassword);
    }
    
    private void startServer() throws Exception {
        // 创建消息存储工厂
        storeFactory = new FileStoreFactory(settings);
        
        // 创建日志工厂
        logFactory = new FileLogFactory(settings);
        
        // 创建消息工厂
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        // 创建并启动服务器
        acceptor = new ThreadedSocketAcceptor(
                application,
                storeFactory,
                settings,
                logFactory,
                messageFactory
        );
        
        // 设置SSL上下文
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword);
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        
        // 设置自定义SSL套接字工厂
        acceptor.setSSLSocketFactory(sslContext.getSocketFactory());
        
        // 设置允许的密码套件
        String[] enabledCipherSuites = {
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_RSA_WITH_AES_128_GCM_SHA256"
        };
        acceptor.setEnabledCipherSuites(enabledCipherSuites);
        
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
    
    public SessionConfig getSessionConfig(SessionID sessionID) {
        return sessionConfigs.get(sessionID.toString());
    }
    
    public boolean isIpAllowed(SessionID sessionID, String ipAddress) {
        SessionConfig config = getSessionConfig(sessionID);
        if (config == null) {
            return false;
        }
        
        // 检查IP是否在白名单中
        if (config.getAllowedIPs().contains(ipAddress)) {
            // 更新IP最后使用时间
            config.updateIPLastUsed(ipAddress);
            return true;
        }
        
        return false;
    }
    
    public boolean validateCertificate(SessionID sessionID, X509Certificate[] certificates) {
        if (certificates == null || certificates.length == 0) {
            return false;
        }
        
        SessionConfig config = getSessionConfig(sessionID);
        if (config == null) {
            return false;
        }
        
        X509Certificate clientCert = certificates[0];
        
        // 检查证书是否过期
        try {
            clientCert.checkValidity();
        } catch (CertificateException e) {
            System.out.println("Certificate validation failed: " + e.getMessage());
            return false;
        }
        
        // 检查证书指纹是否在允许列表中
        String certFingerprint = getCertificateFingerprint(clientCert);
        if (config.getAllowedCertificates().contains(certFingerprint)) {
            // 更新证书最后使用时间
            config.updateCertificateLastUsed(certFingerprint);
            return true;
        }
        
        return false;
    }
    
    private String getCertificateFingerprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            return hexify(digest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String hexify(byte bytes[]) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', 
                            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            buf.append(hexDigits[(aByte & 0xf0) >> 4]);
            buf.append(hexDigits[aByte & 0x0f]);
        }
        return buf.toString();
    }
    
    public void registerSession(SessionID sessionID) {
        activeSessions.put(sessionID.toString(), Session.lookupSession(sessionID));
        System.out.println("Session registered: " + sessionID);
    }
    
    public void unregisterSession(SessionID sessionID) {
        activeSessions.remove(sessionID.toString());
        System.out.println("Session unregistered: " + sessionID);
    }
    
    public Collection<Session> getActiveSessions() {
        return activeSessions.values();
    }
    
    public void sendMessage(Message message, SessionID sessionID) {
        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFound e) {
            System.out.println("Session not found when sending message: " + e.getMessage());
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
    