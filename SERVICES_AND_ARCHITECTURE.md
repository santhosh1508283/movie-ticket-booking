# Services and Architecture

## Overview

The application uses a layered Spring Boot architecture:

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

Supporting layers include:

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

The base package is:

```text
com.dmg.movieticket
```

---

## 1. Authentication

### `AuthService`

Responsibilities:

- customer registration
- password hashing using BCrypt
- customer login
- JWT generation
- all public registrations receive `Role.CUSTOMER`

### `CurrentUserService`

Reads the current authenticated principal from Spring Security's `SecurityContext`.

Services do not accept `userId` from request payloads for customer-owned resources. Ownership is derived from the authenticated user.

### `CustomUserDetailsService`

Loads users by email and maps roles to Spring authorities:

```text
ADMIN    -> ROLE_ADMIN
CUSTOMER -> ROLE_CUSTOMER
```

### `JwtAuthenticationFilter`

Runs before controllers and:

- reads `Authorization: Bearer ...`
- validates JWT
- extracts the user's email
- loads `UserDetails`
- populates the `SecurityContext`

### `SecurityConfig`

Current rules:

```text
/api/auth/**  -> public
/api/admin/** -> ROLE_ADMIN
everything else -> authenticated
```

---

## 2. CityService

Responsibilities:

- create city
- get city by ID
- list cities
- prevent duplicate names ignoring case

---

## 3. MovieService

Responsibilities:

- create movie
- get movie
- list movies
- search by title
- filter by language
- filter by genre

---

## 4. TheaterService

Responsibilities:

- create theater
- associate theater with city
- prevent duplicate theater name within a city
- get theater
- list theaters
- search theater
- list theaters by city

---

## 5. ScreenService

Responsibilities:

- create screen for a theater
- prevent duplicate screen name inside the same theater
- fetch screens

---

## 6. SeatService

Responsibilities:

- bulk-create physical screen seat layout
- validate duplicate seats in one request
- validate duplicate seats already persisted
- fetch seats by screen

Physical `Seat` records describe the reusable seat layout of a screen.

`Seat.active` controls whether a physical seat should be used when generating seats for new shows.

---

## 7. Pricing

### `PricingStrategy`

```java
boolean supports(PricingContext context);
BigDecimal calculate(PricingContext context);
```

Implemented strategies:

- `DefaultPricingStrategy`
- `WeekendPricingStrategy`

### `PricingStrategyResolver`

Selects the appropriate pricing strategy.

Regular and premium pricing are represented by the base price supplied when a show is created.

Weekend pricing applies a contextual multiplier.

---

## 8. ShowService

Responsibilities:

- create show
- calculate `endTime` from movie duration
- prevent overlapping scheduled shows for the same screen
- load active screen seats
- create a `ShowSeat` per active physical seat
- calculate the final show-seat price using the pricing strategy
- query shows by movie/screen/date range

Show creation generates:

```text
Physical Seat
    ↓
ShowSeat
```

`ShowSeat` represents availability for one seat for one specific show.

---

## 9. ShowSeatService

Responsibilities:

- return show seat availability
- expose seat label, type, price and status

Statuses:

```text
AVAILABLE
HELD
BOOKED
```

---

## 10. SeatHoldService

This service handles the main concurrency requirement.

Responsibilities:

- create temporary seat holds
- retrieve current user's hold
- manually release a hold
- automatically expire stale holds
- release seats when the hold expires

Hold duration is configured using:

```properties
booking.seat-hold-duration-minutes=5
```

### Concurrency

Selected `ShowSeat` rows are loaded with:

```text
PESSIMISTIC_WRITE
```

Multiple seat IDs are sorted before locking to reduce deadlock risk.

Example:

```text
Request A wants A1 + A2
Request B wants A1

Request A locks A1/A2
Request B waits
Request A commits HELD
Request B reads HELD and fails
```

### State transitions

```text
ShowSeat:
AVAILABLE -> HELD -> BOOKED
HELD -> AVAILABLE on hold expiry/release

SeatHold:
ACTIVE -> CONFIRMED
ACTIVE -> EXPIRED
ACTIVE -> CANCELLED
```

---

## 11. SeatHoldExpirationScheduler

Runs periodically:

```properties
booking.seat-hold-cleanup-interval-ms=30000
```

It finds:

```text
status = ACTIVE
expiresAt < now
```

and expires those holds.

The scheduler is cleanup, not the only correctness mechanism. Services also check `expiresAt` synchronously.

---

## 12. DiscountService

Responsibilities:

- validate discount code
- validate active flag and validity window
- validate minimum order amount
- calculate flat or percentage discount
- enforce maximum discount
- prevent discount from exceeding subtotal

Discount result contains:

```text
code
discountAmount
```

---

## 13. BookingService

Responsibilities:

- convert an active hold into a booking
- lock the hold during creation
- prevent multiple bookings from one hold
- snapshot prices and seat details into `BookingItem`
- apply optional discount
- calculate subtotal / discount / final amount
- return customer's bookings

New bookings start as:

```text
PENDING_PAYMENT
```

No seat is converted to `BOOKED` at this stage.

---

## 14. PaymentService

Responsibilities:

- validate idempotency key
- prevent duplicate payment processing
- lock booking
- ensure booking remains payable
- ensure hold is still active
- lock all selected show seats
- create payment attempt
- call `PaymentProcessor`
- atomically confirm booking after success

Successful transaction:

```text
Payment PENDING -> SUCCESS
Booking PENDING_PAYMENT -> CONFIRMED
SeatHold ACTIVE -> CONFIRMED
ShowSeat HELD -> BOOKED
```

Failed payment:

```text
Payment -> FAILED
Booking -> PENDING_PAYMENT
SeatHold -> ACTIVE
ShowSeat -> HELD
```

Customer can retry while the hold remains valid.

### Payment abstraction

`PaymentProcessor` isolates external provider logic.

Current implementation:

```text
MockPaymentProcessor
```

---

## 15. Refund Strategy

### `RefundCalculationStrategy`

Determines refund percentage and amount from cancellation time.

### `ConfiguredRefundStrategy`

Loads active refund policies sorted by required hours before show.

Example:

```text
>= 24h -> 100%
>= 6h  -> 75%
>= 2h  -> 50%
< 2h   -> 0%
```

---

## 16. RefundService

Responsibilities:

- cancel a confirmed booking
- prevent cancellation after show start
- find successful payment
- calculate refund
- lock booked show seats
- process refund
- make seats available again
- mark booking cancelled

State changes:

```text
Booking CONFIRMED -> CANCELLED
ShowSeat BOOKED -> AVAILABLE
Payment SUCCESS -> REFUNDED
```

A zero-value refund still creates a refund audit record.

### Refund abstraction

`RefundProcessor` isolates external provider logic.

Current implementation:

```text
MockRefundProcessor
```

---

## 17. Notification Architecture

Notification delivery uses Strategy + Resolver + Events.

### `NotificationStrategy`

```java
boolean supports(NotificationChannel channel);
void send(String recipient, String message);
```

Current implementation:

```text
EmailNotificationStrategy
```

The email delivery is simulated through application logs.

### `NotificationStrategyResolver`

Chooses a notification implementation based on:

```text
EMAIL
SMS
PUSH
```

Only email is currently implemented.

### `NotificationService`

Responsibilities:

- send booking confirmation
- schedule booking reminder
- send cancellation notification
- send refund processed notification
- process due pending notifications

### Events

```text
BookingConfirmedEvent
BookingCancelledEvent
RefundProcessedEvent
```

Events contain IDs rather than JPA entities.

### `NotificationEventListener`

Uses:

```text
@Async
@TransactionalEventListener(AFTER_COMMIT)
```

This ensures notifications are only sent after the main business transaction commits.

### Reminder scheduler

```properties
notification.processing-interval-ms=60000
```

The reminder is scheduled 2 hours before show time.

---

## 18. Error Handling

`ApplicationException` is the common business exception.

`ErrorCode` provides machine-readable error categories.

`GlobalExceptionHandler` maps errors to HTTP responses.

Examples:

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

## 19. Design Patterns Used

### Repository Pattern

Spring Data JPA repositories isolate persistence.

### Strategy Pattern

Used for:

- pricing
- refund calculation
- notification delivery

### Resolver Pattern

Used to select:

- pricing strategy
- notification strategy

### Observer / Event Pattern

Spring application events decouple notification side effects from booking/payment/refund transactions.

### Mapper Pattern

MapStruct maps entities to DTOs.

### Builder Pattern

Lombok `@Builder` is used to construct domain entities cleanly.

### Explicit State Transition Model

Entities use status enums and services enforce allowed transitions instead of using a heavyweight State Pattern implementation.

### Idempotency

Payment requests require an `Idempotency-Key`.

### Pessimistic Locking

Critical seat and booking/hold transitions use database row locks for serialization.

---

## 20. Primary End-to-End Flow

```text
Admin creates:
City
  ↓
Theater
  ↓
Screen
  ↓
Seat layout
  ↓
Movie
  ↓
Show
  ↓
ShowSeats

Customer:
Browse show
  ↓
View seats
  ↓
Create 5-minute hold
  ↓
Create booking
  ↓
PENDING_PAYMENT
  ↓
Process payment
  ↓
CONFIRMED
  ↓
Async confirmation
  ↓
Scheduled reminder
```

Cancellation:

```text
CONFIRMED booking
   ↓
Cancel
   ↓
Refund policy
   ↓
Refund processing
   ↓
Booking CANCELLED
   ↓
Seats AVAILABLE
   ↓
Async notifications
```
