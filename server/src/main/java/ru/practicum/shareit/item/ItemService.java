package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.CommentDto;

import java.util.List;

public interface ItemService {

    ItemDto create(Long ownerId, ItemDto itemDto);

    ItemDto update(Long userId, Long itemId, ItemDto itemDto);

    ItemDto getById(Long userId, Long itemId);

    List<ItemDto> getAllByOwner(Long ownerId, int from, int size);

    List<ItemDto> search(Long userId, String text);

    CommentDto addComment(Long userId, Long itemId, CommentDto commentDto);
}
