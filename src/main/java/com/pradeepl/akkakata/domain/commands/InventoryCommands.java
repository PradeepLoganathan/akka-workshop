package com.pradeepl.akkakata.domain.commands;

import com.pradeepl.akkakata.domain.model.OrderState.OrderLine;
import java.util.List;

public final class InventoryCommands {

    public record ReserveInventory(String orderId, List<OrderLine> lines) {}
    public record ReleaseInventory(String orderId) {}
}
