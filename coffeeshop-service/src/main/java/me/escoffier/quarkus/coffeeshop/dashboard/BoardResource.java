package me.escoffier.quarkus.coffeeshop.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/services/queue")
@ApplicationScoped
public class BoardResource {

    private static Logger logger = LoggerFactory.getLogger(BoardResource.class);
    private List<Session> clientSessions = Collections.synchronizedList(new ArrayList<>());

    @OnOpen
    public void subscribeToQueue(Session session, EndpointConfig ec) {
        logger.debug("Websocket opened " + session.getId());
        clientSessions.add(session);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        logger.debug("Websocket session closed " + session.getId());
        clientSessions.remove(session);
    }

    @Incoming("beverages")
    public void processQueue(String data) {
        logger.info("GOT: " + data);

        synchronized (clientSessions) {
            for (Session clientSession : clientSessions) {
                try {
                    logger.debug("Sending to websocket client: " + clientSession.getId());
                    clientSession.getBasicRemote().sendText(data);
                } catch (IOException e) {
                    logger.error("Error sending message to websocket client " + clientSession.getId(), e);
                }
            }
        }
    }
}
