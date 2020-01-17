package com.ibm.runtimes.events.coffeeshop;

public interface EventHandler<T> {
    public void handle(T event);
}