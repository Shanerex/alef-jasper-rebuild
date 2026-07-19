package com.alef.api.admin.kb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes KbChangedEvent to the "kb.changed" Redis Stream, AFTER_COMMIT
 * (architecture §2.5, DEC-024).
 *
 * phase = AFTER_COMMIT is the whole point: the admin's HTTP response returns as
 * soon as the DB transaction commits, and the XADD happens off that critical
 * path. A publish failure is logged and swallowed, never surfaced to the admin
 * (constraint 4) -- the worker can reconcile from project.updated_at later if
 * an event is ever lost (architecture §2.5).
 *
 * Minimal reference payload only (event/version/entityType/entityId/slug/
 * changeType/occurredAt) -- deliberately not the full project body, so the
 * future worker re-reads Postgres as the source of truth (DEC-024).
 */
@Component
public class KbChangedPublisher {

    private static final Logger log = LoggerFactory.getLogger(KbChangedPublisher.class);

    /** Redis Stream key -- single-purpose stream, named explicitly for clarity. */
    static final String STREAM_KEY = "kb.changed";

    private final RedisTemplate<String, String> redisTemplate;

    public KbChangedPublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Handles the event after the enclosing transaction commits successfully.
     * Never rethrows -- a Redis outage must not turn into a 500 for an admin
     * who already successfully saved their edit.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKbChanged(KbChangedEvent event) {
        try {
            redisTemplate.opsForStream().add(toRecord(event));
        } catch (Exception ex) {
            log.warn("Failed to publish kb.changed for project {} ({}): {}",
                    event.entityId(), event.changeType(), ex.getMessage());
        }
    }

    /** Builds the Redis Stream record from the event, per architecture §2.5's field table. */
    private MapRecord<String, String, String> toRecord(KbChangedEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("event", "kb.changed");
        fields.put("version", "1");
        fields.put("entityType", event.entityType());
        fields.put("entityId", String.valueOf(event.entityId()));
        fields.put("slug", event.slug());
        fields.put("changeType", event.changeType());
        fields.put("occurredAt", event.occurredAt().toString());
        return StreamRecords.newRecord().ofMap(fields).withStreamKey(STREAM_KEY);
    }
}
