package com.pradeepl.akkakata.api;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;

import com.pradeepl.akkakata.domain.entities.AuditLogEntity;
import com.pradeepl.akkakata.domain.model.AuditLogState;

@HttpEndpoint("/api")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class AuditEndpoint {

    private final ComponentClient client;

    public AuditEndpoint(ComponentClient client) {
        this.client = client;
    }

    @Get("/audit/customers/{customerId}")
    public AuditLogState log(String customerId) {
        return client.forEventSourcedEntity(customerId)
            .method(AuditLogEntity::get)
            .invoke();
    }
}
