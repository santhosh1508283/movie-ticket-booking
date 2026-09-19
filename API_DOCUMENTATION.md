# API Documentation

## Base URL

```text
http://localhost:8080
```

Authentication uses JWT bearer tokens.

```http
Authorization: Bearer <access-token>
```

Admin APIs are protected by `ROLE_ADMIN`.

---

## 1. Authentication

### Register Customer

**POST** `/api/auth/register`

Request:

```json
{
  "name": "Test Customer",
  "email": "customer@test.com",
  "password": "Customer@123"
}
```

Response:

```json
{
  "userId": 1,
  "name": "Test Customer",
  "email": "customer@test.com",
  "role": "CUSTOMER",
  "accessToken": "<jwt>"
}
```

### Login

**POST** `/api/auth/login`

Request:

```json
{
  "email": "customer@test.com",
  "password": "Customer@123"
}
```

---

## 2. Admin User Management

### Promote User to Admin

**PATCH** `/api/admin/users/make-admin`

Admin JWT required.

Request:

```json
{
  "email": "anotheruser@test.com"
}
```

The first admin can be bootstrapped manually in the database by updating one registered user's role to `ADMIN`.

---

## 3. Cities

### Create City

**POST** `/api/admin/cities`

```json
{
  "name": "Bangalore"
}
```

### Get City

**GET** `/api/admin/cities/{cityId}`

### Get All Cities

**GET** `/api/admin/cities`

---

## 4. Movies

### Create Movie

**POST** `/api/admin/movies`

```json
{
  "title": "Interstellar",
  "description": "Sci-fi drama",
  "durationMinutes": 169,
  "language": "English",
  "genre": "Sci-Fi"
}
```

### Get Movie

**GET** `/api/admin/movies/{movieId}`

### Get All Movies

**GET** `/api/admin/movies`

### Search Movies by Title

**GET** `/api/admin/movies/search?title=Inter`

### Filter by Language

**GET** `/api/admin/movies/language?language=English`

### Filter by Genre

**GET** `/api/admin/movies/genre?genre=Sci-Fi`

---

## 5. Theaters

### Create Theater

**POST** `/api/admin/theaters`

```json
{
  "name": "DMG Cinemas",
  "address": "MG Road",
  "landmark": "Near Metro",
  "postalCode": "560001",
  "cityId": 1
}
```

### Get Theater

**GET** `/api/admin/theaters/{theaterId}`

### Get All Theaters

**GET** `/api/admin/theaters`

### Get Theaters by City

**GET** `/api/admin/theaters/city/{cityId}`

### Search Theaters

**GET** `/api/admin/theaters/search?name=DMG`

---

## 6. Screens

### Create Screen

**POST** `/api/admin/screens`

```json
{
  "name": "Screen 1",
  "theaterId": 1
}
```

### Get Screen

**GET** `/api/admin/screens/{screenId}`

### Get All Screens

**GET** `/api/admin/screens`

### Get Screens by Theater

**GET** `/api/admin/screens/theater/{theaterId}`

---

## 7. Seats

### Create Seat Layout

**POST** `/api/admin/seats/layout`

```json
{
  "screenId": 1,
  "seats": [
    {
      "rowLabel": "A",
      "seatNumber": 1,
      "seatType": "REGULAR"
    },
    {
      "rowLabel": "B",
      "seatNumber": 1,
      "seatType": "PREMIUM"
    }
  ]
}
```

### Get Seat

**GET** `/api/admin/seats/{seatId}`

### Get Seats by Screen

**GET** `/api/admin/seats/screen/{screenId}`

---

## 8. Shows

### Create Show

**POST** `/api/admin/shows`

```json
{
  "movieId": 1,
  "screenId": 1,
  "startTime": "2026-09-21T19:00:00",
  "regularBasePrice": 250,
  "premiumBasePrice": 400
}
```

The service calculates `endTime` from movie duration, rejects overlapping scheduled shows, and creates `ShowSeat` records for active physical seats.

### Get Show

**GET** `/api/admin/shows/{showId}`

### Get Shows by Movie

**GET** `/api/admin/shows/movie/{movieId}`

### Get Shows by Screen

**GET** `/api/admin/shows/screen/{screenId}`

### Get Shows by Movie and Date Range

**GET** `/api/admin/shows/movie/{movieId}/range?start=2026-09-20T00:00:00&end=2026-09-30T23:59:59`

---

## 9. Discount Codes

### Create Discount Code

**POST** `/api/admin/discount-codes`

```json
{
  "code": "WELCOME10",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "minimumOrderAmount": 200,
  "maximumDiscountAmount": 150,
  "validFrom": "2026-09-19T00:00:00",
  "validUntil": "2026-12-31T23:59:59"
}
```

### Get All Discount Codes

**GET** `/api/admin/discount-codes`

### Activate / Deactivate Discount

**PATCH** `/api/admin/discount-codes/{discountCodeId}/active?active=true`

---

## 10. Refund Policies

### Create Refund Policy

**POST** `/api/admin/refund-policies`

```json
{
  "hoursBeforeShow": 24,
  "refundPercentage": 100
}
```

Example configuration:

```text
24 hours -> 100%
6 hours  -> 75%
2 hours  -> 50%
```

### Get All Refund Policies

**GET** `/api/admin/refund-policies`

### Activate / Deactivate Refund Policy

**PATCH** `/api/admin/refund-policies/{policyId}/active?active=true`

---

## 11. Customer Catalog

All catalog APIs require an authenticated user with the current security configuration.

### Get Movies

**GET** `/api/catalog/movies`

### Search Movies

**GET** `/api/catalog/movies/search?title=Inter`

### Get Movie

**GET** `/api/catalog/movies/{movieId}`

### Get Theaters by City

**GET** `/api/catalog/cities/{cityId}/theaters`

### Get Shows by Movie

**GET** `/api/catalog/movies/{movieId}/shows`

### Get Shows by Movie and Range

**GET** `/api/catalog/movies/{movieId}/shows/range?start=...&end=...`

### Get Show

**GET** `/api/catalog/shows/{showId}`

### Get Show Seats

**GET** `/api/catalog/shows/{showId}/seats`

---

## 12. Seat Holds

### Create Seat Hold

**POST** `/api/seat-holds`

```json
{
  "showId": 1,
  "showSeatIds": [1, 2]
}
```

Behavior:

- selected `ShowSeat` rows are pessimistically locked
- only `AVAILABLE` seats can be held
- hold expiry defaults to 5 minutes
- stale expired holds are handled synchronously
- background scheduler also expires old holds

### Get Hold

**GET** `/api/seat-holds/{holdId}`

### Release Hold

**DELETE** `/api/seat-holds/{holdId}`

---

## 13. Bookings

### Create Booking

**POST** `/api/bookings`

```json
{
  "holdId": 1,
  "discountCode": "WELCOME10"
}
```

Without discount:

```json
{
  "holdId": 1,
  "discountCode": null
}
```

A newly created booking starts in:

```text
PENDING_PAYMENT
```

### Get Booking

**GET** `/api/bookings/{bookingId}`

### Get My Bookings

**GET** `/api/bookings`

### Cancel Booking

**POST** `/api/bookings/{bookingId}/cancel`

Cancellation evaluates the configured refund policy, releases seats, updates booking status, and processes the refund.

---

## 14. Payments

### Process Payment

**POST** `/api/payments/bookings/{bookingId}`

Headers:

```http
Authorization: Bearer <jwt>
Idempotency-Key: payment-booking-1-attempt-1
```

Request:

```json
{
  "paymentMethod": "UPI"
}
```

Supported methods:

```text
CARD
UPI
NET_BANKING
```

Successful payment transitions:

```text
Payment -> SUCCESS
Booking -> CONFIRMED
SeatHold -> CONFIRMED
ShowSeat -> BOOKED
```

Retrying the same request with the same idempotency key returns the existing payment result.

---

## Common Error Response

```json
{
  "timestamp": "2026-09-19T13:25:10",
  "status": 409,
  "error": "Conflict",
  "code": "SEAT_NOT_AVAILABLE",
  "message": "Seat is currently held: A5",
  "path": "/api/seat-holds",
  "validationErrors": null
}
```

Common error codes:

```text
RESOURCE_NOT_FOUND
DUPLICATE_RESOURCE
INVALID_REQUEST
INVALID_STATE
SEAT_NOT_AVAILABLE
HOLD_EXPIRED
PAYMENT_FAILED
CANCELLATION_NOT_ALLOWED
UNAUTHORIZED
```
