# Handoff: Core Marketing Pages — QA -> Dev (BLOCKED)

| Field | Value |
|---|---|
| Feature # | 011 |
| From / To | QA -> Dev |
| Date | 2026-07-01 |
| Branch | feature/011-core-marketing-pages |
| QA status | BLOCKED — 1 spec violation requires a fix before approval |
| Testing spec | specs/011-core-marketing-pages/testing.md |

---

## What passed

- 58/58 backend tests green, `tsc --noEmit` clean.
- F11-AC1, AC2, AC3, AC5, AC6 (core form flow), AC7, AC8, AC9 all verified.
- All Flyway migrations V6–V11 match the architecture spec exactly.
- Honeypot, rate-limit guard, graceful degradation, reduced-motion fallback — all verified.
- "Cadding" preserved throughout. "International" typo corrected in `CompanyStory`.

---

## Blocking bug — must fix before QA can approve

### Bug 2: `POST /api/leads` 429 response is missing `Retry-After: 3600` header

**File:** `api/src/main/java/com/alef/api/lead/service/LeadService.java`, lines 110–113

**What the architecture mandates** (architecture §3.4, §10 Decision 2):
> "on threshold returns `429 Too Many Requests` with `Retry-After: 3600`"

**What the code does:** throws a bare `ResponseStatusException` with no custom headers. `Retry-After` is never set.

**Fix required:**
The 429 must carry `Retry-After: 3600` in the HTTP response headers. The standard Spring approach is to use the three-argument `ResponseStatusException` constructor that accepts `HttpHeaders`, or to handle the 429 in `LeadExceptionHandler` and set the header there.

A minimal correct implementation:
```java
HttpHeaders headers = new HttpHeaders();
headers.set("Retry-After", String.valueOf(RATE_WINDOW_SECONDS));
throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many enquiries.", null, headers);
```
Or equivalently in a `@ExceptionHandler` that catches `ResponseStatusException` and adds the header.

**Test gap to close:** `LeadControllerTest.submit_rate_limit_breach_returns_429()` must add:
```java
.andExpect(header().string("Retry-After", "3600"));
```

---

## Non-blocking findings (no code fix required)

### Dev handoff document error (no code change needed)
`specs/011-core-marketing-pages/handoffs/4-dev-to-qa.md` line 38 states "Rate limit: 10 submissions per IP per hour". The actual code correctly implements 5/hour (matching the architecture). The handoff document number is wrong. Please update the handoff document to read "5 submissions per IP per hour" so the documentation is consistent. This is a documentation correction only.

### "Concorse" → "Concourse" correction gap (informational)
F11-AC4 requires this typo to be corrected. Neither "Concorse" nor "Concourse" appears in the rendered static text of `CompanyStory` — the content was written fresh rather than copied from the live site, so the misspelling was never introduced. The requirement is effectively satisfied by omission, but the claim in the `company-story.tsx` doc comment is not verifiable in the rendered output. No action required unless the landmark project names containing "Concourse" (Dubai Airport Concourses 3 & 4) are added to the DB seed in this branch, in which case verify correct spelling there.

---

## Re-QA procedure

Once Bug 2 is fixed:
1. Fix `LeadService.enforceRateLimit()` to include the `Retry-After: 3600` header in the 429 response.
2. Add `header().string("Retry-After", "3600")` assertion to `LeadControllerTest.submit_rate_limit_breach_returns_429()`.
3. Confirm `./mvnw test` still reports 58+ tests, 0 failures.
4. Re-deliver to QA with an updated `4-dev-to-qa.md` noting the fix.
5. QA will verify the header is present and update `testing.md` status to `approved` if no new issues are found.
