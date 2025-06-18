package com.fix.example.client;

import com.fix.example.FixSession;
import com.fix.example.UserRoleConfig;
import com.fix.example.security.JwtUtil;
import org.springframework.web.client.RestTemplate;
import quickfix.*;

import java.util.HashMap;
import java.util.Map;

public class FixClient {
    private FixSession fixSession;
    private String jwtToken;
    private String userId;
    private String role;
    private RestTemplate restTemplate;

    public FixClient(String userId, String role) {
        this.userId = userId;
        this.role = role;
        this.restTemplate = new RestTemplate();
    }

    public void initialize() throws Exception {
        // 1. 获取JWT令牌
        authenticate();
        
        // 2. 加载FIX配置
        SessionSettings settings = new SessionSettings("config/client.cfg");
        
        // 3. 创建角色配置
        UserRoleConfig roleConfig = new UserRoleConfig(
            role, 
            "ALL", 
            "ENCRYPTED", 
            30
        );
        
        // 4. 创建FIX会话
        JwtUtil jwtUtil = new JwtUtil();
        fixSession = new FixSession(userId, roleConfig, settings, jwtUtil);
        fixSession.setJwtToken(jwtToken);
        fixSession.initialize();
    }

    private void authenticate() {
        Map<String, String> authRequest = new HashMap<>();
        authRequest.put("username", userId);
        authRequest.put("password", "password"); // 实际应用中应使用安全的密码
        
        // 发送认证请求获取JWT
        String authUrl = "http://localhost:8080/authenticate";
        String response = restTemplate.postForObject(authUrl, authRequest, String.class);
        
        // 提取JWT令牌 (简化处理，实际应解析JSON响应)
        this.jwtToken = response.substring(10, response.length() - 2); // 示例解析逻辑
        System.out.println("获取到JWT令牌: " + jwtToken);
    }

    public void sendFixMessage(String message) throws SessionNotFound {
        if (fixSession != null && fixSession.isConnected()) {
            // 创建并发送FIX消息
            quickfix.Message fixMsg = new quickfix.Message();
            fixMsg.getHeader().setField(new BeginString("FIX.4.4"));
            fixMsg.getHeader().setField(new MsgType("D")); // 订单消息
            fixMsg.setField(new StringField(55, "EUR/USD")); // 符号
            
            fixSession.sendMessage(fixMsg);
            System.out.println("发送FIX消息: " + message);
        } else {
            System.out.println("FIX会话未连接");
        }
    }

    public void disconnect() {
        if (fixSession != null) {
            fixSession.disconnect();
        }
    }
}    