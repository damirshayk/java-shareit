package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookingSpecificationsTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void specificationsShouldCombineUserRoleAndBookingState() {
        User owner = userRepository.save(new User(null, "Owner", "owner-spec@example.com"));
        User booker = userRepository.save(new User(null, "Booker", "booker-spec@example.com"));
        Item item = itemRepository.save(new Item(null, "Drill", "Impact drill", true, owner, null));
        LocalDateTime now = LocalDateTime.now();

        Booking current = bookingRepository.save(new Booking(
                null,
                now.minusHours(1),
                now.plusHours(1),
                item,
                booker,
                BookingStatus.APPROVED
        ));
        Booking waiting = bookingRepository.save(new Booking(
                null,
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                BookingStatus.WAITING
        ));
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "start");

        assertThat(bookingRepository.findAll(
                BookingSpecifications.byOwner(owner.getId())
                        .and(BookingSpecifications.byState(BookingState.CURRENT, now)),
                newestFirst
        )).extracting(Booking::getId).containsExactly(current.getId());

        assertThat(bookingRepository.findAll(
                BookingSpecifications.byBooker(booker.getId())
                        .and(BookingSpecifications.byState(BookingState.WAITING, now)),
                newestFirst
        )).extracting(Booking::getId).containsExactly(waiting.getId());
    }
}
