package ru.practicum.shareit.user;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceImplTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(new InMemoryUserRepository());
    }

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

        assertThat(created.getId())
                .isPositive()
                .isNotEqualTo(99L);
    }

    @Test
    void createShouldRejectDuplicateEmail() {
        userService.create(new UserDto(null, "First", "same@example.com"));

        assertThatThrownBy(() ->
                userService.create(new UserDto(null, "Second", "same@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void concurrentCreateShouldKeepEmailUnique() throws Exception {
        int requestCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> results = IntStream.range(0, requestCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        try {
                            userService.create(new UserDto(
                                    null,
                                    "User " + index,
                                    "same@example.com"
                            ));
                            return true;
                        } catch (ConflictException exception) {
                            return false;
                        }
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long successfulRequests = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    successfulRequests++;
                }
            }

            assertThat(successfulRequests).isEqualTo(1);
            assertThat(userService.getAll())
                    .singleElement()
                    .extracting(UserDto::getEmail)
                    .isEqualTo("same@example.com");
        } finally {
            executor.shutdownNow();
        }
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
    void updateShouldUseTheSameEmailRulesAsCreate() {
        UserDto created = userService.create(new UserDto(null, "User", "old@domain"));

        UserDto updated = userService.update(
                created.getId(),
                new UserDto(null, null, "new@domain")
        );

        assertThat(updated.getEmail()).isEqualTo("new@domain");
    }

    @Test
    void createAndUpdateShouldRejectTheSameInvalidEmail() {
        UserDto invalidUser = new UserDto(null, "User", "a@b..com");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(invalidUser)).isNotEmpty();
        }

        UserDto created = userService.create(new UserDto(null, "User", "old@example.com"));

        assertThatThrownBy(() -> userService.update(
                created.getId(),
                new UserDto(null, null, "a@b..com")
        )).isInstanceOf(ValidationException.class);
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
    void getByIdShouldRejectUnknownUser() {
        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteShouldRemoveUser() {
        UserDto created = userService.create(new UserDto(null, "User", "user@example.com"));

        userService.delete(created.getId());

        assertThatThrownBy(() -> userService.getById(created.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteDuringUpdateShouldNotRestoreUser() throws Exception {
        CoordinatedUserRepository repository = new CoordinatedUserRepository();
        UserService concurrentService = new UserServiceImpl(repository);
        UserDto created = concurrentService.create(
                new UserDto(null, "Old name", "user@example.com")
        );
        repository.coordinateNextFind();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<UserDto> update = executor.submit(() -> concurrentService.update(
                    created.getId(),
                    new UserDto(null, "New name", null)
            ));
            assertThat(repository.awaitUserRead()).isTrue();

            CountDownLatch deleteStarted = new CountDownLatch(1);
            Future<?> delete = executor.submit(() -> {
                deleteStarted.countDown();
                concurrentService.delete(created.getId());
                return null;
            });
            assertThat(deleteStarted.await(5, TimeUnit.SECONDS)).isTrue();
            repository.awaitDeleteAttempt();
            repository.continueUpdate();

            update.get(5, TimeUnit.SECONDS);
            delete.get(5, TimeUnit.SECONDS);

            assertThatThrownBy(() -> concurrentService.getById(created.getId()))
                    .isInstanceOf(NotFoundException.class);
        } finally {
            repository.continueUpdate();
            executor.shutdownNow();
        }
    }

    private static class CoordinatedUserRepository extends InMemoryUserRepository {

        private final CountDownLatch userRead = new CountDownLatch(1);
        private final CountDownLatch allowUpdate = new CountDownLatch(1);
        private final CountDownLatch deleteAttempt = new CountDownLatch(1);
        private boolean coordinateFind;

        void coordinateNextFind() {
            coordinateFind = true;
        }

        boolean awaitUserRead() throws InterruptedException {
            return userRead.await(5, TimeUnit.SECONDS);
        }

        void awaitDeleteAttempt() throws InterruptedException {
            deleteAttempt.await(1, TimeUnit.SECONDS);
        }

        void continueUpdate() {
            allowUpdate.countDown();
        }

        @Override
        public Optional<User> findById(Long userId) {
            Optional<User> user = super.findById(userId);
            if (coordinateFind) {
                coordinateFind = false;
                userRead.countDown();
                try {
                    allowUpdate.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
            return user;
        }

        @Override
        public boolean deleteById(Long userId) {
            deleteAttempt.countDown();
            return super.deleteById(userId);
        }
    }
}
