package com.pradeepl.akkakata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import akka.javasdk.testkit.EventSourcedTestKit;

import com.pradeepl.akkakata.domain.commands.CustomerCommands.CreateCustomer;
import com.pradeepl.akkakata.domain.commands.CustomerCommands.DeleteCustomer;
import com.pradeepl.akkakata.domain.entities.CustomerEntity;
import com.pradeepl.akkakata.domain.events.CustomerEvents.customerCreated;
import com.pradeepl.akkakata.domain.events.CustomerEvents.customerDeleted;
import com.pradeepl.akkakata.domain.model.CustomerState;

/**
 * Baseline test for CustomerEntity — Day 4 lab starting point.
 *
 * Exercises the event-sourced entity in isolation via {@link EventSourcedTestKit}:
 * no cluster, no HTTP, no runtime spin-up. Fast, deterministic.
 */
class CustomerEntityTest {

    @Test
    void createPersistsCustomerCreatedAndRepliesWithId() {
        var testKit = EventSourcedTestKit.of("c-1", CustomerEntity::new);

        var result = testKit.method(CustomerEntity::create)
            .invoke(new CreateCustomer("Ada", "Lovelace"));

        assertThat(result.isReply()).isTrue();
        assertThat(result.getReply()).isEqualTo("c-1");

        var event = result.getNextEventOfType(customerCreated.class);
        assertThat(event.customerId()).isEqualTo("c-1");
        assertThat(event.FirstName()).isEqualTo("Ada");
        assertThat(event.LastName()).isEqualTo("Lovelace");

        CustomerState state = (CustomerState) result.getUpdatedState();
        assertThat(state.deleted()).isFalse();
    }

    @Test
    void createRejectsWhenCustomerAlreadyExists() {
        var testKit = EventSourcedTestKit.of("c-1", CustomerEntity::new);

        testKit.method(CustomerEntity::create).invoke(new CreateCustomer("Ada", "Lovelace"));
        var second = testKit.method(CustomerEntity::create)
            .invoke(new CreateCustomer("Grace", "Hopper"));

        assertThat(second.isError()).isTrue();
        assertThat(second.getError()).isEqualTo("Customer already exists");
    }

    @Test
    void deleteMarksCustomerAsDeleted() {
        var testKit = EventSourcedTestKit.of("c-1", CustomerEntity::new);

        testKit.method(CustomerEntity::create).invoke(new CreateCustomer("Ada", "Lovelace"));
        var result = testKit.method(CustomerEntity::delete).invoke(new DeleteCustomer());

        assertThat(result.isReply()).isTrue();
        assertThat(result.getNextEventOfType(customerDeleted.class)).isNotNull();

        CustomerState state = (CustomerState) result.getUpdatedState();
        assertThat(state.deleted()).isTrue();
    }
}
