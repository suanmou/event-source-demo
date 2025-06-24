package com.example.fixclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Date;

@Configuration
public class FixClientConfig {

    @Bean
    public Application application() {
        return new Application() {
            @Override
            public void onCreate(SessionID sessionID) {
                System.out.println("Session created: " + sessionID);
            }

            @Override
            public void onLogon(SessionID sessionID) {
                System.out.println("Logged on: " + sessionID);
                // 登录成功后可以发送测试消息
                sendTestMessage(sessionID);
            }

            @Override
            public void onLogout(SessionID sessionID) {
                System.out.println("Logged out: " + sessionID);
            }

            @Override
            public void toAdmin(Message message, SessionID sessionID) {
                // 可以在这里添加管理消息的处理逻辑
                System.out.println("Sending admin message: " + message);
            }

            @Override
            public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
                // 处理管理消息（如Logout）
                System.out.println("Received admin message: " + message);
            }

            @Override
            public void toApp(Message message, SessionID sessionID) throws DoNotSend {
                // 可以在这里添加应用消息的处理逻辑
                System.out.println("Sending application message: " + message);
            }

            @Override
            public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
                // 处理应用消息
                System.out.println("Received application message: " + message);
            }

            private void sendTestMessage(SessionID sessionID) {
                try {
                    // 创建一个测试消息（这里使用NewOrderSingle作为示例）
                    NewOrderSingle order = new NewOrderSingle(
                        new ClOrdID("TEST-" + System.currentTimeMillis()),
                        new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION),
                        new Symbol("AAPL"),
                        new Side(Side.BUY),
                        new TransactTime(new Date())
                    );
                    
                    order.set(new OrdType(OrdType.MARKET));
                    order.set(new OrderQty(100));
                    
                    // 发送消息
                    Session.sendToTarget(order, sessionID);
                } catch (SessionNotFound e) {
                    System.err.println("Session not found: " + e.getMessage());
                }
            }
        };
    }

    @Bean
    public SessionSettings sessionSettings() throws ConfigError, IOException {
        // 加载客户端配置文件
        return new SessionSettings(new FileInputStream("src/main/resources/quickfixj-client.cfg"));
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
    public Initiator initiator(Application application, MessageStoreFactory messageStoreFactory,
                               SessionSettings sessionSettings, LogFactory logFactory,
                               MessageFactory messageFactory) throws ConfigError {
        // 初始化FIX客户端
        return new SocketInitiator(application, messageStoreFactory, sessionSettings, logFactory, messageFactory);
    }

    @Bean
    public KeyStore keyStore() throws Exception {
        // 加载客户端证书
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream("src/main/resources/client_keystore.jks"), "password".toCharArray());
        return keyStore;
    }
}
