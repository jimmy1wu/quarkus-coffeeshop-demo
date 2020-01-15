package com.ibm.runtimes.events.coffeeshop;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaEventEmitter implements EventEmitter {

    private KafkaProducer<String, String> producer;
    private String topic;

    public KafkaEventEmitter(String kafkaConnectString, String topicName) {
        topic = topicName;
        Properties producerConfig = new Properties();
        producerConfig.put("bootstrap.servers", kafkaConnectString);
        producerConfig.put("client.id", "foo");
        producerConfig.put("acks", "all");
        producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(producerConfig);
    }

    @Override
    public void sendEvent(String payload) throws InterruptedException, ExecutionException {
        ProducerRecord<String,String> record = new ProducerRecord<String,String>(topic, payload);
        producer.send(record).get();
        
    }

}
