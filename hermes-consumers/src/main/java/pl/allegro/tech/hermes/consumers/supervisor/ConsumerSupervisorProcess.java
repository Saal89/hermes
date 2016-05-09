package pl.allegro.tech.hermes.consumers.supervisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.status.Status;

import java.time.Clock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

public class ConsumerSupervisorProcess implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerSupervisorProcess.class);


    private final Map<SubscriptionName, Long> timestamps = new HashMap<>();

    private final ConsumersExecutorService executor;
    private final Clock clock;

    private long unhealthyAfter = 60_000;

    public ConsumerSupervisorProcess(ConsumersExecutorService executor, Clock clock) {
        this.executor = executor;
        this.clock = clock;
    }

    @Override
    public void run() {
        long currentTime = clock.millis();
        Iterator<Map.Entry<SubscriptionName, Consumer>> iterator = consumers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SubscriptionName, Consumer> entry = iterator.next();
            Status status = entry.getValue().getStatus();
            if (isHealthy(currentTime, entry, status)) {
                switch (status.getType()) {
                    case NEW: start(entry.getKey(), entry.getValue()); break;
                    case STOPPED: remove(entry.getKey(), iterator); break;
                    default:
                        break;
                }
            } else {
                logger.info("Detected unhealthy consumer for {}", entry.getKey().getId());
                // restart?
            };
        }
    }

    private boolean isHealthy(long currentTime, Map.Entry<SubscriptionName, Consumer> entry, Status status) {
        Optional<Long> lastSeen = ofNullable(timestamps.get(entry.getKey()));
        if (lastSeen.isPresent()) {
            long delta = status.getTimestamp() - lastSeen.get();
            if (delta <= 0) {
                logger.info("Lost contact with consumer {}, status {}, delta {}", entry.getKey().getId(), status.getTimestamp(), delta);
                if (currentTime - status.getTimestamp() > unhealthyAfter) {
                    return false;
                }
            }
        }
        return true;
    }

    private void remove(SubscriptionName subscriptionName, Iterator<Map.Entry<SubscriptionName, Consumer>> iterator) {
        logger.info("Deleting consumer for {}", subscriptionName.getId());
        iterator.remove();
        logger.info("Deleted consumer for {}", subscriptionName.getId());
    }

    private void start(SubscriptionName subscriptionName, Consumer consumer) {
        logger.info("Starting consumer for {}", subscriptionName.getId());
        executor.execute(consumer);
    }

    public void addConsumer(SubscriptionName subscriptionName, Consumer consumer) {
        consumers.putIfAbsent(subscriptionName, consumer);
    }

    public void removeConsumer(SubscriptionName subscription, boolean removeOffsets) {
        consumers.get(subscription).stopConsuming();
    }

    public void updateConsumer(Subscription subscription) {
        consumers.get(subscription.toSubscriptionName()).updateSubscription(subscription);
    }
}
