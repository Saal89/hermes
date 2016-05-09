package pl.allegro.tech.hermes.consumers.supervisor;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AssignedConsumers implements Iterable<Consumer> {
    private final ConcurrentHashMap<SubscriptionName, Consumer> consumers = new ConcurrentHashMap<>();


    @Override
    public Iterator<Consumer> iterator() {
        return consumers.values().iterator();
    }
}
