# How to Run

## Prerequisites

- Java 17
- Maven
- MySQL
- IntelliJ IDEA or another Java IDE
- Postman

Default server:

```text
http://localhost:8080
```

---

## 1. Create Database

```sql
CREATE DATABASE movie_ticket_booking;
```

Hibernate is configured with `ddl-auto=update` for development.

---

## 2. Environment Variables

Configure:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Example:

```text
DB_USERNAME=root
DB_PASSWORD=<your-password>
JWT_SECRET=<base64-secret>
```

Do not commit secrets.

---

## 3. Application Properties

```properties
spring.application.name=movie-ticket-booking

spring.datasource.url=jdbc:mysql://localhost:3306/movie_ticket_booking
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

security.jwt.secret=${JWT_SECRET}
security.jwt.expiration-ms=900000
security.refresh-token.expiration-ms=604800000

booking.seat-hold-duration-minutes=5
booking.seat-hold-cleanup-interval-ms=30000

notification.processing-interval-ms=60000
```

---

## 4. Generate JWT Secret

```bash
openssl rand -base64 32
```

Store the generated value in `JWT_SECRET`.

---

## 5. Run from IntelliJ

1. Open the project.
2. Reload Maven.
3. Set Java 17.
4. Configure environment variables.
5. Run `MovieTicketBookingApplication`.

Successful startup should include:

```text
Tomcat started on port 8080
```

---

## 6. Run from Terminal

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Or with global Maven:

```bash
mvn spring-boot:run
```

---

## 7. Bootstrap First Admin

Register normally:

```http
POST /api/auth/register
```

Then update one user manually:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-email@example.com';
```

Login again after the update.

Afterward, admins can promote other users through:

```http
PATCH /api/admin/users/make-admin
```

---

## 8. Postman

Import:

```text
Movie_Ticket_Booking.postman_collection.json
Movie_Ticket_Booking_Local.postman_environment.json
```

Select:

```text
Movie Ticket Booking - Local
```

Important variables include:

```text
baseUrl
adminToken
adminRefreshToken
customerToken
customerRefreshToken
cityId
movieId
theaterId
screenId
showId
showSeatId1
showSeatId2
holdId
bookingId
discountCodeId
refundPolicyId
paymentIdempotencyKey
```

---

## 9. Recommended Test Order

Admin:

```text
Login Admin
  ↓
Create City
  ↓
Create Theater
  ↓
Create Screen
  ↓
Create Seat Layout
  ↓
Create Movie
  ↓
Create Show
  ↓
Create Discount Code
  ↓
Create Refund Policies
```

Customer:

```text
Register/Login
  ↓
Browse Catalog
  ↓
View Show Seats
  ↓
Create Hold
  ↓
Create Booking
  ↓
Process Payment
  ↓
Get Booking
```

Cancellation:

```text
Confirmed Booking
  ↓
Cancel
  ↓
Refund
  ↓
Seats Available Again
```

---

## 10. Refresh Token Test

Login and capture:

```text
accessToken
refreshToken
```

After the access token expires:

```http
POST /api/auth/refresh
```

```json
{
  "refreshToken": "<refresh-token>"
}
```

Use the returned access token for subsequent requests.

Logout:

```http
POST /api/auth/logout
```

```json
{
  "refreshToken": "<refresh-token>"
}
```

The token should no longer work for refresh.

---

## 11. Hold Expiry Test

Create a hold and do not pay.

Default duration:

```text
5 minutes
```

Expected transitions:

```text
SeatHold ACTIVE -> EXPIRED
ShowSeat HELD -> AVAILABLE
Booking PENDING_PAYMENT -> EXPIRED
```

Cleanup scheduler runs every 30 seconds.

---

## 12. Concurrent Booking Test

1. Create an available show seat.
2. Login as two customers.
3. Send two hold requests for the same `showSeatId` almost simultaneously.
4. One succeeds.
5. The other fails with `SEAT_NOT_AVAILABLE`.

This demonstrates no double allocation.

---

## 13. Payment Idempotency Test

Call:

```http
POST /api/payments/bookings/{bookingId}
Idempotency-Key: booking-123-payment-1
```

Repeat with the same key.

Expected: existing payment result is returned instead of creating another payment.

---

## 14. Notifications

Confirmation, cancellation, refund and reminder notifications are processed asynchronously.

The current email strategy writes mock delivery to application logs.

---

## 15. Build

```bash
./mvnw clean package
```

or:

```bash
mvn clean package
```

Run the resulting JAR:

```bash
java -jar target/<jar-name>.jar
```

with environment variables configured.

---