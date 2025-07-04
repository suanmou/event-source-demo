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