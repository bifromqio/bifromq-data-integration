package bifromq.integration.demo;

public interface IProducer {
    IProducer DUMMY = new IProducer() {
        @Override
        public void produce(IntegratedMessage message) {
            System.out.println(message);
        }

        @Override
        public void close() {
            System.out.println("producer is closed");
        }
    };
    void produce(IntegratedMessage message);

    void close();
}
