package me.escoffier.quarkus.coffeeshop.health;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class BaristaReadinessCheck implements HealthCheck {

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.servers" )
    String kafkaServer;

    private boolean isReady() {
        Properties connectionProperties = new Properties();
        connectionProperties.put("bootstrap.servers", kafkaServer);
        AdminClient adminClient = AdminClient.create(connectionProperties);
        ListTopicsResult topicsResult = adminClient.listTopics();
        KafkaFuture<Set<String>> topicsNamesFuture = topicsResult.names();
        try {
            Set<String> topicNames = topicsNamesFuture.get();
            return topicNames.contains("queue") && topicNames.contains("orders");
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }
	
    @Override
    public HealthCheckResponse call() {
        boolean up = isReady();
        return HealthCheckResponse.named(this.getClass().getSimpleName()).state(up).build();
    }
    
}
