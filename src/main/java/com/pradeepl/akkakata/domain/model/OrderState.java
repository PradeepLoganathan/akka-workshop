package com.pradeepl.akkakata.domain.model;

import java.util.List;

public record OrderState(
    String orderId,
    String customerId,
    List<OrderLine> lines,
    long totalCents,
    OrderStatus status
) {
    public static OrderState empty() {
        return new OrderState(null, null, List.of(), 0L, null);
    }

    public OrderState withStatus(OrderStatus next) {
        return new OrderState(orderId, customerId, lines, totalCents, next);
    }

    public record OrderLine(String sku, int qty, long unitPriceCents) {}
}
