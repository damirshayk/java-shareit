package ru.practicum.shareit.booking;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    Optional<Booking> findById(Long bookingId);

    @Override
    @EntityGraph(attributePaths = {"item", "item.owner", "item.request", "booker"})
    List<Booking> findAll(Specification<Booking> specification, Sort sort);

    @EntityGraph(attributePaths = {"booker"})
    List<Booking> findAllByItemIdInAndStatus(Collection<Long> itemIds, BookingStatus status);

    boolean existsByBookerIdAndItemIdAndStatusAndEndBefore(
            Long bookerId,
            Long itemId,
            BookingStatus status,
            LocalDateTime end
    );
}
