package com.pradeepl.akkakata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import akka.javasdk.testkit.EventSourcedTestKit;

import com.pradeepl.akkakata.domain.commands.OrderCommands.MarkPaid;
import com.pradeepl.akkakata.domain.commands.OrderCommands.MarkReserved;
import com.pradeepl.akkakata.domain.commands.OrderCommands.MarkShipped;
import com.pradeepl.akkakata.domain.commands.OrderCommands.PlaceOrder;
import com.pradeepl.akkakata.domain.entities.OrderEntity;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderPlaced;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderShipped;
import com.pradeepl.akkakata.domain.model.OrderState;
import com.pradeepl.akkakata.domain.model.OrderState.OrderLine;
import com.pradeepl.akkakata.domain.model.OrderStatus;

/**
 * Baseline test for OrderEntity — Day 4 lab starting point.
 *
 * Demonstrates:
 *   - Multi-event lifecycle (place → reserved → paid → shipped)
 *   - Guard clauses on state transitions (illegal transitions rejected)
 *   - Total computation from line items
 */
class OrderEntityTest {

    private static final String ORDER_ID = "order-1";

    private static PlaceOrder samplePlaceOrder() {
        return new PlaceOrder("cust-1", List.of(
            new OrderLine("sku-1", 2, 500),
            new OrderLine("sku-2", 1, 250)));
    }

    @Test
    void placeComputesTotalAndSetsStatusToPlaced() {
        var testKit = EventSourcedTestKit.of(ORDER_ID, OrderEntity::new);

        var result = testKit.method(OrderEntity::place).invoke(samplePlaceOrder());

        assertThat(result.getReply()).isEqualTo(ORDER_ID);

        orderPlaced event = result.getNextEventOfType(orderPlaced.class);
        assertThat(event.totalCents()).isEqualTo(1250L);

        OrderState state = (OrderState) result.getUpdatedState();
        assertThat(state.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void fullHappyPath_placedReservedPaidShipped() {
        var testKit = EventSourcedTestKit.of(ORDER_ID, OrderEntity::new);

        testKit.method(OrderEntity::place).invoke(samplePlaceOrder());
        testKit.method(OrderEntity::markReserved).invoke(new MarkReserved());
        testKit.method(OrderEntity::markPaid).invoke(new MarkPaid());
        var shipped = testKit.method(OrderEntity::markShipped).invoke(new MarkShipped());

        assertThat(shipped.isReply()).isTrue();
        assertThat(shipped.getNextEventOfType(orderShipped.class)).isNotNull();

        OrderState state = (OrderState) shipped.getUpdatedState();
        assertThat(state.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void markPaidRejectedIfNotReserved() {
        var testKit = EventSourcedTestKit.of(ORDER_ID, OrderEntity::new);

        testKit.method(OrderEntity::place).invoke(samplePlaceOrder());
        // skip markReserved
        var result = testKit.method(OrderEntity::markPaid).invoke(new MarkPaid());

        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).startsWith("Cannot pay from status");
    }
}
