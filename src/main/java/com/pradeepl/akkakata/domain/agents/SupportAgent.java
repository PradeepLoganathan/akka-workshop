package com.pradeepl.akkakata.domain.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Description;
import akka.javasdk.annotations.FunctionTool;
import akka.javasdk.client.ComponentClient;

import com.pradeepl.akkakata.domain.entities.AuditLogEntity;
import com.pradeepl.akkakata.domain.entities.CustomerEntity;
import com.pradeepl.akkakata.domain.entities.OrderEntity;
import com.pradeepl.akkakata.domain.model.AuditEntry;
import com.pradeepl.akkakata.views.OrdersView;
import com.pradeepl.akkakata.views.OrdersView.OrderRow;

import java.util.stream.Collectors;

@ComponentId("support-agent")
public class SupportAgent extends Agent {

    private static final String SYSTEM_PROMPT = """
        You are Akka Workshop Support — a customer-support assistant for the sample e-commerce
        service in the akka-workshop repository. Your job is to help customers understand the
        state of their account and orders.

        Rules:
        - Never invent customer ids or order ids. If the user gives you an id, use it verbatim.
        - Before answering, use the provided tools to look up real data. Do not guess.
        - If a tool returns no data, say so plainly; do not fabricate a response.
        - Keep answers short and factual. If the user asks a fuzzy question, ask a clarifying one.
        - You have access to customer profiles, order lists, individual order details, and audit history.
        """;

    private final ComponentClient client;

    public SupportAgent(ComponentClient client) {
        this.client = client;
    }

    public Effect<String> ask(String question) {
        return effects()
            .memory(MemoryProvider.limitedWindow().readLast(10))
            .systemMessage(SYSTEM_PROMPT)
            .tools(this)
            .userMessage(question)
            .thenReply();
    }

    @FunctionTool(description = "Look up a customer's profile by customerId. Returns first name, last name, and whether the customer is deleted.")
    public String getCustomer(
        @Description("The customer id, e.g. cust-001") String customerId) {
        var s = client.forEventSourcedEntity(customerId)
            .method(CustomerEntity::get)
            .invoke();
        if (s.customerId() == null) {
            return "No customer found for " + customerId;
        }
        return String.format(
            "customerId=%s, firstName=%s, lastName=%s, deleted=%s",
            s.customerId(), s.FirstName(), s.LastName(), s.deleted());
    }

    @FunctionTool(description = "List all orders placed by a given customer, most recent first. Returns orderId, total (cents), and current status for each order.")
    public String listOrdersForCustomer(
        @Description("The customer id whose orders to list") String customerId) {
        var rows = client.forView()
            .method(OrdersView::byCustomer)
            .invoke(customerId);
        if (rows.orders().isEmpty()) {
            return "No orders found for customer " + customerId;
        }
        return rows.orders().stream()
            .map(r -> String.format("orderId=%s total_cents=%d status=%s",
                r.orderId(), r.totalCents(), r.status()))
            .collect(Collectors.joining("\n"));
    }

    @FunctionTool(description = "Get the full detail of one order by orderId — customer id, line items, total, and current status.")
    public String getOrder(
        @Description("The order id, e.g. order-abc123") String orderId) {
        var s = client.forEventSourcedEntity(orderId)
            .method(OrderEntity::get)
            .invoke();
        if (s.orderId() == null) {
            return "No order found for " + orderId;
        }
        var items = s.lines().stream()
            .map(l -> String.format("%s x%d @ %d cents", l.sku(), l.qty(), l.unitPriceCents()))
            .collect(Collectors.joining("; "));
        return String.format(
            "orderId=%s customerId=%s status=%s total_cents=%d items=[%s]",
            s.orderId(), s.customerId(), s.status(), s.totalCents(), items);
    }

    @FunctionTool(description = "Get the audit log for a customer — a chronological list of what happened to their profile (created, deleted, etc). Useful when answering \"what changed on my account?\".")
    public String getAudit(
        @Description("The customer id whose audit log to retrieve") String customerId) {
        var log = client.forEventSourcedEntity(customerId)
            .method(AuditLogEntity::get)
            .invoke();
        if (log.entries().isEmpty()) {
            return "No audit entries for customer " + customerId;
        }
        return log.entries().stream()
            .map(this::formatEntry)
            .collect(Collectors.joining("\n"));
    }

    private String formatEntry(AuditEntry e) {
        return String.format("at=%s action=%s detail=%s", e.at(), e.action(), e.detail());
    }
}
