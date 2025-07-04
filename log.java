// WebSocket端点
@ServerEndpoint("/fix-gateway")
public class FixGatewayEndpoint {
    private static final Map<String, FixSessionManager> sessionManagers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        session.getAsyncRemote().sendText("[系统] WebSocket连接已建立");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JSONObject json = new JSONObject(message);
            String sessionId = session.getId();
            
            if ("CONNECT".equals(json.optString("action"))) {
                // 新建FIX会话
                JSONObject config = json.getJSONObject("config");
                FixSessionManager manager = new FixSessionManager(session, config);
                sessionManagers.put(sessionId, manager);
                manager.startSession();
            } else {
                // 处理交易指令
                FixSessionManager manager = sessionManagers.get(sessionId);
                if (manager != null) {
                    switch (json.optString("action")) {
                        case "QUOTE_REQUEST":
                            manager.sendQuoteRequest();
                            break;
                        case "NEW_ORDER":
                            manager.sendNewOrderSingle(
                                json.getString("symbol"),
                                json.getDouble("price"),
                                json.getDouble("quantity"),
                                json.getString("side")
                            );
                            break;
                        case "CANCEL_ORDER":
                            manager.sendOrderCancelRequest(json.getString("orderId"));
                            break;
                    }
                }
            }
        } catch (Exception e) {
            session.getAsyncRemote().sendText("[错误] " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        FixSessionManager manager = sessionManagers.remove(session.getId());
        if (manager != null) {
            manager.stopSession();
        }
    }
}

// FIX会话管理器
public class FixSessionManager implements ApplicationExtended {
    private final Session webSocketSession;
    private final JSONObject config;
    private SocketInitiator initiator;
    private SessionID sessionId;
    private final Map<String, String> orderIdMap = new ConcurrentHashMap<>();

    public FixSessionManager(Session webSocketSession, JSONObject config) {
        this.webSocketSession = webSocketSession;
        this.config = config;
    }

    public void startSession() {
        try {
            // 创建动态SessionID
            sessionId = new SessionID(
                "FIX.4.4",
                config.getString("senderCompId"),
                config.getString("targetCompId")
            );
            
            // 动态构建SessionSettings
            SessionSettings settings = new SessionSettings();
            settings.setString(sessionId, "ConnectionType", "initiator");
            settings.setString(sessionId, "SocketConnectHost", config.getString("host"));
            settings.setInt(sessionId, "SocketConnectPort", config.getInt("port"));
            settings.setString(sessionId, "StartTime", "00:00:00");
            settings.setString(sessionId, "EndTime", "23:59:59");
            settings.setBool(sessionId, "UseDataDictionary", false);
            settings.setLong(sessionId, "HeartBtInt", 30);
            
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new WebSocketLogFactory(this);
            MessageFactory messageFactory = new DefaultMessageFactory();
            
            initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
            initiator.start();
            
            sendToWebSocket("[FIX] 正在连接到网关: " + config.getString("host") + ":" + config.getInt("port"));
        } catch (Exception e) {
            sendToWebSocket("[错误] 会话启动失败: " + e.getMessage());
        }
    }

    // 实现ApplicationExtended接口
    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        logMessage("接收管理消息", message);
        
        // 处理Logon响应
        if (message instanceof Logon) {
            sendToWebSocket("[FIX] 登录成功");
        } else if (message instanceof Logout) {
            sendToWebSocket("[FIX] 登出请求");
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) {
        logMessage("接收应用消息", message);
        
        try {
            if (message instanceof ExecutionReport) {
                handleExecutionReport((ExecutionReport) message);
            } else if (message instanceof Quote) {
                handleQuote((Quote) message);
            } else if (message instanceof OrderCancelReject) {
                handleCancelReject((OrderCancelReject) message);
            }
        } catch (Exception e) {
            sendToWebSocket("[错误] 消息处理失败: " + e.getMessage());
        }
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        logMessage("发送管理消息", message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        logMessage("发送应用消息", message);
    }

    // 消息处理逻辑
    private void handleQuote(Quote quote) {
        try {
            String symbol = quote.getString(Symbol.FIELD);
            double bid = quote.getDouble(BidPx.FIELD);
            double ask = quote.getDouble(OfferPx.FIELD);
            sendToWebSocket(String.format("[行情] %s 报价: %.5f/%.5f", symbol, bid, ask));
        } catch (FieldNotFound e) {
            sendToWebSocket("[错误] 报价解析失败: " + e.getMessage());
        }
    }

    private void handleExecutionReport(ExecutionReport report) {
        try {
            String orderId = report.getString(OrderID.FIELD);
            String status = report.getString(OrdStatus.FIELD);
            String execType = report.getString(ExecType.FIELD);
            String symbol = report.getString(Symbol.FIELD);
            double price = report.getDouble(Price.FIELD);
            double quantity = report.getDouble(OrderQty.FIELD);
            
            String statusText = "";
            switch (status) {
                case OrdStatus.NEW: statusText = "新订单"; break;
                case OrdStatus.PARTIALLY_FILLED: statusText = "部分成交"; break;
                case ExecType.FILL: statusText = "完全成交"; break;
                case OrdStatus.CANCELED: statusText = "已取消"; break;
                case OrdStatus.REJECTED: statusText = "已拒绝"; break;
            }
            
            sendToWebSocket(String.format("[订单] %s %s %s @ %.4f x %.2f", 
                orderId, symbol, statusText, price, quantity));
        } catch (FieldNotFound e) {
            sendToWebSocket("[错误] 执行报告解析失败: " + e.getMessage());
        }
    }

    private void handleCancelReject(OrderCancelReject reject) {
        try {
            String orderId = reject.getString(OrderID.FIELD);
            String reason = reject.getString(CxlRejReason.FIELD);
            sendToWebSocket(String.format("[取消] 订单 %s 取消失败: %s", orderId, reason));
        } catch (FieldNotFound e) {
            sendToWebSocket("[错误] 取消拒绝解析失败: " + e.getMessage());
        }
    }

    // 交易操作
    public void sendQuoteRequest() {
        try {
            QuoteRequest request = new QuoteRequest();
            request.setString(QuoteReqID.FIELD, "QR_" + System.currentTimeMillis());
            request.setString(Symbol.FIELD, "EUR/USD");
            request.setUtcTimeStamp(TransactTime.FIELD, new Date());
            
            Session.sendToTarget(request, sessionId);
            sendToWebSocket("[请求] 发送报价请求: EUR/USD");
        } catch (Exception e) {
            sendToWebSocket("[错误] 报价请求失败: " + e.getMessage());
        }
    }

    public void sendNewOrderSingle(String symbol, double price, double quantity, String side) {
        try {
            NewOrderSingle order = new NewOrderSingle();
            String clOrdId = "ORD_" + System.currentTimeMillis();
            
            order.setString(ClOrdID.FIELD, clOrdId);
            order.setString(Symbol.FIELD, symbol);
            order.setChar(OrdType.FIELD, OrdType.LIMIT);
            order.setDouble(Price.FIELD, price);
            order.setDouble(OrderQty.FIELD, quantity);
            order.setChar(Side.FIELD, side.charAt(0));
            order.setUtcTimeStamp(TransactTime.FIELD, new Date());
            
            Session.sendToTarget(order, sessionId);
            orderIdMap.put(clOrdId, symbol);
            sendToWebSocket(String.format("[订单] 发送新订单: %s %s @ %.4f x %.2f", 
                symbol, side, price, quantity));
        } catch (Exception e) {
            sendToWebSocket("[错误] 订单发送失败: " + e.getMessage());
        }
    }

    public void sendOrderCancelRequest(String orderId) {
        try {
            OrderCancelRequest cancel = new OrderCancelRequest();
            String clOrdId = "CXL_" + System.currentTimeMillis();
            
            cancel.setString(OrigClOrdID.FIELD, orderId);
            cancel.setString(ClOrdID.FIELD, clOrdId);
            cancel.setString(Symbol.FIELD, orderIdMap.getOrDefault(orderId, "UNKNOWN"));
            cancel.setUtcTimeStamp(TransactTime.FIELD, new Date());
            
            Session.sendToTarget(cancel, sessionId);
            sendToWebSocket(String.format("[取消] 发送取消请求: %s", orderId));
        } catch (Exception e) {
            sendToWebSocket("[错误] 取消请求失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private void logMessage(String direction, Message message) {
        try {
            String rawMessage = message.toString();
            String formatted = rawMessage.replace('\001', '|');
            sendToWebSocket(String.format("[FIX] %s: %s", direction, formatted));
        } catch (Exception e) {
            sendToWebSocket("[错误] 消息格式化失败: " + e.getMessage());
        }
    }

    private void sendToWebSocket(String message) {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            webSocketSession.getAsyncRemote().sendText(message);
        }
    }
}

// 自定义日志工厂（将FIX日志转发到WebSocket）
public class WebSocketLogFactory implements LogFactory {
    private final FixSessionManager sessionManager;

    public WebSocketLogFactory(FixSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Log create(SessionID sessionID) {
        return new WebSocketLog(sessionManager);
    }

    private static class WebSocketLog implements Log {
        private final FixSessionManager sessionManager;

        public WebSocketLog(FixSessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @Override
        public void onIncoming(String message) {
            sessionManager.sendToWebSocket("[FIX日志] 接收: " + message);
        }

        @Override
        public void onOutgoing(String message) {
            sessionManager.sendToWebSocket("[FIX日志] 发送: " + message);
        }

        @Override
        public void onEvent(String text) {
            sessionManager.sendToWebSocket("[FIX事件] " + text);
        }

        @Override
        public void onErrorEvent(String text) {
            sessionManager.sendToWebSocket("[FIX错误] " + text);
        }

        @Override
        public void clear() {}
    }
}


import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/fix-gateway")
public class FixGatewayEndpoint {
    private static final Map<String, FixSessionManager> sessionManagers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        session.getAsyncRemote().sendText("[系统] WebSocket连接已建立");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JSONObject json = new JSONObject(message);
            String sessionId = session.getId();
            
            if ("CONNECT".equals(json.optString("action"))) {
                // 新建FIX会话
                JSONObject config = json.getJSONObject("config");
                FixSessionManager manager = new FixSessionManager(session, config);
                sessionManagers.put(sessionId, manager);
                manager.startSession();
            } else {
                // 处理交易指令
                FixSessionManager manager = sessionManagers.get(sessionId);
                if (manager != null) {
                    switch (json.optString("action")) {
                        case "LOGON":
                            manager.sendLogon();
                            break;
                        case "LOGOUT":
                            manager.sendLogout();
                            break;
                        case "NEW_ORDER":
                            manager.sendNewOrderSingle(
                                json.getString("clOrdId"),
                                json.getString("symbol"),
                                json.getString("side"),
                                json.getDouble("orderQty"),
                                json.optDouble("price", 0.0),
                                json.optString("ordType", "1")
                            );
                            break;
                        case "MARKET_DATA_REQUEST":
                            manager.sendMarketDataRequest(
                                json.getString("mdReqId"),
                                json.getString("symbol"),
                                json.optInt("subscriptionRequestType", 1)
                            );
                            break;
                        case "HEARTBEAT":
                            manager.sendHeartbeat();
                            break;
                        case "TEST_REQUEST":
                            manager.sendTestRequest(json.getString("testReqId"));
                            break;
                    }
                }
            }
        } catch (Exception e) {
            session.getAsyncRemote().sendText("[错误] " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        FixSessionManager manager = sessionManagers.remove(session.getId());
        if (manager != null) {
            manager.stopSession();
        }
    }
}

public class FixSessionManager implements ApplicationExtended {
    private final Session webSocketSession;
    private final JSONObject config;
    private SocketInitiator initiator;
    private SessionID sessionId;
    private final Map<String, String> orderIdMap = new ConcurrentHashMap<>();

    public FixSessionManager(Session webSocketSession, JSONObject config) {
        this.webSocketSession = webSocketSession;
        this.config = config;
    }

    public void startSession() {
        try {
            // 创建SessionID
            sessionId = new SessionID(
                "FIX.4.4",
                config.getString("senderCompId"),
                config.getString("targetCompId")
            );
            
            // 配置会话设置
            SessionSettings settings = new SessionSettings();
            settings.setString(sessionId, "ConnectionType", "initiator");
            settings.setString(sessionId, "SocketConnectHost", config.getString("host"));
            settings.setInt(sessionId, "SocketConnectPort", config.getInt("port"));
            settings.setString(sessionId, "StartTime", "00:00:00");
            settings.setString(sessionId, "EndTime", "23:59:59");
            settings.setBool(sessionId, "UseDataDictionary", true);
            settings.setLong(sessionId, "HeartBtInt", 30);
            
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new WebSocketLogFactory(this);
            MessageFactory messageFactory = new DefaultMessageFactory();
            
            initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
            initiator.start();
            
            sendToWebSocket("[FIX] 正在连接到网关: " + config.getString("host") + ":" + config.getInt("port"));
        } catch (Exception e) {
            sendToWebSocket("[错误] 会话启动失败: " + e.getMessage());
        }
    }

    // ========== FIX消息发送方法 ==========
    
    public void sendLogon() {
        try {
            Logon logon = new Logon();
            logon.setInt(HeartBtInt.FIELD, 30);
            logon.setUtcTimeStamp(TransactTime.FIELD, new Date());
            
            if (config.has("username")) {
                logon.setString(Username.FIELD, config.getString("username"));
            }
            if (config.has("password")) {
                logon.setString(Password.FIELD, config.getString("password"));
            }
            
            Session.sendToTarget(logon, sessionId);
            sendToWebSocket("[FIX] 发送登录请求");
        } catch (Exception e) {
            sendToWebSocket("[错误] 登录请求失败: " + e.getMessage());
        }
    }
    
    public void sendLogout() {
        try {
            Logout logout = new Logout();
            logout.setString(Text.FIELD, "Normal logout");
            Session.sendToTarget(logout, sessionId);
            sendToWebSocket("[FIX] 发送登出请求");
        } catch (Exception e) {
            sendToWebSocket("[错误] 登出请求失败: " + e.getMessage());
        }
    }
    
    public void sendNewOrderSingle(String clOrdId, String symbol, String side, 
                                  double orderQty, double price, String ordType) {
        try {
            NewOrderSingle order = new NewOrderSingle();
            order.setString(ClOrdID.FIELD, clOrdId);
            order.setString(Symbol.FIELD, symbol);
            order.setString(Side.FIELD, side);
            order.setDouble(OrderQty.FIELD, orderQty);
            order.setString(OrdType.FIELD, ordType);
            
            if ("2".equals(ordType)) { // 限价单
                order.setDouble(Price.FIELD, price);
            }
            
            order.setUtcTimeStamp(TransactTime.FIELD, new Date());
            order.setString(TimeInForce.FIELD, TimeInForce.DAY);
            
            Session.sendToTarget(order, sessionId);
            orderIdMap.put(clOrdId, symbol);
            
            String orderTypeDesc = "1".equals(ordType) ? "市价单" : "限价单";
            sendToWebSocket(String.format("[订单] 发送新订单: %s %s %s %.2f @ %s", 
                clOrdId, symbol, side, orderQty, orderTypeDesc));
        } catch (Exception e) {
            sendToWebSocket("[错误] 订单发送失败: " + e.getMessage());
        }
    }
    
    public void sendMarketDataRequest(String mdReqId, String symbol, int subscriptionRequestType) {
        try {
            MarketDataRequest request = new MarketDataRequest();
            request.setString(MDReqID.FIELD, mdReqId);
            request.setInt(SubscriptionRequestType.FIELD, subscriptionRequestType);
            request.setInt(MarketDepth.FIELD, 1); // 深度1
            
            // 添加相关交易品种
            MarketDataRequest.NoRelatedSym noRelatedSym = new MarketDataRequest.NoRelatedSym();
            noRelatedSym.setString(Symbol.FIELD, symbol);
            request.addGroup(noRelatedSym);
            
            // 添加条目类型 (买价和卖价)
            MarketDataRequest.NoMDEntryTypes bidType = new MarketDataRequest.NoMDEntryTypes();
            bidType.setChar(MDEntryType.FIELD, MDEntryType.BID);
            request.addGroup(bidType);
            
            MarketDataRequest.NoMDEntryTypes offerType = new MarketDataRequest.NoMDEntryTypes();
            offerType.setChar(MDEntryType.FIELD, MDEntryType.OFFER);
            request.addGroup(offerType);
            
            Session.sendToTarget(request, sessionId);
            sendToWebSocket(String.format("[行情] 发送行情请求: %s %s", mdReqId, symbol));
        } catch (Exception e) {
            sendToWebSocket("[错误] 行情请求失败: " + e.getMessage());
        }
    }
    
    public void sendHeartbeat() {
        try {
            Heartbeat heartbeat = new Heartbeat();
            Session.sendToTarget(heartbeat, sessionId);
            sendToWebSocket("[FIX] 发送心跳");
        } catch (Exception e) {
            sendToWebSocket("[错误] 心跳发送失败: " + e.getMessage());
        }
    }
    
    public void sendTestRequest(String testReqId) {
        try {
            TestRequest testRequest = new TestRequest();
            testRequest.setString(TestReqID.FIELD, testReqId);
            Session.sendToTarget(testRequest, sessionId);
            sendToWebSocket("[测试] 发送测试请求: " + testReqId);
        } catch (Exception e) {
            sendToWebSocket("[错误] 测试请求失败: " + e.getMessage());
        }
    }
    
    // ========== FIX消息处理方法 ==========
    
    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        logMessage("接收管理消息", message);
        
        try {
            if (message instanceof Logon) {
                handleLogon((Logon) message);
            } else if (message instanceof Logout) {
                handleLogout((Logout) message);
            } else if (message instanceof Heartbeat) {
                handleHeartbeat((Heartbeat) message);
            } else if (message instanceof TestRequest) {
                handleTestRequest((TestRequest) message);
            }
        } catch (Exception e) {
            sendToWebSocket("[错误] 管理消息处理失败: " + e.getMessage());
        }
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionId) {
        logMessage("接收应用消息", message);
        
        try {
            if (message instanceof ExecutionReport) {
                handleExecutionReport((ExecutionReport) message);
            } else if (message instanceof MarketDataSnapshotFullRefresh) {
                handleMarketDataSnapshot((MarketDataSnapshotFullRefresh) message);
            } else if (message instanceof Quote) {
                handleQuote((Quote) message);
            }
        } catch (Exception e) {
            sendToWebSocket("[错误] 应用消息处理失败: " + e.getMessage());
        }
    }
    
    private void handleLogon(Logon logon) throws FieldNotFound {
        sendToWebSocket("[FIX] 登录成功");
        if (logon.isSetField(HeartBtInt.FIELD)) {
            int hbInterval = logon.getInt(HeartBtInt.FIELD);
            sendToWebSocket("[FIX] 心跳间隔: " + hbInterval + "秒");
        }
    }
    
    private void handleLogout(Logout logout) throws FieldNotFound {
        String text = logout.isSetField(Text.FIELD) ? logout.getString(Text.FIELD) : "";
        sendToWebSocket("[FIX] 登出: " + text);
    }
    
    private void handleHeartbeat(Heartbeat heartbeat) throws FieldNotFound {
        if (heartbeat.isSetField(TestReqID.FIELD)) {
            String testReqId = heartbeat.getString(TestReqID.FIELD);
            sendToWebSocket("[FIX] 收到心跳响应: " + testReqId);
        } else {
            sendToWebSocket("[FIX] 收到心跳");
        }
    }
    
    private void handleTestRequest(TestRequest testRequest) throws FieldNotFound {
        String testReqId = testRequest.getString(TestReqID.FIELD);
        sendToWebSocket("[测试] 收到测试请求: " + testReqId);
        
        // 响应心跳
        Heartbeat response = new Heartbeat();
        response.setString(TestReqID.FIELD, testReqId);
        try {
            Session.sendToTarget(response, sessionId);
            sendToWebSocket("[测试] 发送心跳响应: " + testReqId);
        } catch (SessionNotFound e) {
            sendToWebSocket("[错误] 发送心跳响应失败: " + e.getMessage());
        }
    }
    
    private void handleExecutionReport(ExecutionReport report) throws FieldNotFound {
        String clOrdId = report.getString(ClOrdID.FIELD);
        String orderId = report.getString(OrderID.FIELD);
        String status = report.getString(OrdStatus.FIELD);
        String execType = report.getString(ExecType.FIELD);
        String symbol = report.getString(Symbol.FIELD);
        double price = report.isSetField(Price.FIELD) ? report.getDouble(Price.FIELD) : 0;
        double quantity = report.getDouble(OrderQty.FIELD);
        double cumQty = report.isSetField(CumQty.FIELD) ? report.getDouble(CumQty.FIELD) : 0;
        double avgPx = report.isSetField(AvgPx.FIELD) ? report.getDouble(AvgPx.FIELD) : 0;
        
        String statusText = getOrderStatusText(status, execType);
        
        String message = String.format("[订单] %s %s %s @ %.4f x %.2f (已成交: %.2f @ %.4f)", 
            orderId, symbol, statusText, price, quantity, cumQty, avgPx);
        
        sendToWebSocket(message);
    }
    
    private void handleMarketDataSnapshot(MarketDataSnapshotFullRefresh snapshot) throws FieldNotFound {
        String mdReqId = snapshot.getString(MDReqID.FIELD);
        String symbol = snapshot.getString(Symbol.FIELD);
        int totalEntries = snapshot.getInt(NoMDEntries.FIELD);
        
        StringBuilder snapshotInfo = new StringBuilder();
        snapshotInfo.append(String.format("[行情快照] ReqID=%s, Symbol=%s, 条目数=%d\n", 
            mdReqId, symbol, totalEntries));
        
        for (int i = 1; i <= totalEntries; i++) {
            MarketDataSnapshotFullRefresh.NoMDEntries entry = new MarketDataSnapshotFullRefresh.NoMDEntries();
            snapshot.getGroup(i, entry);
            
            char entryType = entry.getChar(MDEntryType.FIELD);
            double px = entry.getDouble(MDEntryPx.FIELD);
            double size = entry.getDouble(MDEntrySize.FIELD);
            
            String typeDesc = "";
            switch (entryType) {
                case MDEntryType.BID: typeDesc = "买价"; break;
                case MDEntryType.OFFER: typeDesc = "卖价"; break;
                case MDEntryType.TRADE: typeDesc = "成交价"; break;
                case MDEntryType.OPENING_PRICE: typeDesc = "开盘价"; break;
                case MDEntryType.CLOSING_PRICE: typeDesc = "收盘价"; break;
                default: typeDesc = "未知类型";
            }
            
            snapshotInfo.append(String.format("  %s: %.5f x %.2f\n", typeDesc, px, size));
        }
        
        sendToWebSocket(snapshotInfo.toString());
    }
    
    private void handleQuote(Quote quote) throws FieldNotFound {
        String quoteId = quote.getString(QuoteID.FIELD);
        String symbol = quote.getString(Symbol.FIELD);
        double bid = quote.getDouble(BidPx.FIELD);
        double ask = quote.getDouble(OfferPx.FIELD);
        double bidSize = quote.getDouble(BidSize.FIELD);
        double askSize = quote.getDouble(OfferSize.FIELD);
        
        String message = String.format("[报价] %s %s 报价: %.5f (%.2f) / %.5f (%.2f)", 
            quoteId, symbol, bid, bidSize, ask, askSize);
        
        sendToWebSocket(message);
    }
    
    // ========== 辅助方法 ==========
    
    private String getOrderStatusText(String status, String execType) {
        switch (status) {
            case OrdStatus.NEW: 
                return "新订单";
            case OrdStatus.PARTIALLY_FILLED: 
                return "部分成交";
            case OrdStatus.FILLED: 
                return "完全成交";
            case OrdStatus.CANCELED: 
                return "已取消";
            case OrdStatus.REJECTED: 
                return "已拒绝";
            default: 
                return "未知状态";
        }
    }
    
    private void logMessage(String direction, Message message) {
        try {
            String rawMessage = message.toString();
            String formatted = rawMessage.replace('\001', '|');
            sendToWebSocket(String.format("[FIX] %s: %s", direction, formatted));
        } catch (Exception e) {
            sendToWebSocket("[错误] 消息格式化失败: " + e.getMessage());
        }
    }
    
    private void sendToWebSocket(String message) {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            webSocketSession.getAsyncRemote().sendText(message);
        }
    }
    
    // 其他ApplicationExtended接口方法实现...
}

// 自定义日志工厂
public class WebSocketLogFactory implements LogFactory {
    private final FixSessionManager sessionManager;

    public WebSocketLogFactory(FixSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Log create(SessionID sessionID) {
        return new WebSocketLog(sessionManager);
    }

    private static class WebSocketLog implements Log {
        private final FixSessionManager sessionManager;

        public WebSocketLog(FixSessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @Override
        public void onIncoming(String message) {
            sessionManager.sendToWebSocket("[FIX日志] 接收: " + message);
        }

        @Override
        public void onOutgoing(String message) {
            sessionManager.sendToWebSocket("[FIX日志] 发送: " + message);
        }

        @Override
        public void onEvent(String text) {
            sessionManager.sendToWebSocket("[FIX事件] " + text);
        }

        @Override
        public void onErrorEvent(String text) {
            sessionManager.sendToWebSocket("[FIX错误] " + text);
        }

        @Override
        public void clear() {}
    }
}



import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class MockFixServer implements Application {
    // 存储会话状态
    private final Map<SessionID, SessionState> sessionStates = new HashMap<>();
    private final AtomicInteger orderIdCounter = new AtomicInteger(10000);
    private final Random random = new Random();
    
    // 存储市场数据
    private final Map<String, MarketData> marketDataMap = new HashMap<>();
    
    public MockFixServer() {
        // 初始化市场数据
        initializeMarketData();
    }
    
    private void initializeMarketData() {
        marketDataMap.put("EUR/USD", new MarketData(1.0850, 1.0852, 1.0848, 1.0855));
        marketDataMap.put("GBP/USD", new MarketData(1.2700, 1.2702, 1.2695, 1.2705));
        marketDataMap.put("USD/JPY", new MarketData(147.50, 147.52, 147.45, 147.55));
        marketDataMap.put("XAU/USD", new MarketData(2020.00, 2020.50, 2019.50, 2021.00));
    }

    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("FIX Server: 创建新会话: " + sessionId);
        sessionStates.put(sessionId, new SessionState());
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("FIX Server: 客户端登录成功: " + sessionId);
        SessionState state = getSessionState(sessionId);
        state.setLoggedOn(true);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("FIX Server: 客户端登出: " + sessionId);
        SessionState state = getSessionState(sessionId);
        state.setLoggedOn(false);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // 处理管理消息（Logon, Logout等）
        System.out.println("FIX Server [toAdmin]: " + message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue, RejectLogon {
        // 处理管理消息
        System.out.println("FIX Server [fromAdmin]: " + message);
        
        if (message instanceof Logon) {
            handleLogon((Logon) message, sessionId);
        } else if (message instanceof Logout) {
            handleLogout((Logout) message, sessionId);
        } else if (message instanceof Heartbeat) {
            handleHeartbeat((Heartbeat) message, sessionId);
        } else if (message instanceof TestRequest) {
            handleTestRequest((TestRequest) message, sessionId);
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        // 发送应用消息前处理
        System.out.println("FIX Server [toApp]: " + message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
        // 处理应用消息
        System.out.println("FIX Server [fromApp]: " + message);
        
        if (message instanceof NewOrderSingle) {
            handleNewOrderSingle((NewOrderSingle) message, sessionId);
        } else if (message instanceof MarketDataRequest) {
            handleMarketDataRequest((MarketDataRequest) message, sessionId);
        } else if (message instanceof QuoteRequest) {
            handleQuoteRequest((QuoteRequest) message, sessionId);
        }
    }
    
    // ========== 消息处理逻辑 ==========
    
    private void handleLogon(Logon logon, SessionID sessionId) throws FieldNotFound {
        SessionState state = getSessionState(sessionId);
        
        // 验证用户名/密码（如果有）
        if (logon.isSetField(Username.FIELD) && logon.isSetField(Password.FIELD)) {
            String username = logon.getString(Username.FIELD);
            String password = logon.getString(Password.FIELD);
            
            if (!"valid_user".equals(username) || !"valid_pass".equals(password)) {
                throw new RejectLogon("Invalid credentials");
            }
        }
        
        // 发送Logon响应
        Logon response = new Logon();
        response.setInt(HeartBtInt.FIELD, logon.getInt(HeartBtInt.FIELD));
        response.setUtcTimeStamp(TransactTime.FIELD, new Date());
        
        try {
            Session.sendToTarget(response, sessionId);
            System.out.println("FIX Server: 发送Logon响应");
        } catch (SessionNotFound e) {
            System.err.println("FIX Server: 无法发送Logon响应: " + e.getMessage());
        }
    }
    
    private void handleLogout(Logout logout, SessionID sessionId) {
        // 发送Logout响应
        Logout response = new Logout();
        if (logout.isSetField(Text.FIELD)) {
            try {
                response.setString(Text.FIELD, "Logout confirmed: " + logout.getString(Text.FIELD));
            } catch (FieldNotFound e) {
                // 忽略
            }
        } else {
            response.setString(Text.FIELD, "Logout confirmed");
        }
        
        try {
            Session.sendToTarget(response, sessionId);
            System.out.println("FIX Server: 发送Logout响应");
        } catch (SessionNotFound e) {
            System.err.println("FIX Server: 无法发送Logout响应: " + e.getMessage());
        }
    }
    
    private void handleHeartbeat(Heartbeat heartbeat, SessionID sessionId) throws FieldNotFound {
        // 如果有TestReqID，需要响应
        if (heartbeat.isSetField(TestReqID.FIELD)) {
            String testReqId = heartbeat.getString(TestReqID.FIELD);
            
            Heartbeat response = new Heartbeat();
            response.setString(TestReqID.FIELD, testReqId);
            
            try {
                Session.sendToTarget(response, sessionId);
                System.out.println("FIX Server: 发送心跳响应: " + testReqId);
            } catch (SessionNotFound e) {
                System.err.println("FIX Server: 无法发送心跳响应: " + e.getMessage());
            }
        }
    }
    
    private void handleTestRequest(TestRequest testRequest, SessionID sessionId) throws FieldNotFound {
        String testReqId = testRequest.getString(TestReqID.FIELD);
        
        // 发送心跳响应
        Heartbeat response = new Heartbeat();
        response.setString(TestReqID.FIELD, testReqId);
        
        try {
            Session.sendToTarget(response, sessionId);
            System.out.println("FIX Server: 发送测试请求响应: " + testReqId);
        } catch (SessionNotFound e) {
            System.err.println("FIX Server: 无法发送测试响应: " + e.getMessage());
        }
    }
    
    private void handleNewOrderSingle(NewOrderSingle order, SessionID sessionId) throws FieldNotFound {
        ExecutionReport report = new ExecutionReport();
        
        // 设置基本字段
        report.setString(OrderID.FIELD, "ORD" + orderIdCounter.incrementAndGet());
        report.setString(ClOrdID.FIELD, order.getString(ClOrdID.FIELD));
        report.setString(Symbol.FIELD, order.getString(Symbol.FIELD));
        report.setChar(OrdStatus.FIELD, OrdStatus.NEW);
        report.setChar(ExecType.FIELD, ExecType.NEW);
        report.setChar(Side.FIELD, order.getChar(Side.FIELD));
        report.setDouble(OrderQty.FIELD, order.getDouble(OrderQty.FIELD));
        report.setUtcTimeStamp(TransactTime.FIELD, new Date());
        
        // 设置价格
        if (order.isSetField(Price.FIELD)) {
            double price = order.getDouble(Price.FIELD);
            report.setDouble(Price.FIELD, price);
            report.setDouble(LastPx.FIELD, price);
        } else {
            // 市价单 - 使用当前市场价
            MarketData data = getMarketData(order.getString(Symbol.FIELD));
            double marketPrice = data.getMidPrice();
            report.setDouble(LastPx.FIELD, marketPrice);
        }
        
        // 设置累计数量
        report.setDouble(CumQty.FIELD, 0.0);
        report.setDouble(AvgPx.FIELD, 0.0);
        
        try {
            Session.sendToTarget(report, sessionId);
            System.out.println("FIX Server: 发送新订单执行报告");
            
            // 模拟订单成交
            simulateOrderExecution(order, sessionId);
            
        } catch (SessionNotFound e) {
            System.err.println("FIX Server: 无法发送执行报告: " + e.getMessage());
        }
    }
    
    private void simulateOrderExecution(NewOrderSingle order, SessionID sessionId) throws FieldNotFound {
        // 随机决定是否部分成交或完全成交
        double fillRatio = 0.3 + random.nextDouble() * 0.7; // 30%-100% 成交
        double orderQty = order.getDouble(OrderQty.FIELD);
        double fillQty = Math.floor(orderQty * fillRatio);
        
        ExecutionReport report = new ExecutionReport();
        
        // 设置基本字段
        report.setString(OrderID.FIELD, "ORD" + orderIdCounter.get()); // 使用相同的订单ID
        report.setString(ClOrdID.FIELD, order.getString(ClOrdID.FIELD));
        report.setString(Symbol.FIELD, order.getString(Symbol.FIELD));
        report.setChar(Side.FIELD, order.getChar(Side.FIELD));
        report.setDouble(OrderQty.FIELD, orderQty);
        
        // 设置价格
        if (order.isSetField(Price.FIELD)) {
            double price = order.getDouble(Price.FIELD);
            report.setDouble(Price.FIELD, price);
            report.setDouble(LastPx.FIELD, price);
        } else {
            // 市价单 - 使用当前市场价
            MarketData data = getMarketData(order.getString(Symbol.FIELD));
            double marketPrice = data.getMidPrice();
            report.setDouble(LastPx.FIELD, marketPrice);
        }
        
        // 设置成交状态
        if (fillQty >= orderQty) {
            report.setChar(OrdStatus.FIELD, OrdStatus.FILLED);
            report.setChar(ExecType.FIELD, ExecType.FILL);
            report.setDouble(CumQty.FIELD, orderQty);
            report.setDouble(LastQty.FIELD, orderQty);
        } else {
            report.setChar(OrdStatus.FIELD, OrdStatus.PARTIALLY_FILLED);
            report.setChar(ExecType.FIELD, ExecType.PARTIAL_FILL);
            report.setDouble(CumQty.FIELD, fillQty);
            report.setDouble(LastQty.FIELD, fillQty);
        }
        
        report.setDouble(AvgPx.FIELD, report.getDouble(LastPx.FIELD));
        report.setUtcTimeStamp(TransactTime.FIELD, new Date());
        
        try {
            Thread.sleep(500); // 模拟处理延迟
            Session.sendToTarget(report, sessionId);
            System.out.println("FIX Server: 发送订单成交报告");
        } catch (Exception e) {
            System.err.println("FIX Server: 无法发送成交报告: " + e.getMessage());
        }
    }
    
    private void handleMarketDataRequest(MarketDataRequest request, SessionID sessionId) throws FieldNotFound {
        String mdReqId = request.getString(MDReqID.FIELD);
        int subscriptionType = request.getInt(SubscriptionRequestType.FIELD);
        int marketDepth = request.getInt(MarketDepth.FIELD);
        
        // 获取请求的交易品种
        MarketDataRequest.NoRelatedSym noRelatedSym = new MarketDataRequest.NoRelatedSym();
        request.getGroup(1, noRelatedSym);
        String symbol = noRelatedSym.getString(Symbol.FIELD);
        
        MarketDataSnapshotFullRefresh snapshot = new MarketDataSnapshotFullRefresh();
        snapshot.setString(MDReqID.FIELD, mdReqId);
        snapshot.setString(Symbol.FIELD, symbol);
        
        // 添加市场数据条目
        MarketData data = getMarketData(symbol);
        
        // 添加买价
        MarketDataSnapshotFullRefresh.NoMDEntries bidEntry = new MarketDataSnapshotFullRefresh.NoMDEntries();
        bidEntry.setChar(MDEntryType.FIELD, MDEntryType.BID);
        bidEntry.setDouble(MDEntryPx.FIELD, data.getBid());
        bidEntry.setDouble(MDEntrySize.FIELD, 1000000); // 100万单位
        snapshot.addGroup(bidEntry);
        
        // 添加卖价
        MarketDataSnapshotFullRefresh.NoMDEntries offerEntry = new MarketDataSnapshotFullRefresh.NoMDEntries();
        offerEntry.setChar(MDEntryType.FIELD, MDEntryType.OFFER);
        offerEntry.setDouble(MDEntryPx.FIELD, data.getAsk());
        offerEntry.setDouble(MDEntrySize.FIELD, 1000000); // 100万单位
        snapshot.addGroup(offerEntry);
        
        // 添加当日最高价
        MarketDataSnapshotFullRefresh.NoMDEntries highEntry = new MarketDataSnapshotFullRefresh.NoMDEntries();
        highEntry.setChar(MDEntryType.FIELD, MDEntryType.TRADE);
        highEntry.setChar(MDEntryPx.FIELD, MDEntryType.HIGH);
        highEntry.setDouble(MDEntryPx.FIELD, data.getHigh());
        highEntry.setDouble(MDEntrySize.FIELD, 0); // 没有成交量
        snapshot.addGroup(highEntry);
        
        // 添加当日最低价
        MarketDataSnapshotFullRefresh.NoMDEntries lowEntry = new MarketDataSnapshotFullRefresh.NoMDEntries();
        lowEntry.setChar(MDEntryType.FIELD, MDEntryType.TRADE);
        lowEntry.setChar(MDEntryPx.FIELD, MDEntryType.LOW);
        lowEntry.setDouble(MDEntryPx.FIELD, data.getLow());
        lowEntry.setDouble(MDEntrySize.FIELD, 0); // 没有成交量
        snapshot.addGroup(lowEntry);
        
        try {
            Session.sendToTarget(snapshot, sessionId);
            System.out.println("FIX Server: 发送市场数据快照: " + symbol);
            
            // 如果是订阅类型，模拟后续更新
            if (subscriptionType == SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES) {
                simulateMarketDataUpdates(mdReqId, symbol, sessionId);
            }
        } catch (SessionNotFound e) {
            System.err.println("FIX Server: 无法发送市场数据: " + e.getMessage());
        }
    }
    
    private void simulateMarketDataUpdates(String mdReqId, String symbol, SessionID sessionId) {
        new Thread(() -> {
            try {
                MarketData data = getMarketData(symbol);
                
                // 发送3次更新
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(2000); // 2秒间隔
                    
                    // 小幅波动市场数据
                    double spread = 0.0002 + random.nextDouble() * 0.0003;
                    double mid = data.getMidPrice() * (0.9999 + random.nextDouble() * 0.0002);
                    double newBid = mid - spread / 2;
                    double newAsk = mid + spread / 2;
                    
                    // 更新市场数据
                    data.setBid(newBid);
                    data.setAsk(newAsk);
                    
                    // 创建市场数据增量刷新消息
                    MarketDataIncrementalRefresh refresh = new MarketDataIncrementalRefresh();
                    refresh.setString(MDReqID.FIELD, mdReqId);
                    
                    // 添加买价更新
                    MarketDataIncrementalRefresh.NoMDEntries bidEntry = new MarketDataIncrementalRefresh.NoMDEntries();
                    bidEntry.setChar(MDUpdateAction.FIELD, MDUpdateAction.CHANGE);
                    bidEntry.setChar(MDEntryType.FIELD, MDEntryType.BID);
                    bidEntry.setDouble(MDEntryPx.FIELD, newBid);
                    bidEntry.setDouble(MDEntrySize.FIELD, 1000000);
                    refresh.addGroup(bidEntry);
                    
                    // 添加卖价更新
                    MarketDataIncrementalRefresh.NoMDEntries askEntry = new MarketDataIncrementalRefresh.NoMDEntries();
                    askEntry.setChar(MDUpdateAction.FIELD, MDUpdateAction.CHANGE);
                    askEntry.setChar(MDEntryType.FIELD, MDEntryType.OFFER);
                    askEntry.setDouble(MDEntryPx.FIELD, newAsk);
                    askEntry.setDouble(MDEntrySize.FIELD, 1000000);
                    refresh.addGroup(askEntry);
                    
                    Session.sendToTarget(refresh, sessionId);
                    System.out.println("FIX Server: 发送市场数据更新: " + symbol);
                }
            } catch (Exception e) {
                System.err.println("FIX Server: 市场数据更新失败: " + e.getMessage());
            }
        }).start();
    }
    
    private void handleQuoteRequest(QuoteRequest request, SessionID sessionId) throws FieldNotFound {
        String quoteReqId = request.getString(QuoteReqID.FIELD);
        String symbol = request.getString(Symbol.FIELD);
        
        Quote quote = new Quote();
        quote.setString(QuoteReqID.FIELD, quoteReqId);
        quote.setString(QuoteID.FIELD, "Q" + System.currentTimeMillis());
        quote.setString(Symbol.FIELD, symbol);
        quote.setUtcTimeStamp(TransactTime.FIELD, new Date());
        
        MarketData data = getMarketData(symbol);
        quote.setDouble(BidPx.FIELD, data.getBid());
        quote.setDouble(OfferPx.FIELD, data.getAsk());
        quote.setDouble(BidSize.FIELD, 1000000);
        quote.setDouble(OfferSize.FIELD, 1000000);
        
        try {
            Session.sendToTarget(quote, sessionId);
            System.out.println("FIX Server: 发送报价响应: " + symbol);
        } catch (SessionNotFound e) {
            System.err.println("FIX Server: 无法发送报价: " + e.getMessage());
        }
    }
    
    // ========== 辅助方法 ==========
    
    private SessionState getSessionState(SessionID sessionId) {
        return sessionStates.computeIfAbsent(sessionId, k -> new SessionState());
    }
    
    private MarketData getMarketData(String symbol) {
        MarketData data = marketDataMap.get(symbol);
        if (data == null) {
            // 如果未找到，创建默认数据
            data = new MarketData(1.0, 1.0002, 0.9998, 1.0005);
            marketDataMap.put(symbol, data);
        }
        return data;
    }
    
    // 会话状态类
    private static class SessionState {
        private boolean loggedOn;
        
        public SessionState() {
            this.loggedOn = false;
        }
        
        public boolean isLoggedOn() {
            return loggedOn;
        }
        
        public void setLoggedOn(boolean loggedOn) {
            this.loggedOn = loggedOn;
        }
    }
    
    // 市场数据类
    private static class MarketData {
        private double bid;
        private double ask;
        private final double low;
        private final double high;
        
        public MarketData(double bid, double ask, double low, double high) {
            this.bid = bid;
            this.ask = ask;
            this.low = low;
            this.high = high;
        }
        
        public double getBid() {
            return bid;
        }
        
        public void setBid(double bid) {
            this.bid = bid;
        }
        
        public double getAsk() {
            return ask;
        }
        
        public void setAsk(double ask) {
            this.ask = ask;
        }
        
        public double getMidPrice() {
            return (bid + ask) / 2;
        }
        
        public double getLow() {
            return low;
        }
        
        public double getHigh() {
            return high;
        }
    }

    // ========== 启动服务器 ==========
    
    public static void main(String[] args) throws ConfigError, InterruptedException {
        String configFile = "mock_fix_server.cfg";
        SessionSettings settings = new SessionSettings(configFile);
        
        Application application = new MockFixServer();
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        SocketAcceptor acceptor = new SocketAcceptor(
            application, storeFactory, settings, logFactory, messageFactory);
        
        System.out.println("Starting FIX Server...");
        acceptor.start();
        
        // 保持服务器运行
        System.out.println("FIX Server is running. Press Ctrl+C to exit.");
        Thread.currentThread().join();
    }
}