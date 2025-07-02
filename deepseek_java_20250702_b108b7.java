import lombok.Data;

@Data
public class FixClientConfig {
    private String id;
    private String name;
    private String host;
    private int port;
    private String senderCompId;
    private String targetCompId;
    private String beginString = "FIX.4.4";
}