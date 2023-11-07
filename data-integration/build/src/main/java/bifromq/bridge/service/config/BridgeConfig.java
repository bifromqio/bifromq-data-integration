package bifromq.bridge.service.config;

import lombok.Getter;

@Getter
public class BridgeConfig {

    private IntegratorConfig integratorConfig;
    private ProducerConfig producerConfig;
}
