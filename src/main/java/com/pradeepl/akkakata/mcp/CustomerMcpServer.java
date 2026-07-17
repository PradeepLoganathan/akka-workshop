package com.pradeepl.akkakata.mcp;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.Description;
import akka.javasdk.annotations.mcp.McpEndpoint;
import akka.javasdk.annotations.mcp.McpTool;
import akka.javasdk.client.ComponentClient;

import com.pradeepl.akkakata.domain.entities.CustomerEntity;
import com.pradeepl.akkakata.domain.entities.OrderEntity;
import com.pradeepl.akkakata.views.OrdersView;

import java.util.stream.Collectors;

@McpEndpoint(
    path = "/mcp",
    serverName = "akka-workshop-customer",
    serverVersion = "0.1.0",
    instructions = "Read-only customer and order lookup tools for the akka-workshop sample."
)
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class CustomerMcpServer {

    private final ComponentClient client;

    public CustomerMcpServer(ComponentClient client) {
        this.client = client;
    }

    @McpTool(description = "Get a customer profile by customerId. Returns first name, last name, deleted flag.")
    public String getCustomer(
        @Description("The customer id, e.g. cust-001") String customerId) {
        var s = client.forEventSourcedEntity(customerId)
            .method(CustomerEntity::get)
            .invoke();
        if (s.customerId() == null) {
            return "No customer found for " + customerId;
        }
        return String.format(
            "customerId=%s firstName=%s lastName=%s deleted=%s",
            s.customerId(), s.FirstName(), s.LastName(), s.deleted());
    }

    @McpTool(description = "List all orders for a given customer with their current status.")
    public String listOrdersForCustomer(
        @Description("The customer id whose orders to list") String customerId) {
        var rows = client.forView()
            .method(OrdersView::byCustomer)
            .invoke(customerId);
        if (rows.orders().isEmpty()) {
            return "No orders found for customer " + customerId;
        }
        return rows.orders().stream()
            .map(r -> String.format("orderId=%s status=%s total_cents=%d",
                r.orderId(), r.status(), r.totalCents()))
            .collect(Collectors.joining("\n"));
    }

    @McpTool(description = "Get the full details of one order by orderId.")
    public String getOrder(
        @Description("The order id, e.g. order-abc123") String orderId) {
        var s = client.forEventSourcedEntity(orderId)
            .method(OrderEntity::get)
            .invoke();
        if (s.orderId() == null) {
            return "No order found for " + orderId;
        }
        return String.format(
            "orderId=%s customerId=%s status=%s total_cents=%d line_count=%d",
            s.orderId(), s.customerId(), s.status(), s.totalCents(), s.lines().size());
    }
}
