package com.ibm.runtimes.events.coffeeshop;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class KafkaEventSource implements EventSource {

	
	private String brokerList;
	private Map<String,KafkaConsumerWorker<?>> workers;

	public KafkaEventSource(String brokerList) {
		this.brokerList = brokerList;
		this.workers = new HashMap<>();
	}

	public <T> void subscribeToTopic(String topic, EventHandler<T> handler, Class<T> thing) {
		subscribeToTopic(topic,handler,thing, "myConsumer");
	}

	@Override
	public void close() {
		for (KafkaConsumerWorker worker: workers.values()) {
			worker.close();
		}
	}

	public <T> void subscribeToTopic(String topic, EventHandler<T> handler, Class<T> thing, String consumerGroupId) {
		KafkaConsumerWorker<T> worker = new KafkaConsumerWorker<>(brokerList, consumerGroupId, topic, "Mr. Hungry", handler, thing);
		workers.put(topic,worker);

		Executors.newSingleThreadExecutor().submit(worker);
	}
}
