package com.ibm.runtimes.events.coffeeshop;

import java.util.concurrent.ExecutionException;

public interface EventEmitter {

	public void sendEvent(String string) throws InterruptedException, ExecutionException;

}
