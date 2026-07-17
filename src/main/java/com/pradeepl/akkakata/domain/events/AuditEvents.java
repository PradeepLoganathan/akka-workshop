package com.pradeepl.akkakata.domain.events;

import java.time.Instant;

public sealed interface AuditEvents {

    record auditAppended(
        String customerId,
        String action,
        Instant at,
        String detail
    ) implements AuditEvents {}
}
