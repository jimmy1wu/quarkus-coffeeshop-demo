package com.ibm.runtimes.events.coffeeshop;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.runtimes.events.coffeeshop.model.Order;
import com.ibm.runtimes.events.coffeeshop.model.PreparationState;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CoffeeShopResource {
    private static Logger logger = LoggerFactory.getLogger(CoffeeShopResource.class);
    
    @Inject
    Jsonb jsonb;

    class EventProducer {

        private BlockingQueue<String> buffer;
        private String name;

        EventProducer(String name) {
            this.name = name;
            buffer = new LinkedBlockingQueue<>();
        }

        String nextEvent() {
            String event;
            try {
                logger.debug("Producer: " + name + " asked for next event");
                event = buffer.take();
                logger.debug("Producer: " + name + " Returning event: " + event);
                return event;
            } catch (InterruptedException e) {
                logger.error("Producer: " + name + " interrupted!");
                return null;
            }
        }    

        void addEvent(String event) {
            logger.debug("Producer: " + name + " Adding event: " + event);
            buffer.add(event);
        }
    }

    private EventProducer queueBuffer = new EventProducer("queue");
    private EventProducer orderBuffer = new EventProducer("orders");

    @Path("/messaging")  
    @POST
    public Order messaging(final Order order) {
        final Order processed = process(order);
        logger.info("Received an order: " + order);
        queueBuffer.addEvent(getPreparationState(processed));
        orderBuffer.addEvent(toJson(processed));
        return processed;
    }

    @Outgoing("queue")
    public PublisherBuilder<String> addToQueue() {
       return ReactiveStreams.generate(() -> queueBuffer.nextEvent());
    }

    @Outgoing("orders") 
    public PublisherBuilder<String> sendOrder() {
        return ReactiveStreams.generate(() -> orderBuffer.nextEvent());
    }

    private String toJson(final Order processed) {
        return jsonb.toJson(processed);
    }

    private String getPreparationState(final Order processed) {
        return PreparationState.queued(processed);
    }

    private Order process(final Order order) {
        return order.setOrderId(UUID.randomUUID().toString());
    }
}
