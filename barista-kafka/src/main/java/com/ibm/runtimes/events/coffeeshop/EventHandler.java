package com.ibm.runtimes.events.coffeeshop;

public interface EventHandler {
    public void handle(CoffeeEventType type, String message);
}