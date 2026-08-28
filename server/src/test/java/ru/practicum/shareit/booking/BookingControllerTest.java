package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingCreateDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Test
    void shouldForwardAllBookingEndpoints() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content("{\"itemId\":2,\"start\":\"2030-01-02T10:00:00\","
                                + "\"end\":\"2030-01-03T10:00:00\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/bookings/3")
                        .header(USER_HEADER, 1)
                        .param("approved", "true"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/3").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, 1)
                        .param("state", "PAST")
                        .param("from", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/owner")
                        .header(USER_HEADER, 1)
                        .param("state", "FUTURE")
                        .param("from", "4")
                        .param("size", "6"))
                .andExpect(status().isOk());

        verify(bookingService).create(
                org.mockito.ArgumentMatchers.eq(1L),
                any(BookingCreateDto.class)
        );
        verify(bookingService).approve(1L, 3L, true);
        verify(bookingService).getById(1L, 3L);
        verify(bookingService).getByBooker(1L, "PAST", 2, 5);
        verify(bookingService).getByOwner(1L, "FUTURE", 4, 6);
    }
}
