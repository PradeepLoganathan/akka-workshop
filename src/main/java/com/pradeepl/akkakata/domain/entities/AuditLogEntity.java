package com.pradeepl.akkakata.domain.entities;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;

import com.pradeepl.akkakata.domain.commands.AuditCommands.Append;
import com.pradeepl.akkakata.domain.events.AuditEvents;
import com.pradeepl.akkakata.domain.events.AuditEvents.auditAppended;
import com.pradeepl.akkakata.domain.model.AuditEntry;
import com.pradeepl.akkakata.domain.model.AuditLogState;

@ComponentId("audit-log")
public class AuditLogEntity extends EventSourcedEntity<AuditLogState, AuditEvents> {

    @Override
    public AuditLogState emptyState() {
        return AuditLogState.empty();
    }

    public Effect<String> append(Append cmd) {
        var customerId = commandContext().entityId();
        var ev = new auditAppended(customerId, cmd.action(), cmd.at(), cmd.detail());
        return effects().persist(ev).thenReply(__ -> "OK");
    }

    public ReadOnlyEffect<AuditLogState> get() {
        return effects().reply(currentState());
    }

    @Override
    public AuditLogState applyEvent(AuditEvents event) {
        return switch (event) {
            case auditAppended e -> currentState().append(
                e.customerId(),
                new AuditEntry(e.action(), e.at(), e.detail())
            );
        };
    }
}
