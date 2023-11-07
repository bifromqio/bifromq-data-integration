package bifromq.bridge.service.config;

import lombok.Getter;

@Getter
public class IntegratorConfig {
    private String groupName = "g1";
    private String username = "dev";
    private String password = "dev";
    private boolean cleanSession = true;
    private int clientNum = 10;
    private String topicFilter;
    private String host;
    private int port;
}
