package com.dmg.movieticket.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings/8");
    @ParameterizedTest @CsvSource({"RESOURCE_NOT_FOUND,404", "DUPLICATE_RESOURCE,409", "UNAUTHORIZED,401",
            "SEAT_NOT_AVAILABLE,409", "HOLD_EXPIRED,410", "PAYMENT_FAILED,400", "CANCELLATION_NOT_ALLOWED,400",
            "INVALID_STATE,409", "INVALID_REQUEST,400"})
    void mapsEveryDomainErrorToHttpStatus(ErrorCode code, int status) {
        var response = handler.handleApplicationException(new ApplicationException(code, "Message"), request);
        assertEquals(status, response.getStatusCode().value());
        assertEquals(code.name(), response.getBody().code()); assertEquals("Message", response.getBody().message());
        assertEquals("/api/bookings/8", response.getBody().path()); assertNotNull(response.getBody().timestamp());
    }
    @Test void unexpectedErrorsDoNotExposeInternalDetails() {
        var response = handler.handleUnexpectedException(new IllegalStateException("private database details"), request);
        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }
}
