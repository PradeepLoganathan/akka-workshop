package com.pradeepl.akkakata.api;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;

import com.pradeepl.akkakata.domain.entities.OrderEntity;
import com.pradeepl.akkakata.domain.model.OrderState;
import com.pradeepl.akkakata.domain.model.OrderState.OrderLine;
import com.pradeepl.akkakata.domain.workflows.OrderWorkflow;
import com.pradeepl.akkakata.domain.workflows.OrderWorkflow.StartOrder;
import com.pradeepl.akkakata.views.OrdersView;
import com.pradeepl.akkakata.views.OrdersView.OrderRows;

import java.util.List;
import java.util.UUID;

@HttpEndpoint("/api")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class OrderEndpoint {

    public record PlaceOrderRequest(String customerId, List<OrderLine> lines) {}
    public record PlaceOrderResponse(String orderId, String status) {}

    private final ComponentClient client;

    public OrderEndpoint(ComponentClient client) {
        this.client = client;
    }

    @Post("/orders")
    public PlaceOrderResponse place(PlaceOrderRequest req) {
        String orderId = "order-" + UUID.randomUUID();
        String result = client.forWorkflow(orderId)
            .method(OrderWorkflow::start)
            .invoke(new StartOrder(req.customerId(), req.lines()));
        return new PlaceOrderResponse(result, "STARTED");
    }

    @Get("/orders")
    public OrderRows list() {
        return client.forView().method(OrdersView::all).invoke();
    }

    @Get("/orders/customer/{customerId}")
    public OrderRows byCustomer(String customerId) {
        return client.forView().method(OrdersView::byCustomer).invoke(customerId);
    }

    @Get("/orders/{orderId}")
    public OrderState get(String orderId) {
        return client.forEventSourcedEntity(orderId)
            .method(OrderEntity::get)
            .invoke();
    }
}
