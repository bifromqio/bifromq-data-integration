package bifromq.bridge.service.producer;

import bifromq.bridge.integration.IProducer;
import bifromq.bridge.service.config.BridgeConfig;
import bifromq.bridge.service.config.ProducerConfig;

public class ProducerFactory {
    public static IProducer getProducer(ProducerConfig producerConfig) {
        IProducer producer;
        switch (producerConfig.getType()) {
            case Dummy:
                producer = IProducer.DUMMY;
                break;
            case Kafka:
            default:
                producer = new KafkaDownStream(producerConfig.getKafkaConfig());
        }
        return producer;
    }
}
