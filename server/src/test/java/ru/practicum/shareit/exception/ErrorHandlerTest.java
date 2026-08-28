package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserController.class, ItemController.class, BookingController.class})
class ErrorHandlerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ItemService itemService;

    @MockBean
    private BookingService bookingService;

    @Test
    void shouldMapApplicationExceptionsToHttpStatuses() throws Exception {
        when(userService.getById(1L)).thenThrow(new NotFoundException("Not found"));
        when(userService.getById(2L)).thenThrow(new ForbiddenException("Forbidden"));
        when(userService.getById(3L)).thenThrow(new ValidationException("Invalid"));
        when(userService.getById(4L)).thenThrow(new ConflictException("Conflict"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
        mockMvc.perform(get("/users/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
        mockMvc.perform(get("/users/3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid"));
        mockMvc.perform(get("/users/4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldMapDatabaseConflict() throws Exception {
        when(userService.create(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content("{\"name\":\"User\",\"email\":\"user@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void shouldMapInvalidFrameworkArguments() throws Exception {
        mockMvc.perform(get("/items/not-a-number").header(USER_HEADER, 1))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content("not-json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/bookings/1").header(USER_HEADER, 1))
                .andExpect(status().isBadRequest());
    }
}
