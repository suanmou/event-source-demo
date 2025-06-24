package com.example.fixclient.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import java.util.Date;

@Service
public class FixClientService {

    @Autowired
    private Initiator initiator;

    /**
     * 启动FIX客户端
     */
    public void startClient() throws ConfigError {
        if (!initiator.isLoggedOn()) {
            initiator.start();
        }
    }

    /**
     * 停止FIX客户端
     */
    public void stopClient() {
        if (initiator.isLoggedOn()) {
            initiator.stop();
        }
    }

    /**
     * 发送登录请求
     */
    public void sendLogon() throws SessionNotFound {
        // 获取第一个会话ID
        SessionID sessionID = initiator.getSessions().get(0);
        
        // 创建登录消息
        quickfix.fix44.Logon logon = new quickfix.fix44.Logon();
        
        // 设置加密相关字段
        logon.set(new EncryptMethod(EncryptMethod.NONE_OTHER));
        
        // 发送登录消息
        Session.sendToTarget(logon, sessionID);
    }

    /**
     * 发送登出请求
     */
    public void sendLogout() throws SessionNotFound {
        // 获取第一个会话ID
        SessionID sessionID = initiator.getSessions().get(0);
        
        // 创建登出消息
        quickfix.fix44.Logout logout = new quickfix.fix44.Logout();
        
        // 发送登出消息
        Session.sendToTarget(logout, sessionID);
    }

    /**
     * 发送订单消息
     */
    public void sendOrder(String symbol, char side, double quantity, char ordType, double price) throws SessionNotFound {
        // 获取第一个会话ID
        SessionID sessionID = initiator.getSessions().get(0);
        
        // 创建订单消息
        NewOrderSingle order = new NewOrderSingle(
            new ClOrdID("ORD-" + System.currentTimeMillis()),
            new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION),
            new Symbol(symbol),
            new Side(side),
            new TransactTime(new Date())
        );
        
        // 设置订单类型
        order.set(new OrdType(ordType));
        
        // 设置订单数量
        order.set(new OrderQty(quantity));
        
        // 如果是限价单，设置价格
        if (ordType == OrdType.LIMIT) {
            order.set(new Price(price));
        }
        
        // 发送订单消息
        Session.sendToTarget(order, sessionID);
    }

    /**
     * 发送测试请求
     */
    public void sendTestRequest() throws SessionNotFound {
        // 获取第一个会话ID
        SessionID sessionID = initiator.getSessions().get(0);
        
        // 创建测试请求消息
        TestRequest testRequest = new TestRequest(new TestReqID("TEST-" + System.currentTimeMillis()));
        
        // 发送测试请求消息
        Session.sendToTarget(testRequest, sessionID);
    }

    /**
     * 检查客户端连接状态
     */
    public boolean isConnected() {
        return initiator.isLoggedOn();
    }
}
