package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemDto create(Long ownerId, ItemDto itemDto) {
        User owner = findUser(ownerId);
        Item saved = itemRepository.save(ItemMapper.toItem(itemDto, owner));
        return ItemMapper.toItemDto(saved);
    }

    @Override
    public synchronized ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        findUser(userId);
        Item current = findItem(itemId);
        if (!current.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Вещь с id " + itemId + " не принадлежит пользователю");
        }

        if (itemDto.getName() != null) {
            validateText(itemDto.getName(), "Название вещи не может быть пустым");
            current.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            validateText(itemDto.getDescription(), "Описание вещи не может быть пустым");
            current.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            current.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toItemDto(itemRepository.save(current));
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        return ItemMapper.toItemDto(findItem(itemId));
    }

    @Override
    public List<ItemDto> getAllByOwner(Long ownerId) {
        return itemRepository.findAllByOwnerId(ownerId).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(Long userId, String text) {
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
    }

    private Item findItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Вещь с id " + itemId + " не найдена"
                ));
    }

    private void validateText(String value, String message) {
        if (value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
