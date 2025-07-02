import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SocketIOHandler {
    private final SocketIOServer server;
    private final FixEngineService fixEngine;

    @Autowired
    public SocketIOHandler(SocketIOServer server, FixEngineService fixEngine) {
        this.server = server;
        this.fixEngine = fixEngine;
        server.start();
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        System.out.println("客户端连接: " + client.getSessionId());
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        System.out.println("客户端断开: " + client.getSessionId());
        // 清理相关FIX会话
    }

    @OnEvent("fix-config")
    public void onFixConfig(SocketIOClient client, FixClientConfig config) {
        try {
            client.sendEvent("fix-event", "正在连接FIX服务器: " + config.getHost() + ":" + config.getPort());
            fixEngine.startFixSession(config);
            client.sendEvent("fix-event", "FIX连接成功!");
        } catch (Exception e) {
            client.sendEvent("fix-event", "连接失败: " + e.getMessage());
        }
    }
}