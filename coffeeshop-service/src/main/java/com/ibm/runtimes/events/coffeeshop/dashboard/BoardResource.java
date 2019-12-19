package com.ibm.runtimes.events.coffeeshop.dashboard;

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
    private SseBroadcaster broadcaster;
    private Sse sse;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribeToQueue(@Context SseEventSink eventSink, @Context Sse sse) {
        logger.debug("Client subscribed to SSE events " + eventSink + " this = " + this);
        if (this.sse  == null) {
            this.sse = sse;
        }
        if (this.broadcaster == null) {
            this.broadcaster = sse.newBroadcaster();
        }
        broadcaster.register(eventSink);
    }

    @Incoming("beverages")
    public void processQueue(String data) {
        logger.debug("GOT: " + data);
        if (broadcaster == null) {
            logger.info("broadcaster == null, don't seem to have context yet");
            return;
        } 

        OutboundSseEvent queueEvent = sse.newEvent(data);
        if (queueEvent != null) {
            logger.debug("About to send SSE: " + queueEvent + "to broadcaster: " + broadcaster);
            broadcaster.broadcast(queueEvent);
        } else {
            logger.error("Created a null event! this = " + this);
        } 
    }
}
