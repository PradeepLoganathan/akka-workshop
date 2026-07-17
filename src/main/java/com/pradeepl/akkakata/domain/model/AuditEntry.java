package com.pradeepl.akkakata.domain.model;

import java.time.Instant;

public record AuditEntry(
    String action,
    Instant at,
    String detail
) {}
