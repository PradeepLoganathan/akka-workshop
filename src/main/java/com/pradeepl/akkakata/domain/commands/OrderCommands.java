package com.pradeepl.akkakata.domain.commands;

import com.pradeepl.akkakata.domain.model.OrderState.OrderLine;
import java.util.List;

public final class OrderCommands {

    public record PlaceOrder(
        String customerId,
        List<OrderLine> lines
    ) {}

    public record MarkReserved() {}
    public record MarkPaid() {}
    public record MarkShipped() {}
    public record MarkFailed(String reason) {}
    public record CancelIfNotPaid() {}
}
