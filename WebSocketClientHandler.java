import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class WebSocketClientHandler extends WebSocketClient {
    private final WebSocketFixMessageProcessor messageProcessor;
    
    public WebSocketClientHandler(URI serverUri, WebSocketFixMessageProcessor processor) {
        super(serverUri);
        this.messageProcessor = processor;
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("WebSocket连接已建立");
    }
    
    @Override
    public void onMessage(String message) {
        System.out.println("收到WebSocket消息: " + message);
        
        // 解析WebSocket消息（假设消息格式为JSON）
        // 实际应用中应使用Jackson或Gson等库解析
        if (isFixMessageCommand(message)) {
            String fixMessage = extractFixMessage(message);
            boolean success = messageProcessor.processAndSendMessage(fixMessage);
            
            // 向WebSocket服务器发送处理结果
            sendProcessingResult(success, fixMessage);
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket连接已关闭: " + reason);
    }
    
    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket错误: " + ex.getMessage());
    }
    
    /**
     * 判断消息是否为FIX消息发送命令
     */
    private boolean isFixMessageCommand(String message) {
        // 实际实现中应根据消息格式判断
        return message.contains("\"type\":\"send_fix_message\"");
    }
    
    /**
     * 从WebSocket消息中提取FIX消息内容
     */
    private String extractFixMessage(String message) {
        // 实际实现中应使用JSON解析库提取
        // 这里简化处理，实际应用需根据具体格式实现
        int startIndex = message.indexOf("\"content\":\"") + 11;
        int endIndex = message.lastIndexOf("\"");
        return message.substring(startIndex, endIndex);
    }
    
    /**
     * 向WebSocket服务器发送处理结果
     */
    private void sendProcessingResult(boolean success, String fixMessage) {
        String result = String.format(
            "{\"type\":\"message_result\",\"success\":%b,\"message\":\"%s\"}",
            success, fixMessage.replace("\"", "\\\"")
        );
        this.send(result);
    }
}
