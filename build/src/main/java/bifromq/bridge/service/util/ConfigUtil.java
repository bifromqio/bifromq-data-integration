package bifromq.bridge.service.util;

import bifromq.bridge.service.config.BridgeConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class ConfigUtil {
    public static BridgeConfig build(File confFile) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(confFile, BridgeConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read config file: " + confFile, e);
        }
    }
}
