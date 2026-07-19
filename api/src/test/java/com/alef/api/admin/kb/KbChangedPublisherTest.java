package com.alef.api.admin.kb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KbChangedPublisher (architecture §2.5, DEC-024, F12-AC25).
 *
 * Exercises the handler method body directly rather than driving a real
 * Spring transaction -- @TransactionalEventListener(phase=AFTER_COMMIT)'s
 * dispatch timing is Spring's own tested behaviour; what 012 owns and must
 * verify is the payload shape and that a publish failure never propagates
 * (constraint 4: must not surface to the admin).
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class KbChangedPublisherTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private StreamOperations streamOps;

    @Test
    void publishes_a_record_with_the_documented_fields_for_a_created_event() {
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        var publisher = new KbChangedPublisher(redisTemplate);
        var event = KbChangedEvent.created(42L, "doha-metro-gold-line");

        publisher.onKbChanged(event);

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        MapRecord record = captor.getValue();
        Map<String, String> fields = (Map<String, String>) record.getValue();
        assertThat(record.getStream()).isEqualTo("kb.changed");
        assertThat(fields.get("event")).isEqualTo("kb.changed");
        assertThat(fields.get("version")).isEqualTo("1");
        assertThat(fields.get("entityType")).isEqualTo("project");
        assertThat(fields.get("entityId")).isEqualTo("42");
        assertThat(fields.get("slug")).isEqualTo("doha-metro-gold-line");
        assertThat(fields.get("changeType")).isEqualTo("created");
        assertThat(fields.get("occurredAt")).isNotNull();
    }

    @Test
    void publishes_deleted_change_type_for_a_deleted_event() {
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        var publisher = new KbChangedPublisher(redisTemplate);

        publisher.onKbChanged(KbChangedEvent.deleted(7L, "old-slug"));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        Map<String, String> fields = (Map<String, String>) captor.getValue().getValue();
        assertThat(fields.get("changeType")).isEqualTo("deleted");
    }

    @Test
    void a_redis_failure_is_swallowed_and_never_propagates() {
        when(redisTemplate.opsForStream()).thenThrow(new RuntimeException("redis is down"));
        var publisher = new KbChangedPublisher(redisTemplate);

        // Must not throw -- constraint 4: a publish failure must never surface
        // to the admin who already successfully saved their edit.
        publisher.onKbChanged(KbChangedEvent.updated(1L, "slug"));
    }

    @Test
    void updated_event_factory_sets_project_entity_type_and_occurred_at() {
        var event = KbChangedEvent.updated(9L, "slug-9");

        assertThat(event.entityType()).isEqualTo("project");
        assertThat(event.entityId()).isEqualTo(9L);
        assertThat(event.changeType()).isEqualTo("updated");
        assertThat(event.occurredAt()).isBeforeOrEqualTo(Instant.now());
    }
}
