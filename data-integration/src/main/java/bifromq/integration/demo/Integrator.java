package bifromq.integration.demo;

import io.reactivex.subjects.PublishSubject;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Integrator implements IIntegrator {
    private int clientNum;
    private String topicFilter;
    private String userName;
    private String password;
    private boolean cleanSession;
    private String host;
    private int port;
    private String clientPrefix;
    private Vertx vertx;
    private List<MqttClient> clients = new ArrayList<>();
    private final PublishSubject<IntegratedMessage> emitter = PublishSubject.create();
    private final String DEFAULT_CLIENT_PREFIX = "data-integrator-";

    @Builder
    public Integrator(@NonNull String topicFilter,
                      @NonNull String groupName,
                      @NonNull String userName,
                      @NonNull String password,
                      @NonNull int clientNum,
                      @NonNull boolean cleanSession,
                      @NonNull String host,
                      @NonNull int port,
                      Vertx vertx,
                      String clientPrefix) {
        this.topicFilter = getTopicFilter(groupName, topicFilter);
        this.userName = userName;
        this.password = password;
        this.clientNum = clientNum;
        this.cleanSession = cleanSession;
        this.host = host;
        this.port = port;
        this.vertx = vertx == null ? Vertx.vertx() : vertx;
        this.clientPrefix = clientPrefix == null ? DEFAULT_CLIENT_PREFIX : clientPrefix;
        initClients();
    }

    @Override
    public PublishSubject<IntegratedMessage> onMessageArrive() {
        return emitter;
    }

    private void initClients() {
        for (int idx = 0; idx < clientNum; idx++) {
            MqttClientOptions options = getMqttClientOptions(idx);
            MqttClient client = MqttClient.create(vertx, options);
            client.connect(port, host, connAck -> {
                if (connAck.failed()) {
                    log.error("clientId: {} connect to BifroMQ failed: ", client.clientId(), connAck.cause());
                }else {
                    client.publishHandler(this::handlePublishedMsg);
                    client.subscribe(topicFilter, 1, event -> {
                        if (event.failed()) {
                            log.error("clientId: {} subscribe topicFilter: {} failed: ", event.cause());
                        }
                    });
                }
            });
            clients.add(client);
        }
    }

    private String getTopicFilter(String groupName, String actualTopicFilter) {
        return "$share/" + groupName + "/" + actualTopicFilter;
    }

    private MqttClientOptions getMqttClientOptions(int idx) {
        MqttClientOptions options = new MqttClientOptions()
                .setClientId(clientPrefix + idx)
                .setUsername(userName)
                .setPassword(password)
                .setMaxInflightQueue(1000)
                .setAckTimeout(30)
                .setCleanSession(cleanSession)
                .setKeepAliveInterval(600)
                .setMaxMessageSize(512)
                .setKeepAliveInterval(30);
        return options;
    }

    private void handlePublishedMsg(MqttPublishMessage message) {
        IntegratedMessage integratedMessage = IntegratedMessage.builder()
                .topic(message.topicName())
                .qos(message.qosLevel().value())
                .payload(message.payload().getBytes())
                .build();
        emitter.onNext(integratedMessage);
    }
}
