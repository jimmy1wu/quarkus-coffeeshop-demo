package com.ibm.runtimes.events.coffeeshop;

import java.util.concurrent.Executors;

public class KafkaEventSource implements EventSource {

	
	private String brokerList;

	public KafkaEventSource(String brokerList) {
		this.brokerList = brokerList;
	}

	public <T> void subscribeToTopic(String topic, EventHandler<T> handler, Class<T> thing) {
		KafkaConsumerWorker<T> worker = new KafkaConsumerWorker<T>(brokerList, "myConsumer", topic, "Mr. Hungry", handler, thing);
		Executors.newSingleThreadExecutor().submit(worker);
	}

	
}
