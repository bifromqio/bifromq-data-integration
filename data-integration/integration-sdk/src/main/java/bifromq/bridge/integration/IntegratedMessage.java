package bifromq.bridge.integration;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class IntegratedMessage {
    private byte[] payload;
    private String topic;
    private int qos;

    @Override
    public String toString() {
        return String.format("topic: %s, QoS: %d, payload: %s", topic, qos, new String(payload));
    }
}
