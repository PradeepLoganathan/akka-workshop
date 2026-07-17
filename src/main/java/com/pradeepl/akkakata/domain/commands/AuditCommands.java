package com.pradeepl.akkakata.domain.commands;

import java.time.Instant;

public final class AuditCommands {

    public record Append(
        String action,
        Instant at,
        String detail
    ) {}
}
