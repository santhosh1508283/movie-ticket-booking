# API Documentation

## Base URL

```text
http://localhost:8080
```

Authentication uses JWT bearer access tokens.

```http
Authorization: Bearer <access-token>
```

Admin APIs require `ROLE_ADMIN`.

---

## Authentication

### Register Customer

**POST** `/api/auth/register`

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
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-refresh-token>"
}
```

### Login

**POST** `/api/auth/login`

```json
{
  "email": "customer@test.com",
  "password": "Customer@123"
}
```

Returns both an access token and a refresh token.

### Refresh Access Token

**POST** `/api/auth/refresh`

```json
{
  "refreshToken": "<refresh-token>"
}
```

Returns a new access token while keeping the valid refresh token.

### Logout

**POST** `/api/auth/logout`

```json
{
  "refreshToken": "<refresh-token>"
}
```

Returns `204 No Content` and revokes the refresh token.

---

## Admin User Management

### Promote User to Admin

**PATCH** `/api/admin/users/make-admin`

```json
{
  "email": "anotheruser@test.com"
}
```

The first admin can be bootstrapped manually by updating one registered user's role to `ADMIN` in the database.

---

## Cities

- **POST** `/api/admin/cities`
- **GET** `/api/admin/cities/{cityId}`
- **GET** `/api/admin/cities`

Create request:

```json
{
  "name": "Bangalore"
}
```

---

## Movies

- **POST** `/api/admin/movies`
- **GET** `/api/admin/movies/{movieId}`
- **GET** `/api/admin/movies`
- **GET** `/api/admin/movies/search?title=Inter`
- **GET** `/api/admin/movies/language?language=English`
- **GET** `/api/admin/movies/genre?genre=Sci-Fi`

Create request:

```json
{
  "title": "Interstellar",
  "description": "Sci-fi drama",
  "durationMinutes": 169,
  "language": "English",
  "genre": "Sci-Fi"
}
```

---

## Theaters

- **POST** `/api/admin/theaters`
- **GET** `/api/admin/theaters/{theaterId}`
- **GET** `/api/admin/theaters`
- **GET** `/api/admin/theaters/city/{cityId}`
- **GET** `/api/admin/theaters/search?name=Central`

Create request:

```json
{
  "name": "Central Cinemas",
  "address": "MG Road",
  "landmark": "Near Metro",
  "postalCode": "560001",
  "cityId": 1
}
```

---

## Screens

- **POST** `/api/admin/screens`
- **GET** `/api/admin/screens/{screenId}`
- **GET** `/api/admin/screens`
- **GET** `/api/admin/screens/theater/{theaterId}`

Create request:

```json
{
  "name": "Screen 1",
  "theaterId": 1
}
```

---

## Seats

- **POST** `/api/admin/seats/layout`
- **GET** `/api/admin/seats/{seatId}`
- **GET** `/api/admin/seats/screen/{screenId}`

Create layout request:

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

---

## Shows

- **POST** `/api/admin/shows`
- **GET** `/api/admin/shows/{showId}`
- **GET** `/api/admin/shows/movie/{movieId}`
- **GET** `/api/admin/shows/screen/{screenId}`
- **GET** `/api/admin/shows/movie/{movieId}/range?start=...&end=...`

Create request:

```json
{
  "movieId": 1,
  "screenId": 1,
  "startTime": "2026-09-21T19:00:00",
  "regularBasePrice": 250,
  "premiumBasePrice": 400
}
```

Show creation calculates `endTime`, rejects overlapping scheduled shows, and generates `ShowSeat` records for active physical seats.

---

## Discount Codes

- **POST** `/api/admin/discount-codes`
- **GET** `/api/admin/discount-codes`
- **PATCH** `/api/admin/discount-codes/{discountCodeId}/active?active=true`

Example:

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

---

## Refund Policies

- **POST** `/api/admin/refund-policies`
- **GET** `/api/admin/refund-policies`
- **PATCH** `/api/admin/refund-policies/{policyId}/active?active=true`

Example:

```json
{
  "hoursBeforeShow": 24,
  "refundPercentage": 100
}
```

---

## Customer Catalog

- **GET** `/api/catalog/movies`
- **GET** `/api/catalog/movies/search?title=Inter`
- **GET** `/api/catalog/movies/{movieId}`
- **GET** `/api/catalog/cities/{cityId}/theaters`
- **GET** `/api/catalog/movies/{movieId}/shows`
- **GET** `/api/catalog/movies/{movieId}/shows/range?start=...&end=...`
- **GET** `/api/catalog/shows/{showId}`
- **GET** `/api/catalog/shows/{showId}/seats`

---

## Seat Holds

### Create Hold

**POST** `/api/seat-holds`

```json
{
  "showId": 1,
  "showSeatIds": [1, 2]
}
```

Behavior:

- locks selected `ShowSeat` rows using pessimistic locking
- prevents double allocation
- defaults to a 5-minute hold
- expires stale holds synchronously and via scheduler

### Get Hold

**GET** `/api/seat-holds/{holdId}`

### Release Hold

**DELETE** `/api/seat-holds/{holdId}`

---

## Bookings

### Create Booking

**POST** `/api/bookings`

```json
{
  "holdId": 1,
  "discountCode": "WELCOME10"
}
```

New bookings start as `PENDING_PAYMENT`.

### Get Booking

**GET** `/api/bookings/{bookingId}`

### Get My Bookings

**GET** `/api/bookings`

### Cancel Booking

**POST** `/api/bookings/{bookingId}/cancel`

---

## Payments

### Process Payment

**POST** `/api/payments/bookings/{bookingId}`

Headers:

```http
Authorization: Bearer <access-token>
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

Repeating the same request with the same idempotency key returns the existing payment result.

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
