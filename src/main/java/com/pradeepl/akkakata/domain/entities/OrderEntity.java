package com.pradeepl.akkakata.domain.entities;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;

import com.pradeepl.akkakata.domain.commands.OrderCommands.CancelIfNotPaid;
import com.pradeepl.akkakata.domain.commands.OrderCommands.MarkFailed;
import com.pradeepl.akkakata.domain.commands.OrderCommands.MarkPaid;
import com.pradeepl.akkakata.domain.commands.OrderCommands.MarkReserved;
import com.pradeepl.akkakata.domain.commands.OrderCommands.MarkShipped;
import com.pradeepl.akkakata.domain.commands.OrderCommands.PlaceOrder;
import com.pradeepl.akkakata.domain.events.OrderEvents;
import com.pradeepl.akkakata.domain.events.OrderEvents.inventoryReserved;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderCancelled;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderFailed;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderPlaced;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderShipped;
import com.pradeepl.akkakata.domain.events.OrderEvents.paymentRecorded;
import com.pradeepl.akkakata.domain.model.OrderState;
import com.pradeepl.akkakata.domain.model.OrderStatus;

@ComponentId("Order")
public class OrderEntity extends EventSourcedEntity<OrderState, OrderEvents> {

    @Override
    public OrderState emptyState() {
        return OrderState.empty();
    }

    public Effect<String> place(PlaceOrder cmd) {
        if (currentState().orderId() != null) {
            return effects().error("Order already exists");
        }
        var orderId = commandContext().entityId();
        long total = cmd.lines().stream()
            .mapToLong(l -> l.unitPriceCents() * l.qty())
            .sum();
        var ev = new orderPlaced(orderId, cmd.customerId(), cmd.lines(), total);
        return effects().persist(ev).thenReply(__ -> orderId);
    }

    public Effect<String> markReserved(MarkReserved cmd) {
        if (currentState().status() != OrderStatus.PLACED) {
            return effects().error("Cannot reserve from status " + currentState().status());
        }
        return effects()
            .persist(new inventoryReserved(currentState().orderId()))
            .thenReply(__ -> "OK");
    }

    public Effect<String> markPaid(MarkPaid cmd) {
        if (currentState().status() != OrderStatus.RESERVED) {
            return effects().error("Cannot pay from status " + currentState().status());
        }
        return effects()
            .persist(new paymentRecorded(currentState().orderId()))
            .thenReply(__ -> "OK");
    }

    public Effect<String> markShipped(MarkShipped cmd) {
        if (currentState().status() != OrderStatus.PAID) {
            return effects().error("Cannot ship from status " + currentState().status());
        }
        return effects()
            .persist(new orderShipped(currentState().orderId()))
            .thenReply(__ -> "OK");
    }

    public Effect<String> markFailed(MarkFailed cmd) {
        return effects()
            .persist(new orderFailed(currentState().orderId(), cmd.reason()))
            .thenReply(__ -> "OK");
    }

    public Effect<String> cancelIfNotPaid(CancelIfNotPaid cmd) {
        var s = currentState().status();
        if (s == OrderStatus.PAID || s == OrderStatus.SHIPPED) {
            return effects().reply("Already paid");
        }
        if (s == OrderStatus.CANCELLED || s == OrderStatus.FAILED) {
            return effects().reply("Already terminal");
        }
        return effects()
            .persist(new orderCancelled(currentState().orderId()))
            .thenReply(__ -> "CANCELLED");
    }

    public ReadOnlyEffect<OrderState> get() {
        return effects().reply(currentState());
    }

    @Override
    public OrderState applyEvent(OrderEvents event) {
        return switch (event) {
            case orderPlaced e ->
                new OrderState(e.orderId(), e.customerId(), e.lines(), e.totalCents(), OrderStatus.PLACED);
            case inventoryReserved e -> currentState().withStatus(OrderStatus.RESERVED);
            case paymentRecorded e   -> currentState().withStatus(OrderStatus.PAID);
            case orderShipped e      -> currentState().withStatus(OrderStatus.SHIPPED);
            case orderFailed e       -> currentState().withStatus(OrderStatus.FAILED);
            case orderCancelled e    -> currentState().withStatus(OrderStatus.CANCELLED);
        };
    }
}
