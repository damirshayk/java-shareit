package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRequestTest {

    @Test
    void equalityShouldDependOnlyOnPersistedId() {
        ItemRequest first = new ItemRequest(
                1L,
                "Первая версия",
                new User(1L, "First", "first@example.com"),
                LocalDateTime.now()
        );
        ItemRequest second = new ItemRequest(
                1L,
                "Вторая версия",
                new User(2L, "Second", "second@example.com"),
                LocalDateTime.now().plusHours(1)
        );

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void transientRequestsShouldNotBeEqual() {
        ItemRequest first = new ItemRequest(null, "Запрос", null, LocalDateTime.now());
        ItemRequest second = new ItemRequest(null, "Запрос", null, first.getCreated());

        assertThat(first).isNotEqualTo(second);
    }
}
