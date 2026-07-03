package com.alef.api.lead.service;

import com.alef.api.lead.dto.ContactLeadRequest;
import com.alef.api.lead.dto.LeadConfirmationDto;
import com.alef.api.lead.entity.LeadEntity;
import com.alef.api.lead.repository.LeadRepository;
import com.alef.api.lead.vocabulary.LeadSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LeadService abuse-mitigation and persistence logic
 * (architecture §3.4 / §10 Decision 2, feature 011).
 *
 * This is a risk-weighted test -- LeadService owns the only write path in 011
 * and contains the honeypot and rate-limit guards that must behave correctly
 * for production readiness (ARCHITECTURE.md production-readiness bar).
 *
 * RedisTemplate and LeadRepository are mocked; no Spring context required.
 *
 * Note on stub discipline: opsForValue() is stubbed per-test (not in @BeforeEach)
 * because the honeypot tests exit before reaching any Redis call, which would
 * cause Mockito strict stubbing to report an UnnecessaryStubbingException.
 * stubRateOk() and stubRateBreach() both set up the opsForValue() stub so each
 * test needing Redis is self-contained.
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository repository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private LeadService service;

    @BeforeEach
    void setUp() {
        service = new LeadService(repository, redisTemplate);
    }

    // ── Honeypot tests (highest-risk path) ────────────────────────────────────

    @Test
    void submit_returns_fake_201_when_honeypot_is_filled_without_persisting() {
        var request = request("Alice", "alice@example.com", null, null,
                "Help me", "i-am-a-bot");   // website non-blank — no Redis stub needed

        LeadConfirmationDto result = service.submit(request, "1.2.3.4");

        assertThat(result.id()).isEqualTo(-1L);
        assertThat(result.status()).isEqualTo("received");
        // No row persisted
        verify(repository, never()).save(any());
        // No rate counter incremented (honeypot check runs before rate-limit per spec)
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void submit_proceeds_when_honeypot_is_blank() {
        var request = request("Alice", "alice@example.com", null, null, "Help me", "");
        stubRateOk("1.2.3.4", 1L);
        when(repository.save(any())).thenReturn(savedEntity(42L));

        LeadConfirmationDto result = service.submit(request, "1.2.3.4");

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.status()).isEqualTo("received");
        verify(repository).save(any());
    }

    @Test
    void submit_proceeds_when_honeypot_is_null() {
        var request = request("Alice", "alice@example.com", null, null, "Help me", null);
        stubRateOk("1.2.3.4", 1L);
        when(repository.save(any())).thenReturn(savedEntity(7L));

        LeadConfirmationDto result = service.submit(request, "1.2.3.4");

        assertThat(result.id()).isEqualTo(7L);
    }

    // ── Rate-limit tests ───────────────────────────────────────────────────────

    @Test
    void submit_throws_429_when_rate_limit_exceeded() {
        var request = validRequest();
        stubRateBreach("1.2.3.4", 6L);   // 6th submission over the 5/hour limit

        assertThatThrownBy(() -> service.submit(request, "1.2.3.4"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(429));
        verify(repository, never()).save(any());
    }

    @Test
    void submit_sets_redis_expiry_on_first_submission_from_ip() {
        var request = validRequest();
        stubRateOk("10.0.0.1", 1L);   // count == 1 means first submission from this IP
        when(repository.save(any())).thenReturn(savedEntity(1L));

        service.submit(request, "10.0.0.1");

        verify(redisTemplate).expire("rate:lead:10.0.0.1", 3600L, TimeUnit.SECONDS);
    }

    @Test
    void submit_does_not_reset_expiry_on_subsequent_submissions() {
        var request = validRequest();
        stubRateOk("10.0.0.2", 3L);   // count == 3 means third submission; expiry already set
        when(repository.save(any())).thenReturn(savedEntity(2L));

        service.submit(request, "10.0.0.2");

        // expire must NOT be called again (only set when count == 1)
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    void submit_allows_exactly_five_submissions_before_rejecting() {
        var request = validRequest();
        stubRateOk("5.5.5.5", 5L);   // 5th submission must still succeed
        when(repository.save(any())).thenReturn(savedEntity(5L));

        LeadConfirmationDto result = service.submit(request, "5.5.5.5");

        assertThat(result.id()).isEqualTo(5L);
    }

    // ── Persistence mapping tests ──────────────────────────────────────────────

    @Test
    void submit_sets_source_to_contact_form_server_side() {
        var request = validRequest();
        stubRateOk("1.2.3.4", 1L);
        when(repository.save(any())).thenReturn(savedEntity(1L));

        service.submit(request, "1.2.3.4");

        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(LeadSource.CONTACT_FORM);
    }

    @Test
    void submit_sets_created_at_server_side() {
        var request = validRequest();
        stubRateOk("1.2.3.4", 1L);
        when(repository.save(any())).thenReturn(savedEntity(1L));

        service.submit(request, "1.2.3.4");

        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void submit_maps_all_request_fields_to_entity() {
        var request = request("Bob", "bob@example.com", "+971 50 123456", "ACME",
                "Need shop drawings", "");
        stubRateOk("1.2.3.4", 1L);
        when(repository.save(any())).thenReturn(savedEntity(9L));

        service.submit(request, "1.2.3.4");

        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(repository).save(captor.capture());
        LeadEntity entity = captor.getValue();
        assertThat(entity.getName()).isEqualTo("Bob");
        assertThat(entity.getEmail()).isEqualTo("bob@example.com");
        assertThat(entity.getPhone()).isEqualTo("+971 50 123456");
        assertThat(entity.getCompany()).isEqualTo("ACME");
        assertThat(entity.getMessage()).isEqualTo("Need shop drawings");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ContactLeadRequest validRequest() {
        return request("Alice", "alice@example.com", null, null, "Help me", "");
    }

    private ContactLeadRequest request(String name, String email, String phone,
                                        String company, String message, String website) {
        return new ContactLeadRequest(name, email, phone, company, message, website);
    }

    /**
     * Stubs the Redis increment to return count (within the rate limit).
     * Also stubs opsForValue() since LeadService calls it before increment().
     */
    private void stubRateOk(String ip, long count) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rate:lead:" + ip)).thenReturn(count);
    }

    /**
     * Stubs the Redis increment to return a count that exceeds the rate limit.
     * Also stubs opsForValue() since LeadService calls it before increment().
     */
    private void stubRateBreach(String ip, long count) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rate:lead:" + ip)).thenReturn(count);
    }

    /**
     * Returns a LeadEntity whose getId() yields the given id.
     * LeadEntity.id is GENERATED ALWAYS with no public setter, so an anonymous
     * subclass that overrides getId() is the cleanest reflection-free approach.
     */
    private LeadEntity savedEntity(long id) {
        return new LeadEntity() {
            @Override
            public Long getId() {
                return id;
            }
        };
    }
}
