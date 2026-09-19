# Movie Ticket Booking System

A backend-only Movie Ticket Booking System built using Spring Boot and MySQL as part of the DMG Java Developer take-home assignment.

The system is designed to support multiple cities, theaters, screens, movies, shows, seat-level booking, temporary seat holds, pricing, discount codes, payments, cancellations, refunds, and asynchronous notifications.

The primary engineering focus is preventing double booking when multiple users attempt to reserve the same seat concurrently.

---

## Tech Stack

- Java 17
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Hibernate
- Spring Security
- Bean Validation
- MySQL
- Lombok
- Maven
- JUnit / Spring Test

---

## Assignment Scope

The system supports:

- Multiple cities
- Multiple theaters per city
- Multiple screens per theater
- Seat layouts per screen
- Multiple movies and shows
- Seat-level availability
- Time-bound seat holds
- Regular and premium seats
- Dynamic show pricing
- Discount codes
- Payment processing
- Booking confirmation
- Booking cancellation
- Configurable refund policies
- Booking notifications and reminders
- Basic role-based access control
- Unit and integration testing

The assignment specifically requires concurrent users attempting to book the same seat to be handled without double allocation. Confirmation and reminder notifications should also not block the booking flow.

---

## Roles

### ADMIN

An administrator will be able to manage:

- Cities
- Theaters
- Screens
- Seat layouts
- Movies
- Shows
- Pricing
- Discount codes
- Refund policies

### CUSTOMER

A customer will be able to:

- Browse movies and shows
- View seat availability
- Hold seats temporarily
- Create bookings
- Make payments
- View booking history
- Cancel bookings
- Receive refunds based on policy

---

## Package Structure

```text
com.dmg.movieticket
│
├── controller
├── service
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── strategy
├── resolver
├── mapper
├── exception
├── config
├── security
├── event
├── scheduler
├── util
│
└── MovieTicketBookingApplication
```

The project follows a layered architecture.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Cross-cutting functionality such as pricing strategies, refund strategies, notification resolution, security, exception handling, and scheduled jobs is kept in dedicated packages.

---

# Database

MySQL is used as the persistence layer.

Local database:

```text
movie_ticket_booking
```

Application configuration uses environment variables for database credentials.

```properties
spring.application.name=movie-ticket-booking

spring.datasource.url=jdbc:mysql://localhost:3306/movie_ticket_booking
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Database passwords are intentionally not committed to the repository.

Required environment variables:

```text
DB_USERNAME
DB_PASSWORD
```

---

# Domain Model

## User

Represents both administrators and customers.

Fields:

```text
id
name
email
password
role
createdAt
updatedAt
```

Roles:

```text
ADMIN
CUSTOMER
```

Email addresses are unique.

---

## City

Represents a city in which theaters operate.

Fields:

```text
id
name
```

City names are unique within the scope of this application.

---

## Theater

Represents a movie theater.

Fields:

```text
id
name
address
landmark
postalCode
city
```

Relationship:

```text
City 1 ---- N Theater
```

A theater belongs to exactly one city.

---

## Screen

Represents an individual screen inside a theater.

Fields:

```text
id
name
theater
```

Relationship:

```text
Theater 1 ---- N Screen
```

Screen names are unique within a theater.

For example:

```text
PVR Koramangala
├── Screen 1
├── Screen 2
└── Screen 3
```

---

## Seat

Represents a physical seat inside a screen.

Fields:

```text
id
rowLabel
seatNumber
seatType
screen
```

Seat types:

```text
REGULAR
PREMIUM
```

Example:

```text
rowLabel = A
seatNumber = 5

Seat = A5
```

A combination of:

```text
screen + rowLabel + seatNumber
```

must be unique.

The `Seat` entity does not contain booking availability because seat availability varies between shows.

---

## Movie

Represents a movie.

Fields:

```text
id
title
description
durationMinutes
language
genre
```

Movie titles are not unique because different movies can share the same title.

---

## Show

Represents a particular movie playing on a particular screen at a particular time.

Fields:

```text
id
movie
screen
startTime
endTime
status
```

Show statuses:

```text
SCHEDULED
CANCELLED
COMPLETED
```

Relationships:

```text
Movie  1 ---- N Show

Screen 1 ---- N Show
```

Service-level validation will later prevent overlapping shows from being scheduled on the same screen.

---

# ShowSeat

`ShowSeat` represents a physical seat for one particular show.

This entity exists because availability and price belong to the combination:

```text
Show + Seat
```

and not to the physical seat itself.

Fields:

```text
id
show
seat
price
status
```

Statuses:

```text
AVAILABLE
HELD
BOOKED
```

Example:

```text
Show:
Interstellar
7:00 PM

Seat:
A5

ShowSeat:
A5
price = 350
status = AVAILABLE
```

The combination:

```text
show_id + seat_id
```

is unique.

---

# Seat Hold

A temporary seat hold prevents another customer from acquiring selected seats while the first customer completes payment.

A seat hold will remain active for:

```text
5 minutes
```

The duration will eventually be configurable rather than hardcoded.

Fields:

```text
id
user
show
status
expiresAt
createdAt
items
```

Hold statuses:

```text
ACTIVE
CONFIRMED
EXPIRED
CANCELLED
```

Lifecycle:

```text
ACTIVE
 ├── CONFIRMED
 ├── EXPIRED
 └── CANCELLED
```

An expired hold is invalid even if the cleanup scheduler has not yet executed.

For example:

```text
hold expires at 10:05

current time = 10:05:30

Result:
hold is already considered expired
```

The scheduler will only perform cleanup and release stale records.

---

# SeatHoldItem

A hold may contain multiple seats.

Example:

```text
SeatHold H101
├── A1
├── A2
└── A3
```

`SeatHoldItem` represents this relationship.

Fields:

```text
id
seatHold
showSeat
price
```

The price is stored as a snapshot so a price change after the hold is created does not affect the amount already shown to the customer.

Relationship:

```text
SeatHold 1 ---- N SeatHoldItem
```

---

# Booking

Represents a customer's booking.

Fields:

```text
id
bookingReference
user
show
seatHold
subtotal
discountAmount
appliedDiscountCode
totalAmount
status
items
createdAt
confirmedAt
cancelledAt
```

Booking statuses:

```text
PENDING_PAYMENT
CONFIRMED
CANCELLED
EXPIRED
```

Lifecycle:

```text
PENDING_PAYMENT
       |
       +---- payment success ----> CONFIRMED
       |
       +---- hold expiry --------> EXPIRED

CONFIRMED
       |
       +---- cancellation -------> CANCELLED
```

A failed payment attempt does not immediately terminate the booking.

Instead:

```text
Payment = FAILED
Booking = PENDING_PAYMENT
```

The customer may retry payment while the seat hold is still active.

---

# BookingItem

Represents one seat contained in a booking.

Fields:

```text
id
booking
showSeat
seatLabel
seatType
price
```

Relationship:

```text
Booking 1 ---- N BookingItem
```

The following values are stored as snapshots:

```text
seatLabel
seatType
price
```

This ensures historical booking information remains unchanged if seat configuration or pricing changes later.

---

# DiscountCode

Represents a discount that can be applied while creating a booking.

Fields:

```text
id
code
discountType
discountValue
minimumOrderAmount
maximumDiscountAmount
validFrom
validUntil
active
```

Discount types:

```text
FLAT
PERCENTAGE
```

Example:

```text
Code:
MOVIE20

Type:
PERCENTAGE

Value:
20%

Minimum order:
500

Maximum discount:
200
```

Another example:

```text
FLAT100

Discount:
₹100
```

A booking stores the applied discount code as a snapshot instead of depending permanently on the mutable discount configuration.

---

# Payment

Represents a payment attempt against a booking.

Fields:

```text
id
booking
idempotencyKey
amount
status
providerReference
failureReason
createdAt
updatedAt
```

Payment statuses:

```text
PENDING
SUCCESS
FAILED
REFUNDED
```

A booking can have multiple payment attempts.

Example:

```text
Booking 101
├── Payment 1 -> FAILED
└── Payment 2 -> SUCCESS
```

## Idempotency

Payment requests use an idempotency key.

Example:

```text
Idempotency-Key: abc-123
```

If the client retries the same payment request due to a network failure, the existing payment result will be returned rather than creating another payment.

The database therefore enforces:

```text
UNIQUE(idempotency_key)
```

---

# RefundPolicy

Refund policies are configurable and stored in the database.

Fields:

```text
id
hoursBeforeShow
refundPercentage
active
```

Example configuration:

```text
More than 24 hours before show
100% refund

More than 6 hours before show
75% refund

More than 2 hours before show
50% refund

Less than 2 hours before show
0% refund
```

The refund service will select the appropriate active policy based on the amount of time remaining before the show.

---

# Refund

Represents a refund generated when an eligible confirmed booking is cancelled.

Fields:

```text
id
booking
payment
amount
refundPercentage
status
providerReference
createdAt
processedAt
```

Statuses:

```text
PENDING
SUCCESS
FAILED
```

For the current assignment scope, one cancelled booking produces at most one refund.

---

# Notification

Notifications are persisted so delivery state can be tracked.

Fields:

```text
id
user
booking
notificationType
channel
status
message
failureReason
scheduledAt
createdAt
sentAt
```

Notification channels:

```text
EMAIL
SMS
PUSH
```

Notification types:

```text
BOOKING_CONFIRMED
BOOKING_REMINDER
BOOKING_CANCELLED
REFUND_PROCESSED
SHOW_CANCELLED
```

Notification statuses:

```text
PENDING
SENT
FAILED
```

Notifications will eventually be processed asynchronously.

Planned flow:

```text
BookingService
      |
      | publishes event
      ↓
BookingConfirmedEvent
      |
      ↓
Notification Listener
      |
      | async
      ↓
NotificationService
      |
      ↓
NotificationSenderResolver
      |
      ├── EMAIL
      ├── SMS
      └── PUSH
```

This ensures notification delivery does not block booking confirmation.

---

# Entity Relationships

Current high-level relationship model:

```text
City
 │
 └── Theater
      │
      └── Screen
           │
           ├── Seat
           │
           └── Show
                │
                └── ShowSeat
                     │
                     ├── SeatHoldItem
                     │
                     └── BookingItem


Movie
 │
 └── Show


User
 │
 ├── SeatHold
 │    │
 │    └── SeatHoldItem
 │
 └── Booking
      │
      ├── BookingItem
      ├── Payment
      ├── Refund
      └── Notification


DiscountCode

RefundPolicy
```

---

# Seat Lifecycle

The key seat state machine is:

```text
AVAILABLE
    |
    | customer holds
    ↓
HELD
    |
    | payment succeeds
    ↓
BOOKED
```

If the hold expires:

```text
HELD -> AVAILABLE
```

If an eligible confirmed booking is cancelled:

```text
BOOKED -> AVAILABLE
```

---

# Seat Hold Consistency

`ShowSeat` and `SeatHold` intentionally represent two different pieces of information.

`ShowSeat` answers:

```text
What is the current availability state of this seat?
```

`SeatHold` answers:

```text
Who owns the temporary reservation?
When does it expire?
Which seats belong to it?
What happened to the hold?
```

They must therefore be updated consistently.

Hold creation:

```text
ShowSeat:
AVAILABLE -> HELD

SeatHold:
created as ACTIVE
```

Hold expiry:

```text
ShowSeat:
HELD -> AVAILABLE

SeatHold:
ACTIVE -> EXPIRED
```

Payment success:

```text
ShowSeat:
HELD -> BOOKED

SeatHold:
ACTIVE -> CONFIRMED

Booking:
PENDING_PAYMENT -> CONFIRMED
```

These changes will later be executed inside transactional service methods.

---

# Concurrency Strategy

The most important concurrency problem is:

```text
User A -> Seat A1
User B -> Seat A1
```

Both may attempt to hold the seat simultaneously.

The planned solution is database-level pessimistic locking.

The repository will later fetch selected `ShowSeat` records using:

```text
PESSIMISTIC_WRITE
```

Conceptual flow:

```text
User A                   User B

lock A1
                         attempts lock
                         waits

A1 = AVAILABLE

A1 -> HELD

commit
                         obtains lock

                         sees A1 = HELD

                         request rejected
```

Exactly one user can therefore successfully acquire the seat.

This is one of the primary invariants of the system.

---

# Pricing Design

Pricing is stored at the `ShowSeat` level.

This allows:

```text
same physical seat
+
different show
=
different price
```

Seat type provides the base category:

```text
REGULAR
PREMIUM
```

Additional pricing rules such as weekend pricing will later be implemented through a pricing strategy.

Planned architecture:

```text
PricingService
      |
      ↓
PricingStrategyResolver
      |
      ↓
PricingStrategy
```

The final calculated price is persisted into `ShowSeat`.

---

# Design Patterns

The project is intentionally using design patterns only where they solve an actual problem.

## Layered Architecture

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

---

## Repository Pattern

Spring Data JPA repositories will abstract persistence operations.

Custom repository methods will also implement database locking where required.

---

## Strategy Pattern

Planned for:

```text
Pricing calculation
Refund calculation
Notification delivery
```

This avoids spreading complex conditional logic across services.

---

## Resolver Pattern

Strategies will be selected through resolvers.

Example:

```text
NotificationSenderResolver
        |
        ├── EMAIL -> EmailNotificationSender
        ├── SMS   -> SmsNotificationSender
        └── PUSH  -> PushNotificationSender
```

---

## Observer / Event Pattern

Booking-related events will trigger notification processing asynchronously.

Example:

```text
Booking confirmed
      ↓
BookingConfirmedEvent
      ↓
Notification listener
      ↓
Notification service
```

---

## Idempotency Pattern

Payment requests will use a unique idempotency key to prevent duplicate payment processing.

---

## Mapper Pattern

JPA entities will not be exposed directly through controllers.

The API layer will use:

```text
Request DTO
Response DTO
Mapper
```

---

## Explicit State Transition Model

The system models the lifecycle of:

```text
ShowSeat
SeatHold
Booking
Payment
Refund
Notification
```

using enums and controlled service-level transitions.

Invalid transitions will be rejected by domain/service validation.

---

# Assumptions

Current assumptions:

1. Seat holds remain valid for 5 minutes.
2. A customer may hold multiple seats in one hold.
3. A seat may belong to only one active valid hold at a time.
4. A hold produces at most one booking.
5. Failed payment attempts may be retried while the hold is still active.
6. Payment requests are idempotent.
7. A single discount code may be applied to a booking.
8. Pricing is snapshotted when the hold/booking is created.
9. Booking history must remain unchanged even if configuration changes later.
10. Refund policies are database-configurable.
11. Notifications are processed asynchronously.
12. Basic authentication and RBAC are sufficient for the take-home assignment.
13. External payment and notification providers may be simulated.
14. No frontend is required.
15. The application is implemented as a single Spring Boot service.
