package com.ibm.runtimes.events.coffeeshop;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

public class KafkaConsumerWorker<T> implements Runnable
{
    private final String topic;
    private ExecutorService executor;
    private String consumerName;
    private KafkaConsumer<String, String> consumer;
    private EventHandler<T> handler;
    private Jsonb jsonb = JsonbBuilder.create();
    private Class<T> eventType;
    private Map<TopicPartition, OffsetAndMetadata> offsetMap = Collections.synchronizedMap(new HashMap<>());

    public KafkaConsumerWorker(String bootstrapServer, String consumerGroupId, String topic, String consumerName,
            EventHandler<T> handler, Class<T> eventType) {
        this.consumerName = consumerName;
        this.handler = handler;
        this.eventType = eventType;
        this.topic = topic;
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);        
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // The interval to commit the offset when automatic commit is enabled
        // props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Arrays.asList(topic));
        executor = Executors.newSingleThreadExecutor();
    }

    public void run() {
        try {
            while (true) {

                ConsumerRecords<String, String> records = this.consumer.poll(Duration.ofSeconds(7));
                System.out.printf("Consuming %d records %n", records.count());

                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("%s received partition %d offset %d: %s \n", this.consumerName, record.partition(), record.offset(), record.value());

                    executor.submit(() -> {
                        handler.handle(jsonb.fromJson(record.value(), eventType));
                        System.out.println("Event handler processed event: " + record.value());
                        offsetMap.put(new TopicPartition(record.topic(), record.partition()),
                                new OffsetAndMetadata(record.offset()+1));
                    });

                }
                consumer.commitSync(offsetMap);
                for (Map.Entry<TopicPartition,OffsetAndMetadata> commmitEntry: offsetMap.entrySet()) {
                    System.out.printf("Committed partition %d offset %d \n", commmitEntry.getKey().partition(), commmitEntry.getValue().offset());
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            this.consumer.close();
        }
    }

    public void close() {
        this.consumer.wakeup();
    }
}