# Contributing to ParcelHub

Thanks for helping improve ParcelHub. This guide keeps contributions consistent and easy to review.

## Workflow

1. Fork the repository and create a topic branch from `main`
   (`feature/<short-name>`, `fix/<short-name>` or `docs/<short-name>`).
2. Make your change in small, focused commits.
3. Run the checks locally (see below) and make sure they pass.
4. Open a pull request that explains **what** changed and **why**.

## Commit messages

Use the imperative mood and a short type prefix:

```
feat: add postponed-parcel re-assignment
fix: reject onboarding with duplicate national id
docs: describe pricing properties
test: cover invalid status transitions
```

## Local checks

```bash
./gradlew spotlessApply   # format the code (Palantir Java Format, tabs)
./gradlew test            # unit tests (H2 in-memory, no external services)
./gradlew build           # compile + tests + coverage report
```

## Code conventions

- Layers: `controller` -> `service` -> `repository`. Controllers never touch repositories
  for write operations.
- Request objects live in `dto.request`, view objects in `dto.response`.
- Business rules (pricing, status transitions, balances) belong in services and need a unit test.
- Never commit secrets. Anything environment specific is read from environment variables
  (see the configuration table in the README).
- Schema changes are new Flyway migrations (`V<next>__description.sql`); never edit an applied one.

## Pull request checklist

- [ ] Code is formatted (`spotlessApply`) and the build is green
- [ ] New behaviour is covered by tests
- [ ] No credentials, personal data or environment-specific URLs were added
- [ ] Documentation / diagrams updated when behaviour changed
