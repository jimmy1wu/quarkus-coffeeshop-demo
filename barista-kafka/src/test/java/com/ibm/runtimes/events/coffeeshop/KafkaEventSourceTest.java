package com.ibm.runtimes.events.coffeeshop;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.ObserveKeyValues.on;
import static net.mguenther.kafka.junit.SendValues.to;
import static net.mguenther.kafka.junit.ReadKeyValues.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kafka.server.KafkaConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;
import net.mguenther.kafka.junit.TopicConfig;

public class KafkaEventSourceTest {
    private static final String EVENT_DATA = "{\"name\":\"Demo-1\", \"orderId\":\"1\", \"product\":\"espresso\"}";
    private static final String TOPIC_NAME = "orders";
    private EmbeddedKafkaCluster kafkaBroker;
    private Jsonb jsonb = JsonbBuilder.create();

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
        kafkaBroker.observe(on(TOPIC_NAME, 1).useDefaults());

        // Before we allow the handler function to complete, there should be no committed offset
        assertThat(testable.getCommittedOffset(TOPIC_NAME, 0), is(equalTo(-1L)));

        // Release the handler function
        latch.countDown();

        assertThatEventually(() -> testable.getCommittedOffset(TOPIC_NAME, 0), is(equalTo(0L)), Duration.ofSeconds(20));
    }

    @Test
    public void shouldNotReplayMessagesAfterRebalance() throws InterruptedException {
        kafkaBroker.createTopic(TopicConfig.forTopic(TOPIC_NAME).withNumberOfPartitions(2).build());
        kafkaBroker.send(to(TOPIC_NAME,
                makeOrderJson("pig"),
                makeOrderJson("cow"),
                makeOrderJson("sheep"),
                makeOrderJson("chicken"),
                makeOrderJson("dog"),
                makeOrderJson("cat"))
                .useDefaults());

        // Used to control the number of events the handler will consume
        Semaphore sem = new Semaphore(4);
        // Used to synchronise the test with handler
        CountDownLatch latch = new CountDownLatch(4);

        KafkaEventSource testable = new KafkaEventSource(kafkaBroker.getBrokerList());
        List<Order> processedMessages = new ArrayList<>();

        testable.subscribeToTopic(TOPIC_NAME, order -> {
            try {
                sem.acquire();
                processedMessages.add(order);
                latch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, Order.class);

        //kafkaBroker.observe(on(TOPIC_NAME,4).useDefaults());
        latch.await();
        assertThat(processedMessages, hasSize(4));

        kafkaBroker.readValues(from(TOPIC_NAME).with(ConsumerConfig.GROUP_ID_CONFIG, "myConsumer").build());

        sem.release();
        sem.release();
        sem.release();
        sem.release();

        Thread.sleep(1000);

        System.out.println(processedMessages);
        assertThat(processedMessages, hasSize(6));

    }

    private String makeOrderJson(String name) {
        Order order = new Order();
        order.setName(name);
        order.setOrderId(name);
        return jsonb.toJson(order);
    }

    private static <T> void assertThatEventually(Supplier<T> yieldsExpected, Matcher<T> matcher, Duration timeout) throws InterruptedException {
        Duration step = Duration.ofMillis(500);
        Duration timeRemaining = timeout;

        while (!timeRemaining.isZero()) {
            if (matcher.matches(yieldsExpected.get())) {
                return;
            }
            Thread.sleep(step.toMillis());
            timeRemaining = timeRemaining.minus(step);
        }
        assertThat(yieldsExpected.get(),matcher);
    }

}