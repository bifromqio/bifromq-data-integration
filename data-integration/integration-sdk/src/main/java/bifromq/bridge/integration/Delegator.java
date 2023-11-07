package bifromq.bridge.integration;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Delegator implements IProducer {
    private final IProducer delegator;
    private final Executor ioExecutor;

    public Delegator(IProducer delegator, int workerThreads) {
        this.delegator = delegator;
        this.ioExecutor = ExecutorServiceMetrics.monitor(Metrics.globalRegistry,
                new ThreadPoolExecutor(workerThreads,
                        workerThreads, 0L,
                        TimeUnit.MILLISECONDS, new LinkedTransferQueue<>(),
                        getThreadFactory()), "bridge-executor");
    }

    private ThreadFactory getThreadFactory() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private AtomicInteger threadCount = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("bridge-executor-" + threadCount.incrementAndGet());
                return thread;
            }
        };
        return threadFactory;
    }

    @Override
    public void produce(IntegratedMessage message) {
        ioExecutor.execute(() -> delegator.produce(message));
    }

    @Override
    public void close() {
        delegator.close();
    }
}
