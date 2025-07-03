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