package com.ibm.runtimes.events.coffeeshop;

public interface EventSource {

	 <T> void subscribeToTopic(String topicName, EventHandler<T> handler, Class<T> type);

    void close();
}
