package me.escoffier.quarkus.coffeeshop.health;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class CoffeeshopReadinessCheck implements HealthCheck {

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.servers")
    String kafkaServer;

    private boolean isReady() {
        AdminClient adminClient = createAdminClient();
        return checkIfCoffeeshopConsumerGroupRegistered(adminClient);
    }

    private boolean checkIfCoffeeshopConsumerGroupRegistered(AdminClient adminClient) {
        ListConsumerGroupsResult groupsResult = adminClient.listConsumerGroups();
        KafkaFuture<Collection<ConsumerGroupListing>> consumerGroupsFuture = groupsResult.valid();
        try {
            Collection<ConsumerGroupListing> consumerGroups = consumerGroupsFuture.get();
            return consumerGroups.stream().anyMatch(group -> group.groupId().equals("coffeeshop"));
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    private AdminClient createAdminClient() {
        Properties connectionProperties = new Properties();
        connectionProperties.put("bootstrap.servers", kafkaServer);
        AdminClient adminClient = AdminClient.create(connectionProperties);
        return adminClient;
    }
	
    @Override
    public HealthCheckResponse call() {
        boolean up = isReady();
        return HealthCheckResponse.named(this.getClass().getSimpleName()).state(up).build();
    }
    
}
