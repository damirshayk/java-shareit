package ru.practicum.shareit.booking;

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

final class BookingSpecifications {

    private BookingSpecifications() {
    }

    static Specification<Booking> byBooker(Long bookerId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("booker").get("id"),
                bookerId
        );
    }

    static Specification<Booking> byOwner(Long ownerId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("item").get("owner").get("id"),
                ownerId
        );
    }

    static Specification<Booking> byState(BookingState state, LocalDateTime now) {
        return switch (state) {
            case ALL -> (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
            case CURRENT -> (root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.lessThan(root.get("start"), now),
                    criteriaBuilder.greaterThan(root.get("end"), now)
            );
            case PAST -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get("end"), now);
            case FUTURE -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThan(root.get("start"), now);
            case WAITING -> withStatus(BookingStatus.WAITING);
            case REJECTED -> withStatus(BookingStatus.REJECTED);
        };
    }

    static Specification<Booking> withDetails() {
        return (root, query, criteriaBuilder) -> {
            Fetch<Object, Object> item = root.fetch("item", JoinType.INNER);
            item.fetch("owner", JoinType.INNER);
            root.fetch("booker", JoinType.INNER);
            query.distinct(true);
            return criteriaBuilder.conjunction();
        };
    }

    private static Specification<Booking> withStatus(BookingStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }
}
