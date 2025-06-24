package com.example.fixserver;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class FIXServerApplication implements Application {
    private final FIXServer server;
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    
    public FIXServerApplication(FIXServer server) {
        this.server = server;
    }
    
    @Override
    public void onCreate(SessionID sessionID) {
        System.out.println("Session created: " + sessionID);
    }
    
    @Override
    public void onLogon(SessionID sessionID) {
        System.out.println("Logon received: " + sessionID);
        
        // 注册会话
        server.registerSession(sessionID);
        
        // 重置登录尝试计数
        loginAttempts.remove(sessionID.toString());
    }

    @Override
    public void onLogout(SessionID sessionID) {
        System.out.println("Logout received: " + sessionID);
        
        // 注销会话
        server.unregisterSession(sessionID);
    }
    
    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        // 在发送登录消息前验证IP和证书
        if (message instanceof Logon) {
            // 获取客户端IP地址
            String clientIP = getClientIP(sessionID);
            if (clientIP == null) {
                rejectLogin(sessionID, "无法获取客户端IP地址");
                return;
            }
            
            // 验证IP白名单
            if (!server.isIpAllowed(sessionID, clientIP)) {
                rejectLogin(sessionID, "IP地址不在白名单中: " + clientIP);
                return;
            }
            
            // 验证证书
            X509Certificate[] clientCerts = getClientCertificates(sessionID);
            if (clientCerts == null || clientCerts.length == 0) {
                rejectLogin(sessionID, "未提供客户端证书");
                return;
            }
            
            if (!server.validateCertificate(sessionID, clientCerts)) {
                rejectLogin(sessionID, "客户端证书验证失败");
                return;
            }
            
            // 检查登录频率
            if (isLoginRateLimited(sessionID)) {
                rejectLogin(sessionID, "登录频率过高，请稍后再试");
                return;
            }
            
            // 记录登录尝试
            recordLoginAttempt(sessionID);
            
            System.out.println("登录验证通过: " + sessionID + ", IP: " + clientIP);
        }
    }
    
    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // 处理来自客户端的管理消息
    }
    
    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        // 处理发送到应用层的消息
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // 处理来自应用层的消息
        try {
            if (message instanceof NewOrderSingle) {
                handleNewOrderSingle((NewOrderSingle) message, sessionID);
            } else if (message instanceof OrderCancelRequest) {
                handleOrderCancelRequest((OrderCancelRequest) message, sessionID);
            } else {
                System.out.println("收到未处理的消息类型: " + message.getHeader().getField(new MsgType()).getValue());
            }
        } catch (Exception e) {
            System.out.println("处理消息时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleNewOrderSingle(NewOrderSingle order, SessionID sessionID) {
        try {
            System.out.println("收到新订单: " + order.getClOrdID().getValue());
            
            // 创建订单执行报告
            ExecutionReport executionReport = new ExecutionReport(
                    new OrderID(generateOrderID()),
                    new ExecID(generateExecID()),
                    new ExecType(ExecType.NEW),
                    new OrdStatus(OrdStatus.NEW),
                    order.getSymbol(),
                    order.getSide(),
                    new LeavesQty(order.getOrderQty().getValue()),
                    new CumQty(0),
                    new AvgPx(0)
            );
            
            // 复制相关字段
            executionReport.set(order.getClOrdID());
            executionReport.set(order.getTransactTime());
            
            // 发送执行报告
            server.sendMessage(executionReport, sessionID);
            
            System.out.println("订单已处理: " + order.getClOrdID().getValue());
        } catch (Exception e) {
            System.out.println("处理新订单时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleOrderCancelRequest(OrderCancelRequest cancelRequest, SessionID sessionID) {
        try {
            System.out.println("收到订单取消请求: " + cancelRequest.getClOrdID().getValue());
            
            // 创建订单执行报告
            ExecutionReport executionReport = new ExecutionReport(
                    new OrderID(cancelRequest.getOrderID().getValue()),
                    new ExecID(generateExecID()),
                    new ExecType(ExecType.CANCELED),
                    new OrdStatus(OrdStatus.CANCELED),
                    cancelRequest.getSymbol(),
                    cancelRequest.getSide(),
                    new LeavesQty(0),
                    new CumQty(cancelRequest.getOrderQty().getValue()),
                    new AvgPx(0)
            );
            
            // 复制相关字段
            executionReport.set(cancelRequest.getClOrdID());
            executionReport.set(cancelRequest.getOrigClOrdID());
            executionReport.set(cancelRequest.getTransactTime());
            
            // 发送执行报告
            server.sendMessage(executionReport, sessionID);
            
            System.out.println("订单取消已处理: " + cancelRequest.getClOrdID().getValue());
        } catch (Exception e) {
            System.out.println("处理订单取消请求时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getClientIP(SessionID sessionID) {
         try {
            // 使用反射获取Session的内部字段
            java.lang.reflect.Field field = Session.class.getDeclaredField("transport");
            field.setAccessible(true);
            
            // 获取Transport对象
            Object transport = field.get(Session.lookupSession(sessionId));
            
            if (transport instanceof SocketInitiatorTransport) {
                SocketInitiatorTransport socketTransport = (SocketInitiatorTransport) transport;
                java.lang.reflect.Field socketField = SocketInitiatorTransport.class.getDeclaredField("socket");
                socketField.setAccessible(true);
                java.net.Socket socket = (java.net.Socket) socketField.get(socketTransport);
                return socket.getInetAddress().getHostAddress();
            } else if (transport instanceof SocketAcceptorTransport) {
                SocketAcceptorTransport socketTransport = (SocketAcceptorTransport) transport;
                java.lang.reflect.Field socketField = SocketAcceptorTransport.class.getDeclaredField("socket");
                socketField.setAccessible(true);
                java.net.Socket socket = (java.net.Socket) socketField.get(socketTransport);
                return socket.getInetAddress().getHostAddress();
            }
        } catch (Exception e) {
            System.out.println("Error getting client IP: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        
        return null;
    }
    
    private X509Certificate[] getClientCertificates(SessionID sessionID) {
        try {
            Socket socket = Session.lookupSession(sessionID).getSocket();
            if (socket instanceof SSLSocket) {
                SSLSession sslSession = ((SSLSocket) socket).getSession();
                Certificate[] peerCertificates = sslSession.getPeerCertificates();
                X509Certificate[] x509Certificates = new X509Certificate[peerCertificates.length];
                for (int i = 0; i < peerCertificates.length; i++) {
                    x509Certificates[i] = (X509Certificate) peerCertificates[i];
                }
                return x509Certificates;
            }
            return null;
        } catch (SSLPeerUnverifiedException e) {
            System.out.println("客户端证书未验证: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("获取客户端证书时发生错误: " + e.getMessage());
            return null;
        }
    }
    
    private void rejectLogin(SessionID sessionID, String reason) {
        try {
            // 记录拒绝日志
            System.out.println("登录被拒绝: " + sessionID + ", 原因: " + reason);
            
            // 发送拒绝登录消息
            Session session = Session.lookupSession(sessionID);
            if (session != null) {
                session.logout(reason);
            }
        } catch (Exception e) {
            System.out.println("拒绝登录时发生错误: " + e.getMessage());
        }
    }
    
    private boolean isLoginRateLimited(SessionID sessionID) {
        LoginAttempt attempt = loginAttempts.get(sessionID.toString());
        if (attempt == null) {
            return false;
        }
        
        // 检查是否在10秒内尝试了超过3次
        long count = attempt.getAttemptsInLastSeconds(10);
        return count > 3;
    }
    
    private void recordLoginAttempt(SessionID sessionID) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(
                sessionID.toString(),
                k -> new LoginAttempt()
        );
        attempt.recordAttempt();
    }
    
    private String generateOrderID() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    private String generateExecID() {
        return "EXEC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // 登录尝试记录类
    private static class LoginAttempt {
        private final List<Date> attempts = new ArrayList<>();
        
        public synchronized void recordAttempt() {
            attempts.add(new Date());
            // 清理超过1分钟的记录
            attempts.removeIf(attempt -> {
                long diffInSeconds = ChronoUnit.SECONDS.between(
                        attempt.toInstant(),
                        new Date().toInstant()
                );
                return diffInSeconds > 60;
            });
        }
        
        public synchronized long getAttemptsInLastSeconds(int seconds) {
            Date now = new Date();
            return attempts.stream()
                    .filter(attempt -> {
                        long diffInSeconds = ChronoUnit.SECONDS.between(
                                attempt.toInstant(),
                                now.toInstant()
                        );
                        return diffInSeconds <= seconds;
                    })
                    .count();
        }
    }
}
    