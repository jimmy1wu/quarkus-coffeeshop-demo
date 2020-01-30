package com.ibm.runtimes.events.coffeeshop;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.ibm.runtimes.events.coffeeshop.PreparationState.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawKafkaBarista {

    private static Logger logger = LoggerFactory.getLogger(RawKafkaBarista.class);
    private Jsonb jsonb = JsonbBuilder.create();
    private EventEmitter emitter;
    private Executor executor;
    private Set<Order> completedOrders = Collections.synchronizedSet(new HashSet<Order>());

    public RawKafkaBarista(EventEmitter emitter, Executor executor, KafkaEventSource source) {
        this.emitter = emitter;
        this.executor = executor;
        logger.debug("Starting raw kafka barista");

        source.subscribeToTopic("orders", this::handleIncomingOrder, Order.class, "baristas");
        source.subscribeToTopic("queue", this::handleOrderUpdate, PreparationState.class, "fredrique");
    }

    public void handleIncomingOrder(Order order) {
        if (completedOrders.contains(order)) {
            logger.debug("Order " + order.getOrderId() + " has already been completed, skipping");
        } else {
            logger.debug("Barista Fred has received order " + order.getOrderId());
            Beverage beverage = makeIt(order);
            try {
                emitter.sendEvent(PreparationState.ready(order, beverage));
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Failed to emit event",e);
            }

        }
    }

    public void handleOrderUpdate(PreparationState result) {
        if (result.getState() == State.READY) {
            completedOrders.add(result.getOrder());
        }
    }

    private Beverage makeIt(Order order) {
        prepareCoffee();
        logger.debug("Order " + order.getOrderId() + " completed");
        completedOrders.add(order);
        return new Beverage(order, "Fred");
    }

    private void prepareCoffee() {
        try {
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
