package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestItemDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long requestorId, ItemRequestDto requestDto) {
        User requestor = findUser(requestorId);
        ItemRequest saved = itemRequestRepository.save(
                ItemRequestMapper.toItemRequest(requestDto, requestor)
        );
        return ItemRequestMapper.toItemRequestDto(saved, List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(Long requestorId) {
        ensureUserExists(requestorId);
        return addResponseItems(
                itemRequestRepository.findAllByRequestorIdOrderByCreatedDescIdDesc(requestorId)
        );
    }

    @Override
    public List<ItemRequestDto> getAll(Long userId, int from, int size) {
        ensureUserExists(userId);
        return addResponseItems(itemRequestRepository.findAllByOtherUsers(userId, from, size));
    }

    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        ensureUserExists(userId);
        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> requestNotFound(requestId));
        return addResponseItems(List.of(request)).getFirst();
    }

    private List<ItemRequestDto> addResponseItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .toList();
        Map<Long, List<ItemRequestItemDto>> itemsByRequest = new HashMap<>();
        for (Item item : itemRepository.findAllByRequestIdInOrderByIdAsc(requestIds)) {
            itemsByRequest.computeIfAbsent(item.getRequest().getId(), ignored -> new ArrayList<>())
                    .add(new ItemRequestItemDto(
                            item.getId(),
                            item.getName(),
                            item.getOwner().getId()
                    ));
        }

        return requests.stream()
                .map(request -> ItemRequestMapper.toItemRequestDto(
                        request,
                        itemsByRequest.getOrDefault(request.getId(), List.of())
                ))
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> userNotFound(userId));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw userNotFound(userId);
        }
    }

    private NotFoundException userNotFound(Long userId) {
        return new NotFoundException("Пользователь с id " + userId + " не найден");
    }

    private NotFoundException requestNotFound(Long requestId) {
        return new NotFoundException("Запрос вещи с id " + requestId + " не найден");
    }
}
