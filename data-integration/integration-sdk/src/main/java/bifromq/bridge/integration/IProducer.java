package bifromq.bridge.integration;

public interface IProducer {
    IProducer DUMMY = new IProducer() {
        @Override
        public void produce(IntegratedMessage message) {

        }

        @Override
        public void close() {

        }
    };
    void produce(IntegratedMessage message);

    void close();
}
