package com.alef.api.admin.kb;

import java.time.Instant;

/**
 * Internal application event for a project content change (architecture §2.5, DEC-024).
 *
 * Published synchronously (within the write transaction) by AdminProjectService;
 * KbChangedPublisher listens with phase=AFTER_COMMIT so the Redis XADD only runs
 * once the DB transaction has actually committed, and never blocks or fails the
 * admin's save (constraint 4 / DEC-004). v1 emits for projects only.
 */
public record KbChangedEvent(
        String entityType,
        Long entityId,
        String slug,
        String changeType,
        Instant occurredAt
) {

    /** Convenience factory: "created" change type, occurredAt = now. */
    public static KbChangedEvent created(Long entityId, String slug) {
        return new KbChangedEvent("project", entityId, slug, "created", Instant.now());
    }

    /** Convenience factory: "updated" change type, occurredAt = now. */
    public static KbChangedEvent updated(Long entityId, String slug) {
        return new KbChangedEvent("project", entityId, slug, "updated", Instant.now());
    }

    /** Convenience factory: "deleted" change type, occurredAt = now. */
    public static KbChangedEvent deleted(Long entityId, String slug) {
        return new KbChangedEvent("project", entityId, slug, "deleted", Instant.now());
    }
}
