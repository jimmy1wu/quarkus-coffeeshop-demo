package com.ibm.runtimes.events.coffeeshop;

public interface EventSource {

	 void subscribeToTopic(String topicName, CoffeeEventType eventType, EventHandler handler);

}
