package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Test
    void createShouldAssignIdAndReturnUser() {
        UserDto created = userService.create(new UserDto(null, "Damir", "damir@example.com"));

        assertThat(created.getId()).isPositive();
        assertThat(created.getName()).isEqualTo("Damir");
        assertThat(created.getEmail()).isEqualTo("damir@example.com");
    }

    @Test
    void createShouldIgnoreProvidedId() {
        UserDto created = userService.create(new UserDto(99L, "Damir", "damir@example.com"));

        assertThat(created.getId()).isPositive().isNotEqualTo(99L);
    }

    @Test
    void createShouldRejectDuplicateEmailIgnoringCase() {
        userService.create(new UserDto(null, "First", "same@example.com"));

        assertThatThrownBy(() ->
                userService.create(new UserDto(null, "Second", "SAME@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateShouldChangeOnlyProvidedFields() {
        UserDto created = userService.create(new UserDto(null, "Old name", "old@example.com"));

        UserDto updated = userService.update(
                created.getId(),
                new UserDto(null, "New name", null)
        );

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getEmail()).isEqualTo("old@example.com");
    }

    @Test
    void updateShouldRejectAnotherUsersEmail() {
        UserDto first = userService.create(new UserDto(null, "First", "first@example.com"));
        userService.create(new UserDto(null, "Second", "second@example.com"));

        assertThatThrownBy(() ->
                userService.update(first.getId(), new UserDto(null, null, "second@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getAllShouldReturnUsersInIdOrder() {
        UserDto first = userService.create(new UserDto(null, "First", "first@example.com"));
        UserDto second = userService.create(new UserDto(null, "Second", "second@example.com"));

        assertThat(userService.getAll())
                .extracting(UserDto::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void deleteShouldRemoveUser() {
        UserDto created = userService.create(new UserDto(null, "User", "user@example.com"));

        userService.delete(created.getId());

        assertThatThrownBy(() -> userService.getById(created.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
