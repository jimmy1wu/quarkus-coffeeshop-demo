package com.ibm.runtimes.events.coffeeshop;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.Executors;

@ApplicationScoped
public class RawKafkaBaristaBoostrap {

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.server")
    String kafkaBootstrapServer;

    @PostConstruct
    public void startBarista() {
        KafkaEventEmitter emitter = new KafkaEventEmitter(kafkaBootstrapServer, "queue");
        KafkaEventSource source = new KafkaEventSource(kafkaBootstrapServer);
        new RawKafkaBarista(emitter, Executors.newSingleThreadExecutor(), source );
    }
}
