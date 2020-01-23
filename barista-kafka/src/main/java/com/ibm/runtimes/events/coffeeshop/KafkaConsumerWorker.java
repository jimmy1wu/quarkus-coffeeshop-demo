package com.ibm.runtimes.events.coffeeshop;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.internals.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConsumerWorker<T> implements Runnable, ConsumerRebalanceListener
{
    private final String topic;
    private ExecutorService executor;
    private String consumerName;
    private KafkaConsumer<String, String> consumer;
    private EventHandler<T> handler;
    private Jsonb jsonb = JsonbBuilder.create();
    private Class<T> eventType;
    private Map<TopicPartition, OffsetAndMetadata> offsetMap = Collections.synchronizedMap(new HashMap<>());
    private final BlockingQueue<ConsumerRecord<String, String>> messageQueue;
    private static Logger logger = LoggerFactory.getLogger(KafkaConsumerWorker.class);

    public KafkaConsumerWorker(String bootstrapServer, String consumerGroupId, String topic, String consumerName,
            EventHandler<T> handler, Class<T> eventType) {
        this.consumerName = consumerName;
        this.handler = handler;
        this.eventType = eventType;
        this.topic = topic;
        Properties props = getConsumerConfig(bootstrapServer, consumerGroupId);
        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(topic), this);
        messageQueue = new LinkedBlockingQueue<>();
        executor = Executors.newSingleThreadExecutor();
    }

    private Properties getConsumerConfig(String bootstrapServer, String consumerGroupId) {
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
        return props;
    }

    public void run() {
        try {
            executor.submit(this::processMessages);

            while (true) {
                ConsumerRecords<String, String> records = this.consumer.poll(Duration.ofSeconds(7));
                logger.debug("Consuming {} records \n", records.count());
                deliverMessagesToHandler(records);
                commitProcessedOffsets();
            }
        } catch (Exception e){
            e.printStackTrace();
            this.consumer.close();
        }
    }

    private void commitProcessedOffsets() {
        consumer.commitSync(offsetMap);
        for (Map.Entry<TopicPartition, OffsetAndMetadata> commmitEntry: offsetMap.entrySet()) {
            logger.debug("Committed partition {} offset {} \n", commmitEntry.getKey().partition(), commmitEntry.getValue().offset());
        }
    }

    private void deliverMessagesToHandler(ConsumerRecords<String, String> records) throws InterruptedException {
        for (ConsumerRecord<String, String> record : records) {
            logger.debug("{} received partition {} offset {}: {} \n", this.consumerName, record.partition(), record.offset(), record.value());
            messageQueue.put(record);
            logger.debug("Message added, queue now contains {} messages", messageQueue.size());
        }
    }

    public void processMessages() {
        logger.debug("Starting message processor thread");
        while (true) {
            try {
                ConsumerRecord<String, String> record  = messageQueue.take();
                TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                logger.debug("Message removed, queue now contains {} messages", messageQueue.size());
                OffsetAndMetadata result = offsetMap.get(tp);
                if (result != null && result.offset() > record.offset()) {
                    logger.debug("Already processed message from partition {} offset {}, skipping", record.partition(), record.offset());
                    continue;
                }
                handler.handle(jsonb.fromJson(record.value(), eventType));
                logger.debug("Event handler processed event: {} \n", record.value());
                offsetMap.put(tp,new OffsetAndMetadata(record.offset() + 1));
            } catch (InterruptedException e) {
                logger.error("Interrupted while taking message from message queue", e);
            }
        }
    }
    public void close() {
        this.consumer.wakeup();
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        logger.debug("Partition revoked, clearing message queue");
        messageQueue.clear();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

    }
}