import org.springframework.stereotype.Service;
import quickfix.*;

@Service
public class FixEngineService {
    private SocketInitiator initiator;
    private final Map<String, SessionID> activeSessions = new ConcurrentHashMap<>();

    public void startFixSession(FixClientConfig config) throws ConfigError {
        try {
            SessionSettings settings = new SessionSettings();
            settings.setString(SessionSettings.BEGINSTRING, config.getBeginString());
            settings.setString(SessionSettings.SENDERCOMPID, config.getSenderCompId());
            settings.setString(SessionSettings.TARGETCOMPID, config.getTargetCompId());
            settings.setString(SessionSettings.SOCKET_CONNECT_HOST, config.getHost());
            settings.setInt(SessionSettings.SOCKET_CONNECT_PORT, config.getPort());
            
            Application application = new FixApplication();
            MessageStoreFactory storeFactory = new MemoryStoreFactory();
            LogFactory logFactory = new ScreenLogFactory();
            MessageFactory messageFactory = new DefaultMessageFactory();
            
            initiator = new SocketInitiator(
                application, storeFactory, settings, logFactory, messageFactory);
            
            initiator.start();
            activeSessions.put(config.getId(), initiator.getSessions().get(0));
        } catch (Exception e) {
            throw new ConfigError("FIX初始化失败: " + e.getMessage());
        }
    }

    public void stopFixSession(String configId) {
        SessionID sessionId = activeSessions.get(configId);
        if (sessionId != null) {
            Session.lookupSession(sessionId).logout();
            activeSessions.remove(configId);
        }
    }
    
    // 内部FIX应用处理类
    private static class FixApplication extends ApplicationAdapter {
        @Override
        public void onLogon(SessionID sessionId) {
            System.out.println("FIX登录成功: " + sessionId);
        }
        
        @Override
        public void onLogout(SessionID sessionId) {
            System.out.println("FIX登出: " + sessionId);
        }
        
        @Override
        public void fromApp(Message message, SessionID sessionId) {
            System.out.println("收到FIX消息: " + message);
        }
    }
}