# LEARNINGS.md

> Append-only. Each PITFALL records a non-obvious failure encountered during implementation and how to avoid it in future features.

---

## PITFALL-001 through PITFALL-007

Not recorded here. See earlier feature handoffs for context if needed. PITFALL-007 is the Docker Compose build-time API unavailability issue (use `force-dynamic`; ISR is safe at deploy time once the API is up — see CLAUDE.md TODOs).

---

## PITFALL-008 — Mockito strict stubbing: UnnecessaryStubbingException when a test exits before a stubbed call is reached

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Symptom:** `UnnecessaryStubbingException` on `when(redisTemplate.opsForValue()).thenReturn(valueOps)` placed in `@BeforeEach`. The honeypot test exits `LeadService.submit()` before the Redis call is reached, making the stub unnecessary for that test. Mockito strict stubbing (the JUnit 5 `MockitoExtension` default) rejects this.

**Root cause:** `@BeforeEach` stubs are shared across all tests in the class. If any test exits a code path before reaching the stubbed method, Mockito flags the stub as unnecessary and fails the suite.

**Fix:** Do not stub in `@BeforeEach` for stubs that are not reached by every test. Instead, create per-test helper methods (`stubRateOk()`, `stubRateBreach()`) that each set up their own `opsForValue()` stub. Only tests that reach the Redis path call these helpers. Tests that exit early (honeypot) call none and can verify with `verify(redisTemplate, never()).opsForValue()`.

**Rule for future features:** Never put Mockito stubs in `@BeforeEach` unless every single test in the class unconditionally reaches that call.

---

## PITFALL-009 — Redis AutoConfiguration causes test failures when no Redis server is running

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Symptom:** All Spring integration tests fail at application context startup with `Connection refused localhost:6379` once `spring-boot-starter-data-redis` is added to the pom.

**Root cause:** Spring Boot's Redis AutoConfiguration registers a `RedisConnectionFactory` bean eagerly. If `@SpringBootTest` or `@WebMvcTest` loads the full autoconfiguration and no Redis server is available, context startup fails before any test runs.

**Fix:** In `api/src/test/resources/application-test.yml`, add both:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
```
This applies to all tests that load the `test` profile (all of them via `@ActiveProfiles("test")`). `LeadServiceTest` uses Mockito-only (no Spring context at all) so it never hits the Redis bean.

**Rule for future features:** Any time a new infrastructure autoconfiguration is added (Redis, Kafka, RabbitMQ, etc.), immediately add its exclusion to `application-test.yml` unless tests provision that infrastructure via Testcontainers.

---

## PITFALL-010 — Hibernate 6 TEXT[] mapping for Postgres arrays requires @JdbcTypeCode(SqlTypes.ARRAY)

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Symptom:** `OfficeEntity.addressLines` and `OfficeEntity.phones` are `TEXT[]` in Postgres. Without extra annotations, Hibernate 6 cannot map them to `List<String>`.

**Root cause:** Hibernate 6's `@Column` alone does not instruct the JDBC layer on how to read a Postgres array column. The type resolver needs an explicit hint.

**Fix:** Use the same idiom as `ProjectEntity.scope`:
```java
@JdbcTypeCode(SqlTypes.ARRAY)
@Column(name = "address_lines", columnDefinition = "text[]", nullable = false)
private List<String> addressLines;
```
Both `@JdbcTypeCode(SqlTypes.ARRAY)` and `columnDefinition = "text[]"` are required together.

**Rule for future features:** Whenever a new entity maps a Postgres `TEXT[]` column, copy this exact two-annotation pattern from `ProjectEntity` or `OfficeEntity`. The pattern is tested implicitly by the controller slice tests which use mock service returns, not DB reads, so add a DB integration test if the array-read path is high-risk.

---

## PITFALL-011 — BIGINT GENERATED ALWAYS AS IDENTITY primary keys have no public setter; anonymous subclass override needed in tests

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Symptom:** `LeadServiceTest` needs to verify that the service returns the persisted entity's id. The entity's `id` field is `@GeneratedValue` from a `BIGINT GENERATED ALWAYS AS IDENTITY` column and has no public setter (adding one would be wrong — the DB owns the value).

**Root cause:** `GENERATED ALWAYS` columns deliberately cannot be overridden via `INSERT ... VALUES (id, ...)`. The JPA entity correctly has no public setter. `LeadEntity entity = new LeadEntity(); entity.setId(42L)` does not compile.

**Fix:** Use an anonymous subclass override in the test helper:
```java
private LeadEntity savedEntity(long id) {
    return new LeadEntity() {
        @Override public Long getId() { return id; }
    };
}
```
Pass this to `when(leadRepository.save(any())).thenReturn(savedEntity(42L))`.

**Rule for future features:** All BIGINT GENERATED ALWAYS AS IDENTITY entities follow this pattern. Never add a `setId()` to an entity to make tests easier — use the anonymous subclass pattern in the test instead. If many tests need it, extract into a shared `TestEntityBuilders` class in `src/test/java/com/alef/api/testutil/`.
