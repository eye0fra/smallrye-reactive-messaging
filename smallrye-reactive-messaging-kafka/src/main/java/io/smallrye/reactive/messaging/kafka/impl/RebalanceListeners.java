package io.smallrye.reactive.messaging.kafka.impl;

import static io.smallrye.reactive.messaging.kafka.i18n.KafkaLogging.log;

import java.lang.reflect.Field;
import java.util.*;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;

import io.smallrye.reactive.messaging.kafka.KafkaConnectorIncomingConfiguration;
import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import io.smallrye.reactive.messaging.kafka.commit.KafkaCommitHandler;
import io.smallrye.reactive.messaging.kafka.i18n.KafkaExceptions;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import io.vertx.kafka.client.consumer.impl.KafkaReadStreamImpl;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;

public class RebalanceListeners {

    static ConsumerRebalanceListener createRebalanceListener(
            KafkaConnectorIncomingConfiguration config,
            String consumerGroup,
            Instance<KafkaConsumerRebalanceListener> instances,
            Consumer<?, ?> consumer,
            KafkaCommitHandler commitHandler) {
        Optional<KafkaConsumerRebalanceListener> rebalanceListener = findMatchingListener(config, consumerGroup, instances);

        if (rebalanceListener.isPresent()) {
            KafkaConsumerRebalanceListener listener = rebalanceListener.get();
            return new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    log.executingConsumerRevokedRebalanceListener(consumerGroup);
                    // TODO Why don't we call the commit handler?
                    try {
                        listener.onPartitionsRevoked(consumer, partitions);
                        log.executedConsumerRevokedRebalanceListener(consumerGroup);
                    } catch (RuntimeException e) {
                        log.unableToExecuteConsumerRevokedRebalanceListener(consumerGroup, e);
                        throw e;
                    }
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    commitHandler.partitionsAssigned(partitions);
                    try {
                        listener.onPartitionsAssigned(consumer, partitions);
                        log.executedConsumerAssignedRebalanceListener(consumerGroup);
                    } catch (RuntimeException e) {
                        log.reEnablingConsumerForGroup(consumerGroup);
                        throw e;
                    }
                }
            };
        } else {
            return new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    commitHandler.partitionsRevoked(partitions);
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    commitHandler.partitionsAssigned(partitions);
                }
            };
        }
    }

    private static Optional<KafkaConsumerRebalanceListener> findMatchingListener(
            KafkaConnectorIncomingConfiguration config, String consumerGroup,
            Instance<KafkaConsumerRebalanceListener> instances) {
        return config.getConsumerRebalanceListenerName()
                .map(name -> {
                    log.loadingConsumerRebalanceListenerFromConfiguredName(name);
                    Instance<KafkaConsumerRebalanceListener> matching = instances.select(NamedLiteral.of(name));
                    // We want to fail if a name if set, but no match or too many matches
                    if (matching.isUnsatisfied()) {
                        throw KafkaExceptions.ex.unableToFindRebalanceListener(name, config.getChannel());
                    } else if (matching.stream().count() > 1) {
                        throw KafkaExceptions.ex.unableToFindRebalanceListener(name, config.getChannel(),
                                (int) matching.stream().count());
                    } else if (matching.stream().count() == 1) {
                        return Optional.of(matching.get());
                    } else {
                        return Optional.<KafkaConsumerRebalanceListener> empty();
                    }
                })
                .orElseGet(() -> {
                    Instance<KafkaConsumerRebalanceListener> matching = instances.select(NamedLiteral.of(consumerGroup));
                    if (!matching.isUnsatisfied()) {
                        log.loadingConsumerRebalanceListenerFromGroupId(consumerGroup);
                        return Optional.of(matching.get());
                    }
                    return Optional.empty();
                });
    }

    /**
     * HACK - inject the listener using reflection to replace the one set by vert.x
     *
     * @param consumer the consumer
     * @param listener the listener
     */
    @SuppressWarnings("rawtypes")
    public static void inject(KafkaConsumer<?, ?> consumer, ConsumerRebalanceListener listener) {
        KafkaReadStream readStream = consumer.getDelegate().asStream();
        if (readStream instanceof KafkaReadStreamImpl) {
            try {
                Field field = readStream.getClass().getDeclaredField("rebalanceListener");
                field.setAccessible(true);
                field.set(readStream, listener);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot inject rebalance listener", e);
            }
        } else {
            throw new IllegalArgumentException("Cannot inject rebalance listener - not a Kafka Read Stream");
        }
    }
}
