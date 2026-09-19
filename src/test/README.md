# Test suite

Run the unit and HTTP tests without a database:

```powershell
.\mvnw.cmd '-Dtest=*Test' test
```

If Maven is already installed, the equivalent command is:

```powershell
mvn '-Dtest=*Test' test
```

The `*Test` selector includes the existing City, Movie, Theater, and Screen tests and all new tests. It intentionally excludes `MovieTicketBookingApplicationTests`, the existing full application context smoke test. That test requires the application's database and runtime configuration. No production configuration or dependencies were changed for this suite.

## Coverage

- Services: catalog management, bulk seat layouts, show scheduling and pricing, seat holds and expiry, booking snapshots and ownership, payment retries and idempotency, cancellation/refunds, discounts, refund policies, authentication, refresh tokens, user administration, and notifications.
- Mappers: real generated MapStruct implementations are exercised through service tests, including nested relationships, seat labels, prices, and booking snapshots.
- HTTP: every controller route is exercised through Spring MVC with mocked services and the real security configuration. Tests cover request binding, response status/serialization, validation errors, anonymous access, and administrator-only routes.
- Security: JWT signing, expiry and wrong signatures, bearer-token filtering, current-user lookup, and user authorities.
- Validation: valid DTOs, missing fields, nested seat validation, numeric boundaries, string lengths, and email/password constraints.
- Strategies and background entry points: weekday/weekend pricing, refund thresholds and rounding, notification strategy resolution, mock payment/refund processors, event listener dispatch, and scheduler dispatch.

## Test boundaries

Repositories and external processors are mocked. These tests check lock-method selection, sorted seat IDs, state transitions, ownership scoping, and failure behavior; they do not prove database locking, transaction rollback, unique constraints, or concurrent double-booking prevention.

Listener and scheduler tests invoke their entry points directly. They do not prove asynchronous execution, scheduling cadence, or after-commit event delivery.

Database integration tests should use an isolated MySQL test database and cover simultaneous seat holds, overlapping show creation, payment races, cancellation/expiry races, transaction rollback after provider failures, and repository query behavior. The existing full-context smoke test can be run with the complete suite once that environment is configured:

```powershell
.\mvnw.cmd test
```

Surefire writes per-class results to `target/surefire-reports`. Old report files may remain from previous runs; use the current Maven run's summary when checking totals.
