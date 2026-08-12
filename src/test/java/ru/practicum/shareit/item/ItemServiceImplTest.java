package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.InMemoryUserRepository;
import ru.practicum.shareit.user.UserDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.UserServiceImpl;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemServiceImplTest {

    private ItemService itemService;
    private UserService userService;
    private InMemoryUserRepository userRepository;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        userService = new UserServiceImpl(userRepository);
        itemService = new ItemServiceImpl(new InMemoryItemRepository(), userRepository);
        ownerId = userService.create(
                new UserDto(null, "Owner", "owner@example.com")
        ).getId();
    }

    @Test
    void createShouldAssignIdAndOwner() {
        ItemDto created = itemService.create(
                ownerId,
                new ItemDto(null, "Drill", "Impact drill", true)
        );

        assertThat(created.getId()).isPositive();
        assertThat(created.getName()).isEqualTo("Drill");
        assertThat(itemService.getAllByOwner(ownerId))
                .extracting(ItemDto::getId)
                .containsExactly(created.getId());
    }

    @Test
    void mapperShouldIncludeRequestId() {
        ItemRequest request = new ItemRequest();
        request.setId(42L);
        Item item = new Item(
                1L,
                "Drill",
                "Impact drill",
                true,
                userRepository.findById(ownerId).orElseThrow(),
                request
        );

        ItemDto itemDto = ItemMapper.toItemDto(item);

        assertThat(itemDto.getRequestId()).isEqualTo(request.getId());
    }

    @Test
    void getAllByOwnerShouldReturnEmptyForUnknownOwner() {
        Long unknownOwnerId = ownerId + 1000;

        assertThat(itemService.getAllByOwner(unknownOwnerId)).isEmpty();
    }

    @Test
    void createShouldIgnoreProvidedId() {
        ItemDto created = itemService.create(
                ownerId,
                new ItemDto(99L, "Drill", "Impact drill", true)
        );

        assertThat(created.getId())
                .isPositive()
                .isNotEqualTo(99L);
    }

    @Test
    void createShouldRejectUnknownOwner() {
        assertThatThrownBy(() -> itemService.create(
                99L,
                new ItemDto(null, "Drill", "Impact drill", true)
        )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByIdShouldAllowUnknownViewer() {
        ItemDto created = itemService.create(
                ownerId,
                new ItemDto(null, "Drill", "Impact drill", true)
        );
        Long unknownUserId = ownerId + 1000;

        ItemDto result = itemService.getById(unknownUserId, created.getId());

        assertThat(result.getId()).isEqualTo(created.getId());
        assertThat(result.getName()).isEqualTo(created.getName());
    }

    @Test
    void updateShouldChangeOnlyProvidedFields() {
        ItemDto created = itemService.create(
                ownerId,
                new ItemDto(null, "Old name", "Old description", true)
        );

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
        ItemDto created = itemService.create(
                ownerId,
                new ItemDto(null, "Drill", "Impact drill", true)
        );
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
    void concurrentUpdatesShouldNotLoseFields() throws Exception {
        ItemService concurrentService = new ItemServiceImpl(
                new CoordinatedItemRepository(),
                userRepository
        );
        ItemDto created = concurrentService.create(
                ownerId,
                new ItemDto(null, "Old name", "Old description", true)
        );
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ItemDto> nameUpdate = executor.submit(() -> {
                ready.countDown();
                start.await();
                return concurrentService.update(
                        ownerId,
                        created.getId(),
                        new ItemDto(null, "New name", null, null)
                );
            });
            Future<ItemDto> descriptionUpdate = executor.submit(() -> {
                ready.countDown();
                start.await();
                return concurrentService.update(
                        ownerId,
                        created.getId(),
                        new ItemDto(null, null, "New description", null)
                );
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            nameUpdate.get(5, TimeUnit.SECONDS);
            descriptionUpdate.get(5, TimeUnit.SECONDS);

            ItemDto result = concurrentService.getById(ownerId, created.getId());
            assertThat(result.getName()).isEqualTo("New name");
            assertThat(result.getDescription()).isEqualTo("New description");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void searchShouldIgnoreCaseAndSkipUnavailableItems() {
        itemService.create(
                ownerId,
                new ItemDto(null, "Impact drill", "For concrete", true)
        );
        itemService.create(
                ownerId,
                new ItemDto(null, "Hammer", "For a DRILL project", true)
        );
        itemService.create(
                ownerId,
                new ItemDto(null, "Drill set", "Unavailable", false)
        );

        assertThat(itemService.search(ownerId, "DRILL"))
                .extracting(ItemDto::getName)
                .containsExactly("Impact drill", "Hammer");
    }

    @Test
    void searchShouldFindAnotherUsersItem() {
        Long anotherOwnerId = userService.create(
                new UserDto(null, "Another owner", "another-owner@example.com")
        ).getId();
        ItemDto anotherUsersItem = itemService.create(
                anotherOwnerId,
                new ItemDto(null, "Concrete drill", "For renovation", true)
        );

        assertThat(itemService.search(ownerId, "drill"))
                .extracting(ItemDto::getId)
                .containsExactly(anotherUsersItem.getId());
    }

    @Test
    void searchShouldAllowUnknownUser() {
        ItemDto created = itemService.create(
                ownerId,
                new ItemDto(null, "Drill", "Impact drill", true)
        );
        Long unknownUserId = ownerId + 1000;

        assertThat(itemService.search(unknownUserId, "drill"))
                .extracting(ItemDto::getId)
                .containsExactly(created.getId());
    }

    @Test
    void searchShouldReturnEmptyListForBlankText() {
        itemService.create(
                ownerId,
                new ItemDto(null, "Drill", "Impact drill", true)
        );

        assertThat(itemService.search(ownerId, "   ")).isEmpty();
    }

    private static class CoordinatedItemRepository extends InMemoryItemRepository {

        private final CountDownLatch readers = new CountDownLatch(2);

        @Override
        public Optional<Item> findById(Long itemId) {
            Optional<Item> item = super.findById(itemId);
            readers.countDown();
            try {
                readers.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return item;
        }
    }
}
