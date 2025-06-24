package com.example.fixserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class FixConfig {

    // IP白名单配置
    private static final Set<String> IP_WHITELIST = new HashSet<>(Arrays.asList(
            "192.168.1.1",
            "192.168.1.2",
            "192.168.1.3"
    ));

    // 会话过期配置（188天）
    private static final long SESSION_EXPIRATION_DAYS = 188;

    // 记录会话最后登录时间
    private static final Map<String, LocalDateTime> LAST_LOGIN_TIME = new ConcurrentHashMap<>();

    // 记录IP最后登录时间
    private static final Map<String, LocalDateTime> IP_LAST_LOGIN_TIME = new ConcurrentHashMap<>();

    // 证书信息存储
    private static final Map<String, X509Certificate> CERTIFICATES = new ConcurrentHashMap<>();

    @Bean
    public Application application() {
        return new MessageCracker() {
            @Override
            public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
                // 处理管理消息（如Logon）
                if (message instanceof quickfix.fix44.Logon) {
                    validateLogon(message, sessionID);
                }
                super.fromAdmin(message, sessionID);
            }

            @Override
            public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
                // 处理应用消息
                crack(message, sessionID);
            }

            @Override
            public void onMessage(NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
                // 处理订单消息
                System.out.println("Received new order: " + order);
            }

            private void validateLogon(Message message, SessionID sessionID) throws RejectLogon {
                try {
                    // 1. 验证IP白名单
                    String ipAddress = getClientIP(sessionID);
                    if (!IP_WHITELIST.contains(ipAddress)) {
                        throw new RejectLogon("IP address not whitelisted: " + ipAddress);
                    }

                    // 2. 验证IP登录频率
                    if (IP_LAST_LOGIN_TIME.containsKey(ipAddress)) {
                        LocalDateTime lastLogin = IP_LAST_LOGIN_TIME.get(ipAddress);
                        if (ChronoUnit.SECONDS.between(lastLogin, LocalDateTime.now()) < 1) {
                            throw new RejectLogon("Too frequent login attempts from IP: " + ipAddress);
                        }
                    }

                    // 3. 验证会话是否过期
                    if (LAST_LOGIN_TIME.containsKey(sessionID.toString())) {
                        LocalDateTime lastLogin = LAST_LOGIN_TIME.get(sessionID.toString());
                        if (ChronoUnit.DAYS.between(lastLogin, LocalDateTime.now()) > SESSION_EXPIRATION_DAYS) {
                            throw new RejectLogon("Session expired: last login was " + lastLogin);
                        }
                    }

                    // 4. 验证TLS证书
                    validateCertificate(sessionID);

                    // 更新登录时间
                    LAST_LOGIN_TIME.put(sessionID.toString(), LocalDateTime.now());
                    IP_LAST_LOGIN_TIME.put(ipAddress, LocalDateTime.now());

                } catch (Exception e) {
                    throw new RejectLogon("Logon validation failed: " + e.getMessage());
                }
            }

            private String getClientIP(SessionID sessionID) {
                // 实际实现中需要从网络连接获取客户端IP
                return "192.168.1.1"; // 示例
            }

            private void validateCertificate(SessionID sessionID) throws Exception {
                // 从会话中获取证书并验证
                // 实际实现需要从SSL连接获取客户端证书
                String sessionKey = sessionID.toString();
                if (!CERTIFICATES.containsKey(sessionKey)) {
                    throw new Exception("No certificate found for session: " + sessionKey);
                }

                X509Certificate cert = CERTIFICATES.get(sessionKey);
                
                // 验证证书是否过期
                if (cert.getNotAfter().before(new Date())) {
                    throw new Exception("Certificate expired: " + cert.getNotAfter());
                }

                // 验证证书密钥长度
                if (cert.getPublicKey().getEncoded().length < 2048 / 8) {
                    throw new Exception("Certificate key length too short, minimum 2048 bits required");
                }
            }
        };
    }

    @Bean
    public SessionSettings sessionSettings() throws ConfigError, IOException {
        // 加载FIX配置文件
        return new SessionSettings(new FileInputStream("src/main/resources/quickfixj.cfg"));
    }

    @Bean
    public MessageStoreFactory messageStoreFactory(SessionSettings settings) {
        return new FileStoreFactory(settings);
    }

    @Bean
    public LogFactory logFactory(SessionSettings settings) {
        return new FileLogFactory(settings);
    }

    @Bean
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean
    public Acceptor acceptor(Application application, MessageStoreFactory messageStoreFactory,
                             SessionSettings sessionSettings, LogFactory logFactory,
                             MessageFactory messageFactory) throws ConfigError {
        // 初始化FIX服务器
        return new SocketAcceptor(application, messageStoreFactory, sessionSettings, logFactory, messageFactory);
    }

    @Bean
    public KeyStore keyStore() throws Exception {
        // 加载服务器证书
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream("src/main/resources/server_keystore.jks"), "password".toCharArray());
        return keyStore;
    }
}
