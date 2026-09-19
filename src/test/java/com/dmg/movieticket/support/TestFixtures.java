package com.dmg.movieticket.support;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.exception.*;
import org.junit.jupiter.api.function.Executable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public final class TestFixtures {
    private TestFixtures() {}
    public static BigDecimal money(String value) { return new BigDecimal(value); }
    public static User user() {
        return User.builder().id(1L).name("Customer").email("customer@example.test")
                .password("encoded").role(Role.CUSTOMER).build();
    }
    public static Screen screen() {
        City city = City.builder().id(2L).name("Bengaluru").build();
        Theater theater = Theater.builder().id(3L).name("PVR").address("Main Road").city(city).build();
        return Screen.builder().id(4L).name("Screen 1").theater(theater).build();
    }
    public static Show show() {
        return Show.builder().id(5L).screen(screen())
                .movie(Movie.builder().id(6L).title("Interstellar").durationMinutes(169).build())
                .startTime(LocalDateTime.now().plusDays(3)).endTime(LocalDateTime.now().plusDays(3).plusMinutes(169))
                .status(ShowStatus.SCHEDULED).build();
    }
    public static ShowSeat showSeat(long id, ShowSeatStatus status) {
        return ShowSeat.builder().id(id).show(show()).status(status).price(money("150.00"))
                .seat(Seat.builder().id(id + 100).rowLabel("A").seatNumber((int) id)
                        .seatType(SeatType.REGULAR).screen(screen()).build()).build();
    }
    public static SeatHold hold() {
        SeatHold hold = SeatHold.builder().id(7L).user(user()).show(show()).status(HoldStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(10)).build();
        hold.getItems().add(SeatHoldItem.builder().seatHold(hold)
                .showSeat(showSeat(10L, ShowSeatStatus.HELD)).price(money("150.00")).build());
        return hold;
    }
    public static Booking booking() {
        SeatHold hold = hold();
        Booking booking = Booking.builder().id(8L).bookingReference("DMG-TEST1234").user(hold.getUser())
                .show(hold.getShow()).seatHold(hold).subtotal(money("150.00")).discountAmount(BigDecimal.ZERO)
                .totalAmount(money("150.00")).status(BookingStatus.PENDING_PAYMENT).build();
        booking.getItems().add(BookingItem.builder().booking(booking).showSeat(hold.getItems().get(0).getShowSeat())
                .seatLabel("A10").seatType(SeatType.REGULAR).price(money("150.00")).build());
        return booking;
    }
    public static ApplicationException error(ErrorCode expected, Executable action) {
        ApplicationException exception = assertThrows(ApplicationException.class, action);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }
}
