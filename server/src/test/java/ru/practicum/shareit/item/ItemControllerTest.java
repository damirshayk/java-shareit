package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Test
    void shouldForwardAllItemEndpoints() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content("{\"name\":\"Drill\",\"description\":\"Tool\",\"available\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/items/2")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/2").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items")
                        .header(USER_HEADER, 1)
                        .param("from", "3")
                        .param("size", "4"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/search")
                        .header(USER_HEADER, 1)
                        .param("text", "drill"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/items/2/comment")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content("{\"text\":\"Good\"}"))
                .andExpect(status().isOk());

        verify(itemService).create(org.mockito.ArgumentMatchers.eq(1L), any(ItemDto.class));
        verify(itemService).update(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2L),
                any(ItemDto.class)
        );
        verify(itemService).getById(1L, 2L);
        verify(itemService).getAllByOwner(1L, 3, 4);
        verify(itemService).search(1L, "drill");
        verify(itemService).addComment(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2L),
                any(CommentDto.class)
        );
    }

    @Test
    void shouldRejectMissingUserHeader() throws Exception {
        mockMvc.perform(get("/items/2"))
                .andExpect(status().isBadRequest());
    }
}
