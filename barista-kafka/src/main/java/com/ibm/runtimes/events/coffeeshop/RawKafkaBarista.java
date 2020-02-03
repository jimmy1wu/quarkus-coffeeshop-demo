package com.ibm.runtimes.events.coffeeshop;

import com.ibm.runtimes.events.coffeeshop.PreparationState.State;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class RawKafkaBarista {

    private static Logger logger = LoggerFactory.getLogger(RawKafkaBarista.class);
    private final String baristaName;
    private Jsonb jsonb = JsonbBuilder.create();
    private EventEmitter emitter;
    private Executor executor;
    private Set<Order> completedOrders = Collections.synchronizedSet(new HashSet<Order>());

    public RawKafkaBarista(EventEmitter emitter, Executor executor, KafkaEventSource source, String baristaName) {
        this.emitter = emitter;
        this.executor = executor;
        this.baristaName = baristaName;
        logger.debug("Starting raw kafka barista");

        source.subscribeToTopic("orders", this::handleIncomingOrder, Order.class, "baristas");
        source.subscribeToTopic("queue", this::handleOrderUpdate, PreparationState.class, baristaName);
    }

    public void handleIncomingOrder(Order order) {
        if (completedOrders.contains(order)) {
            logger.debug("Order " + order.getOrderId() + " has already been completed, skipping");
        } else {
            logger.debug("Barista " + baristaName + " + has received order " + order.getOrderId());
            Beverage beverage = makeIt(order);
            if (completedOrders.contains(order)) {
                logger.info("Someone else has now prepared order " + order.getOrderId() +" - I'll throw it away.");
                return;
            }
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
        return new Beverage(order, baristaName);
    }

    private void prepareCoffee() {
        try {
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
