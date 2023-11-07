package bifromq.bridge.service.metrics;

import io.micrometer.core.instrument.Meter;

public enum ProducerMetrics {
    DownstreamSendCount("downstream.send.count", Meter.Type.COUNTER);
    public final String metricName;
    public final Meter.Type meterType;

    ProducerMetrics(String metricName, Meter.Type meterType) {
        this.metricName = metricName;
        this.meterType = meterType;
    }
}
