package ru.practicum.shareit.item;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryItemRepository implements ItemRepository {

    private final Map<Long, Item> items = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    @Override
    public Item save(Item item) {
        Item saved = copy(item);
        if (saved.getId() == null) {
            saved.setId(nextId.incrementAndGet());
        }
        items.put(saved.getId(), saved);
        return copy(saved);
    }

    @Override
    public Optional<Item> findById(Long itemId) {
        return Optional.ofNullable(items.get(itemId)).map(this::copy);
    }

    @Override
    public List<Item> findAllByOwnerId(Long ownerId) {
        return items.values().stream()
                .filter(item -> item.getOwner().getId().equals(ownerId))
                .sorted(Comparator.comparing(Item::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public List<Item> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String query = text.toLowerCase(Locale.ROOT);
        return items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item -> item.getName().toLowerCase(Locale.ROOT).contains(query)
                        || item.getDescription().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Item::getId))
                .map(this::copy)
                .toList();
    }

    private Item copy(Item item) {
        User owner = item.getOwner();
        User ownerCopy = new User(owner.getId(), owner.getName(), owner.getEmail());
        return new Item(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getAvailable(),
                ownerCopy,
                item.getRequest()
        );
    }
}
