package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestClient itemRequestClient;

    @Test
    void createShouldValidateAndForwardRequest() throws Exception {
        ItemRequestDto request = new ItemRequestDto(null, "Нужна дрель", null, null);
        when(itemRequestClient.create(1L, request)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(itemRequestClient).create(1L, request);
    }

    @Test
    void createShouldRejectBlankDescription() throws Exception {
        ItemRequestDto request = new ItemRequestDto(null, " ", null, null);

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1)
                .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        verifyNoInteractions(itemRequestClient);
    }

    @Test
    void getOwnShouldForwardRequest() throws Exception {
        when(itemRequestClient.getOwn(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));

        mockMvc.perform(get("/requests").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));

        verify(itemRequestClient).getOwn(1L);
    }

    @Test
    void getAllShouldUseDefaults() throws Exception {
        when(itemRequestClient.getAll(1L, 0, 10)).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/requests/all").header(USER_HEADER, 1))
                .andExpect(status().isOk());

        verify(itemRequestClient).getAll(1L, 0, 10);
    }

    @Test
    void getAllShouldRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header(USER_HEADER, 1)
                        .param("from", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        verifyNoInteractions(itemRequestClient);
    }

    @Test
    void getByIdShouldForwardRequest() throws Exception {
        when(itemRequestClient.getById(1L, 2L)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(get("/requests/2").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(itemRequestClient).getById(1L, 2L);
    }
}
