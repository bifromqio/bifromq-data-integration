package bifromq.bridge.integration;

import io.netty.channel.EventLoop;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class Integrator implements IIntegrator {
    private int clientNum;
    private int maxMessageSize;
    private int inflightQueueSize;
    private String topicFilter;
    private String userName;
    private String password;
    private boolean cleanSession;
    private String host;
    private int port;
    private String clientPrefix;
    private Vertx vertx;
    private List<MqttClient> clients = new LinkedList<>();
    private final PublishSubject<IntegratedMessage> emitter = PublishSubject.create();
    private final IProducer delegator;
    private final String DEFAULT_CLIENT_PREFIX = "data-integrator-";
    private final int DEFAULT_INFLIGHT_QUEUE_SIZE = 100_000;
    private final int DEFAULT_MAX_MESSAGE_SIZE = 256 * 1024;

    @Builder
    public Integrator(@NonNull String topicFilter,
                      @NonNull String groupName,
                      @NonNull String userName,
                      @NonNull String password,
                      @NonNull int clientNum,
                      @NonNull boolean cleanSession,
                      @NonNull String host,
                      @NonNull int port,
                      @NonNull IProducer producer,
                      Vertx vertx,
                      String clientPrefix,
                      Integer maxMessageSize,
                      Integer inflightQueueSize,
                      Integer workerSize,
                      Integer bufferSize) {
        this.topicFilter = getTopicFilter(groupName, topicFilter);
        this.userName = userName;
        this.password = password;
        this.clientNum = clientNum;
        this.cleanSession = cleanSession;
        this.host = host;
        this.port = port;
        this.vertx = vertx == null ? Vertx.vertx() : vertx;
        this.clientPrefix = clientPrefix == null ? DEFAULT_CLIENT_PREFIX : clientPrefix;
        this.maxMessageSize = maxMessageSize == null ? DEFAULT_MAX_MESSAGE_SIZE : maxMessageSize;
        this.inflightQueueSize = inflightQueueSize == null ? DEFAULT_INFLIGHT_QUEUE_SIZE : inflightQueueSize;
        this.delegator = new Delegator(producer,
                workerSize == null ? Math.max(2, Runtime.getRuntime().availableProcessors() / 4) : workerSize,
                bufferSize == null ? 0 : bufferSize);
        this.emitter.doOnComplete(delegator::close)
                .subscribe(delegator::produce);
    }

    @Override
    public void start() {
        initClients();
    }

    @Override
    public void close() {
        closeClients().thenAccept(v -> {
            clients.clear();
            emitter.onComplete();
            vertx.close().andThen(event -> {
                if (event.failed()) {
                    log.error("close vertx failed: ", event.cause());
                }
            });
        });
    }

    private void initClients() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int idx = 0; idx < clientNum; idx++) {
            EventLoop eventLoop = vertx.nettyEventLoopGroup().next();
            int finalIdx = idx;
            eventLoop.execute(() -> {
                MqttClientOptions options = getMqttClientOptions(finalIdx);
                MqttClient client = MqttClient.create(vertx, options);
                CompletableFuture<Void> connectFuture = new CompletableFuture<>();
                futures.add(connectFuture);
                client.connect(port, host, connAck -> {
                    if (connAck.failed()) {
                        log.error("clientId: {} connect to BifroMQ failed: ", client.clientId(), connAck.cause());
                    }else {
                        client.publishHandler(this::handlePublishedMsg);
                        client.subscribe(topicFilter, 1, event -> {
                            if (event.failed()) {
                                log.error("clientId: {} subscribe topicFilter: {} failed: ", event.cause());
                                connectFuture.completeExceptionally(new RuntimeException("init client error"));
                            }else {
                                clients.add(client);
                                connectFuture.complete(null);
                            }
                        });
                    }
                });
            });
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v,e) -> {
                    if (e != null) {
                        log.error("failed to init clients: ", e);
                    }else {
                        log.info("init clients ok, clientNum: {}", clientNum);
                    }
                });
    }

    private String getTopicFilter(String groupName, String actualTopicFilter) {
        return "$share/" + groupName + "/" + actualTopicFilter;
    }

    private MqttClientOptions getMqttClientOptions(int idx) {
        MqttClientOptions options = new MqttClientOptions()
                .setClientId(clientPrefix + idx)
                .setUsername(userName)
                .setPassword(password)
                .setAckTimeout(30)
                .setCleanSession(cleanSession)
                .setKeepAliveInterval(600)
                .setMaxMessageSize(maxMessageSize)
                .setMaxInflightQueue(inflightQueueSize)
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

    private CompletableFuture<Void> closeClients() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        clients.forEach(client -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            futures.add(future);
            client.unsubscribe(topicFilter, unsubResult -> {
                if (unsubResult.failed()) {
                    log.error("clientId: {} unsubscribe topicFilter: {} failed: ",
                            client.clientId(), topicFilter, unsubResult.cause());
                }else {
                    client.disconnect(disconnectResult -> {
                        if (disconnectResult.failed()) {
                            log.error("clientId: {} disconnect failed: ", client.clientId(), disconnectResult.cause());
                        }else {
                            future.complete(null);
                        }
                    });
                }
            });
        });
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
