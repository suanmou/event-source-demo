package com.fix.example.server;

import com.fix.example.security.JwtUtil;
import quickfix.*;
import quickfix.field.*;

import java.util.HashMap;
import java.util.Map;

public class FixServer implements Application {
    private Acceptor acceptor;
    private SessionSettings settings;
    private JwtUtil jwtUtil;
    private Map<SessionID, String> sessionRoles;

    public FixServer() {
        this.jwtUtil = new JwtUtil();
        this.sessionRoles = new HashMap<>();
    }

    public void start() throws ConfigError {
        settings = new SessionSettings("config/server.cfg");
        Application application = this;
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        acceptor = new SocketAcceptor(
            application, 
            storeFactory, 
            settings, 
            logFactory, 
            messageFactory
        );
        
        acceptor.start();
        System.out.println("FIX服务器已启动，等待连接...");
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        try {
            if (message instanceof Logon) {
                Logon logon = (Logon) message;
                
                // 从自定义字段获取JWT令牌
                StringField jwtField = new StringField(9999);
                if (logon.isSetField(jwtField)) {
                    String jwtToken = logon.getString(9999);
                    
                    // 验证JWT令牌
                    if (!jwtUtil.validateToken(jwtToken, logon.getUsername().getValue())) {
                        throw new RejectLogon("无效的JWT令牌");
                    }
                    
                    // 提取角色信息
                    String role = jwtUtil.getRoleFromToken(jwtToken);
                    sessionRoles.put(sessionID, role);
                    System.out.println("用户 " + logon.getUsername().getValue() + " 已认证，角色: " + role);
                } else {
                    throw new RejectLogon("缺少JWT令牌");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RejectLogon("登录验证失败: " + e.getMessage());
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        String msgType = message.getHeader().getString(MsgType.FIELD);
        String role = sessionRoles.get(sessionID);
        
        // 根据角色权限处理消息
        if (!hasPermission(role, msgType)) {
            throw new UnsupportedMessageType("权限不足: " + role + " 不能处理消息类型 " + msgType);
        }
        
        System.out.println("接收到消息 [" + msgType + "] 来自会话: " + sessionID);
        // 处理业务逻辑
    }

    private boolean hasPermission(String role, String msgType) {
        // 实际应用中应根据角色配置权限
        return role != null && (role.equals("ADMIN") || msgType.equals("D"));
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.stop();
        }
    }
}    