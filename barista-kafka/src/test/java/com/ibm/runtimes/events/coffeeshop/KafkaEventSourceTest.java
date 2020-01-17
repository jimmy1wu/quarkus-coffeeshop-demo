package com.ibm.runtimes.events.coffeeshop;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.SendValues.to;
import static net.mguenther.kafka.junit.ObserveKeyValues.on;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kafka.common.OffsetAndMetadata;
import kafka.server.KafkaConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;

public class KafkaEventSourceTest {
    private static final String EVENT_DATA = "{\"name\":\"Demo-1\", \"orderId\":\"1\", \"product\":\"espresso\"}";
    private static final String TOPIC_NAME = "orders";
    private EmbeddedKafkaCluster kafkaBroker;

    @BeforeEach
    public void setupKafka() {
        kafkaBroker = provisionWith(EmbeddedKafkaClusterConfig.create()
                .provisionWith(
                        EmbeddedKafkaConfig.create().with(KafkaConfig.AutoCreateTopicsEnableProp(), "true").build())
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
        EventHandler<Order> handler = (EventHandler<Order>) mock(EventHandler.class);

        testable.subscribeToTopic(TOPIC_NAME, handler, Order.class);
        kafkaBroker.send(to(TOPIC_NAME, EVENT_DATA).useDefaults());

        Order expectedEvent = new Order();
        expectedEvent.setName("Demo-1");
        expectedEvent.setOrderId("1");
        expectedEvent.setProduct("espresso");
        verify(handler, timeout(5000)).handle(expectedEvent);
    }

    @Test
    public void shouldCommitAfterHandlerReturns() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        KafkaEventSource testable = new KafkaEventSource(kafkaBroker.getBrokerList());

        testable.subscribeToTopic(TOPIC_NAME, order -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }
        }, Order.class);
        
        kafkaBroker.send(to(TOPIC_NAME, EVENT_DATA).useDefaults());
        kafkaBroker.observe(on(TOPIC_NAME,1).useDefaults());
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker.getBrokerList());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "myConsumer");        
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
       
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String,String> offsetCheckConsumer = new KafkaConsumer<>(props);
        offsetCheckConsumer.subscribe(Collections.singletonList(TOPIC_NAME));

        
        assertThat(offsetCheckConsumer.committed(new TopicPartition(TOPIC_NAME, 0)), is(nullValue()));
        
        latch.countDown();

        Thread.sleep(1000);

        assertThat(offsetCheckConsumer.committed(new TopicPartition(TOPIC_NAME, 0)), is(notNullValue()));
        assertThat(offsetCheckConsumer.committed(new TopicPartition(TOPIC_NAME, 0)).offset(), is(equalTo(0L)));
    }

    public class Thing {
        String name;

        public Thing() {
            
        }
        public void setName(String name) {
            this.name = name;
        }
    }
}