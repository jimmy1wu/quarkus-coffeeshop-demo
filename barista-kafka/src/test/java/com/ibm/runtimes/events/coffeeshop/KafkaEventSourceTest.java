package com.ibm.runtimes.events.coffeeshop;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;

import static net.mguenther.kafka.junit.SendValues.to;
import static net.mguenther.kafka.junit.ReadKeyValues.from;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kafka.server.KafkaConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;

public class KafkaEventSourceTest {
    private static final String EVENT_DATA = "event-data";
    private static final String TOPIC_NAME = "orders";
    private EmbeddedKafkaCluster kafkaBroker;

    @BeforeEach
    public void setupKafka() {
        kafkaBroker = provisionWith(EmbeddedKafkaClusterConfig.create()
                                   .provisionWith(EmbeddedKafkaConfig.create()
                                   .with(KafkaConfig.AutoCreateTopicsEnableProp(), "true")
                                   .build())
                                   .build());
        kafkaBroker.start();
    }

    @AfterEach
    public void tearDownKafka() {
        kafkaBroker.stop();
    }

    @Test
    public void shouldDeliverEventToHandler() throws InterruptedException {
        KafkaEventSource testable = new KafkaEventSource(kafkaBroker.getBrokerList());
        EventHandler handler = mock(EventHandler.class);
        
        testable.subscribeToTopic(TOPIC_NAME,CoffeeEventType.ORDER,handler);
        kafkaBroker.send(to(TOPIC_NAME,EVENT_DATA).useDefaults());
        
        verify(handler, timeout(5000)).handle(CoffeeEventType.ORDER, EVENT_DATA);
    }
}