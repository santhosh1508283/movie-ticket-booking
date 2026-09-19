package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.*;
import com.dmg.movieticket.dto.response.*;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.exception.*;
import com.dmg.movieticket.security.*;
import com.dmg.movieticket.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.money;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(ControllerHttpTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {"security.jwt.expiration-ms=60000", "security.jwt.secret=test-only-unused-mock-key"})
class ControllerHttpTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    <T> T service(Class<T> type) { return context.getBean(type); }
    @BeforeEach void setUp() {
        for (Object bean : context.getBeansOfType(Object.class).values()) {
            if (mockingDetails(bean).isMock()) reset(bean);
        }
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
            AuthController.class, BookingController.class, CatalogController.class, CityController.class,
            DiscountCodeController.class, MovieController.class, PaymentController.class, RefundPolicyController.class,
            ScreenController.class, SeatController.class, SeatHoldController.class, ShowController.class,
            TheaterController.class, UserAdminController.class})
    static class Config {
        @Bean AuthService authService() { return mock(AuthService.class); }
        @Bean BookingService bookingService() { return mock(BookingService.class); }
        @Bean RefundService refundService() { return mock(RefundService.class); }
        @Bean MovieService movieService() { return mock(MovieService.class); }
        @Bean TheaterService theaterService() { return mock(TheaterService.class); }
        @Bean ShowService showService() { return mock(ShowService.class); }
        @Bean ShowSeatService showSeatService() { return mock(ShowSeatService.class); }
        @Bean CityService cityService() { return mock(CityService.class); }
        @Bean DiscountCodeService discountCodeService() { return mock(DiscountCodeService.class); }
        @Bean PaymentService paymentService() { return mock(PaymentService.class); }
        @Bean RefundPolicyService refundPolicyService() { return mock(RefundPolicyService.class); }
        @Bean ScreenService screenService() { return mock(ScreenService.class); }
        @Bean SeatService seatService() { return mock(SeatService.class); }
        @Bean SeatHoldService seatHoldService() { return mock(SeatHoldService.class); }
        @Bean UserAdminService userAdminService() { return mock(UserAdminService.class); }
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean CustomUserDetailsService userDetailsService() { return mock(CustomUserDetailsService.class); }
    }
    @Test void bindsCityRequest() throws Exception {
        mvc.perform(post("/api/admin/cities").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bengaluru\"}"))
                .andExpect(status().is(201));
        verify(service(CityService.class)).createCity(new CreateCityRequest("Bengaluru"));
    }
    @Test void bindsMovieRequest() throws Exception {
        mvc.perform(post("/api/admin/movies").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Interstellar\",\"durationMinutes\":169,\"language\":\"English\",\"genre\":\"SciFi\"}"))
                .andExpect(status().is(201));
        verify(service(MovieService.class)).createMovie(new CreateMovieRequest("Interstellar", null, 169, "English", "SciFi"));
    }
    @Test void bindsTheaterRequest() throws Exception {
        mvc.perform(post("/api/admin/theaters").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"PVR\",\"address\":\"Main Road\",\"cityId\":2}"))
                .andExpect(status().is(201));
        verify(service(TheaterService.class)).createTheater(new CreateTheaterRequest("PVR", "Main Road", null, null, 2L));
    }
    @Test void bindsScreenRequest() throws Exception {
        mvc.perform(post("/api/admin/screens").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Screen 1\",\"theaterId\":3}"))
                .andExpect(status().is(201));
        verify(service(ScreenService.class)).createScreen(new CreateScreenRequest("Screen 1", 3L));
    }
    @Test void bindsSeatLayoutRequest() throws Exception {
        mvc.perform(post("/api/admin/seats/layout").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"screenId\":4,\"seats\":[{\"rowLabel\":\"A\",\"seatNumber\":1,\"seatType\":\"REGULAR\"}]}"))
                .andExpect(status().is(201));
        verify(service(SeatService.class)).createSeatLayout(new CreateSeatLayoutRequest(4L, List.of(new CreateSeatRequest("A", 1, SeatType.REGULAR))));
    }
    @Test void bindsShowRequest() throws Exception {
        mvc.perform(post("/api/admin/shows").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":6,\"screenId\":4,\"startTime\":\"2030-01-05T18:00:00\",\"regularBasePrice\":100,\"premiumBasePrice\":200}"))
                .andExpect(status().is(201));
        verify(service(ShowService.class)).createShow(new CreateShowRequest(6L, 4L, LocalDateTime.of(2030,1,5,18,0), money("100"), money("200")));
    }
    @Test void bindsDiscountCodeRequest() throws Exception {
        mvc.perform(post("/api/admin/discount-codes").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"SAVE\",\"discountType\":\"FLAT\",\"discountValue\":20,\"validFrom\":\"2030-01-01T00:00:00\",\"validUntil\":\"2030-02-01T00:00:00\"}"))
                .andExpect(status().is(201));
        verify(service(DiscountCodeService.class)).createDiscountCode(new CreateDiscountCodeRequest("SAVE", DiscountType.FLAT, money("20"), null, null, LocalDateTime.of(2030,1,1,0,0), LocalDateTime.of(2030,2,1,0,0)));
    }
    @Test void bindsRefundPolicyRequest() throws Exception {
        mvc.perform(post("/api/admin/refund-policies").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"hoursBeforeShow\":24,\"refundPercentage\":75}"))
                .andExpect(status().is(201));
        verify(service(RefundPolicyService.class)).createPolicy(new CreateRefundPolicyRequest(24, money("75")));
    }
    @Test void bindsHoldRequest() throws Exception {
        mvc.perform(post("/api/seat-holds").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"showId\":5,\"showSeatIds\":[10,11]}"))
                .andExpect(status().is(201));
        verify(service(SeatHoldService.class)).createHold(new CreateSeatHoldRequest(5L, List.of(10L,11L)));
    }
    @Test void bindsBookingRequest() throws Exception {
        mvc.perform(post("/api/bookings").with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"holdId\":7,\"discountCode\":\"SAVE\"}"))
                .andExpect(status().is(201));
        verify(service(BookingService.class)).createBooking(new CreateBookingRequest(7L, "SAVE"));
    }
    @Test void bindsRegistrationRequest() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Customer\",\"email\":\"customer@example.test\",\"password\":\"password123\"}"))
                .andExpect(status().is(201));
        verify(service(AuthService.class)).register(new RegisterRequest("Customer", "customer@example.test", "password123"));
    }
    @Test void bindsLoginRequest() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"customer@example.test\",\"password\":\"password123\"}"))
                .andExpect(status().is(200));
        verify(service(AuthService.class)).login(new LoginRequest("customer@example.test", "password123"));
    }
    @Test void bindsRefreshRequest() throws Exception {
        mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().is(200));
        verify(service(AuthService.class)).refresh(new RefreshTokenRequest("refresh"));
    }
    @Test void bindsLogoutRequest() throws Exception {
        mvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().is(204));
        verify(service(AuthService.class)).logout(new RefreshTokenRequest("refresh"));
    }
    @Test void routesAllCitiesRead() throws Exception {
        mvc.perform(get("/api/admin/cities").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(CityService.class)).getAllCities();
    }
    @Test void routesCityRead() throws Exception {
        mvc.perform(get("/api/admin/cities/2").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(CityService.class)).getCityById(2L);
    }
    @Test void routesAllMoviesRead() throws Exception {
        mvc.perform(get("/api/admin/movies").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).getAllMovies();
    }
    @Test void routesMovieRead() throws Exception {
        mvc.perform(get("/api/admin/movies/6").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).getMovieById(6L);
    }
    @Test void routesMovieSearchRead() throws Exception {
        mvc.perform(get("/api/admin/movies/search?title=Inter").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).searchMovies("Inter");
    }
    @Test void routesMovieLanguageRead() throws Exception {
        mvc.perform(get("/api/admin/movies/language?language=English").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).getMoviesByLanguage("English");
    }
    @Test void routesMovieGenreRead() throws Exception {
        mvc.perform(get("/api/admin/movies/genre?genre=SciFi").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).getMoviesByGenre("SciFi");
    }
    @Test void routesAllTheatersRead() throws Exception {
        mvc.perform(get("/api/admin/theaters").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(TheaterService.class)).getAllTheaters();
    }
    @Test void routesTheaterRead() throws Exception {
        mvc.perform(get("/api/admin/theaters/3").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(TheaterService.class)).getTheaterById(3L);
    }
    @Test void routesTheatersByCityRead() throws Exception {
        mvc.perform(get("/api/admin/theaters/city/2").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(TheaterService.class)).getTheatersByCity(2L);
    }
    @Test void routesTheaterSearchRead() throws Exception {
        mvc.perform(get("/api/admin/theaters/search?name=PVR").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(TheaterService.class)).searchTheaters("PVR");
    }
    @Test void routesAllScreensRead() throws Exception {
        mvc.perform(get("/api/admin/screens").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(ScreenService.class)).getAllScreens();
    }
    @Test void routesScreenRead() throws Exception {
        mvc.perform(get("/api/admin/screens/4").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(ScreenService.class)).getScreenById(4L);
    }
    @Test void routesScreensByTheaterRead() throws Exception {
        mvc.perform(get("/api/admin/screens/theater/3").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(ScreenService.class)).getScreensByTheater(3L);
    }
    @Test void routesSeatRead() throws Exception {
        mvc.perform(get("/api/admin/seats/10").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(SeatService.class)).getSeatById(10L);
    }
    @Test void routesSeatsByScreenRead() throws Exception {
        mvc.perform(get("/api/admin/seats/screen/4").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(SeatService.class)).getSeatsByScreen(4L);
    }
    @Test void routesShowRead() throws Exception {
        mvc.perform(get("/api/admin/shows/5").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(ShowService.class)).getShowById(5L);
    }
    @Test void routesShowsByMovieRead() throws Exception {
        mvc.perform(get("/api/admin/shows/movie/6").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(ShowService.class)).getShowsByMovie(6L);
    }
    @Test void routesShowsByScreenRead() throws Exception {
        mvc.perform(get("/api/admin/shows/screen/4").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(ShowService.class)).getShowsByScreen(4L);
    }
    @Test void routesShowsByRangeRead() throws Exception {
        mvc.perform(get("/api/admin/shows/movie/6/range?start=2030-01-01T00:00:00&end=2030-02-01T00:00:00").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(ShowService.class)).getShowsByMovieAndDateRange(6L, LocalDateTime.of(2030,1,1,0,0), LocalDateTime.of(2030,2,1,0,0));
    }
    @Test void routesDiscountCodesRead() throws Exception {
        mvc.perform(get("/api/admin/discount-codes").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(DiscountCodeService.class)).getAllDiscountCodes();
    }
    @Test void routesRefundPoliciesRead() throws Exception {
        mvc.perform(get("/api/admin/refund-policies").with(user("test").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service(RefundPolicyService.class)).getAllPolicies();
    }
    @Test void routesOwnedHoldRead() throws Exception {
        mvc.perform(get("/api/seat-holds/7").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(SeatHoldService.class)).getHold(7L);
    }
    @Test void routesOwnedBookingRead() throws Exception {
        mvc.perform(get("/api/bookings/8").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(BookingService.class)).getBooking(8L);
    }
    @Test void routesBookingHistoryRead() throws Exception {
        mvc.perform(get("/api/bookings").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(BookingService.class)).getMyBookings();
    }
    @Test void routesCatalogMoviesRead() throws Exception {
        mvc.perform(get("/api/catalog/movies").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).getAllMovies();
    }
    @Test void routesCatalogMovieRead() throws Exception {
        mvc.perform(get("/api/catalog/movies/6").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).getMovieById(6L);
    }
    @Test void routesCatalogSearchRead() throws Exception {
        mvc.perform(get("/api/catalog/movies/search?title=Inter").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(MovieService.class)).searchMovies("Inter");
    }
    @Test void routesCatalogTheatersRead() throws Exception {
        mvc.perform(get("/api/catalog/cities/2/theaters").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(TheaterService.class)).getTheatersByCity(2L);
    }
    @Test void routesCatalogShowsRead() throws Exception {
        mvc.perform(get("/api/catalog/movies/6/shows").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(ShowService.class)).getShowsByMovie(6L);
    }
    @Test void routesCatalogShowRead() throws Exception {
        mvc.perform(get("/api/catalog/shows/5").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(ShowService.class)).getShowById(5L);
    }
    @Test void routesCatalogSeatsRead() throws Exception {
        mvc.perform(get("/api/catalog/shows/5/seats").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(ShowSeatService.class)).getSeatsByShow(5L);
    }
    @Test void routesCatalogRangeRead() throws Exception {
        mvc.perform(get("/api/catalog/movies/6/shows/range?start=2030-01-01T00:00:00&end=2030-02-01T00:00:00").with(user("test").roles("CUSTOMER")))
                .andExpect(status().isOk());
        verify(service(ShowService.class)).getShowsByMovieAndDateRange(6L, LocalDateTime.of(2030,1,1,0,0), LocalDateTime.of(2030,2,1,0,0));
    }
    @Test void bindsPaymentHeaderAndBody() throws Exception {
        mvc.perform(post("/api/payments/bookings/8").with(user("test")).header("Idempotency-Key", "key")
                .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"CARD\"}")).andExpect(status().isOk());
        verify(service(PaymentService.class)).processPayment(8L, "key", new CreatePaymentRequest(PaymentMethod.CARD));
    }
    @Test void releasesHoldWithNoContent() throws Exception {
        mvc.perform(delete("/api/seat-holds/7").with(user("test"))).andExpect(status().isNoContent());
        verify(service(SeatHoldService.class)).releaseHold(7L);
    }
    @Test void cancelsBooking() throws Exception {
        mvc.perform(post("/api/bookings/8/cancel").with(user("test"))).andExpect(status().isOk());
        verify(service(RefundService.class)).cancelBooking(8L);
    }
    @Test void changesDiscountActivation() throws Exception {
        mvc.perform(patch("/api/admin/discount-codes/1/active?active=false").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        verify(service(DiscountCodeService.class)).setActive(1L, false);
    }
    @Test void changesRefundPolicyActivation() throws Exception {
        mvc.perform(patch("/api/admin/refund-policies/1/active?active=true").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        verify(service(RefundPolicyService.class)).setActive(1L, true);
    }
    @Test void promotesUserWithAdminRole() throws Exception {
        mvc.perform(patch("/api/admin/users/make-admin").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"customer@example.test\"}")).andExpect(status().isOk());
        verify(service(UserAdminService.class)).makeAdmin(new UpdateUserRoleRequest("customer@example.test"));
    }
    @Test void serializesResponseFields() throws Exception {
        when(service(CityService.class).getCityById(2L)).thenReturn(new CityResponse(2L, "Bengaluru"));
        mvc.perform(get("/api/admin/cities/2").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.name").value("Bengaluru"));
    }
    @Test void serializesApplicationErrors() throws Exception {
        when(service(BookingService.class).getBooking(99L))
                .thenThrow(new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Booking not found"));
        mvc.perform(get("/api/bookings/99").with(user("test"))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/bookings/99"));
    }
    @ParameterizedTest @ValueSource(strings = {"/api/admin/cities", "/api/admin/movies", "/api/admin/theaters",
            "/api/admin/screens", "/api/admin/seats/10", "/api/admin/shows/5", "/api/admin/discount-codes",
            "/api/admin/refund-policies"})
    void customersCannotAccessAdminRoutes(String path) throws Exception {
        mvc.perform(get(path).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
    }
    @Test void customerCannotPromoteUsers() throws Exception {
        mvc.perform(patch("/api/admin/users/make-admin").with(user("customer").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"customer@example.test\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service(UserAdminService.class));
    }
    @ParameterizedTest @ValueSource(strings = {"/api/catalog/movies", "/api/bookings", "/api/seat-holds/7", "/api/admin/cities"})
    void protectedRoutesRejectAnonymousRequests(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().is4xxClientError());
    }
    @ParameterizedTest @ValueSource(strings = {"/api/admin/cities", "/api/admin/movies", "/api/admin/theaters", "/api/admin/screens", "/api/admin/seats/layout", "/api/admin/shows", "/api/admin/discount-codes", "/api/admin/refund-policies", "/api/seat-holds", "/api/bookings", "/api/auth/register", "/api/auth/login", "/api/auth/refresh"})
    void rejectsMissingRequiredRequestFields(String path) throws Exception {
        mvc.perform(post(path).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.validationErrors").isNotEmpty());
    }
    @Test void validatesNestedSeatLayout() throws Exception {
        mvc.perform(post("/api/admin/seats/layout").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"screenId\":4,\"seats\":[{\"rowLabel\":\"\",\"seatNumber\":0}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service(SeatService.class));
    }
    @Test void paymentRequiresMethod() throws Exception {
        mvc.perform(post("/api/payments/bookings/8").with(user("test")).header("Idempotency-Key","key")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service(PaymentService.class));
    }
}
