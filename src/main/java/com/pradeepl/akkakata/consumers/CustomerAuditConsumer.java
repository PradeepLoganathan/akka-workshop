package com.pradeepl.akkakata.consumers;

import java.time.Instant;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;

import com.pradeepl.akkakata.domain.commands.AuditCommands.Append;
import com.pradeepl.akkakata.domain.entities.AuditLogEntity;
import com.pradeepl.akkakata.domain.entities.CustomerEntity;
import com.pradeepl.akkakata.domain.events.CustomerEvents;
import com.pradeepl.akkakata.domain.events.CustomerEvents.customerCreated;
import com.pradeepl.akkakata.domain.events.CustomerEvents.customerDeleted;

@ComponentId("customer-audit-consumer")
@Consume.FromEventSourcedEntity(CustomerEntity.class)
public class CustomerAuditConsumer extends Consumer {

    private final ComponentClient client;

    public CustomerAuditConsumer(ComponentClient client) {
        this.client = client;
    }

    public Effect onEvent(CustomerEvents event) {
        return switch (event) {
            case customerCreated e -> record(
                e.customerId(),
                "customer.created",
                String.format("%s %s", e.FirstName(), e.LastName())
            );
            case customerDeleted e -> record(
                e.customerId(),
                "customer.deleted",
                String.format("%s %s", e.FirstName(), e.LastName())
            );
        };
    }

    private Effect record(String customerId, String action, String detail) {
        client.forEventSourcedEntity(customerId)
            .method(AuditLogEntity::append)
            .invoke(new Append(action, Instant.now(), detail));
        return effects().done();
    }
}
