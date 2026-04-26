# Quarkus Backend — A Tour for Spring Boot Developers

A guide to this project's backend tailored for someone fluent in Spring Boot but new to Quarkus.

## High-level shape

```
backend/
├── pom.xml                       # Maven, Quarkus 3.15.2, Java 21
└── src/main/java/com/stocktracker/
    ├── api/                      # JAX-RS resources (≈ @RestController)
    ├── service/                  # @ApplicationScoped business logic
    ├── persistence/              # Panache repositories
    ├── domain/                   # JPA entities (Panache)
    ├── dto/                      # Java records for request/response
    └── bootstrap/                # @Observes StartupEvent seeders
```

Familiar layered architecture: resource → service → repository → entity.

## Spring Boot → Quarkus translation cheat sheet

| Spring Boot | Quarkus | Notes |
|---|---|---|
| `@SpringBootApplication` + main | *(none)* | Quarkus generates the bootstrap. Just run `./mvnw quarkus:dev`. |
| `@RestController` | `@Path("/api/...")` | JAX-RS, not Spring MVC |
| `@GetMapping` / `@PostMapping` | `@GET` / `@POST` + `@Path` | from `jakarta.ws.rs.*` |
| `@PathVariable` / `@RequestParam` / `@RequestBody` | `@PathParam` / `@QueryParam` / plain method param | |
| `@Component` / `@Service` | `@ApplicationScoped` (CDI) | |
| `@Autowired` | `@Inject` | Field injection is idiomatic in Quarkus |
| `@Value("${x}")` | `@ConfigProperty(name="x")` | from MicroProfile Config |
| `@ControllerAdvice` + `@ExceptionHandler` | `@Provider implements ExceptionMapper<T>` | See `api/ApiExceptionMapper.java` |
| `@SpringBootTest` + MockMvc | `@QuarkusTest` + REST Assured | |
| Spring Data `JpaRepository` | `PanacheRepository` / Active Record on entity | See below |
| `application-dev.yml` | `application.properties` with `%dev.foo=bar` prefix | |
| Spring Boot DevTools | `quarkus:dev` live reload | Faster, no class reloader hacks |

## Concrete things to look at

**Entry points (`api/`):**
- `DashboardResource` — `GET /api/dashboard`
- `InstrumentResource` — `GET /api/instruments/{ticker}`
- `WatchlistResource` — full CRUD on `/api/watchlists` and nested tickers
- `TransactionsResource` — list/create/delete + CSV import/export via `@RestForm FileUpload`
- `ApiExceptionMapper` — central error → JSON translation

**Services (`service/`)** — same role as Spring `@Service`. `@Transactional` is **per method**, not auto-applied to the class — watch for this.

**Persistence — Panache:**
This project uses the **repository style** of Panache (closer to what you're used to):

```java
@ApplicationScoped
public class InstrumentRepository implements PanacheRepository<Instrument> {
    public Optional<Instrument> findBySymbol(String s) {
        return find("upper(symbol) = ?1", s.toUpperCase()).firstResultOptional();
    }
}
```

Two flavors you'll see:
- `PanacheRepository<E>` — for `Long` PK
- `PanacheRepositoryBase<E, ID>` — for other PK types (used here for `UUID` PKs)

Entities extend `PanacheEntityBase` and **expose public fields** (no getters/setters) — Panache rewrites bytecode at build time. Lifecycle is via standard JPA `@PrePersist` / `@PreUpdate` (e.g., for setting `id`/`createdAt`).

You can call static finders directly on the entity (`Instrument.findById(...)`) — Active Record style — but this codebase prefers explicit repositories.

**Configuration:** `src/main/resources/application.properties`
- MySQL datasource, env-overridable via `QUARKUS_DATASOURCE_*`
- `quarkus.hibernate-orm.database.generation=validate` (no auto-DDL)
- **Flyway** runs migrations at startup from `db/migration/V1__init_schema.sql`
- CORS open for the frontend dev server

**Startup hooks (`bootstrap/`):**
```java
@ApplicationScoped
public class ReferenceDataBootstrap {
    @Transactional
    void onStart(@Observes StartupEvent e) { ... }
}
```
This is the CDI replacement for `ApplicationRunner` / `@EventListener(ApplicationReadyEvent)`.

**Tests (`src/test/...`):**
- `@QuarkusTest` boots the full app per test class
- REST Assured (`given().when().get(...).then()...`) instead of MockMvc
- `quarkus.datasource.devservices.enabled=true` in test config → Quarkus auto-spins a MySQL Testcontainer. No manual Docker setup for tests.
- `IntegrationTestSupport` gives you `UserTransaction` for explicit tx control and a `@BeforeEach` reset

## Three things that bite Spring devs

1. **Build-time DI.** Quarkus resolves the bean graph during the Maven build, not at runtime. Adding a dependency that registers beans via classpath scanning won't "just work" — it must be a Quarkus extension. Pure libraries that you'd `@Bean`-wrap in Spring need a CDI producer (`@Produces`).
2. **`@Transactional` is not implicit on services.** Forget it on a write method and you'll get `TransactionRequiredException`.
3. **Public fields on entities look wrong but are correct.** Don't "fix" them — Panache rewrites accessors at build time so framework code still goes through getters/setters.

## Run it

```bash
cd backend
./mvnw quarkus:dev          # live-reload dev mode, port 8080
./mvnw test                 # spins up a MySQL container automatically
./mvnw package              # fast JVM jar in target/quarkus-app/
```

Dev UI at http://localhost:8080/q/dev — bean graph, config, datasource, Flyway. Worth a poke.
