# CLAUDE.md — Backend

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Regole specifiche del backend. Contratto API, auth, multi-tenancy e macchina a stati sono nel `CLAUDE.md` della radice del monorepo.

Stack: **Java 25**, **Spring Boot 4.0.x** (Spring Security 7, Hibernate, Flyway, Jackson), PostgreSQL 16, Lombok, jjwt, bucket4j, Resend (email), springdoc. Build con Maven wrapper.

## Comandi

```bash
docker compose up -d                                     # Postgres 16 "prenota24-postgres" su localhost:5432, volume backend_pgdata
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run         # API su :8080, Swagger su /swagger-ui.html
./mvnw test                                              # tutti i test
./mvnw test -Dtest=AppointmentStateMachineTest           # una classe
./mvnw test -Dtest='SlotCalculatorServiceTest#nomeMetodo' # un metodo
./mvnw clean package                                     # jar (esegue anche i test)
```

- **Non esiste un profilo di default.** Imposta `SPRING_PROFILES_ACTIVE` a `dev`, `devmessaging` o `prod`. Senza profilo, `application.yml` richiede dall'ambiente `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `RESEND_*`, ecc.
- Il database di sviluppo è il servizio di `docker-compose.yml` (`5432:5432`, db/user/pass `prenota24`), che `application-dev.yml` raggiunge su `localhost:5432/prenota24`.
- `devmessaging` usa il database `messaging_v1_test` su `localhost:5433`.
- `JwtSecretGuard` blocca l'avvio se un secret che inizia con `dev-only-` viene usato fuori da `dev`/`devmessaging`. Swagger e api-docs sono attivi solo nei profili dev.

## Struttura (`com.prenota24.backend`)

```
auth/        AuthService (login, registrazione, refresh, verifica email, recupero password), JwtAuthenticationFilter, entry point, pulizia refresh token
common/      AuthHelper, GlobalExceptionHandler, eccezioni custom, EmailNormalizer
config/      SecurityConfig, RateLimitFilter, CORS/JWT/Resend/WhatsApp properties (@ConfigurationProperties), Jackson, OpenAPI, scheduling
controller/  solo HTTP
domain/      entity JPA + enum
dto/         record di richiesta/risposta, ErrorResponse
email/       BaseEmailLayout (layout HTML delle email)
messaging/   gateway WhatsApp Cloud API, policy, verifica firma webhook, normalizzazione telefoni
repository/  Spring Data JPA
service/     interfacce I*Service, NotificationSender, NotificationTemplate
service/impl implementazioni e componenti di supporto
```

## Layering

- I controller dipendono solo dalle interfacce `I*Service`: nessun controller importa repository o classi di `service/impl`. Non contengono logica: `@Valid` sul body, identità tramite `AuthHelper`, delega al service, ritorno del DTO, `@ResponseStatus(HttpStatus.CREATED)` / `NO_CONTENT` dove serve. Niente `try/catch`: gli errori li gestisce `GlobalExceptionHandler`.
- Ogni service usato da un controller ha l'interfaccia `service/IXxxService` e l'implementazione `service/impl/XxxService`. I componenti interni usati solo dai service sono classi concrete senza interfaccia: `AppointmentStateMachine`, `AppointmentProposalValidator`, `ServiceTypeAssignmentValidator`, `SlotCalculatorService`, `NotificationService`, `NotificationDispatcher`, `MessageDispatchService` e gli scheduler.
- La logica di business vive nei service: transizioni di stato (sempre tramite `AppointmentStateMachine`), controlli di proprietà e tenant, calcolo degli slot, validazioni oltre Bean Validation. Il mapping entity → DTO avviene in metodi privati `toXxxResponse` dentro il service.
- Injection solo via costruttore (`@RequiredArgsConstructor` + campi `final`). Nel codice main non c'è alcun `@Autowired`.
- Transazioni: `@Transactional(readOnly = true)` sui metodi di lettura, `@Transactional` su quelli di scrittura, solo su metodi pubblici chiamati da un altro bean (il proxy non intercetta le chiamate interne). Con `open-in-view=false` i campi lazy vanno letti dentro la transazione del service, mapping DTO compreso.

## Entity e DTO

- PK `UUID` con `@GeneratedValue(strategy = GenerationType.UUID)`. Timestamp sempre `Instant` (nel codice non c'è alcun `LocalDateTime`), `LocalDate`/`LocalTime` per data o ora. Colonne con `@Column(name = "snake_case")` esplicito.
- `createdAt`/`updatedAt` impostati in `@PrePersist`/`@PreUpdate`.
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`. Mai `@Data` sulle entity, né `@ToString` che tocchi associazioni lazy.
- Tutte le `@ManyToOne` sono `FetchType.LAZY`. Per le liste usa `@EntityGraph` o `JOIN FETCH` nel repository per evitare N+1.
- DTO come `record`: le risposte non hanno annotazioni, le richieste usano Bean Validation. Naming: `Create{X}Request`, `Update{X}Request`, `{X}Response`, `{X}SummaryResponse` per le liste, `{Azione}{X}Request` per le azioni (`CancelAppointmentRequest`, `ProposeNewTimeRequest`). Non restituire mai entity dai controller.
- Le email passano da `EmailNormalizer` prima di confronti e salvataggi (vedi migrazione V19).

## Eccezioni

Le eccezioni stanno in `common/`, estendono `RuntimeException` e hanno un handler dedicato in `GlobalExceptionHandler` con un codice `error` stabile. Usa `HttpStatus.UNPROCESSABLE_CONTENT`, non il deprecato `UNPROCESSABLE_ENTITY`. Messaggi in italiano.

| Eccezione | HTTP | `error` |
|---|---|---|
| `EntityNotFoundException` (anche per risorse di altri tenant) | 404 | `NOT_FOUND` |
| `EmailAlreadyRegisteredException` | 409 | `EMAIL_ALREADY_REGISTERED` |
| `SlotNotAvailableException` | 409 | `SLOT_NOT_AVAILABLE` |
| `ResourceInUseException` (cancellazione bloccata da dati dipendenti) | 409 | `RESOURCE_IN_USE` |
| `MessagingConflictException` | 409 | `IDEMPOTENCY_CONFLICT` |
| `DataIntegrityViolationException` | 409 | `APPOINTMENT_OVERLAP` se il vincolo è `no_overlapping*`, altrimenti `DATA_CONFLICT` |
| `IllegalStateTransitionException` | 422 | `INVALID_STATE_TRANSITION` |
| `InvalidVerificationCodeException` / `InvalidPasswordResetCodeException` | 422 | `INVALID_VERIFICATION_CODE` / `INVALID_RESET_CODE` |
| `BookingLimitExceededException` | 422 | `TOO_MANY_PENDING_BOOKINGS` |
| `MessageSendBlockedException` | 422 | `MESSAGE_SEND_BLOCKED` |
| `EmailNotVerifiedException` / `AccessDeniedException` | 403 | `EMAIL_NOT_VERIFIED` / `ACCESS_DENIED` |
| `JwtAuthenticationException` / `BadCredentialsException` | 401 | `UNAUTHORIZED` / `INVALID_CREDENTIALS` |
| validazione / body illeggibile / parametro errato / `IllegalArgument`/`IllegalStateException` | 400 | `VALIDATION_ERROR` / `MALFORMED_REQUEST` / `BAD_PARAMETER` / `BAD_REQUEST` |

Per aggiungerne una: crea la classe in `common/` e aggiungi il metodo handler.

## Notifiche, job e rate limiting

- Le email non partono mai direttamente da un service. I service chiamano `NotificationService` (`scheduleForTransition`, `scheduleReminder`), che salva una `Notification` e registra un callback **after-commit**. Il callback invoca `NotificationDispatcher.dispatch` (`@Async`), che usa un `NotificationSender` (`EmailNotificationSender` via Resend; template in `service/impl/templates`, layout in `email/`).
- Job `@Scheduled`: `ReminderScheduler` (promemoria e invio delle notifiche pendenti), `RefreshTokenCleanupTask` (ogni giorno alle 3:00), `WhatsappDispatchScheduler` (coda WhatsApp), pulizia dei bucket in `RateLimitFilter`. I job devono essere idempotenti.
- `RateLimitFilter` (bucket4j, in memoria) filtra solo `/api/auth/**` e la creazione di prenotazioni pubbliche (`/api/public/book/**`). Un nuovo endpoint pubblico che scrive dati va aggiunto lì.
- Logging: `private static final Logger logger = LoggerFactory.getLogger(X.class)` con messaggi parametrizzati.

## Database e Flyway

- Con `ddl-auto=validate` ogni modifica a un'entity richiede una migrazione in `src/main/resources/db/migration/`.
- Naming: numerazione sequenziale `V{N}__descrizione_con_underscore.sql`, con N = ultimo numero esistente + 1 (attualmente si arriva a V19). **Non modificare mai una migrazione già applicata.**
- Le FK, gli indici, gli unique e i check si dichiarano nell'SQL con nomi espliciti: `fk_{tabella}_{rif}`, `idx_{tabella}_{col}`, `uq_{tabella}_{col}`, `chk_{tabella}_{col}`.
- Le migrazioni girano in transazione, quindi niente `CREATE INDEX CONCURRENTLY`. Segnala esplicitamente le operazioni distruttive (`DROP`/`RENAME COLUMN`, `ALTER ... TYPE`).
- Tipi: `uuid`, `timestamptz` per `Instant`, `date`, `time`, `varchar(N)`/`text`, `numeric(10,2)` per `BigDecimal`. Gli enum JPA sono stringhe vincolate da `CHECK`.

## Test

- Quasi tutti i test sono unit test JUnit 5 senza contesto Spring: i service vengono costruiti a mano con repository `mock(...)` (Mockito) e asserzioni AssertJ. Un esempio è `TenantIsolationTest`. Segui questo stile per i nuovi test di service, senza `@SpringBootTest`.
- `MessagingDatabaseTest` è l'unico test d'integrazione (`@SpringBootTest` su Postgres reale). Gira solo con `MESSAGING_DB_TEST=true` e un database usa-e-getta passato tramite le variabili `DB_*`. Usa il profilo `test`, che non ha un file YAML.
