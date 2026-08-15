package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRequestService itemRequestService;

    @Test
    void shouldForwardAllRequestEndpoints() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1)
                        .contentType("application/json")
                        .content("{\"description\":\"Need a drill\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/requests").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/requests/all")
                        .header(USER_HEADER, 1)
                        .param("from", "2")
                        .param("size", "3"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/requests/4").header(USER_HEADER, 1))
                .andExpect(status().isOk());

        verify(itemRequestService).create(
                org.mockito.ArgumentMatchers.eq(1L),
                any(ItemRequestDto.class)
        );
        verify(itemRequestService).getOwn(1L);
        verify(itemRequestService).getAll(1L, 2, 3);
        verify(itemRequestService).getById(1L, 4L);
    }
}
