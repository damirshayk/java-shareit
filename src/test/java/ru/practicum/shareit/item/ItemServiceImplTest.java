package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserDto;
import ru.practicum.shareit.user.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemServiceImplTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    private Long ownerId;

    @BeforeEach
    void setUp() {
        ownerId = userService.create(
                new UserDto(null, "Owner", "owner@example.com")
        ).getId();
    }

    @Test
    void createShouldAssignIdAndOwner() {
        ItemDto created = createItem("Drill", "Impact drill", true);

        assertThat(created.getId()).isPositive();
        assertThat(itemService.getAllByOwner(ownerId))
                .extracting(ItemDto::getId)
                .containsExactly(created.getId());
    }

    @Test
    void createShouldIgnoreProvidedId() {
        ItemDto created = itemService.create(
                ownerId,
                new ItemDto(99L, "Drill", "Impact drill", true)
        );

        assertThat(created.getId()).isPositive().isNotEqualTo(99L);
    }

    @Test
    void createShouldRejectUnknownOwner() {
        assertThatThrownBy(() -> itemService.create(
                Long.MAX_VALUE,
                new ItemDto(null, "Drill", "Impact drill", true)
        )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByIdShouldAllowUnknownViewer() {
        ItemDto created = createItem("Drill", "Impact drill", true);

        ItemDto result = itemService.getById(Long.MAX_VALUE, created.getId());

        assertThat(result.getId()).isEqualTo(created.getId());
        assertThat(result.getName()).isEqualTo(created.getName());
    }

    @Test
    void updateShouldChangeOnlyProvidedFields() {
        ItemDto created = createItem("Old name", "Old description", true);

        ItemDto updated = itemService.update(
                ownerId,
                created.getId(),
                new ItemDto(null, "New name", null, null)
        );

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getDescription()).isEqualTo("Old description");
        assertThat(updated.getAvailable()).isTrue();
    }

    @Test
    void updateShouldRejectUserWhoIsNotOwner() {
        ItemDto created = createItem("Drill", "Impact drill", true);
        Long anotherUserId = userService.create(
                new UserDto(null, "Another", "another@example.com")
        ).getId();

        assertThatThrownBy(() -> itemService.update(
                anotherUserId,
                created.getId(),
                new ItemDto(null, "Changed", null, null)
        )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchShouldIgnoreCaseAndSkipUnavailableItems() {
        createItem("Impact drill", "For concrete", true);
        createItem("Hammer", "For a DRILL project", true);
        createItem("Drill set", "Unavailable", false);

        assertThat(itemService.search(ownerId, "DRILL"))
                .extracting(ItemDto::getName)
                .containsExactly("Impact drill", "Hammer");
    }

    @Test
    void searchShouldReturnEmptyListForBlankText() {
        createItem("Drill", "Impact drill", true);

        assertThat(itemService.search(ownerId, "   ")).isEmpty();
    }

    private ItemDto createItem(String name, String description, boolean available) {
        return itemService.create(
                ownerId,
                new ItemDto(null, name, description, available)
        );
    }
}
