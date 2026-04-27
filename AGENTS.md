# AGENTS.md — Backend (Java Spring Boot)

> Read `/AGENTS.md` first for monorepo-level rules. This file adds backend-specific constraints.

---

## 1. Package Architecture

```
com.prenota24.backend/
├── auth/               JWT filter, entry point, token utilities
├── common/             Cross-cutting: exceptions, AuthHelper, GlobalExceptionHandler
├── config/             Spring config beans (Security, CORS, Jackson, OpenAPI, Scheduling)
├── controller/         HTTP layer only — no business logic
├── domain/             JPA entities + enums
├── dto/                Request/response records — never expose domain entities directly
├── repository/         Spring Data JPA interfaces
└── service/
    ├── I*.java         Service interfaces (contracts)
    └── impl/           Service implementations (business logic lives here)
```

**Strict layer discipline:**
- Controllers → call services via interfaces (`IAppointmentService`, not `AppointmentService`)
- Services → call repositories, domain objects, other services
- Repositories → database access only
- Domain → pure data + JPA lifecycle callbacks. No Spring beans inside domain classes.

---

## 2. Entity Patterns

### 2.1 Standard Entity Conventions
Every entity follows this pattern (see `Appointment.java`, `Professional.java`):

```java
@Entity
@Table(name = "snake_case_table_name")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Timestamps managed via @PrePersist / @PreUpdate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() { Instant now = Instant.now(); createdAt = updatedAt = now; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
```

- Primary keys: always `UUID` with `GenerationType.UUID`
- Timestamps: always `Instant` (UTC). Never `LocalDateTime` for timestamps.
- Date-only fields: `LocalDate`. Time-only fields: `LocalTime`.
- Column names: always explicit `snake_case` via `@Column(name = "...")`
- String lengths: always specify `length =` on `@Column` where relevant

### 2.2 Associations
- `@ManyToOne`: always `fetch = FetchType.LAZY`. Eager is a deliberate exception requiring a comment.
- `@OneToMany`: use `mappedBy`, `cascade = CascadeType.ALL`, `orphanRemoval = true` for owned collections.
- `@ManyToMany`: use join table with explicit `@JoinTable`. Keep the owning side consistent.
- Never expose bidirectional relationships in DTOs. Map only what is needed.

### 2.3 Lombok Usage
Use: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@RequiredArgsConstructor`
Avoid: `@Data` on JPA entities (breaks `equals`/`hashCode` on proxies), `@ToString` with lazy associations (triggers N+1).

---

## 3. DTO Patterns

### 3.1 Use Java Records for DTOs
```java
// Response
public record AppointmentResponse(
        UUID id,
        UUID studioId,
        Instant startDatetime,
        AppointmentStatus status,
        String notes
) {}

// Request with validation
public record CreateAppointmentRequest(
        @NotNull UUID professionalId,
        @NotNull UUID clientId,
        @NotNull Instant startDatetime,
        @NotNull Instant endDatetime,
        @Size(max = 2000) String notes,
        Boolean confirmImmediately
) {}
```

- Response DTOs: plain records, no validation annotations
- Request DTOs: use Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Valid` for nested objects)
- Never return raw domain entities from controllers

### 3.2 Naming Conventions
| Pattern | Example |
|---|---|
| Create request | `Create{Entity}Request` |
| Update request | `Update{Entity}Request` |
| Patch request | `Edit{Entity}Request` (or `Patch{Entity}Request`) |
| Single response | `{Entity}Response` |
| Summary (list) | `{Entity}SummaryResponse` |
| Action request | `{Action}{Entity}Request` (e.g., `CancelAppointmentRequest`) |

---

## 4. Controller Rules

### 4.1 Controllers Are Thin
```java
@RestController
@RequestMapping("/api/portal")
@PreAuthorize("hasRole('PROFESSIONAL')")
@RequiredArgsConstructor
@Tag(name = "Professional Portal", description = "...")
public class ProfessionalPortalController {

    private final IProfessionalPortalService portalService;
    private final AuthHelper authHelper;

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(
            @RequestBody @Valid CreateAppointmentRequest request,
            Authentication auth) {
        return portalService.createAppointment(
                request,
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }
}
```

Controllers must:
- Validate input with `@Valid`
- Extract identity via `AuthHelper` (never parse JWT manually in controllers)
- Delegate all logic to a service
- Return DTOs directly (Spring handles serialization)
- Annotate with `@ResponseStatus` when returning non-200

Controllers must NOT:
- Contain `if/else` business logic
- Access repositories directly
- Construct domain entities
- Call other controllers

### 4.2 Route Naming
- Resource nouns in kebab-case: `/service-types`, `/client-notes`
- State transition actions as verbs: `POST /appointments/{id}/confirm`
- Keep routes RESTful. Avoid `/getAll`, `/createNew`, etc.

### 4.3 Authorization
- Class-level `@PreAuthorize` for role guard
- Method-level `@PreAuthorize` for ownership checks when needed
- Always verify resource ownership in the service (not just role), e.g., ensure `appointment.professional.id == professionalId`

---

## 5. Service Layer Rules

### 5.1 Interface + Implementation Separation
Every feature service has:
- `IMyFeatureService` interface in `service/`
- `MyFeatureService` implementation in `service/impl/`

Controllers depend on the interface. This enables clean testing and keeps the contract explicit.

### 5.2 Transaction Boundaries
```java
@Override
@Transactional(readOnly = true)   // ← for read-only operations
public Page<AppointmentResponse> getMyAppointments(...) { ... }

@Override
@Transactional                    // ← for write operations
public AppointmentResponse createAppointment(...) { ... }
```

- `@Transactional(readOnly = true)` on all GET methods — enables read-only optimizations in Hibernate
- `@Transactional` on all write methods
- Never call `@Transactional` methods from within the same class (proxy bypass). Extract to a separate bean if needed.
- Manage cascades through the owning side of associations (e.g., `exception.getSlots().add(slot)`)

### 5.3 Business Logic Ownership
Business rules live in services. Examples of what belongs in a service:
- State machine transitions
- Multi-tenant ownership checks
- Slot availability calculations
- Business validation (beyond Bean Validation)
- Cross-entity operations

### 5.4 Private Helper Methods
Keep services readable by extracting private helpers:
```java
private Professional findProfessional(UUID id) {
    return professionalRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato"));
}

private AppointmentResponse toAppointmentResponse(Appointment a) { ... }
```

---

## 6. Exception Handling

### 6.1 Exception Hierarchy
All custom exceptions are in `common/`:

| Exception | HTTP Status | Use When |
|---|---|---|
| `EntityNotFoundException` | `404 NOT_FOUND` | Resource not found or not owned by tenant |
| `EmailAlreadyRegisteredException` | `409 CONFLICT` | Duplicate email on registration |
| `IllegalStateTransitionException` | `422 UNPROCESSABLE_CONTENT` | Invalid appointment state machine transition |
| `SlotNotAvailableException` | `409 CONFLICT` | Booking slot already taken |
| `JwtAuthenticationException` | `401 UNAUTHORIZED` | JWT parse/validation failure |
| `EmailNotVerifiedException` | `403 FORBIDDEN` | Login attempt before email verification |

`GlobalExceptionHandler` in `common/` handles all of these. Do not add `try/catch` in controllers.

### 6.2 Adding New Exceptions
1. Create exception class in `common/` extending `RuntimeException`
2. Add handler method to `GlobalExceptionHandler`
3. Use `HttpStatus.UNPROCESSABLE_CONTENT` (not the deprecated `UNPROCESSABLE_ENTITY`)

---

## 7. Database & Flyway

### 7.1 Flyway Migration Rules
- All schema changes go through Flyway migrations in `src/main/resources/db/migration/`
- Naming: `V{major}_{minor}_{patch}__{Description_with_underscores}.sql`
- **Never modify an already-executed migration.** Create a new one.
- Safe evolution: `ADD COLUMN` with `DEFAULT` or `NULL`, `CREATE INDEX CONCURRENTLY`, `CREATE TABLE`
- Dangerous (warn explicitly): `DROP COLUMN`, `ALTER COLUMN TYPE`, `RENAME COLUMN` — these can break running instances

### 7.2 PostgreSQL Column Conventions
| Java Type | SQL Type |
|---|---|
| `UUID` | `uuid` |
| `Instant` | `timestamptz` |
| `LocalDate` | `date` |
| `LocalTime` | `time` |
| `String` (short) | `varchar(N)` |
| `String` (long) | `text` |
| `boolean` / `Boolean` | `boolean` |
| `BigDecimal` | `numeric(10,2)` |
| `Integer` / `Long` | `integer` / `bigint` |

### 7.3 Indexes & Constraints
- Foreign keys: always declared in Flyway SQL, not just via JPA annotations
- Index on: foreign key columns, `status` enum columns used in WHERE clauses, `date` columns for range queries
- Unique constraints: at DB level, not just via `@Column(unique=true)`
- Named constraints: `fk_{table}_{ref}`, `idx_{table}_{col}`, `uq_{table}_{col}`

### 7.4 N+1 Prevention
- Use `@EntityGraph` or JPQL `JOIN FETCH` when loading associations in list queries
- `@Transactional(readOnly = true)` scope keeps the session open for lazy loading within the transaction
- Avoid calling lazy getters outside a transaction
- If a mapping method (`toXxxResponse`) accesses multiple lazy fields, ensure it runs within a transaction

---

## 8. Security Rules

### 8.1 Input Security
- All request bodies annotated with `@Valid` in controllers
- Bean Validation constraints on all request DTOs
- Never trust client-supplied IDs for ownership — always verify against the authenticated user's `studioId`/`professionalId`

### 8.2 JWT
- JWT secret loaded from environment variable via `JwtProperties` config record — never hardcoded
- Access tokens are short-lived; refresh tokens have longer TTL
- `JwtAuthenticationFilter` extracts claims into Spring `Authentication` principal
- `AuthHelper` provides `getProfessionalId()`, `getStudioId()`, `getUserId()` — use these everywhere

### 8.3 Secrets & Config
- No secrets in source code or `application.properties` committed to VCS
- All environment-specific config via `application-{profile}.properties` or environment variables
- `DataSourceProperties`, `JwtProperties`, `MailProperties`, `CorsProperties` are typed config records annotated with `@ConfigurationProperties`

### 8.4 CORS
- Allowed origins configured via `CorsProperties` (not hardcoded)
- CORS configured at Spring Security level, not at controller level

---

## 9. Coding Style

### 9.1 Injection
Always constructor injection via `@RequiredArgsConstructor` + `final` fields. Never `@Autowired` field injection.

### 9.2 Java Style
- Use `var` for local variables where type is obvious from the right-hand side
- Prefer streams and `toList()` over explicit `ArrayList` construction
- Use pattern matching and modern switch expressions (Java 21)
- Method names: `camelCase` verbs — `getMyAppointments`, `createClient`, `toAppointmentResponse`
- No magic strings — use constants or enums

### 9.3 Comments
- Comment **why**, not **what**
- Javadoc on public interface methods only
- Inline comments for non-obvious business rules or workarounds

---

## 10. Performance & Reliability

- **Pagination:** Any endpoint returning collections of unbounded size must use `Page<T>` with `Pageable`
- **Scheduling:** Background jobs in `ReminderScheduler`. Use `@Scheduled` with fixed rate or cron. Keep jobs idempotent.
- **Notification system:** `NotificationDispatcher` + `NotificationSender` interface. Never send emails directly from services.
- **Rate limiting:** `RateLimitFilter` applies to auth endpoints — ensure new public endpoints are protected too
- **Logging:** Use `LoggerFactory.getLogger(MyClass.class)`. Log at `WARN`/`ERROR` for unexpected failures, `INFO` for significant business events, `DEBUG` for dev-only detail.

---

## 11. Anti-Patterns to Reject

| Anti-pattern | Correct alternative |
|---|---|
| Business logic in controllers | Move to service |
| `@Autowired` field injection | `@RequiredArgsConstructor` + `final` |
| Returning entities from controllers | Map to DTO first |
| Calling `EntityManager` directly in services | Use repositories |
| `findAll()` without pagination on large tables | `findAll(Pageable)` |
| Catching `Exception` broadly and swallowing it | Let `GlobalExceptionHandler` handle it |
| Hardcoded strings for status/role comparisons | Use enums |
| Magic numbers in queries | Named constants |
| `@Transactional` on private methods | Public methods only (proxy limitation) |
| Modifying executed Flyway migrations | Create a new migration |
| `HttpStatus.UNPROCESSABLE_ENTITY` | `HttpStatus.UNPROCESSABLE_CONTENT` (Spring 7+) |
