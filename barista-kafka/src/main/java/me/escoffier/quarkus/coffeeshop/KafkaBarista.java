package me.escoffier.quarkus.coffeeshop;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.inject.Inject;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class KafkaBarista {

    @Inject
    @ConfigProperty(name = "barista.name")
    String name;

    private static Logger logger = LoggerFactory.getLogger(KafkaBarista.class);

    private Random random = new Random();
    
    @Incoming("orders")
    @Outgoing("queue")
    public CompletionStage<String> prepare(String message) {
        Jsonb jsonb = JsonbBuilder.create();
        Order order = jsonb.fromJson(message, Order.class);
        logger.debug("Barista " + name + " has received order " + order.getOrderId());
        return makeIt(order)
                .thenApply(beverage -> PreparationState.ready(order, beverage));
                
    }

    private CompletionStage<Beverage> makeIt(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            prepare();
            logger.debug("Order " + order.getOrderId() + " completed");
            return new Beverage(order, name);
        }, executor);
    }

    private Executor executor = Executors.newSingleThreadExecutor();

    private void prepare() {
        try {
            Thread.sleep(random.nextInt(5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
