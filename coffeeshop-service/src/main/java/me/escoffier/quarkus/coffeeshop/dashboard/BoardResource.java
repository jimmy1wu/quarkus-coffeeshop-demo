package me.escoffier.quarkus.coffeeshop.dashboard;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/queue")
@ApplicationScoped
public class BoardResource {

    private static Logger logger = LoggerFactory.getLogger(BoardResource.class);

    private AtomicInteger sseId = new AtomicInteger(0);

    @Context
    private Sse sse;
    private SseBroadcaster broadcaster;

    private SseBroadcaster getBroadcaster() {
        if (broadcaster == null) {
            broadcaster = sse.newBroadcaster();
        }
        return broadcaster;
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribeToQueue(@Context SseEventSink eventSink) {
        logger.info("Client subscribed to SSE events " + eventSink);
        // OutboundSseEvent queueEvent =
        // sse.newEvent("dummy","{\"order\":{\"name\":\"Dummy\",\"orderId\":\"FAKE_ONE\",\"product\":\"test\"},\"state\":\"IN_QUEUE\"}");
        // eventSink.send(queueEvent);

        getBroadcaster().register(eventSink);
    }

    @Incoming("beverages")
    public void processQueue(String data) {
        logger.info("GOT: " + data);
        OutboundSseEvent queueEvent = sse.newEventBuilder()
                                        .name("beverage")
                                        .id(Integer.toString(sseId.getAndIncrement()))
                                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                        .build();
        if (queueEvent != null) {
            logger.info("About to send SSE: " + queueEvent + "to broadcaster: " + getBroadcaster());
            getBroadcaster().broadcast(queueEvent);
        } else {
            logger.info("Created a null event!");
        }
    }
}
