# Services and Architecture

## Overview

This project uses a layered Spring Boot architecture:

```text
Controller
   ↓
Service Interface
   ↓
Service Implementation
   ↓
Repository
   ↓
MySQL
```

Supporting layers:

```text
DTO
Mapper
Strategy
Resolver
Security
Event
Scheduler
Exception
```

Base package:

```text
com.dmg.movieticket
```

The package name is only a Java namespace and does not affect the API or project behavior.

---

## Authentication and Security

### `AuthService`

Responsibilities:

- customer registration
- BCrypt password hashing
- login
- JWT access-token generation
- refresh-token creation
- access-token refresh
- logout / refresh-token revocation

Public registration always creates `Role.CUSTOMER`.

### `RefreshTokenService`

Responsibilities:

- create one refresh token per user
- invalidate the previous refresh token on a new login
- verify expiry/revocation
- revoke token on logout

Current token lifetime:

```text
Access token  -> 15 minutes
Refresh token -> 7 days
```

### `CurrentUserService`

Reads the authenticated user from Spring Security's `SecurityContext`.

Customer-owned service methods do not accept a `userId` supplied by the client.

### `CustomUserDetailsService`

Loads users by email and maps:

```text
ADMIN    -> ROLE_ADMIN
CUSTOMER -> ROLE_CUSTOMER
```

### `JwtAuthenticationFilter`

- reads bearer token
- validates JWT
- extracts email
- loads `UserDetails`
- populates the `SecurityContext`

### `SecurityConfig`

```text
/api/auth/**  -> public
/api/admin/** -> ROLE_ADMIN
everything else -> authenticated
```

Authorization is checked before the protected controller executes.

---

## CityService

- create city
- fetch city
- list cities
- prevent duplicate names

## MovieService

- create movie
- fetch/list movies
- search by title
- filter by language
- filter by genre

## TheaterService

- create theater
- associate theater with city
- prevent duplicate theater name within the same city
- fetch/search/list theaters

## ScreenService

- create screen
- prevent duplicate screen names within the same theater
- fetch screens

## SeatService

- bulk-create screen seat layout
- detect request duplicates
- detect persisted duplicates
- fetch seats by screen
- preserve `Seat.active` for future show creation

---

## Pricing Strategy

`PricingStrategy` is used to calculate show-seat prices.

Implementations:

```text
DefaultPricingStrategy
WeekendPricingStrategy
```

`PricingStrategyResolver` selects the applicable strategy.

Regular/premium prices are supplied as show base prices; weekend pricing is a contextual adjustment.

---

## ShowService

Responsibilities:

- create shows
- derive end time from movie duration
- reject overlapping scheduled shows for a screen
- load active physical seats
- generate `ShowSeat` records
- calculate final seat price
- query shows by movie, screen and date range

---

## ShowSeatService

Returns seat availability for a show.

States:

```text
AVAILABLE
HELD
BOOKED
```

---

## SeatHoldService

Core concurrency service.

Responsibilities:

- create temporary holds
- retrieve current user's hold
- release a hold
- expire stale holds
- release seats on expiry/cancellation

Configuration:

```properties
booking.seat-hold-duration-minutes=5
```

### Pessimistic Locking

Selected `ShowSeat` rows use `PESSIMISTIC_WRITE`.

IDs are sorted before locking to reduce deadlock risk.

State transitions:

```text
ShowSeat:
AVAILABLE -> HELD -> BOOKED
HELD -> AVAILABLE

SeatHold:
ACTIVE -> CONFIRMED
ACTIVE -> EXPIRED
ACTIVE -> CANCELLED
```

---

## SeatHoldExpirationScheduler

Configured with:

```properties
booking.seat-hold-cleanup-interval-ms=30000
```

It finds expired active holds and releases their seats.

If a related booking is still `PENDING_PAYMENT`, it becomes `EXPIRED`.

The scheduler is cleanup; services also check expiry synchronously.

---

## DiscountService

- validate discount code
- validate active flag
- validate validity window
- validate minimum order amount
- support FLAT/PERCENTAGE discounts
- enforce maximum discount
- cap discount at subtotal

---

## BookingService

- lock seat hold
- prevent multiple bookings from one hold
- snapshot seat information and prices
- calculate subtotal
- apply optional discount
- create `PENDING_PAYMENT` booking
- return current user's bookings

---

## PaymentService

- require idempotency key
- return existing result for repeated idempotent requests
- lock booking
- validate active hold
- lock all show seats
- create payment attempt
- call `PaymentProcessor`
- confirm booking atomically on success

Success:

```text
Payment PENDING -> SUCCESS
Booking PENDING_PAYMENT -> CONFIRMED
SeatHold ACTIVE -> CONFIRMED
ShowSeat HELD -> BOOKED
```

Failure:

```text
Payment -> FAILED
Booking -> PENDING_PAYMENT
SeatHold -> ACTIVE
ShowSeat -> HELD
```

`MockPaymentProcessor` simulates the external payment provider.

---

## Refund Strategy

`RefundCalculationStrategy` calculates refund percentage and amount.

`ConfiguredRefundStrategy` uses persisted active `RefundPolicy` records.

Example policy set:

```text
>= 24 hours -> 100%
>= 6 hours  -> 75%
>= 2 hours  -> 50%
< 2 hours   -> 0%
```

---

## RefundService

- cancel confirmed booking
- prevent cancellation after show start
- locate successful payment
- calculate refund
- lock seats
- process refund
- make seats available again
- mark booking cancelled

Transitions:

```text
Booking CONFIRMED -> CANCELLED
ShowSeat BOOKED -> AVAILABLE
Payment SUCCESS -> REFUNDED
```

A zero-value refund can still create an audit record.

`MockRefundProcessor` simulates the external provider.

---

## Notification Architecture

Notification delivery uses Strategy + Resolver + Events.

### Strategy

`NotificationStrategy`

Current implementation:

```text
EmailNotificationStrategy
```

Email delivery is simulated via application logs.

### Resolver

`NotificationStrategyResolver` selects a strategy using `NotificationChannel`.

### Events

```text
BookingConfirmedEvent
BookingCancelledEvent
RefundProcessedEvent
```

Events carry IDs instead of JPA entities.

### Listener

`NotificationEventListener` uses:

```text
@Async
@TransactionalEventListener(AFTER_COMMIT)
```

This prevents notifications from being sent for rolled-back business transactions.

### Reminder Scheduler

```properties
notification.processing-interval-ms=60000
```

Booking reminders are scheduled for two hours before show time.

---

## Error Handling

`ApplicationException` is the common business exception.

`GlobalExceptionHandler` maps business errors to consistent API responses.

Typical mappings:

```text
RESOURCE_NOT_FOUND       -> 404
DUPLICATE_RESOURCE       -> 409
UNAUTHORIZED             -> 401
SEAT_NOT_AVAILABLE       -> 409
HOLD_EXPIRED             -> 410
INVALID_STATE            -> 409
INVALID_REQUEST          -> 400
CANCELLATION_NOT_ALLOWED -> 400
```

---

## Design Patterns

- Repository Pattern
- Strategy Pattern
- Resolver Pattern
- Observer/Event Pattern
- Mapper Pattern with MapStruct
- Builder Pattern with Lombok
- Explicit state-transition model
- Idempotency for payment processing
- Pessimistic locking for critical booking flows

---

## End-to-End Flow

```text
Admin:
City
  ↓
Theater
  ↓
Screen
  ↓
Seat Layout
  ↓
Movie
  ↓
Show
  ↓
ShowSeats

Customer:
Browse
  ↓
Hold Seats
  ↓
Create Booking
  ↓
PENDING_PAYMENT
  ↓
Payment
  ↓
CONFIRMED
  ↓
Async Confirmation
  ↓
Scheduled Reminder
```

Cancellation:

```text
CONFIRMED
   ↓
Cancel
   ↓
Refund Policy
   ↓
Refund Processing
   ↓
CANCELLED
   ↓
Seats AVAILABLE
```
