package bifromq.integration.demo;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class DemoMain {
    public static void main(String[] args) {
        IIntegrator integrator = Integrator.builder()
                .groupName("g1")
                .topicFilter("test/data/integration")
                .userName("dev")
                .password("dev")
                .cleanSession(true)
                .clientNum(5)
                .port(1883)
                .host("BifroMQ host")
                .build();

        String bootstrapServers = "Kafka cluster";
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducerExample producerExample = new KafkaProducerExample(properties);
        integrator.onMessageArrive()
                .doOnComplete(producerExample::close)
                .subscribe(producerExample::produce);
    }
}