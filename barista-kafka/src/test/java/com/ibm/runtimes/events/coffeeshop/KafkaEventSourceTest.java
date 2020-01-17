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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
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
        kafkaBroker.observe(on(TOPIC_NAME,1).useDefaults());
        CommittedOffsetObserver observer = new CommittedOffsetObserver(kafkaBroker.getBrokerList(), TOPIC_NAME, "myConsumer");

        // Before we allow the handler function to complete, there should be no committed offset
        assertThat(observer.getCommittedOffset(), is(equalTo(-1L)));
        
        // Release the handler function
        latch.countDown();

        Thread.sleep(1000);

        assertThat(observer.getCommittedOffset(), is(equalTo(0L)));
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
        //CountDownLatch latch = new CountDownLatch(4);

        KafkaEventSource testable = new KafkaEventSource(kafkaBroker.getBrokerList());
        List<Order> processedMessages = new ArrayList<>();

        testable.subscribeToTopic(TOPIC_NAME, order -> {
            try {
                sem.acquire();
                processedMessages.add(order);
                //latch.countDown();
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }
        }, Order.class);

        kafkaBroker.observe(on(TOPIC_NAME,4).useDefaults());
        //latch.await();
        assertThat(processedMessages,hasSize(4));

        kafkaBroker.readValues(from(TOPIC_NAME).with(ConsumerConfig.GROUP_ID_CONFIG, "myConsumer").build());

        sem.release();
        sem.release();
        sem.release();
        sem.release();

        Thread.sleep(1000);

        assertThat(processedMessages,hasSize(6));

    }

    private String makeOrderJson(String name) {
        Order order = new Order();
        order.setName(name);
        return jsonb.toJson(order);
    }

    class CommittedOffsetObserver {

        private KafkaConsumer<String,String> consumer;
        private String topic;

        CommittedOffsetObserver(String brokerList, String topic, String consumerGroup) {
            this.topic = topic;
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);        
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
           
            props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(topic));
        }

        long getCommittedOffset() {
            OffsetAndMetadata data = consumer.committed(new TopicPartition(topic,0));
            if (data == null) return -1;
            return data.offset();
        }


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