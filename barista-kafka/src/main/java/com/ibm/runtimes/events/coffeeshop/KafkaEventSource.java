package com.ibm.runtimes.events.coffeeshop;

import java.util.concurrent.Executors;

public class KafkaEventSource implements EventSource {

	
	private String brokerList;

	public KafkaEventSource(String brokerList) {
		this.brokerList = brokerList;
	}

	public void subscribeToTopic(String topic, CoffeeEventType eventType, EventHandler handler) {
		KafkaConsumerWorker worker = new KafkaConsumerWorker(brokerList, "myConsumer", topic, "Mr. Hungry", handler, eventType);
		Executors.newSingleThreadExecutor().submit(worker);
	}

	
}
