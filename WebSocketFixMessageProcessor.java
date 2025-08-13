import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.Message;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class WebSocketFixMessageProcessor {
    private final SessionID sessionID;
    private final Map<String, String> sessionFields = new HashMap<>(); // 存储会话级字段
    
    // 时间戳格式器 (FIX协议要求: YYYYMMDD-HH:MM:SS.sss)
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");

    public WebSocketFixMessageProcessor(SessionID sessionID) {
        this.sessionID = sessionID;
    }
    
    /**
     * 处理从WebSocket接收的消息并发送到FIX Server
     * @param rawMessage 从WebSocket接收的原始FIX消息字符串 (tag=value格式，使用|作为SOH替代)
     * @return 处理后的消息是否成功发送
     */
    public boolean processAndSendMessage(String rawMessage) {
        try {
            // 1. 将WebSocket消息转换为QuickFIX/J的Message对象
            Message message = parseRawMessage(rawMessage);
            
            // 2. 处理会话级字段
            processSessionFields(message);
            
            // 3. 处理应用级字段（根据需要）
            processApplicationFields(message);
            
            // 4. 发送消息到FIX Server
            boolean sent = Session.sendToTarget(message, sessionID);
            
            if (sent) {
                System.out.println("消息发送成功: " + message);
                // 更新本地序列号跟踪
                updateLocalSequenceNumber(message);
            } else {
                System.err.println("消息发送失败: " + message);
            }
            
            return sent;
        } catch (Exception e) {
            System.err.println("处理消息时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 解析原始FIX消息字符串为QuickFIX/J Message对象
     */
    private Message parseRawMessage(String rawMessage) throws InvalidMessage {
        // 将|替换为实际的SOH字符 (ASCII 1)
        String fixMessage = rawMessage.replace("|", String.valueOf((char) 1));
        Message message = new Message();
        message.fromString(fixMessage, Session.lookupSession(sessionID).getDataDictionary(), true);
        return message;
    }
    
    /**
     * 处理会话级字段，重点处理序列号和时间戳
     */
    private void processSessionFields(Message message) throws FieldNotFound {
        // 1. 处理时间戳 (SendingTime, 标签52)
        // 始终使用当前时间覆盖，确保准确性
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(TIMESTAMP_FORMATTER);
        message.setField(new SendingTime(timestamp));
        
        // 2. 处理序列号 (MsgSeqNum, 标签34)
        // 从会话中获取正确的下一个发送序列号
        Session session = Session.lookupSession(sessionID);
        int nextSeqNum = session.getNextSenderMsgSeqNum();
        
        // 替换消息中的序列号
        if (message.isSetField(MsgSeqNum.FIELD)) {
            System.out.println("替换消息中的序列号: " + message.getField(new MsgSeqNum()) + " -> " + nextSeqNum);
        } else {
            System.out.println("消息中未包含序列号，添加正确序列号: " + nextSeqNum);
        }
        message.setField(new MsgSeqNum(nextSeqNum));
        
        // 3. 处理其他会话级字段（确保与当前会话一致）
        // 发送方ID (SenderCompID, 标签49)
        if (!message.isSetField(SenderCompID.FIELD)) {
            message.setField(new SenderCompID(sessionID.getSenderCompID()));
        }
        
        // 接收方ID (TargetCompID, 标签56)
        if (!message.isSetField(TargetCompID.FIELD)) {
            message.setField(new TargetCompID(sessionID.getTargetCompID()));
        }
        
        // 处理BeginString (标签8) - 确保与会话版本一致
        BeginString beginString = new BeginString();
        session.getSessionID().getBeginString().getField(beginString);
        if (message.isSetField(BeginString.FIELD) && 
            !message.getField(beginString).getValue().equals(beginString.getValue())) {
            System.out.println("警告: 消息中的BeginString与会话不一致，已自动修正");
            message.setField(beginString);
        }
    }
    
    /**
     * 处理应用级字段（根据业务需求定制）
     */
    private void processApplicationFields(Message message) throws FieldNotFound {
        char msgType = message.getHeader().getField(new MsgType()).getValue().charAt(0);
        
        switch (msgType) {
            case MsgType.NEW_ORDER_SINGLE:
                processNewOrderSingle(message);
                break;
            case MsgType.ORDER_CANCEL_REQUEST:
                processOrderCancelRequest(message);
                break;
            // 可以添加其他消息类型的处理逻辑
        }
    }
    
    /**
     * 处理新订单消息特定字段
     */
    private void processNewOrderSingle(Message message) throws FieldNotFound {
        // 例如: 如果没有指定ClOrdID，则自动生成一个
        if (!message.isSetField(ClOrdID.FIELD)) {
            String clOrdID = "TEST_" + System.currentTimeMillis();
            message.setField(new ClOrdID(clOrdID));
            System.out.println("自动生成ClOrdID: " + clOrdID);
        }
        
        // 例如: 验证价格和数量是否合理
        Price price = new Price();
        if (message.isSetField(price)) {
            if (price.getValue().doubleValue() <= 0) {
                throw new IllegalArgumentException("价格必须大于0");
            }
        }
    }
    
    /**
     * 处理取消订单消息特定字段
     */
    private void processOrderCancelRequest(Message message) throws FieldNotFound {
        // 例如: 确保取消请求引用的订单ID存在
        String origClOrdID = message.getField(new OrigClOrdID()).getValue();
        if (!orderExists(origClOrdID)) {
            System.out.println("警告: 取消请求引用的订单不存在: " + origClOrdID);
            // 可以选择抛出异常或继续处理
        }
    }
    
    /**
     * 更新本地序列号跟踪
     */
    private void updateLocalSequenceNumber(Message message) throws FieldNotFound {
        MsgSeqNum msgSeqNum = new MsgSeqNum();
        message.getField(msgSeqNum);
        Session session = Session.lookupSession(sessionID);
        session.setNextSenderMsgSeqNum(msgSeqNum.getValue() + 1);
    }
    
    /**
     * 检查订单是否存在（实际实现中应查询订单缓存）
     */
    private boolean orderExists(String clOrdID) {
        // 实际实现中应检查本地缓存的订单
        return true;
    }
}
