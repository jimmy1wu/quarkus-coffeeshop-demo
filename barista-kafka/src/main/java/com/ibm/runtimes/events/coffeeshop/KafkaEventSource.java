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
		KafkaConsumerWorker<T> worker = new KafkaConsumerWorker<T>(brokerList, "myConsumer", topic, "Mr. Hungry", handler, thing);
		workers.put(topic,worker);

		Executors.newSingleThreadExecutor().submit(worker);
	}

	@Override
	public void close() {
		for (KafkaConsumerWorker worker: workers.values()) {
			worker.close();
		}
	}


	public long getCommittedOffset(String topicName, int partition) {
		return workers.get(topicName).getCommittedOffset(partition);
	}
}
