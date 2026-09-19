# Movie Ticket Booking System

A Spring Boot backend for a multi-city movie ticket booking platform with seat-level inventory, time-bound holds, dynamic pricing, discount codes, payments, cancellations/refunds, role-based access control, refresh-token authentication, and asynchronous notifications.

## Core Capabilities

- Customer registration and JWT authentication
- Refresh tokens and logout/revocation
- `ADMIN` and `CUSTOMER` roles
- Multiple cities, theaters, screens, movies and shows
- Reusable physical seat layouts
- Per-show seat inventory
- Regular/premium pricing with weekend adjustment
- 5-minute temporary seat holds
- Pessimistic locking to prevent double booking
- Discount codes
- Idempotent payment processing
- Configurable refund policies
- Booking cancellation and seat release
- Async booking/refund notifications
- Scheduled booking reminders
- Centralized validation and error handling

## Technology

```text
Java 17
Spring Boot
Spring MVC
Spring Data JPA
Spring Security
JWT
MySQL
MapStruct
Lombok
Maven
```

## Architecture

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
MySQL
```

Supporting components include strategies, resolvers, mappers, schedulers, Spring events, and security filters.

## Important State Models

```text
ShowSeat:
AVAILABLE -> HELD -> BOOKED
HELD -> AVAILABLE

SeatHold:
ACTIVE -> CONFIRMED
ACTIVE -> EXPIRED
ACTIVE -> CANCELLED

Booking:
PENDING_PAYMENT -> CONFIRMED
PENDING_PAYMENT -> EXPIRED
CONFIRMED -> CANCELLED

Payment:
PENDING -> SUCCESS
PENDING -> FAILED
SUCCESS -> REFUNDED
```

## Seat Concurrency

Seat holds use database `PESSIMISTIC_WRITE` locks. Multiple seat IDs are sorted before locking, reducing deadlock risk.

A second customer requesting the same seat must wait for the first transaction. After the first commits the seat as `HELD`, the second request is rejected.

## Authentication

Access tokens are JWTs. Refresh tokens are opaque values stored in MySQL.

```text
Access token  -> 15 minutes
Refresh token -> 7 days
```

Public registration creates `CUSTOMER`. The first admin can be bootstrapped manually in the database; afterward an existing admin can promote other registered users through the admin API.

## Pricing

Physical seats are `REGULAR` or `PREMIUM`.

When a show is created, regular and premium base prices are supplied. `PricingStrategyResolver` applies the relevant pricing strategy, including weekend adjustment.

## Booking Flow

```text
Browse Show
   ↓
View Show Seats
   ↓
Create Seat Hold
   ↓
Create Booking
   ↓
PENDING_PAYMENT
   ↓
Process Payment
   ↓
CONFIRMED
   ↓
Async Confirmation
   ↓
Scheduled Reminder
```

## Cancellation Flow

```text
Confirmed Booking
   ↓
Cancel
   ↓
Refund Policy Evaluation
   ↓
Refund Processing
   ↓
Booking CANCELLED
   ↓
Seats AVAILABLE
```

## Notifications

Spring events are handled with:

```text
@Async
@TransactionalEventListener(AFTER_COMMIT)
```

This ensures notifications are emitted only after the main transaction commits.

## Documentation

- `API_DOCUMENTATION.md`
- `SERVICES_AND_ARCHITECTURE.md`
- `HOW_TO_RUN.md`

## Postman

Import:

```text
Movie_Ticket_Booking.postman_collection.json
Movie_Ticket_Booking_Local.postman_environment.json
```

See `HOW_TO_RUN.md` for the recommended test order.

## Scope

The implementation focuses on backend correctness and API behavior. A production deployment would normally add stronger observability, external payment/email integrations, distributed coordination where needed, and secret management.
