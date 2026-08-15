package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static Item toItem(ItemDto itemDto, User owner, ItemRequest request) {
        return new Item(
                null,
                itemDto.getName(),
                itemDto.getDescription(),
                itemDto.getAvailable(),
                owner,
                request
        );
    }

    public static ItemDto toItemDto(Item item) {
        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getAvailable(),
                item.getRequest() == null ? null : item.getRequest().getId()
        );
    }
}
