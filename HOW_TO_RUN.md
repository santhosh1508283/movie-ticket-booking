# How to Run the Project

## Prerequisites

Install:

- Java 17
- Maven
- MySQL
- IntelliJ IDEA or another Java IDE
- Postman for API testing

The application runs on:

```text
http://localhost:8080
```

---

## 1. Create the MySQL Database

Open MySQL Workbench or the MySQL CLI and run:

```sql
CREATE DATABASE movie_ticket_booking;
```

The application uses Hibernate schema update during development, so tables are created/updated automatically.

---

## 2. Configure Environment Variables

The application expects:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Example IntelliJ environment configuration:

```text
DB_USERNAME=root
DB_PASSWORD=<your-mysql-password>
JWT_SECRET=<base64-jwt-secret>
```

Do not commit real database passwords or JWT secrets to Git.

---

## 3. Application Properties

Expected configuration:

```properties
spring.application.name=movie-ticket-booking

spring.datasource.url=jdbc:mysql://localhost:3306/movie_ticket_booking
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

security.jwt.secret=${JWT_SECRET}
security.jwt.expiration-ms=86400000

booking.seat-hold-duration-minutes=5
booking.seat-hold-cleanup-interval-ms=30000

notification.processing-interval-ms=60000
```

---

## 4. Generate a JWT Secret

`JWT_SECRET` should be a sufficiently long Base64-encoded key.

For example, generate one locally using OpenSSL:

```bash
openssl rand -base64 32
```

Copy the generated value into your IDE environment variable configuration.

---

## 5. Run From IntelliJ

1. Open the project.
2. Reload Maven.
3. Verify project SDK is Java 17.
4. Open the main application class:

```text
MovieTicketBookingApplication
```

5. Configure environment variables.
6. Run the application.

Successful startup should contain a message similar to:

```text
Tomcat started on port 8080
```

---

## 6. Run From Terminal

From the project root:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

Or, if Maven is installed globally:

```bash
mvn spring-boot:run
```

---

## 7. Bootstrap the First Admin

Public registration always creates a `CUSTOMER`.

First register your user:

```http
POST /api/auth/register
```

Then manually promote the first admin in MySQL:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-email@example.com';
```

Login again after the database update to obtain a fresh token.

After the first admin exists, use:

```http
PATCH /api/admin/users/make-admin
```

to promote other registered users.

---

## 8. Import the Postman Collection

Import:

```text
DMG_Movie_Ticket_Booking.postman_collection.json
DMG_Movie_Ticket_Booking_Local.postman_environment.json
```

Select:

```text
DMG Movie Ticket Booking - Local
```

The environment contains variables such as:

```text
baseUrl
adminToken
customerToken
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

Many values are populated automatically by Postman test scripts.

---

## 9. Recommended API Test Order

### Admin setup

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

### Customer flow

```text
Register / Login Customer
   ↓
Browse Movies
   ↓
Browse Shows
   ↓
Get Show Seats
   ↓
Create Seat Hold
   ↓
Create Booking
   ↓
Process Payment
   ↓
Get Booking
```

### Cancellation flow

```text
Confirmed Booking
   ↓
Cancel Booking
   ↓
Refund Policy Evaluated
   ↓
Refund Processed
   ↓
Seats Available Again
```

---

## 10. Testing Seat Hold Expiry

The default hold duration is:

```text
5 minutes
```

Create a hold and do not pay.

After expiry:

```text
SeatHold ACTIVE -> EXPIRED
ShowSeat HELD -> AVAILABLE
Booking PENDING_PAYMENT -> EXPIRED
```

The scheduler runs every:

```text
30 seconds
```

Services also validate `expiresAt` directly, so correctness does not depend exclusively on the scheduler.

---

## 11. Testing Concurrent Seat Booking

To demonstrate the concurrency requirement:

1. Create one available show seat.
2. Login as two different customers.
3. Send two hold requests for the same `showSeatId` at almost the same time.
4. One request should acquire the pessimistic lock and succeed.
5. The second request should eventually see the seat as `HELD` and fail with:

```text
SEAT_NOT_AVAILABLE
```

This demonstrates that the application does not double-allocate the same seat.

---

## 12. Testing Payment Idempotency

Send:

```http
POST /api/payments/bookings/{bookingId}
Idempotency-Key: booking-123-payment-1
```

Then send the exact same request again using the same key.

The second call should return the existing payment result rather than processing another payment.

---

## 13. Notifications

Booking confirmation, cancellation, refund, and reminders use asynchronous notification handling.

The current implementation simulates email delivery through application logs.

After payment confirmation, look for a log similar to:

```text
Mock email sent to customer@test.com: Booking confirmed...
```

A reminder record is also persisted and processed by the notification scheduler.

---

## 14. Common Startup Problems

### MySQL connection refused

Check:

- MySQL is running
- database exists
- username/password are correct
- port 3306 is available

### JWT secret error

Verify `JWT_SECRET` is:

- present in the run configuration
- Base64 encoded
- sufficiently long

### 401 Unauthorized

Check:

```http
Authorization: Bearer <token>
```

and login again if necessary.

### 403 Forbidden

The user is authenticated but does not have the required role.

Admin APIs require:

```text
ROLE_ADMIN
```

### Port 8080 already in use

Stop the existing process or configure another server port.

---

## 15. Build the Project

Run:

```bash
./mvnw clean package
```

or:

```bash
mvn clean package
```

The generated JAR will be under:

```text
target/
```

Run it using:

```bash
java -jar target/<generated-jar-name>.jar
```

with the required environment variables available.

---