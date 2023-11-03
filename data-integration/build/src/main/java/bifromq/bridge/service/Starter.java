package bifromq.bridge.service;

import bifromq.bridge.integration.IProducer;
import bifromq.bridge.integration.Integrator;
import bifromq.bridge.service.config.BridgeConfig;
import bifromq.bridge.service.producer.DownStreamProducer;
import bifromq.bridge.service.util.ConfigUtil;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;

@Slf4j
public class Starter {
    static {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> log.error("Caught an exception in thread[{}]", t.getName(), e));
        RxJavaPlugins.setErrorHandler(e -> log.error("Uncaught RxJava exception", e));
    }
    private static final String PROMETHEUS_PORT = "prometheus.port";
    private static final String PROMETHEUS_CONTEXT = "prometheus.context";
    private PrometheusMeterRegistry registry;
    private HttpServer prometheusExportServer;
    private Thread serverThread;
    private Integrator integrator;
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(cliOption(), args);
            File configFile = new File(cmd.getOptionValue("c"));
            if (!configFile.exists()) {
                throw new RuntimeException("Conf file does not exist");
            }
            BridgeConfig bridgeConfig = ConfigUtil.build(configFile);
            Starter starter = new Starter();
            starter.buildIntegrator(bridgeConfig);
            starter.buildPrometheus();

            starter.start();
        }catch (Exception exception) {
            log.error("start failed: {}", exception);
        }
    }

    private static Options cliOption() {
        return new Options()
                .addOption(Option.builder()
                        .option("c")
                        .longOpt("conf")
                        .desc("the conf file for Starter")
                        .hasArg(true)
                        .optionalArg(false)
                        .argName("CONF_FILE")
                        .required(true)
                        .build());
    }

    private void start() {
        serverThread.start();
        log.debug("Prometheus exporter started");
        integrator.start();
        log.debug("integrator started");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }

    private void stop() {
        integrator.close();
        log.debug("integrator stopped");
        prometheusExportServer.stop(0);
        Metrics.removeRegistry(registry);
        log.debug("Prometheus exporter stopped");
    }

    private void buildIntegrator(BridgeConfig bridgeConfig) {
        BridgeConfig.IntegratorConfig integratorConfig = bridgeConfig.getIntegratorConfig();
        BridgeConfig.KafkaConfig kafkaConfig = bridgeConfig.getKafkaConfig();
        this.integrator = Integrator.builder()
                .producer(buildProducer(kafkaConfig))
                .host(integratorConfig.getHost())
                .port(integratorConfig.getPort())
                .cleanSession(integratorConfig.isCleanSession())
                .clientNum(integratorConfig.getClientNum())
                .groupName(integratorConfig.getGroupName())
                .topicFilter(integratorConfig.getTopicFilter())
                .userName(integratorConfig.getUsername())
                .password(integratorConfig.getPassword())
                .build();
    }

    private IProducer buildProducer(BridgeConfig.KafkaConfig kafkaConfig) {
        return new DownStreamProducer(kafkaConfig.getBootstrapServers());
    }

    private void buildPrometheus() {
        new JvmInfoMetrics().bindTo(Metrics.globalRegistry);
        new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
        new JvmGcMetrics().bindTo(Metrics.globalRegistry);
        new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
        new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
        new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
        new ProcessorMetrics().bindTo(Metrics.globalRegistry);
        new LogbackMetrics().bindTo(Metrics.globalRegistry);

        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                switch (id.getType()) {
                    case TIMER:
                    case DISTRIBUTION_SUMMARY:
                }
                return DistributionStatisticConfig.builder()
                        .expiry(Duration.ofSeconds(5))
                        .build().merge(config);
            }
        });
        Metrics.addRegistry(registry);
        try {
            prometheusExportServer = HttpServer.create(
                    new InetSocketAddress(port()), 0);
            prometheusExportServer.createContext(context(), httpExchange -> {
                String response = registry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            serverThread = new Thread(prometheusExportServer::start);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int port() {
        String prometheusPort = System.getProperty(PROMETHEUS_PORT, "9090");
        try {
            return Integer.parseUnsignedInt(prometheusPort);
        } catch (Throwable e) {
            return 9090;
        }
    }

    private String context() {
        String ctx = System.getProperty(PROMETHEUS_CONTEXT, "/metrics");
        if (ctx.startsWith("/")) {
            return ctx;
        }
        return "/" + ctx;
    }
}
