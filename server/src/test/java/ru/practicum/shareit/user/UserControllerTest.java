package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void shouldForwardAllUserEndpoints() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content("{\"name\":\"User\",\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/users/1")
                        .contentType("application/json")
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).create(any(UserDto.class));
        verify(userService).update(org.mockito.ArgumentMatchers.eq(1L), any(UserDto.class));
        verify(userService).getById(1L);
        verify(userService).getAll();
        verify(userService).delete(1L);
    }
}
