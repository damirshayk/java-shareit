package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserDto;
import ru.practicum.shareit.user.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemRequestServiceImplTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long requestorId;
    private Long responderId;

    @BeforeEach
    void setUp() {
        requestorId = createUser("Requestor", "requestor@example.com");
        responderId = createUser("Responder", "responder@example.com");
    }

    @Test
    void createShouldAssignIdAndCreationTime() {
        ItemRequestDto created = itemRequestService.create(
                requestorId,
                new ItemRequestDto(null, "Нужна дрель", null, null)
        );

        assertThat(created.getId()).isPositive();
        assertThat(created.getCreated()).isNotNull();
        assertThat(created.getItems()).isEmpty();
    }

    @Test
    void createShouldRejectUnknownRequestor() {
        assertThatThrownBy(() -> itemRequestService.create(
                Long.MAX_VALUE,
                new ItemRequestDto(null, "Нужна дрель", null, null)
        )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwnShouldReturnRequestsWithResponseItemsNewestFirst() {
        ItemRequestDto first = createRequest("Нужна дрель");
        ItemRequestDto second = createRequest("Нужен молоток");
        ItemDto responseItem = itemService.create(
                responderId,
                new ItemDto(null, "Дрель", "Аккумуляторная", true, first.getId())
        );

        assertThat(itemRequestService.getOwn(requestorId))
                .extracting(ItemRequestDto::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(itemRequestService.getById(requestorId, first.getId()).getItems())
                .extracting(item -> item.id())
                .containsExactly(responseItem.getId());

        JsonNode responseJson = objectMapper.valueToTree(
                itemRequestService.getById(requestorId, first.getId())
        );
        assertThat(responseJson.path("items").get(0).path("ownerId").asLong())
                .isEqualTo(responderId);
    }

    @Test
    void getAllShouldExcludeOwnRequestsAndApplyOffsetPagination() {
        ItemRequestDto first = createRequest("Первый запрос");
        ItemRequestDto second = createRequest("Второй запрос");
        ItemRequestDto third = createRequest("Третий запрос");
        itemRequestService.create(
                responderId,
                new ItemRequestDto(null, "Свой запрос второго пользователя", null, null)
        );

        assertThat(itemRequestService.getAll(responderId, 1, 1))
                .extracting(ItemRequestDto::getId)
                .containsExactly(second.getId());
        assertThat(itemRequestService.getAll(requestorId, 0, 10))
                .extracting(ItemRequestDto::getId)
                .doesNotContain(first.getId(), second.getId(), third.getId());
    }

    @Test
    void getByIdShouldRejectUnknownRequest() {
        assertThatThrownBy(() -> itemRequestService.getById(requestorId, Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }

    private ItemRequestDto createRequest(String description) {
        return itemRequestService.create(
                requestorId,
                new ItemRequestDto(null, description, null, null)
        );
    }

    private Long createUser(String name, String email) {
        return userService.create(new UserDto(null, name, email)).getId();
    }
}
