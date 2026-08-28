package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingClient bookingClient;

    @Test
    void createShouldValidateAndForwardRequest() throws Exception {
        BookItemRequestDto request = validRequest();
        when(bookingClient.create(1L, request)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(bookingClient).create(1L, request);
    }

    @Test
    void createShouldRejectMissingItemId() throws Exception {
        BookItemRequestDto request = new BookItemRequestDto(
                null,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        verifyNoInteractions(bookingClient);
    }

    @Test
    void approveShouldForwardRequest() throws Exception {
        when(bookingClient.approve(1L, 2L, true)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(patch("/bookings/2")
                        .header(USER_HEADER, 1)
                        .param("approved", "true"))
                .andExpect(status().isOk());

        verify(bookingClient).approve(1L, 2L, true);
    }

    @Test
    void getByIdShouldForwardRequest() throws Exception {
        when(bookingClient.getById(1L, 2L)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(get("/bookings/2").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(bookingClient).getById(1L, 2L);
    }

    @Test
    void getByBookerShouldUseDefaults() throws Exception {
        when(bookingClient.getByBooker(1L, BookingState.ALL, 0, 10))
                .thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/bookings").header(USER_HEADER, 1))
                .andExpect(status().isOk());

        verify(bookingClient).getByBooker(1L, BookingState.ALL, 0, 10);
    }

    @Test
    void getByOwnerShouldForwardStateAndPagination() throws Exception {
        when(bookingClient.getByOwner(1L, BookingState.FUTURE, 3, 2))
                .thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/bookings/owner")
                        .header(USER_HEADER, 1)
                        .param("state", "future")
                        .param("from", "3")
                        .param("size", "2"))
                .andExpect(status().isOk());

        verify(bookingClient).getByOwner(1L, BookingState.FUTURE, 3, 2);
    }

    @Test
    void getBookingsShouldRejectUnknownState() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, 1)
                        .param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        verifyNoInteractions(bookingClient);
    }

    @Test
    void getBookingsShouldRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, 1)
                        .param("from", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        verifyNoInteractions(bookingClient);
    }

    private BookItemRequestDto validRequest() {
        return new BookItemRequestDto(
                2L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
    }
}
