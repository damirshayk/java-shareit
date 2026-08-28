package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

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

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemClient itemClient;

    @Test
    void createShouldValidateAndForwardRequest() throws Exception {
        ItemDto request = new ItemDto(null, "Дрель", "Аккумуляторная", true, null);
        when(itemClient.create(1L, request)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(itemClient).create(1L, request);
    }

    @Test
    void createShouldRejectMissingRequiredFields() throws Exception {
        ItemDto request = new ItemDto(null, " ", null, null, null);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemClient);
    }

    @Test
    void updateShouldAllowPartialRequest() throws Exception {
        ItemDto request = new ItemDto(null, "Новая дрель", null, null, null);
        when(itemClient.update(1L, 2L, request)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(patch("/items/2")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(itemClient).update(1L, 2L, request);
    }

    @Test
    void updateShouldRejectProvidedBlankDescription() throws Exception {
        ItemDto request = new ItemDto(null, null, " ", null, null);

        mockMvc.perform(patch("/items/2")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemClient);
    }

    @Test
    void getByIdShouldForwardRequest() throws Exception {
        when(itemClient.getById(1L, 2L)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mockMvc.perform(get("/items/2").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(itemClient).getById(1L, 2L);
    }

    @Test
    void getAllShouldForwardPagination() throws Exception {
        when(itemClient.getAll(1L, 3, 2)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));

        mockMvc.perform(get("/items")
                        .header(USER_HEADER, 1)
                        .param("from", "3")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));

        verify(itemClient).getAll(1L, 3, 2);
    }

    @Test
    void getAllShouldUseDefaultPagination() throws Exception {
        when(itemClient.getAll(1L, 0, 10)).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/items").header(USER_HEADER, 1))
                .andExpect(status().isOk());

        verify(itemClient).getAll(1L, 0, 10);
    }

    @Test
    void getAllShouldRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/items")
                        .header(USER_HEADER, 1)
                        .param("from", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemClient);
    }

    @Test
    void searchShouldForwardText() throws Exception {
        when(itemClient.search(1L, "дрель")).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/items/search")
                        .header(USER_HEADER, 1)
                        .param("text", "дрель"))
                .andExpect(status().isOk());

        verify(itemClient).search(1L, "дрель");
    }

    @Test
    void addCommentShouldValidateAndForwardRequest() throws Exception {
        CommentDto request = new CommentDto("Отличная вещь");
        when(itemClient.addComment(1L, 2L, request)).thenReturn(ResponseEntity.ok(Map.of("id", 3)));

        mockMvc.perform(post("/items/2/comment")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));

        verify(itemClient).addComment(1L, 2L, request);
    }

    @Test
    void addCommentShouldRejectBlankText() throws Exception {
        CommentDto request = new CommentDto(" ");

        mockMvc.perform(post("/items/2/comment")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemClient);
    }
}
