package bifromq.integration.demo;

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
        integrator.onMessageArrive()
                .doOnComplete(IProducer.DUMMY::close)
                .subscribe(IProducer.DUMMY::produce);
    }
}