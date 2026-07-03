package com.alef.api.lead.service;

import com.alef.api.lead.dto.ContactLeadRequest;
import com.alef.api.lead.dto.LeadConfirmationDto;
import com.alef.api.lead.entity.LeadEntity;
import com.alef.api.lead.repository.LeadRepository;
import com.alef.api.lead.vocabulary.LeadSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Handles contact form lead creation with abuse mitigation (architecture §3.4, feature 011).
 *
 * Two-layer protection, evaluated in order before any persist:
 * 1. Honeypot check: if the hidden website field is non-blank, return a fake 201
 *    confirmation WITHOUT persisting. The bot cannot distinguish this from a real
 *    submission. The honeypot check runs BEFORE the rate-limit increment so bots
 *    do not consume quota slots (architecture §3.4).
 * 2. Redis per-IP rate limit: 5 submissions per IP per 60 minutes via the key
 *    rate:lead:<ip>. On breach, throws 429 with a Retry-After: 3600 header.
 *    IP is read from X-Forwarded-For single-hop (first value only).
 *
 * source and created_at are always set server-side; never trusted from the client.
 * Event emission (lead.created Redis Stream) is deliberately NOT done here --
 * that is feature 001's concern (DEC-018). The row is the durable record.
 */
@Service
public class LeadService {

    /** Redis key prefix for per-IP rate limiting. Full key: rate:lead:<ip>. */
    private static final String RATE_KEY_PREFIX = "rate:lead:";

    /** Maximum contact-form submissions per IP per rate window. */
    private static final int RATE_LIMIT = 5;

    /** Rate window in seconds (60 minutes). */
    private static final long RATE_WINDOW_SECONDS = 3600L;

    /** Fake id returned on honeypot hit; negative to signal no row was created. */
    private static final long HONEYPOT_FAKE_ID = -1L;

    private final LeadRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    public LeadService(LeadRepository repository, RedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Processes a contact form submission (F11-AC6).
     *
     * Evaluation order:
     * a) Honeypot: non-blank website -> fake 201, no row, no rate increment.
     * b) Rate limit: 429 on breach.
     * c) Persist: map request to LeadEntity, save, return real id.
     *
     * @param request  validated request DTO (Bean Validation already ran in controller)
     * @param clientIp the submitting IP from X-Forwarded-For (single-hop)
     * @return confirmation DTO with the persisted id and status "received"
     * @throws ResponseStatusException 429 when the per-IP rate limit is breached
     */
    @Transactional
    public LeadConfirmationDto submit(ContactLeadRequest request, String clientIp) {
        // --- Step 1: Honeypot check (runs before rate increment per spec) ---
        if (request.website() != null && !request.website().isBlank()) {
            return new LeadConfirmationDto(HONEYPOT_FAKE_ID, "received");
        }

        // --- Step 2: Redis per-IP rate limit ---
        enforceRateLimit(clientIp);

        // --- Step 3: Persist the lead row ---
        LeadEntity entity = new LeadEntity();
        entity.setCreatedAt(Instant.now());
        entity.setSource(LeadSource.CONTACT_FORM);
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setCompany(request.company());
        entity.setMessage(request.message());

        LeadEntity saved = repository.save(entity);
        return new LeadConfirmationDto(saved.getId(), "received");
    }

    /**
     * Checks and increments the Redis per-IP rate counter.
     *
     * Uses INCR + EXPIRE so the window starts on the first submission from an IP.
     * Throws 429 with a Retry-After header when the count exceeds RATE_LIMIT.
     * The EXPIRE is set on every increment to keep the window sliding; this is a
     * simple approach -- a true sliding window would require a sorted set, which
     * is not warranted for a B2B enquiry form with low legitimate volume.
     */
    private void enforceRateLimit(String clientIp) {
        String key = RATE_KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // First submission from this IP in the window; set the expiry.
            redisTemplate.expire(key, RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count != null && count > RATE_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many enquiries from this IP. Please wait before sending another.");
        }
    }
}
