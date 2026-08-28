package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserClient userClient;

    @Test
    void createShouldValidateAndForwardRequest() throws Exception {
        UserDto request = new UserDto(null, "Damir", "damir@example.com");
        when(userClient.create(any())).thenReturn(ResponseEntity.status(201).body(Map.of("id", 1)));

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(userClient).create(request);
    }

    @Test
    void createShouldRejectMissingRequiredFields() throws Exception {
        UserDto request = new UserDto(null, " ", null);

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userClient);
    }

    @Test
    void updateShouldAllowPartialRequest() throws Exception {
        UserDto request = new UserDto(null, "New name", null);
        when(userClient.update(3L, request)).thenReturn(ResponseEntity.ok(Map.of("id", 3)));

        mockMvc.perform(patch("/users/3")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));

        verify(userClient).update(3L, request);
    }

    @Test
    void updateShouldRejectProvidedBlankName() throws Exception {
        UserDto request = new UserDto(null, " ", null);

        mockMvc.perform(patch("/users/3")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userClient);
    }

    @Test
    void getByIdShouldForwardRequest() throws Exception {
        when(userClient.getById(4L)).thenReturn(ResponseEntity.ok(Map.of("id", 4)));

        mockMvc.perform(get("/users/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));

        verify(userClient).getById(4L);
    }

    @Test
    void getAllShouldForwardRequest() throws Exception {
        when(userClient.getAll()).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(userClient).getAll();
    }

    @Test
    void deleteShouldForwardRequest() throws Exception {
        when(userClient.delete(5L)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/users/5"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userClient).delete(5L);
    }
}
