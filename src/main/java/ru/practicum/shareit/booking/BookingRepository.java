package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    Optional<Booking> findById(Long bookingId);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByBookerIdOrderByStartDesc(Long bookerId);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(
            Long bookerId,
            LocalDateTime start,
            LocalDateTime end
    );

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByBookerIdAndEndBeforeOrderByStartDesc(Long bookerId, LocalDateTime end);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByBookerIdAndStartAfterOrderByStartDesc(Long bookerId, LocalDateTime start);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByBookerIdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByItemOwnerIdOrderByStartDesc(Long ownerId);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(
            Long ownerId,
            LocalDateTime start,
            LocalDateTime end
    );

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByItemOwnerIdAndEndBeforeOrderByStartDesc(Long ownerId, LocalDateTime end);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByItemOwnerIdAndStartAfterOrderByStartDesc(Long ownerId, LocalDateTime start);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAllByItemOwnerIdAndStatusOrderByStartDesc(Long ownerId, BookingStatus status);

    @EntityGraph(attributePaths = {"booker"})
    List<Booking> findAllByItemIdInAndStatus(Collection<Long> itemIds, BookingStatus status);

    boolean existsByBookerIdAndItemIdAndStatusAndEndBefore(
            Long bookerId,
            Long itemId,
            BookingStatus status,
            LocalDateTime end
    );
}
