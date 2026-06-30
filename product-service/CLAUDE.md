# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1.0 REST service — Java 17, Maven, Spring Data JPA (H2 in-memory), Bean Validation, Lombok.

## Build & Run

```bash
./mvnw spring-boot:run      # start the service (Windows: mvnw.cmd)
./mvnw test                 # run all tests
./mvnw compile              # compile only
./mvnw package              # build fat JAR to target/
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Architecture

Layered: `controller` → `service` → `repository`. Keep each layer in its own package under `com.naveen.productservice`.

- Controllers: REST endpoints only — no business logic.
- Services: business logic, transaction boundaries (`@Transactional`).
- Repositories: Spring Data JPA interfaces only — no custom SQL unless necessary.
- Use separate DTO classes for request/response; do not expose JPA entities directly from controllers.

## Lombok

Lombok is configured as an annotation processor. Prefer:
- `@Data` / `@Value` for simple model classes
- `@Builder` for complex construction
- `@RequiredArgsConstructor` for constructor injection in services/controllers

Do not write boilerplate getters/setters/constructors that Lombok can generate.

## Database

H2 in-memory database is used for development and tests. Schema is auto-created by Hibernate (`spring.jpa.hibernate.ddl-auto`). Configure in `application.properties` or `application-test.properties` as needed.

## Testing

JUnit 5 via Spring Boot test starters. Run a single test class:

```bash
./mvnw test -Dtest=MyTestClass
```

Use `@SpringBootTest` for integration tests. Use `@WebMvcTest` for controller-layer tests in isolation.
