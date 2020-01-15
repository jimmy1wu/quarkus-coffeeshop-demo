package com.ibm.runtimes.events.coffeeshop;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.ReadKeyValues.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kafka.server.KafkaConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;

public class KafkaEventEmitterTest {

    private static final String TEST_DATA = "testData";
    private static final String TOPIC_NAME = "test-topic";
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
    public void shouldProduceMessageToKafkaTopic() throws InterruptedException, ExecutionException {
        KafkaEventEmitter testable = new KafkaEventEmitter(kafkaBroker.getBrokerList(), TOPIC_NAME);
        testable.sendEvent(TEST_DATA);
        List<String> topicValues = kafkaBroker.readValues(from(TOPIC_NAME).useDefaults());
        assertThat(topicValues, hasSize(1));
        assertThat(topicValues.get(0), is(equalTo(TEST_DATA)));
    }
}