package com.pradeepl.akkakata.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AuditLogState(
    String customerId,
    List<AuditEntry> entries
) {
    public static AuditLogState empty() {
        return new AuditLogState(null, List.of());
    }

    public AuditLogState append(String forCustomer, AuditEntry entry) {
        var next = new ArrayList<>(entries);
        next.add(entry);
        return new AuditLogState(forCustomer, Collections.unmodifiableList(next));
    }
}
