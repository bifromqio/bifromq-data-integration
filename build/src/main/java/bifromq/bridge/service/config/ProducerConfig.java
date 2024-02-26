package bifromq.bridge.service.config;

import lombok.Getter;

@Getter
public class ProducerConfig {
    public enum ProducerType {
        Dummy,
        Kafka
    }

    private ProducerType type;
    private KafkaConfig kafkaConfig;
}
