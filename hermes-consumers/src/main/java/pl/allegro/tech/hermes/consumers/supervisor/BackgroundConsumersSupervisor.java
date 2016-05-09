package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class BackgroundConsumersSupervisor implements ConsumersSupervisor {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundConsumersSupervisor.class);
    private final SubscriptionRepository subscriptionRepository;
    private final TopicRepository topicRepository;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;

    private ConsumerSupervisorProcess supervision;
    private ConsumerFactory consumerFactory;
    private final List<OffsetsStorage> offsetsStorages;
    private final HermesMetrics hermesMetrics;
    private final UndeliveredMessageLogPersister undeliveredMessageLogPersister;
    private ConsumersExecutorService consumersExecutorService;
    private ConfigFactory configs;
    private OffsetCommitter offsetCommitter;
    private List<MessageCommitter> messageCommitters;

    private final ScheduledExecutorService scheduledExecutor;

    private final AssignedConsumers assignedConsumers;

    public BackgroundConsumersSupervisor(ConfigFactory configFactory,
                                         SubscriptionRepository subscriptionRepository,
                                         TopicRepository topicRepository,
                                         SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                                         ConsumersExecutorService executor,
                                         ConsumerFactory consumerFactory,
                                         List<MessageCommitter> messageCommitters,
                                         List<OffsetsStorage> offsetsStorages,
                                         HermesMetrics hermesMetrics,
                                         UndeliveredMessageLogPersister undeliveredMessageLogPersister) {
        this.subscriptionRepository = subscriptionRepository;
        this.topicRepository = topicRepository;
        this.subscriptionOffsetChangeIndicator = subscriptionOffsetChangeIndicator;
        this.consumersExecutorService = executor;
        this.consumerFactory = consumerFactory;
        this.offsetsStorages = offsetsStorages;
        this.hermesMetrics = hermesMetrics;
        this.undeliveredMessageLogPersister = undeliveredMessageLogPersister;
        this.messageCommitters = messageCommitters;

        this.assignedConsumers = new AssignedConsumers();
        this.offsetCommitter = new OffsetCommitter(() -> assignedConsumers, messageCommitters, configFactory);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("BackgroundConsumersSupervisor-%d")
                .setUncaughtExceptionHandler((t, e) -> logger.error("Exception from supervisor with name {}", t.getName(), e)).build();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public void assignConsumerForSubscription(Subscription subscription) {
        logger.info("Creating consumer for {}", subscription.getId());
        try {
            Consumer consumer = consumerFactory.createConsumer(subscription);
            logger.info("Created consumer for {}", subscription.getId());
            supervision.addConsumer(subscription.toSubscriptionName(), consumer);
            logger.info("Consumer for {} was added for execution", subscription.getId());
        } catch (Exception ex) {
            logger.info("Failed to create consumer for subscription {} ", subscription.getId(), ex);
        }
    }

    @Override
    public void deleteConsumerForSubscriptionName(SubscriptionName subscription) {
        supervision.removeConsumer(subscription, true);
    }

    @Override
    public void updateSubscription(Subscription subscription) {
        supervision.updateConsumer(subscription);
    }

    @Override
    public void start() {
        scheduledExecutor.scheduleAtFixedRate(
                supervision,
                configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL),
                configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL),
                TimeUnit.SECONDS);
        offsetCommitter.start();
        undeliveredMessageLogPersister.start();
    }

    @Override
    public void shutdown() {
        scheduledExecutor.shutdown();
    }

    @Override
    public void retransmit(SubscriptionName subscription) {

    }

    @Override
    public void restartConsumer(SubscriptionName subscription) {

    }


}
