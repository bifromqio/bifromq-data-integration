package bifromq.bridge.integration;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class IntegratedMessage {
    private byte[] payload;
    private @NonNull String topic;
    private @NonNull int qos;

    @Override
    public String toString() {
        if (payload == null || payload.length == 0) {
            return String.format("topic: %s, QoS: %d", topic, qos);
        }else {
            return String.format("topic: %s, QoS: %d, payload: %s", topic, qos, new String(payload));
        }
    }
}
