package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {

    ItemRequestDto create(Long requestorId, ItemRequestDto requestDto);

    List<ItemRequestDto> getOwn(Long requestorId);

    List<ItemRequestDto> getAll(Long userId, int from, int size);

    ItemRequestDto getById(Long userId, Long requestId);
}
