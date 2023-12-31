package bifromq.bridge.service.producer;

import bifromq.bridge.integration.IProducer;
import bifromq.bridge.integration.IntegratedMessage;
import bifromq.bridge.service.config.KafkaConfig;
import bifromq.bridge.service.metrics.ProducerMeter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
public class KafkaDownStream implements IProducer {
    private final KafkaProducer<String, String> kafkaProducer;
    private final String dummyTopic = "bridge-kafka";
    private final ProducerMeter producerMeter;

    public KafkaDownStream(KafkaConfig config) {
        producerMeter = new ProducerMeter("kafka");
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.kafkaProducer = new KafkaProducer<>(properties);
    }
    @Override
    public void produce(IntegratedMessage message) {
        producerMeter.recordCount(1);
        try {
            ProducerRecord<String, String> record
                    = new ProducerRecord<>(dummyTopic, new String(message.getPayload(), StandardCharsets.UTF_8));
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error while producing", exception);
                }
            });
        }catch (Exception exception) {
            log.error("error in send: {}", exception);
        }
    }

    private String getCustomizedTopic(String topicFromBifroMQ) {
        return topicFromBifroMQ;
    }

    @Override
    public void close() {
        kafkaProducer.flush();
        kafkaProducer.close();
    }
}
