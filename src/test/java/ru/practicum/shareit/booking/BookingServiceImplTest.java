package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserDto;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BookingServiceImplTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ownerShouldApproveBookingCreatedByAnotherUser() {
        UserDto owner = createUser("Owner", "owner@example.com");
        UserDto booker = createUser("Booker", "booker@example.com");
        ItemDto item = createItem(owner.getId(), true);
        BookingCreateDto request = new BookingCreateDto(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        BookingDto created = bookingService.create(booker.getId(), request);
        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);

        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(approved.getItem().id()).isEqualTo(item.getId());
        assertThat(approved.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void bookingResponseShouldContainOnlyItemIdAndName() {
        UserDto owner = createUser("Owner", "owner@example.com");
        UserDto booker = createUser("Booker", "booker@example.com");
        ItemDto item = createItem(owner.getId(), true);

        BookingDto booking = bookingService.create(
                booker.getId(),
                new BookingCreateDto(
                        item.getId(),
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2)
                )
        );
        JsonNode itemJson = objectMapper.valueToTree(booking.getItem());

        assertThat(itemJson.has("id")).isTrue();
        assertThat(itemJson.has("name")).isTrue();
        assertThat(itemJson.size()).isEqualTo(2);
    }

    @Test
    void ownerShouldNotBookOwnItem() {
        UserDto owner = createUser("Owner", "owner@example.com");
        ItemDto item = createItem(owner.getId(), true);
        BookingCreateDto request = new BookingCreateDto(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        assertThatThrownBy(() -> bookingService.create(owner.getId(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void commentShouldRequireCompletedApprovedBooking() {
        UserDto owner = createUser("Owner", "owner@example.com");
        UserDto booker = createUser("Booker", "booker@example.com");
        ItemDto item = createItem(owner.getId(), true);

        assertThatThrownBy(() -> itemService.addComment(
                booker.getId(),
                item.getId(),
                new CommentDto(null, "Good tool", null, null)
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void completedApprovedBookingShouldAllowComment() {
        UserDto owner = createUser("Owner", "owner@example.com");
        UserDto booker = createUser("Booker", "booker@example.com");
        ItemDto itemDto = createItem(owner.getId(), true);
        Item item = itemRepository.findById(itemDto.getId()).orElseThrow();
        User bookerEntity = userRepository.findById(booker.getId()).orElseThrow();
        bookingRepository.save(new Booking(
                null,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                item,
                bookerEntity,
                BookingStatus.APPROVED
        ));

        CommentDto comment = itemService.addComment(
                booker.getId(),
                item.getId(),
                new CommentDto(null, "Good tool", null, null)
        );
        ItemDto result = itemService.getById(booker.getId(), item.getId());

        assertThat(comment.getId()).isPositive();
        assertThat(comment.getAuthorName()).isEqualTo(booker.getName());
        assertThat(result.getComments())
                .extracting(CommentDto::getId)
                .containsExactly(comment.getId());
    }

    @Test
    void ownerShouldSeeLastAndNextApprovedBookings() {
        UserDto owner = createUser("Owner", "owner@example.com");
        UserDto booker = createUser("Booker", "booker@example.com");
        ItemDto itemDto = createItem(owner.getId(), true);
        Item item = itemRepository.findById(itemDto.getId()).orElseThrow();
        User bookerEntity = userRepository.findById(booker.getId()).orElseThrow();
        Booking last = bookingRepository.save(new Booking(
                null,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                item,
                bookerEntity,
                BookingStatus.APPROVED
        ));
        Booking next = bookingRepository.save(new Booking(
                null,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                item,
                bookerEntity,
                BookingStatus.APPROVED
        ));

        ItemDto result = itemService.getById(owner.getId(), item.getId());

        assertThat(result.getLastBooking().id()).isEqualTo(last.getId());
        assertThat(result.getNextBooking().id()).isEqualTo(next.getId());
    }

    @Test
    void bookingListsShouldFilterByStateAndSortByStartDescending() {
        UserDto owner = createUser("Owner", "owner@example.com");
        UserDto booker = createUser("Booker", "booker@example.com");
        ItemDto item = createItem(owner.getId(), true);
        BookingDto first = bookingService.create(
                booker.getId(),
                new BookingCreateDto(
                        item.getId(),
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2)
                )
        );
        BookingDto second = bookingService.create(
                booker.getId(),
                new BookingCreateDto(
                        item.getId(),
                        LocalDateTime.now().plusDays(3),
                        LocalDateTime.now().plusDays(4)
                )
        );
        bookingService.approve(owner.getId(), first.getId(), false);

        assertThat(bookingService.getByBooker(booker.getId(), "ALL"))
                .extracting(BookingDto::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(bookingService.getByBooker(booker.getId(), "REJECTED"))
                .extracting(BookingDto::getId)
                .containsExactly(first.getId());
        assertThatThrownBy(() -> bookingService.getByBooker(booker.getId(), "UNKNOWN"))
                .isInstanceOf(ValidationException.class);
    }

    private UserDto createUser(String name, String email) {
        return userService.create(new UserDto(null, name, email));
    }

    private ItemDto createItem(Long ownerId, boolean available) {
        return itemService.create(
                ownerId,
                new ItemDto(null, "Drill", "Impact drill", available)
        );
    }
}
