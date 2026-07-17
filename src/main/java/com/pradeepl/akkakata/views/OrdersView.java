package com.pradeepl.akkakata.views;

import java.util.List;

import com.pradeepl.akkakata.domain.entities.OrderEntity;
import com.pradeepl.akkakata.domain.events.OrderEvents.inventoryReserved;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderCancelled;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderFailed;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderPlaced;
import com.pradeepl.akkakata.domain.events.OrderEvents.orderShipped;
import com.pradeepl.akkakata.domain.events.OrderEvents.paymentRecorded;
import com.pradeepl.akkakata.domain.model.OrderStatus;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.annotations.Table;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;

@ComponentId("orders-view")
public class OrdersView extends View {

    public record OrderRow(
        String orderId,
        String customerId,
        long totalCents,
        String status
    ) {
        public OrderRow withStatus(String next) {
            return new OrderRow(orderId, customerId, totalCents, next);
        }
    }

    public record OrderRows(List<OrderRow> orders) {}

    @Query("SELECT * AS orders FROM orders_table")
    public QueryEffect<OrderRows> all() {
        return queryResult();
    }

    @Query("SELECT * AS orders FROM orders_table WHERE customerId = :customerId")
    public QueryEffect<OrderRows> byCustomer(String customerId) {
        return queryResult();
    }

    @Table("orders_table")
    @Consume.FromEventSourcedEntity(OrderEntity.class)
    public static class OrdersUpdater extends TableUpdater<OrderRow> {

        public Effect<OrderRow> onEvent(orderPlaced e) {
            return effects().updateRow(new OrderRow(
                e.orderId(), e.customerId(), e.totalCents(), OrderStatus.PLACED.name()));
        }

        public Effect<OrderRow> onEvent(inventoryReserved e) {
            return effects().updateRow(rowState().withStatus(OrderStatus.RESERVED.name()));
        }

        public Effect<OrderRow> onEvent(paymentRecorded e) {
            return effects().updateRow(rowState().withStatus(OrderStatus.PAID.name()));
        }

        public Effect<OrderRow> onEvent(orderShipped e) {
            return effects().updateRow(rowState().withStatus(OrderStatus.SHIPPED.name()));
        }

        public Effect<OrderRow> onEvent(orderFailed e) {
            return effects().updateRow(rowState().withStatus(OrderStatus.FAILED.name()));
        }

        public Effect<OrderRow> onEvent(orderCancelled e) {
            return effects().updateRow(rowState().withStatus(OrderStatus.CANCELLED.name()));
        }
    }
}
