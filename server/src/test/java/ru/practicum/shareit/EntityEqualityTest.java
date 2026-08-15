package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EntityEqualityTest {

    @Test
    void userEqualityShouldDependOnlyOnPersistedId() {
        User first = new User(1L, "First", "first@example.com");
        User second = new User(1L, "Second", "second@example.com");

        assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(null).isNotEqualTo(new User(null, "First", "first@example.com"));
        assertThat(first).isNotEqualTo("not a user");
        assertThat(first.toString()).contains("id=1", "name=First", "email=first@example.com");
    }

    @Test
    void itemEqualityShouldDependOnlyOnPersistedId() {
        User owner = new User(1L, "Owner", "owner@example.com");
        Item first = new Item(2L, "Drill", "Tool", true, owner, null);
        Item second = new Item(2L, "Hammer", "Tool", false, owner, null);

        assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(null).isNotEqualTo(new Item(null, "Drill", "Tool", true, owner, null));
        assertThat(first).isNotEqualTo(owner);
        assertThat(first.toString()).contains("id=2", "name=Drill").doesNotContain("owner=", "request=");
    }

    @Test
    void commentEqualityShouldDependOnlyOnPersistedId() {
        LocalDateTime created = LocalDateTime.of(2030, 1, 1, 10, 0);
        Comment first = new Comment(3L, "Good", null, null, created);
        Comment second = new Comment(3L, "Updated", null, null, created.plusHours(1));

        assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(null).isNotEqualTo(new Comment(null, "Good", null, null, created));
        assertThat(first).isNotEqualTo("not a comment");
        assertThat(first.toString()).contains("id=3", "text=Good").doesNotContain("item=", "author=");
    }

    @Test
    void bookingEqualityShouldDependOnlyOnPersistedId() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        Booking first = new Booking(4L, start, start.plusDays(1), null, null, BookingStatus.WAITING);
        Booking second = new Booking(4L, start.plusDays(2), start.plusDays(3), null, null, BookingStatus.APPROVED);

        assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(null)
                .isNotEqualTo(new Booking(null, start, start.plusDays(1), null, null, BookingStatus.WAITING));
        assertThat(first).isNotEqualTo("not a booking");
        assertThat(first.toString()).contains("id=4", "status=WAITING").doesNotContain("item=", "booker=");
    }
}
