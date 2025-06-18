package com.fix.example;

import com.fix.example.client.FixClient;
import com.fix.example.server.FixServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class SimulatedJwtAuthTest {
    private FixServer server;
    private FixClient client;

    @BeforeEach
    public void setUp() throws Exception {
        // 启动服务器
        server = new FixServer();
        server.start();
        
        // 等待服务器启动
        Thread.sleep(1000);
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.disconnect();
        }
        
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testJwtAuthenticatedFixSession() throws Exception {
        // 创建并初始化客户端
        client = new FixClient("testUser", "TRADER");
        client.initialize();
        
        // 等待会话建立
        Thread.sleep(2000);
        
        // 发送消息
        client.sendFixMessage("买入EUR/USD");
        
        // 等待消息处理
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);
    }
}    