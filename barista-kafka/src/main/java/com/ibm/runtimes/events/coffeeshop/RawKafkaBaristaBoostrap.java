package com.ibm.runtimes.events.coffeeshop;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.Executors;

@ApplicationScoped
public class RawKafkaBaristaBoostrap {

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.servers")
    String kafkaBootstrapServer;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        KafkaEventEmitter emitter = new KafkaEventEmitter(kafkaBootstrapServer, "queue");
        KafkaEventSource source = new KafkaEventSource(kafkaBootstrapServer);
        new RawKafkaBarista(emitter, Executors.newSingleThreadExecutor(), source );
    }
}
