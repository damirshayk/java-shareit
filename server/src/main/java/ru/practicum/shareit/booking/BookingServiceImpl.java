package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.OffsetPageRequest;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingDto create(Long bookerId, BookingCreateDto bookingDto) {
        User booker = findUser(bookerId);
        Item item = findItem(bookingDto.getItemId());
        validateBookingDates(bookingDto);

        if (item.getOwner().getId().equals(bookerId)) {
            throw new NotFoundException("Владелец не может бронировать собственную вещь");
        }
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        Booking booking = new Booking(
                null,
                bookingDto.getStart(),
                bookingDto.getEnd(),
                item,
                booker,
                BookingStatus.WAITING
        );
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, boolean approved) {
        Booking booking = findBooking(bookingId);
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException(
                    "Бронирование с id " + bookingId + " недоступно пользователю"
            );
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже рассмотрено");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = findBooking(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Бронирование с id " + bookingId + " недоступно пользователю");
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getByBooker(Long userId, String state, int from, int size) {
        ensureUserExists(userId);
        BookingState bookingState = BookingState.from(state);
        return findByUserAndState(
                BookingSpecifications.byBooker(userId),
                bookingState,
                from,
                size
        ).stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    @Override
    public List<BookingDto> getByOwner(Long userId, String state, int from, int size) {
        ensureUserExists(userId);
        BookingState bookingState = BookingState.from(state);
        return findByUserAndState(
                BookingSpecifications.byOwner(userId),
                bookingState,
                from,
                size
        ).stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private List<Booking> findByUserAndState(
            Specification<Booking> userSpecification,
            BookingState state,
            int from,
            int size
    ) {
        LocalDateTime now = LocalDateTime.now();
        Specification<Booking> specification = userSpecification
                .and(BookingSpecifications.byState(state, now))
                .and(BookingSpecifications.withDetails());
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "start")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable page = new OffsetPageRequest(from, size, newestFirst);
        ScrollPosition position = page.getOffset() == 0
                ? ScrollPosition.offset()
                : ScrollPosition.offset(page.getOffset() - 1);
        return bookingRepository.findBy(specification, query -> query
                .sortBy(page.getSort())
                .limit(page.getPageSize())
                .scroll(position)
                .getContent()
        );
    }

    private void validateBookingDates(BookingCreateDto bookingDto) {
        LocalDateTime start = bookingDto.getStart();
        LocalDateTime end = bookingDto.getEnd();
        if (start == null || end == null) {
            throw new ValidationException("Даты бронирования должны быть указаны");
        }
        if (!start.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Дата начала бронирования должна быть в будущем");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("Дата окончания должна быть позже даты начала");
        }
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(
                        "Бронирование с id " + bookingId + " не найдено"
                ));
    }

    private Item findItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }
}
