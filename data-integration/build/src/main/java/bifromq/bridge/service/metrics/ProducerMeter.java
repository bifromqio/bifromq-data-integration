package bifromq.bridge.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

public class ProducerMeter {
    private final Tags tags;
    private Meter counter;

    public ProducerMeter(String producerName) {
        this.tags = Tags.of("producer", producerName);
        this.counter = Metrics.counter(ProducerMetrics.DownstreamSendCount.metricName, tags);
    }
    public void recordCount(double inc) {
        ((Counter)counter).increment(inc);
    }
}
