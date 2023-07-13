package bifromq.integration.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

@Slf4j
public class KafkaProducerExample implements IProducer {
    private KafkaProducer<String, byte[]> producer;

    KafkaProducerExample(Properties properties) {
        producer = new KafkaProducer<>(properties);
    }
    @Override
    public void produce(IntegratedMessage message) {
        ProducerRecord<String, byte[]> record
                = new ProducerRecord<>(getCustomizedTopic(message.getTopic()), message.getPayload());
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error while producing", exception);
            }
        });
    }

    private String getCustomizedTopic(String topicFromBifroMQ) {
        return topicFromBifroMQ;
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
    }
}
