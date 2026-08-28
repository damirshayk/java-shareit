package ru.practicum.shareit.request.dto;

public record ItemRequestItemDto(
        Long id,
        String name,
        Long ownerId
) {
}
