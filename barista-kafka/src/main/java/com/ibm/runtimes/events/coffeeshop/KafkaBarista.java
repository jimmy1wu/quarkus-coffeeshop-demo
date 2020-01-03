package com.ibm.runtimes.events.coffeeshop;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.ibm.runtimes.events.coffeeshop.PreparationState.State;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Processor;

@ApplicationScoped
public class KafkaBarista {

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.completed.group.id")
    String name;

    private Random random = new Random();

    private Set<Order> completedOrders = Collections.synchronizedSet(new HashSet<Order>());

    private Jsonb jsonb = JsonbBuilder.create();

    @Incoming("orders")
    @Outgoing("pendingOrders")
    public Processor<Message<String>,Message<String>> filterCompletedOrders() {
        return ReactiveStreams.<Message<String>>builder().filter(message -> {
            Order order = jsonb.fromJson(message.getPayload(), Order.class);
            if (completedOrders.contains(order)) {
                System.out.println("Order " + order.getOrderId() + " already completed, filtering.");
                return false;
            }
            message.ack();
            return true;
        }).buildRs();
    }

    @Incoming("pendingOrders")
    @Outgoing("queue")
    public CompletionStage<String> prepare(String message) {
        Order order = jsonb.fromJson(message, Order.class);
        System.out.println("Barista " + name + " is going to prepare a " + order.getProduct());
        return makeIt(order)
                .thenApply(beverage -> PreparationState.ready(order, beverage));
    }

    @Incoming("completed")
    public void notifyOrderCompleted(String message) {
        PreparationState completion = jsonb.fromJson(message, PreparationState.class);
        if (completion.getState() == State.READY) {
            completedOrders.add(completion.getOrder());
        }
    }

    private CompletionStage<Beverage> makeIt(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Preparing a " + order.getProduct());
            prepareCoffee();
            return new Beverage(order, name);
        }, executor);
    }

    private Executor executor = Executors.newSingleThreadExecutor();

    private void prepareCoffee() {
        try {
            Thread.sleep(random.nextInt(5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
