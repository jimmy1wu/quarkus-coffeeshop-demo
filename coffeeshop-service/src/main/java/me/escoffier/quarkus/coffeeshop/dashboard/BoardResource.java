package me.escoffier.quarkus.coffeeshop.dashboard;

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

@Path("/queue")
public class BoardResource {

    private Sse sse;
    private SseBroadcaster broadcaster;

    @Context
    public void setSse(Sse sse) {
        this.sse = sse;
        broadcaster = sse.newBroadcaster();
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribeToQueue(@Context SseEventSink eventSink) {
        broadcaster.register(eventSink);
    }

    @Incoming("beverages") 
    public void processQueue(String data) {
        System.out.println("GOT: " + data);
        OutboundSseEvent queueEvent = sse.newEvent("beverage",data);
        broadcaster.broadcast(queueEvent);
    }
}
