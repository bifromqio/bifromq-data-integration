package bifromq.integration.demo;

import io.reactivex.subjects.PublishSubject;

public interface IIntegrator {
    PublishSubject<IntegratedMessage> onMessageArrive();
}
