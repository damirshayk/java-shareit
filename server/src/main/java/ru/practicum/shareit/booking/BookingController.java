package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    private final BookingService bookingService;

    @PostMapping
    public BookingDto create(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestBody BookingCreateDto bookingDto
    ) {
        return bookingService.create(userId, bookingDto);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approve(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long bookingId,
            @RequestParam boolean approved
    ) {
        return bookingService.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getById(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long bookingId
    ) {
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping
    public List<BookingDto> getByBooker(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bookingService.getByBooker(userId, state, from, size);
    }

    @GetMapping("/owner")
    public List<BookingDto> getByOwner(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bookingService.getByOwner(userId, state, from, size);
    }
}
