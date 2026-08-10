package ru.practicum.shareit.user;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    @Override
    public User save(User user) {
        User saved = copy(user);
        if (saved.getId() == null) {
            saved.setId(nextId.incrementAndGet());
        }
        users.put(saved.getId(), saved);
        return copy(saved);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(users.get(userId)).map(this::copy);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .map(this::copy);
    }

    @Override
    public List<User> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public boolean deleteById(Long userId) {
        return users.remove(userId) != null;
    }

    private User copy(User user) {
        return new User(user.getId(), user.getName(), user.getEmail());
    }
}
