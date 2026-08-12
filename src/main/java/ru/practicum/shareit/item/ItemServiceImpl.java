package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long ownerId, ItemDto itemDto) {
        User owner = findUser(ownerId);
        Item saved = itemRepository.save(ItemMapper.toItem(itemDto, owner));
        return ItemMapper.toItemDto(saved);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
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

        return ItemMapper.toItemDto(current);
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        return enrichItems(List.of(findItem(itemId)), userId).getFirst();
    }

    @Override
    public List<ItemDto> getAllByOwner(Long ownerId) {
        return enrichItems(itemRepository.findAllByOwnerIdOrderByIdAsc(ownerId), ownerId);
    }

    @Override
    public List<ItemDto> search(Long userId, String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = findUser(userId);
        Item item = findItem(itemId);
        validateText(commentDto.getText(), "Текст комментария не может быть пустым");

        boolean completedBooking = bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndBefore(
                userId,
                itemId,
                BookingStatus.APPROVED,
                LocalDateTime.now()
        );
        if (!completedBooking) {
            throw new ValidationException("Оставить комментарий можно только после завершения аренды");
        }

        Comment comment = new Comment(null, commentDto.getText(), item, author, LocalDateTime.now());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private List<ItemDto> enrichItems(List<Item> items, Long viewerId) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, ItemDto> itemDtos = new LinkedHashMap<>();
        for (Item item : items) {
            itemDtos.put(item.getId(), ItemMapper.toItemDto(item));
        }

        List<Long> itemIds = new ArrayList<>(itemDtos.keySet());
        Map<Long, List<CommentDto>> commentsByItem = new HashMap<>();
        commentRepository.findAllByItemIdInOrderByCreatedAscIdAsc(itemIds).forEach(comment ->
                commentsByItem.computeIfAbsent(comment.getItem().getId(), ignored -> new ArrayList<>())
                        .add(CommentMapper.toCommentDto(comment))
        );
        itemDtos.forEach((itemId, itemDto) ->
                itemDto.setComments(commentsByItem.getOrDefault(itemId, List.of()))
        );

        List<Long> ownedItemIds = items.stream()
                .filter(item -> item.getOwner().getId().equals(viewerId))
                .map(Item::getId)
                .toList();
        if (!ownedItemIds.isEmpty()) {
            addBookingData(itemDtos, ownedItemIds);
        }

        return new ArrayList<>(itemDtos.values());
    }

    private void addBookingData(Map<Long, ItemDto> itemDtos, List<Long> itemIds) {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Booking> lastBookings = new HashMap<>();
        Map<Long, Booking> nextBookings = new HashMap<>();

        bookingRepository.findAllByItemIdInAndStatus(itemIds, BookingStatus.APPROVED)
                .forEach(booking -> {
                    Long itemId = booking.getItem().getId();
                    if (booking.getStart().isBefore(now)) {
                        lastBookings.merge(itemId, booking, (first, second) ->
                                first.getStart().isAfter(second.getStart()) ? first : second
                        );
                    } else {
                        nextBookings.merge(itemId, booking, (first, second) ->
                                first.getStart().isBefore(second.getStart()) ? first : second
                        );
                    }
                });

        itemIds.forEach(itemId -> {
            ItemDto itemDto = itemDtos.get(itemId);
            Booking lastBooking = lastBookings.get(itemId);
            Booking nextBooking = nextBookings.get(itemId);
            if (lastBooking != null) {
                itemDto.setLastBooking(BookingMapper.toShortDto(lastBooking));
            }
            if (nextBooking != null) {
                itemDto.setNextBooking(BookingMapper.toShortDto(nextBooking));
            }
        });
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
